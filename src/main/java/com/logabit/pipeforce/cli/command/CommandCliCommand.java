package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

        Map<String, String> paramsMap = new HashMap<>(args.getOptions());
        paramsMap.remove(commandName); // Already extracted above

        ObjectNode root = JsonUtil.createObjectNode();

        Set<String> paramKeys = paramsMap.keySet();
        ObjectNode paramNode = JsonUtil.createObjectNode();
        for (String paramKey : paramKeys) {
            paramNode.put(paramKey, paramsMap.get(paramKey));
        }

        ObjectNode commandNode = JsonUtil.createObjectNode();
        commandNode.set(commandName, paramNode);

        ArrayNode pipelineNode = JsonUtil.createArrayNode();
        pipelineNode.add(commandNode);

        root.set("pipeline", pipelineNode);

        Object result = getContext().getPipelineRunner().executePipelineJsonNode(root);
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
