package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.StringUtil;

import java.util.List;

/**
 * Returns the pod name for a given service name.
 *
 * @author sniederm
 */
public class KpodnameCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        String serviceName = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(serviceName)) {
            throw new CliException("Specify a service name!");
        }

        String namespace = getContext().getCurrentInstance().getNamespace();
        List<String> pods = getContext().getKubectlService().getPodNamesByServiceName(namespace, serviceName);
        out.println(StringUtil.concat(", ", pods));

        return 0;
    }

    public String getUsageHelp() {

        return "pi kpodname <SERVICE>\n" +
                "   Returns the internal pod name of a service. If more than one pods: Returns the first one. \n" +
                "   Examples: \n" +
                "     pi podname hub - Returns the podname of hub.";
    }
}
