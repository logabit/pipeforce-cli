package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.JsonUtil;

import static com.logabit.pipeforce.cli.uri.ClientPipeforceURIResolver.Method.POST;

/**
 * Executes a pipeline and displays the result.
 *
 * @author sniederm
 * @since 2.8
 */
public class PipelineCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        // pi pipeline PATH
        String path = args.getOriginalArgs()[0];
        String option = detectType(path);

        switch (option) {

            case "file":
                executeFile(path);
                break;
            case "remote":
                executePipelineRemote(path);
                break;
            case "none":
                out.println("Prefix in path not supported: " + path);
                return -1;
        }

        return 0;
    }

    private String detectType(String path) {

        if (path.startsWith(getContext().getConfigService().getWorkspaceConfig().getPropertiesHome() + "/")) {
            return "file";
        }

        if (path.startsWith("global/")) {
            return "remote";
        }

        return "none";
    }

    private void executePipelineRemote(String path) {

        CliPathArg arg = getContext().createPathArg(path);
        Object result = getContext().getResolver().resolveToObject(
                POST, "$uri:pipeline:" + arg.getRemotePattern(), String.class);
        out.printResult(result);
    }

    private void executeFile(String path) {

        CliPathArg pathArg = getContext().createPathArg(path);
        String pipelineString = out.readFileToString(pathArg.getLocalPattern());

        if (pipelineString == null) {
            throw new RuntimeException("Local pipeline script not found: " + pathArg.getLocalPattern());
        }

        JsonNode node = JsonUtil.yamlStringToJsonNode(pipelineString);
        Object result = getContext().getResolver().resolveToObject(
                POST, "$uri:pipeline", node, null, null, String.class);
        out.printResult(result);
    }

    public String getUsageHelp() {
        return "pi pipeline <PATH>\n" +
                "   Executes a pipeline locally or remote depending on prefix src/ or global/ of path.\n" +
                "   Examples:\n" +
                "     pi pipeline src/global/app/myapp/pipeline/hello.pi.yaml - Executes a local pipeline.\n" +
                "     pi pipeline global/app/myapp/hello - Executes a remote pipeline.";
    }
}
