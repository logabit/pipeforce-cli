package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;

/**
 * Here in order to show deprecation message.
 *
 * @author sniederm
 * @since 7.0
 */
public class PushCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        out.println("This command has been deprecated. Instead use: ");
        ICliCommand publish = getContext().createCommandInstance("publish");
        out.println(publish.getUsageHelp());
        return -1;
    }

    public String getUsageHelp() {
        return null;
    }
}
