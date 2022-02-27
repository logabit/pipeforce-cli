package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.common.util.DateTimeUtil;
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

        CliConfig.Instance instance = new CliConfig.Instance();

        instance.setNamespace(in.ask("Namespace", instance.getNamespace()));

        if (advanced) {
            instance.setHost(in.ask("Host", instance.getHost()));
            instance.setPort(Integer.parseInt(in.ask("Port", instance.getPort() + "")));
            instance.setProtocol(in.ask("Protocol", instance.getProtocol()));
            instance.setApiPath(in.ask("API Path", instance.getApiPath()));
        }

        config.getInstances().add(instance);
        config.setDefaultInstance(instance.getName());

        instance.setUsername(in.ask("Username", instance.getUsername()));

        String password = in.askPassword("Password");
        instance.setApiToken(null);
        String apitoken = loadApiToken(instance.getUsername(), password);

        instance.setApiToken(apitoken);
        instance.setApiTokenCreated(DateTimeUtil.currentDateTimeAsIso8061());


        config.saveConfiguration();

        if (installed) {
            out.println();
            out.println("PIPEFORCE CLI " + config.getInstalledReleaseName() + " successfully installed to: " + pipeforceHome);
            out.println();
            out.println("Tip 1: Install VS Code: https://code.visualstudio.com/download");
            out.println("Tip 2: Install YAML plugin: https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml");
            out.println("Tip 3: Study PIPEFORCE tutorials: https://docs.pipeforce.io");
            out.println();
        }

        return 0;
    }

    public String getUsageHelp() {
        return "pi setup [advanced] [path]\n" +
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
