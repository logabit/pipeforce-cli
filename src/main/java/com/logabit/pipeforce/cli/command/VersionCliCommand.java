package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;

/**
 * Outputs the current version.
 *
 * @author sniederm
 * @since 3.0.9
 */
public class VersionCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 0) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        out.println("Installed CLI version: " + config.getInstalledReleaseTag());
        return 0;
    }

    public String getUsageHelp() {
        return "pi version\n" +
                "   Shows the current version of the CLI.\n" +
                "   Example: pi version";
    }
}
