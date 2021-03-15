package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PathUtil;

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

        Object result = getContext().getPipelineRunner().executePipelineUri("call?uri=property:" + path);
        out.printResult(result);
    }

    private void executePipelineUri(String path) {

        Object result = getContext().getPipelineRunner().executePipelineUri(path);
        out.printResult(result);
    }

    private void executeFile(String path) {

        path = getContext().getRelativeToSrc(path);

        String pipelineScript = PathUtil.path(config.getHome(), "src", path);
        String pipelineString = out.readFileToString(pipelineScript);

        if (pipelineString == null) {
            throw new RuntimeException("Local pipeline script not found: " + pipelineScript);
        }

        JsonNode node = JsonUtil.yamlStringToJsonNode(pipelineString);
        Object result = getContext().getPipelineRunner().executePipelineJsonNode(node);
        out.printResult(result);
    }

    public String getUsageHelp() {
        return "pi pipeline file|remote|uri <PATH>\n" +
                "   Executes a pipeline.\n" +
                "   Example: pi pipeline file  hello.pi.yaml - Execute a local file.\n" +
                "   Example: pi pipeline remote global/app/myapp/hello - Executes a remote pipeline.\n" +
                "   Example: pi pipeline uri \"datetime?format=dd.MM.YY\" - Executes a pipeline uri.";
    }
}
