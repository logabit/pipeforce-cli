package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.net.Request;
import com.logabit.pipeforce.common.util.StringUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shows the server logs.
 *
 * @author sniederm
 * @since 2.12
 */
public class LogsCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 2) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String lines = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(lines)) {
            lines = "100";
        }

        String service = args.getOptionKeyAt(1);

        if (StringUtil.isEmpty(service)) {
            service = "hub";
        }

        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("lines", lines);
        paramsMap.put("service", service);

        Object result = getContext().getResolver().resolve(
                Request.get().uri("$uri:command:log.list").params(paramsMap), String.class);

        out.println(result + "");

        return 0;
    }

    public String getUsageHelp() {
        return "pi logs [<LINES> <SERVICE>]\n" +
                "   Shows the server logs.\n" +
                "   Examples:\n" +
                "     pi logs - Shows the default logs.\n" +
                "     pi logs 20 hub - Shows the last 20 lines of hub service.";
    }
}
