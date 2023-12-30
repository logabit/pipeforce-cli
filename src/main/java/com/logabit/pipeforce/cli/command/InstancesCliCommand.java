package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Outputs the currently configured instances.
 *
 * @author sniederm
 * @since 3.0.9
 */
public class InstancesCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 0) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String defaultInstance = config.getDefaultInstance();
        out.println("Active instance: " + defaultInstance);

        List<CliConfig.Instance> instances = config.getInstances();

        if (instances.size() > 1) {
            List<String> instanceNames = new ArrayList<>();
            for (CliConfig.Instance instance : instances) {
                instanceNames.add(instance.toString());
            }

            out.println("Switch active instance:");
            int selectedInstanceIndex = in.choose(instanceNames, defaultInstance);

            CliConfig.Instance selectedInstance = instances.get(selectedInstanceIndex);

            if (!config.getDefaultInstance().equals(selectedInstance.getName())) {
                config.setDefaultInstance(selectedInstance.getName());
                config.saveConfiguration();
                out.println("Active instance switched to : " + selectedInstance.getName());
            }
        }

        return 0;
    }

    public String getUsageHelp() {
        return "pi instances\n" +
                "   Shows server instances and allows to change the active one.\n" +
                "   Example: pi instances";
    }
}
