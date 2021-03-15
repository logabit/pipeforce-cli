package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;

/**
 * Here in order to show deprecation message.
 *
 * @author sniederm
 * @since 6.0
 */
public class RunCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        out.println("This command has been deprecated. Instead use: ");
        ICliCommand piplineCommand = getContext().createCommandInstance("pipeline");
        out.println(piplineCommand.getUsageHelp());
        return -1;
    }

    public String getUsageHelp() {
        return null;
    }
}
