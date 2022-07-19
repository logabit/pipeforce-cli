package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.StringUtil;

/**
 * Executes the given command inside a given service container.
 *
 * @author sniederm
 */
public class KexecCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        String serviceName = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(serviceName)) {
            throw new CliException("Specify a service name!");
        }

        String command = args.getOptionKeyAt(1);

        if (StringUtil.isEmpty(command)) {
            throw new CliException("Specify a command!");
        }

        // If command is a -- : Use any subsequent arg as arg for the remote command (same as kubectl exec does)
        if ("--".equals(command)) {

            command = "";
            String[] originalArgs = args.getOriginalArgs();

            for (int i = 2; i < originalArgs.length; i++) {

                command = command + " \"" + originalArgs[i] + "\"";
            }
        }

        String namespace = getContext().getCurrentInstance().getNamespace();
        String result = getContext().getKubectlService().exec(namespace, serviceName, command);
        out.println(result);

        return 0;
    }

    public String getUsageHelp() {

        return "pi kexec <SERVICE> -- <COMMAND>\n" +
                "   Executes the given command inside the given service container. \n" +
                "   Examples: \n" +
                "     pi kexec hub -- ls - Lists the resources of the service container hub.";
    }
}
