package com.logabit.pipeforce.cli;

import com.logabit.pipeforce.cli.command.HelpCliCommand;
import com.logabit.pipeforce.cli.command.ICliCommand;
import com.logabit.pipeforce.cli.command.SetupCliCommand;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.cli.service.UpdateCliService;
import com.logabit.pipeforce.common.exception.UncheckedClassNotFoundException;
import com.logabit.pipeforce.common.util.DateTimeUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Arrays;

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
        OutputCliService out = ctx.getOutputService();
        String errorMessage = null;
        LOG.info("Executing command: pi " + StringUtil.concat(" ", args));

        try {
            ctx.getConfigService().loadConfiguration();

            // The very first arg is the command name -> Lookup a class with this name: setup -> SetupCallable

            if (args.length == 0) {
                out.println("USAGE: pi <COMMAND> <SWITCHES>");
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

            } catch (UncheckedClassNotFoundException e) {

                out.println("PIPEFORCE CLI command [" + cmdName + "] not found.");
                ICliCommand cmd = ctx.createCommandInstance("help");
                try {
                    cmd.call(CommandArgs.EMPTY);
                } catch (Exception exception) {
                    LOG.error("Could not execute help command!", e);
                }

            } catch (HttpServerErrorException.InternalServerError e) {

                String message = e.getResponseBodyAsString();

                if (JsonUtil.isJsonString(message)) {
                    errorMessage = JsonUtil.jsonStringToJsonNode(message).toPrettyString();
                } else {
                    errorMessage = e.getMessage();
                }

                LOG.error("Command caused error: pi " + StringUtil.concat(" ", args), e);

            } catch (Exception e) {

                errorMessage = e.getMessage();
                LOG.error("Command caused error: pi " + StringUtil.concat(" ", args), e);
                throw e;
            }


        } finally {

            if (errorMessage != null) {
                out.printResult(errorMessage);
            } else {
                ctx.getConfigService().saveConfiguration();
            }
        }
    }

    /**
     * Checks for an update after every 24h since last check.
     */
    private static void checkForUpdate(CliContext ctx) {

        // If update command was called -> No extra check required here...
        if ("update".equals(ctx.getCommand())) {
            return;
        }

        try {

            String releaseName = System.getProperty("LATEST_VERSION_TAG");

            // In case a "fake" release tag was set as property -> Skip last update date calculation
            if (StringUtil.isEmpty(releaseName)) {
                long lastUpdateCheck = ctx.getConfigService().getUpdateCheckLast();

                // If the last update check was longer than 24h ago -> check for update
                long currentTimeMillis = System.currentTimeMillis();

                long timePassedSinceLastCheck = currentTimeMillis - lastUpdateCheck;

                ctx.getConfigService().setUpdateCheckLast(currentTimeMillis);

                if (timePassedSinceLastCheck < DateTimeUtil.ONE_DAY) {
                    return; // No update check
                }
            }

            UpdateCliService.VersionInfo versionInfo = ctx.getUpdateService().getVersionInfo();
            if (versionInfo.isNewerVersionAvailable()) {
                // Update found. Ask user whether he wants to download + install update.
                ctx.getOutputService().println("Warning: Your CLI version " + versionInfo.getCurrentReleaseTag() +
                        " is outdated. Latest available version: " + Arrays.toString(versionInfo.getLatestVersion()) +
                        ". Auto-update with: pi update");
            }

        } catch (Exception e) {
            LOG.error("Error happened in checking for updates: " + e.getMessage(), e);
        }
    }
}
