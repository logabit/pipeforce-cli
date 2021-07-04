package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.HashUtil;
import com.logabit.pipeforce.common.util.IOUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Internal command which creates a new app, uploads it, downloads it and deletes it.
 *
 * @author sniederm
 * @since 2.20
 */
public class SelftestCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) {

        try {
            String random = StringUtil.randomString(5).toLowerCase();
            String appName = "selftest" + random;

            CliAutomation automation1 = new CliAutomation();

            String description = "someöäü&%$" + random;
            automation1.newStep("new", "app")
                    .setCommandAnswers(appName, "sometitle" + random, description, "");

            automation1.newStep("new", "pipeline")
                    .setCommandAnswers(appName, "somepipeline" + random);

            automation1.newStep("new", "workflow")
                    .setCommandAnswers(appName, "someworkflow" + random);

            automation1.execute(getContext());

            out.println("Selftest: New app, pipeline, workflow OK.");

            // Copy binary logo file, publish, get, check encoding
            String appPath = Paths.get(config.getHome(), "src", "global", "app", appName).toString();
            String logoPath = Paths.get(appPath, "template", "logo.png").toString();
            File logoFile = new File(logoPath);
            logoFile.getParentFile().mkdirs();
            Resource r = FileUtil.readAsResource("classpath:media/logo.png");
            FileOutputStream fos = new FileOutputStream(logoFile);
            IOUtil.copyAndClose(r.getInputStream(), fos);
            String origLogoHash = HashUtil.createHash(logoFile, HashUtil.HashType.MD5);

            CliAutomation automation2 = new CliAutomation();

            automation2.newStep("publish", "global/app/" + appName + "/**")
                    .setCommandAnswers("yes");

            // Run with src/ prefix...
            automation2.newStep("get", "src/global/app/" + appName + "/**")
                    .setCommandAnswers("yes-all"); // Overwrite all

            automation2.newStep("delete", "global/app/" + appName + "/**")
                    .setCommandAnswers("yes");

            automation2.execute(getContext());

            out.println("Selftest: publish, get, delete OK.");

            // Verify that encoding is valid of downloaded properties
            String confPath = Paths.get(appPath, "config", "app.json").toString();
            if (!new File(confPath).exists()) {
                confPath = Paths.get(appPath, "config", appName + ".json").toString();
            }
            String config = FileUtil.readFileToString(confPath);
            Map jsonMap = JsonUtil.jsonStringToMap(config);
            if (!jsonMap.get("description").equals(description)) {
                throw new CliException("Description of downloaded app config [" +
                        jsonMap.get("description") + "] doesnt match with uploaded one [" + description + "] in: " +
                        confPath + ". Keeping app folder: " + appPath);
            } else {
                out.println("Selftest: Encoding test OK.");
            }

            // Verify that downloaded logo is the same as server side
            String downloadLogoHash = HashUtil.createHash(logoFile, HashUtil.HashType.MD5);
            if (!origLogoHash.equals(downloadLogoHash)) {
                throw new CliException("Binary upload and download test failed: Hash of upload [" + origLogoHash +
                        "] and download differ [" + downloadLogoHash + "] of file: " + logoPath);
            } else {
                out.println("Selftest: Binary download test OK.");
            }

            // Locally cleanup selftest folder
            FileUtil.delete(appPath);

            out.println("Selftest successful. Everything seems to be OK.");

            return 0;
        } catch (Exception e) {
            throw new CliException("Selftest failed: " + e.getMessage(), e);
        }
    }

    public String getUsageHelp() {
        return null; // Do not show this to user
    }

    private static class CliAutomation {

        private List<CliAutomationStep> automationSteps = new ArrayList<>();

        public CliAutomationStep newStep(String command, String... args) {
            CliAutomationStep step = new CliAutomationStep();
            step.setCommand(command, args);
            this.automationSteps.add(step);
            return step;
        }

        public void execute(CliContext context) {

            try {
                StringBuffer buffer = new StringBuffer();
                for (CliAutomationStep automationStep : automationSteps) {

                    String[] commandAnswers = automationStep.getCommandAnswers();
                    for (String commandAnswer : commandAnswers) {
                        buffer.append(commandAnswer);
                        buffer.append('\n');
                    }
                }

                InputStream testInput = new ByteArrayInputStream(buffer.toString().getBytes("UTF-8"));
                context.setAnswerInputStream(testInput);

                for (CliAutomationStep automationStep : automationSteps) {

                    ICliCommand newCommand = context.createCommandInstance(automationStep.getCommand());
                    newCommand.call(new CommandArgs(automationStep.getCommandArgs()));
                }
            } catch (Exception e) {
                throw new CliException("Could not automate CLI steps: " + e.getMessage(), e);
            }
        }

        private static class CliAutomationStep {

            private String command;
            private String[] commandArgs;
            private String[] commandAnswers;

            public String[] getCommandArgs() {
                return commandArgs;
            }

            public String getCommand() {
                return command;
            }

            public CliAutomationStep setCommand(String commandName, String... args) {
                this.command = commandName;
                this.commandArgs = args;
                return this;
            }

            public String[] getCommandAnswers() {
                return commandAnswers;
            }

            public CliAutomationStep setCommandAnswers(String... commandAnswers) {
                this.commandAnswers = commandAnswers;
                return this;
            }
        }
    }
}

