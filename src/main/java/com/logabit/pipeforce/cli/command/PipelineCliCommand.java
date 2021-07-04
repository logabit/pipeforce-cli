package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.JsonUtil;

/**
 * Executes a pipeline uri and displays the result.
 *
 * @author sniederm
 * @since 2.8
 */
public class PipelineCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 2) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        // pi pipeline OPTION PATH
        String option = args.getOptionKeyAt(0);
        String path = args.getOptionKeyAt(1);

        switch (option) {

            case "file":
                executeFile(path);
                break;
            case "uri":
                executePipelineUri(args.getOriginalArgs()[1]);
                break;
            case "remote":
                executePipelineRemote(path);
                break;

            default:
                throw new CliException("Unregognized pipeline option: " + option);
        }

        return 0;
    }

    private void executePipelineRemote(String path) {

        CliPathArg pathArg = getContext().createPathArg(path);
        Object result = getContext().getPipelineRunner().executePipelineUri("call?uri=property:" + pathArg.getRemotePattern());
        out.printResult(result);
    }

    private void executePipelineUri(String path) {

        Object result = getContext().getPipelineRunner().executePipelineUri(path);
        out.printResult(result);
    }

    private void executeFile(String path) {

        CliPathArg pathArg = getContext().createPathArg(path);
        String pipelineString = out.readFileToString(pathArg.getLocalPattern());

        if (pipelineString == null) {
            throw new RuntimeException("Local pipeline script not found: " + pathArg.getLocalPattern());
        }

        JsonNode node = JsonUtil.yamlStringToJsonNode(pipelineString);
        Object result = getContext().getPipelineRunner().executePipelineJsonNode(node);
        out.printResult(result);
    }

    public String getUsageHelp() {
        return "pi pipeline <file|remote|uri> <PATH>\n" +
                "   Executes a pipeline.\n" +
                "   Examples:\n" +
                "     pi pipeline file src/global/app/myapp/pipeline/hello.pi.yaml - Executes a local file.\n" +
                "     pi pipeline remote global/app/myapp/hello - Executes a remote pipeline.\n" +
                "     pi pipeline uri \"datetime?format=dd.MM.YY\" - Executes a pipeline uri.";
    }
}
