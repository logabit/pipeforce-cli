package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.PathUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the all property keys matching given property key pattern.
 *
 * @author sniederm
 * @since 6.0
 */
public class ListCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String path = null;
        if (args.getLength() == 1) {

            // pi list PATH
            path = args.getOptionKeyAt(0);
        }

        String keyPattern = path;

        if (!keyPattern.endsWith("*")) {
            keyPattern = PathUtil.path(keyPattern, "**");
        }

        String keyPrefix = PathUtil.path("/pipeforce", config.getNamespace());
        getContext().getOutputService().showProgress("");
        try {
            ArrayNode list = (ArrayNode) getContext().getPipelineRunner()
                    .executePipelineUri("property.list?filter=" + keyPattern);

            List<String> keys = new ArrayList<>();
            for (JsonNode node : list) {
                keys.add(node.get("key").textValue().substring(keyPrefix.length()));
            }

            getContext().getOutputService().printResult(keys);
        } finally {
            getContext().getOutputService().stopProgress();
        }
        return 0;
    }

    public String getUsageHelp() {
        return "pi list <APP_NAME>\n" +
                "   Lists all published remote resources of the app.\n" +
                "   Example: pi list global/app/myapp/**";
    }
}
