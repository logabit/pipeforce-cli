package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.util.InputUtil;
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
            System.out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String relativeToSrc = args.getOptionKeyAt(0);
        relativeToSrc = getContext().getRelativeToSrc(relativeToSrc);

        out.println("Are you sure to remote delete [" + relativeToSrc + "]? This step cannot be undone!");
        Integer selection = InputUtil.choose(ListUtil.asList("no", "yes"), "no");
        if (selection == 0) {
            return 0;
        }

        PublishCliService publishService = getContext().getPublishService();

        publishService.load();

        // Remove extension if there is any: global/app/myapp/file.json -> myapp/file
        relativeToSrc = PathUtil.removeExtensions(relativeToSrc);

        // TODO Find a way to only return keys, no values (flag or PEL selection / projection support)
        ArrayNode founds = (ArrayNode) getContext().getPipelineRunner()
                .executePipelineUri("property.list?filter=" + relativeToSrc);

        String propHome = PathUtil.path("/pipeforce/" + config.getNamespace());

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

        return 0;
    }

    public String getUsageHelp() {
        return "pi delete <APP_PROPERTY_KEY>\n" +
                "   Deletes the given app property from server.\n" +
                "   Example: pi delete myapp/pipeline/test - Deletes the test pipeline.\n" +
                "   Example: pi delete myapp - Deletes all resources of the app myapp.";
    }
}
