package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.net.Request;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes a single command.
 *
 * @author sniederm
 * @since 2.8
 */
public class CommandCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() < 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        // pi command COMMAND_NAME KEY1=VAL1 KEY2=VAL2
        String commandName = args.getOptionKeyAt(0);

        Map<String, String> paramsMap = new LinkedHashMap<>(args.getOptions());
        paramsMap.remove(commandName); // Already extracted above

        Object result = getContext().getResolver().resolve(
                Request.get().uri("$uri:command:" + commandName).params(paramsMap), JsonNode.class);

        out.printResult(result);

        return 0;
    }

    public String getUsageHelp() {
        return "pi command <COMMAND_NAME> [<PARAM1=VAL1>] [<PARAMN=VALN>]\n" +
                "   Executes a command with given optional parameters.\n" +
                "   Examples:\n" +
                "     pi command log message=HELLO - Executes the log command.\n" +
                "     pi command datetime format=dd.MM.YY - Executes the datetime command.";
    }
}
