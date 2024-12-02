package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.common.command.stub.IamApitokenParams;
import com.logabit.pipeforce.common.net.ClientPipeforceURIResolver;
//import com.logabit.pipeforce.common.net.Request;
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

        String namespace = in.ask("Namespace", null);
        String host = in.ask("Host", "pipeforce.net");
        String port = in.ask("Port", "443");
        String protocol = in.ask("Protocol", "https");
        String apiPath = in.ask("API Path", "/api/v3");
        String username = in.ask("Username", null);
        String password = in.askPassword("Password");

        CliConfig.Instance instance = config.getInstanceByNamespaceAndHost(namespace, host);

        if (instance == null) {
            instance = new CliConfig.Instance();
            instance.setNamespace(namespace);
            instance.setHost(host);
            config.getInstances().add(instance);
        }

        instance.setPort(Integer.parseInt(port));
        instance.setProtocol(protocol);
        instance.setApiPath(apiPath);
        instance.setUsername(username);

        config.setDefaultInstance(instance.getName());

        instance.setApiToken(null);
        String apiToken = loadApiToken(instance.getUsername(), password);
        instance.setApiToken(apiToken);
        instance.setApiTokenCreated(DateTimeUtil.currentDateTimeAsIso8061());

        config.saveConfiguration();

        if (installed) {
            out.println();
            out.println("PIPEFORCE CLI " + config.getReleaseTagFromJar() + " successfully installed to: " + pipeforceHome);
            out.println();
            out.println("Tip 1: Install VS Code: https://code.visualstudio.com/download");
            out.println("Tip 2: Install YAML plugin: https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml");
            out.println("Tip 3: Study PIPEFORCE tutorials: https://docs.pipeforce.org");
            out.println();
        }

        return 0;
    }

    public String getUsageHelp() {
        return "pi setup [advanced] [path]\n" +
                "   Optionally installs the CLI + (re-)runs the setup wizard.\n" +
                "   Examples: \n" +
                "     pi setup";
    }

    private String loadApiToken(String username, String password) {

        getContext().getOutputService().showProgress("Checking login");
        ClientPipeforceURIResolver resolver = getContext().getResolver();
        try {

            // Sending sensitive command params in the body as url-encoded -> more secure
            return resolver.command(
                    new IamApitokenParams().setBody(username, password),
                    String.class
            );

        } finally {
            getContext().getOutputService().stopProgress();
        }
    }
}
