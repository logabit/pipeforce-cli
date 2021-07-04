package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.DateTimeUtil;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.PathUtil;

/**
 * Setup wizard + installs to PIPEFORCE home in case folder doesnt exist yet.
 */
public class SetupCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        boolean advanced = false;
        if (args.getLength() == 1) {
            if (args.getOptionKeyAt(0).equals("advanced")) {
                advanced = true;
            } else {
                out.println("USAGE: " + getUsageHelp());
                return -1;
            }
        }

        boolean installed = getContext().getInstallService().install();

        String userHome = System.getProperty("user.home");
        String pipeforceHome = PathUtil.path(userHome, "pipeforce");

        config.setNamespace(in.ask("Namespace", config.getNamespace()));

        if (advanced) {
            config.setHost(in.ask("Host", config.getHost()));
            config.setPort(Integer.parseInt(in.ask("Port", config.getPort() + "")));
            config.setProtocol(in.ask("Protocol", config.getProtocol()));
            config.setApiPath(in.ask("API Path", config.getApiPath()));
        }

        config.setUsername(in.ask("Username", config.getUsername()));

        String password = in.askPassword("Password");
        config.setApiToken(null);
        String apitoken = loadApiToken(config.getUsername(), password);

        config.setApiToken(apitoken);
        config.setApiTokenCreated(DateTimeUtil.currentDateTimeAsIso8061());
        config.saveConfiguration();

        if (installed) {
            out.println();
            out.println("PIPEFORCE CLI successfully installed to: " + pipeforceHome);
            out.println();
            out.println("Tip 1: Install VS Code: https://code.visualstudio.com/download");
            out.println("Tip 2: Install YAML plugin: https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml");
            out.println("Tip 3: Open your workspace: " + PathUtil.path(userHome, "pipeforce", "PIPEFORCE.code-workspace"));
            out.println("Tip 4: PIPEFORCE docs: https://devdocs.pipeforce.io");
            out.println();
        }


        String vsCodeWorkspace = "" +
                "{\n" +
                "        \"folders\": [\n" +
                "           {\n" +
                "               \"path\":\".\"\n" +
                "           }\n" +
                "        ],\n" +
                "\n" +
                "        \"settings\": {\n" +
                "\n" +
                "            \"yaml.schemas\": {\n" +
                "               \"" + config.getHubApiUrl("pipe") + ":pipe.schema.v7\": [\"/*.pi.yaml\"]\n" +
                "            },\n" +
                "\n" +
                "           \"files.exclude\": {\n" +
                "               \"**/PIPEFORCE.code-workspace\": true\n" +
                "           },\n" +
                "\n" +
                "           \"files.encoding\": \"utf8\"\n" +
                "        }\n" +
                "}\n";

        String vsCodeConfig = PathUtil.path(pipeforceHome, "PIPEFORCE.code-workspace");
        FileUtil.saveStringToFile(vsCodeWorkspace, FileUtil.getOrCreateFile(vsCodeConfig));

        return 0;
    }

    public String getUsageHelp() {
        return "pi setup [advanced]\n" +
                "   Optionally installs the CLI + runs the (advanced) setup wizard.\n" +
                "   Examples: \n" +
                "     pi setup\n" +
                "     pi setup advanced";
    }

    private String loadApiToken(String username, String password) {

        getContext().getOutputService().showProgress("Check login");
        try {
            Object result = getContext().getPipelineRunner().executePipelineUri(
                    "iam.apitoken?username=" + username + "&password=" + password);
            return result + "";
        } finally {
            getContext().getOutputService().stopProgress();
        }
    }
}
