package com.logabit.pipeforce.cli;

import com.logabit.pipeforce.cli.command.HelpCliCommand;
import com.logabit.pipeforce.cli.command.ICliCommand;
import com.logabit.pipeforce.cli.command.SetupCliCommand;
import com.logabit.pipeforce.common.exception.UncheckedClassNotFoundException;
import com.logabit.pipeforce.common.util.DateTimeUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Main entry point into the CLI.
 *
 * @author sniederm
 * @since 6.0
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) throws Exception {

        CliContext ctx = new CliContext(args);
        try {
            int exitCode = 0;
            ctx.getConfigService().loadConfiguration();

            // The very first arg is the command name -> Lookup a class with this name: setup -> SetupCallable

            if (args.length == 0) {
                System.out.println("USAGE: pi <COMMAND> <SWITCHES>");
                HelpCliCommand cmd = (HelpCliCommand) ctx.createCommandInstance("help");
                cmd.call(CommandArgs.EMPTY);
                return;
            }

            String cmdName = args[0];

            try {

                checkForUpdate(ctx);

                if (!ctx.getConfigService().isConfigExists() && (!cmdName.equals("setup"))) {
                    SetupCliCommand setupCliCommand = (SetupCliCommand) ctx.createCommandInstance("setup");
                    setupCliCommand.call(CommandArgs.EMPTY);
                }

                ctx.callCommand();
                LOG.info("Command success: pi " + StringUtil.concat(" ", args));

            } catch (UncheckedClassNotFoundException e) {

                System.out.println("PIPEFORCE CLI command [" + cmdName + "] not found.");
                ICliCommand cmd = ctx.createCommandInstance("help");
                try {
                    cmd.call(CommandArgs.EMPTY);
                } catch (Exception exception) {
                    throw new RuntimeException("Could not execute help command: " + e.getMessage(), e);
                }
                exitCode = -1;

            } catch (HttpServerErrorException.InternalServerError e) {

                String message = e.getResponseBodyAsString();

                if (JsonUtil.isJsonString(message)) {
                    System.out.println(JsonUtil.jsonStringToJsonNode(message).toPrettyString());
                } else {
                    ctx.getOutputService().printResult(e);
                }

                LOG.error("Command caused error: pi " + StringUtil.concat(" ", args), e);
                exitCode = -1;

            } catch (Exception e) {

                LOG.error("Command caused error: pi " + StringUtil.concat(" ", args), e);
                ctx.getOutputService().printResult(e);
                exitCode = -1;
            }


        } finally {
            ctx.getConfigService().saveConfiguration();
        }
    }

    /**
     * Checks for an update after every 24h since last check.
     */
    private static void checkForUpdate(CliContext ctx) {

        try {
            long lastUpdateCheck = ctx.getConfigService().getUpdateCheckLast();

            // If the last update check was longer than 24h ago -> check for update
            long currentTimeMillis = System.currentTimeMillis();

            long timePassedSinceLastCheck = currentTimeMillis - lastUpdateCheck;

            ctx.getConfigService().setUpdateCheckLast(currentTimeMillis);

            if (timePassedSinceLastCheck < DateTimeUtil.ONE_DAY) {
                return; // No update check
            }

            ICliCommand updateCommand = ctx.createCommandInstance("update");
            updateCommand.call(CommandArgs.EMPTY);
        } catch (Exception e) {
            LOG.error("Error happened in checking for updates: " + e.getMessage(), e);
        }
    }
}
