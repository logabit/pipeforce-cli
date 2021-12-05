package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;

/**
 * Deletes the given remote property.
 *
 * @author sniederm
 * @since 6.0
 */
public class DeleteCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() == 0) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        CliPathArg pathArg = getContext().createPathArg(args.getOptionKeyAt(0));

        out.println("Are you sure to remote delete [" + pathArg.getRemotePattern() + "]? This step cannot be undone!");
        Integer selection = in.choose(ListUtil.asList("no", "yes"), "no");
        if (selection == 0) {
            return 0;
        }

        delete(pathArg);
        return 0;
    }

    public void delete(CliPathArg pathArg) {

        PublishCliService publishService = getContext().getPublishService();
        publishService.load();

        // TODO Return only keys, not all values (use property.keys, available since 7.0)
        ArrayNode founds = (ArrayNode) getContext().getPipelineRunner()
                .executePipelineUri("property.list?filter=" + PathUtil.removeExtensions(pathArg.getRemotePattern()));

        String propHome = PathUtil.path("/pipeforce/" + config.getNamespace());

        if (founds == null) {
            return;
        }

        for (JsonNode found : founds) {
            String key = found.get("key").textValue();
            out.println("Delete: " + key);
            getContext().getPipelineRunner().executePipelineUri("property.schema.delete?key=" + key);

            // Remove entry from .published file
            String type = found.get("type").textValue();
            String ext = getContext().getMimeTypeService().getFileExtensionForMimeType(type);
            String relPath = key.substring(propHome.length() + 1);
            relPath = relPath + ext;
            String targetPath = PathUtil.path(config.getHome(), relPath);
            publishService.remove(targetPath);
        }

        publishService.save();
    }

    public String getUsageHelp() {
        return "pi delete <PROPERTY_KEY_PATTERN>\n" +
                "   Deletes the given remote property or properties from server.\n" +
                "   Doesn't delete any local file.\n" +
                "   Examples:\n" +
                "     pi delete global/app/myapp/pipeline/test - Deletes the test pipeline.\n" +
                "     pi delete global/app/myapp/ - Same as global/app/myapp/**.\n" +
                "     pi delete global/app/myapp/** - Deletes recursively all inside myapp.\n" +
                "     pi delete global/app/myapp/* - Deletes all inside myapp level. Not recursively.";
    }
}
