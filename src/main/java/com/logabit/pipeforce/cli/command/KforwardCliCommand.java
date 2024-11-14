package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.KubectlCliService;
import com.logabit.pipeforce.common.util.StringUtil;

import java.util.List;

/**
 * Forwards the given port from given pod inside Kubernetes.
 * This is mainly useful for debugging.
 *
 * @author sniederm
 */
public class KforwardCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        String namespace = args.getOptionKeyAt(0);
        if (StringUtil.isEmpty(namespace)) {
            throw new CliException("Specify the namespace: pi kforward <ns> <service> <port>");
        }

        String serviceName = args.getOptionKeyAt(1);
        if (StringUtil.isEmpty(serviceName)) {
            throw new CliException("Specify the name of the service to forward its port: pi kforward <ns> <service> <port>");
        }

        String portString = args.getOptionKeyAt(2);

        // If no port is given -> Try to auto-detect
        if (portString == null) {
            if (serviceName.equals("hub")) {
                portString = "5005";
            } else if (serviceName.equals("workflow")) {
                portString = "5006";
            } else if (serviceName.equals("iam")) {
                portString = "5007";
            } else {
                throw new CliException("Cannot auto-detect port from service name " + serviceName + ": Specify port");
            }
        }
        if (StringUtil.isEmpty(portString)) {
            throw new CliException("Specify the port to be forwarded: pi kforward " + namespace + ", " + serviceName + " <port>");
        }

        KubectlCliService kubectl = getContext().getKubectlService();

        List<String> podNames = kubectl.getPodNamesByServiceName(namespace, serviceName);

        kubectl.localExec(
                "kubectl", "port-forward", "-n", namespace, podNames.get(0), portString + ':' + portString);

        return 0;
    }

    public String getUsageHelp() {

        return "pi kforward <ns> <service> [<port>]\n" +
                "   Forwards the given port to the pod service. \n" +
                "   If port is missing tries to detect from service name. \n" +
                "   Examples: \n" +
                "     pi kforward main hub 5005  \n" +
                "     pi kforward main workflow 5006  \n" +
                "     pi kforward main iam 5007  \n" +
                "     pi kforward main hub - Uses the default forward port for hub";
    }
}
