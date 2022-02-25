package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContextAware;
import com.logabit.pipeforce.cli.CommandArgs;

/**
 * Represents a single command. In order to create a new command you need to do these steps:
 * <ol>
 *     <li>Create a new class with suffix ...CliCommand.java inside the command package.</li>
 *     <li>Let this command class implement {@link ICliCommand} or extend from {@link BaseCliCommand}</li>
 * </ol>
 * You can then call the command by its lower case prefix name. For example a command class <code>HelloCliCommand</code>
 * could be called from terminal using <code>pi hello</code>
 *
 * @author sniederm
 * @since 7.0
 */
public interface ICliCommand extends CliContextAware {

    /**
     * Calls the command.
     *
     * @param args The args passed to to this command. NOTE: Contains only the part of the args after the command name.
     *             So a call in the terminal of this <code>pi COMMAND option1 option2</code> would lead to this to be
     *             set here: <code>option1 option2</code>
     * @return
     * @throws Exception
     */
    int call(CommandArgs args) throws Exception;

    /**
     * The help text to be shown for this command.
     * Return null here in order to show no message.
     */
    String getUsageHelp();

    /**
     * The result after the command has been executed.
     * Returns null in case the command was not executed yet or doesnt support a result object.
     *
     * @return
     */
    CommandResult getResult();
}
