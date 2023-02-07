package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.PathUtil;

import java.util.ArrayList;
import java.util.List;

import static com.logabit.pipeforce.common.property.IProperty.FIELD_PATH;

/**
 * Lists the all property keys matching given property key pattern.
 *
 * @author sniederm
 * @since 6.0
 */
public class ListCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        // pi list PATH
        CliPathArg pathArg = getContext().createPathArg(args.getOptionKeyAt(0));
        String keyPrefix = PathUtil.path("/pipeforce", getContext().getCurrentInstance().getNamespace());

        out.showProgress("");
        try {
            ArrayNode list = (ArrayNode) getContext().getPipelineRunner()
                    .executePipelineUri("property.list?pattern=" + pathArg.getRemotePattern());

            List<String> keys = new ArrayList<>();
            for (JsonNode node : list) {
                keys.add(node.get(FIELD_PATH).textValue().substring(keyPrefix.length()));
            }

            out.printResult(keys);
        } finally {
            out.stopProgress();
        }
        return 0;
    }

    public String getUsageHelp() {
        return "pi list <PATH_PATTERN>\n" +
                "   Lists all published remote resources of the app.\n" +
                "   Examples:\n" +
                "     pi list global/app/myapp/** - Lists the content of myapp recursively.\n" +
                "     pi list global/app/myapp/ - Short-cut of global/app/myapp/**\n" +
                "     pi list global/app/myapp/pipeline/test - Lists the content of test pipeline\n" +
                "     pi list global/app/myapp/* - Lists the content of myapp. Not recursively.\n" +
                "     pi list global/app/*/pipeline/* - Lists all pipelines of all apps.";
    }
}
