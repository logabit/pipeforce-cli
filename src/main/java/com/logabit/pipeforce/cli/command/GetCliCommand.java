package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.content.model.ContentType;
import com.logabit.pipeforce.common.content.service.MimeTypeService;
import com.logabit.pipeforce.common.util.EncodeUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Downloads a given set of properties.
 *
 * @author sniederm
 * @since 6.0
 */
public class GetCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) {

        if (args.getLength() != 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        CliPathArg pathArg = getContext().createPathArg(args.getOptionKeyAt(0));
        get(pathArg, "src");

        return 0;
    }

    /**
     * Downloads all properties matching given pattern.
     *
     * @param pathArg
     * @param localTargetFolderName The folder inside the pipeforce workspace to get files into.
     */
    public void get(CliPathArg pathArg, String localTargetFolderName) {

        PublishCliService publishService = getContext().getPublishService();
        publishService.load();

        MimeTypeService mimeTypeService = getContext().getMimeTypeService();

        ArrayNode list = (ArrayNode) getContext().getPipelineRunner().executePipelineUri("property.list?filter=" + pathArg.getRemotePattern());

        int rememberOverwriteAnswer = -1;
        int filesCounter = 0;
        int updatedCounter = 0;
        int createdCounter = 0;
        int skippedCounter = 0;

        String keyPrefix = PathUtil.path("/pipeforce", config.getNamespace());

        for (JsonNode node : list) {

            // /pipeforce/NAMESPACE/global/app/...
            String key = node.get("key").textValue();

            String type = node.get("type").textValue();
            ContentType contentType = new ContentType(type);

            // /pipeforce/NAMESPACE/global/app... -> global/app...
            String relLocalPath = key.substring(keyPrefix.length());
            String ext = mimeTypeService.getFileExtensionForMimeType(type);
            relLocalPath = relLocalPath + ext;
            out.print("Get " + relLocalPath + " : ");

            // e.g. /Users/someUser/pipeforce/src/....
            String fullLocalPath = PathUtil.path(config.getHome(), localTargetFolderName, relLocalPath);

            long updated = node.get("updated").longValue();
            if (updated == 0) {
                updated = node.get("created").longValue();
            }

            filesCounter++;
            File localPropertyFile = new File(fullLocalPath);
            boolean questionAsked = false;
            if (localPropertyFile.exists()) {

                if (localPropertyFile.lastModified() == updated) {
                    out.println("skipped");
                    skippedCounter++;
                    continue; // Local file has same lastModified as remote file -> Do dont update, do not ask
                }

                int selection;
                if (rememberOverwriteAnswer != -1) {
                    selection = rememberOverwriteAnswer;
                } else {
                    // If lastModified differs: Ask user what to do with local file
                    List<String> items = ListUtil.asList("yes", "yes-all", "no", "no-all", "cancel");
                    out.println("File already exists. Overwrite?");
                    selection = in.choose(items, "no");
                    questionAsked = true;

                    if (selection == 1 || selection == 3) {
                        rememberOverwriteAnswer = selection; // Remember for next cycle
                    }
                }

                if (selection == 2) {
                    skippedCounter++;
                    continue; // Do not overwrite
                }

                if (selection == 3) {
                    skippedCounter++;
                    out.println("skipped");
                    continue;
                }

                if (selection == 4) {
                    skippedCounter++;
                    return; // Cancel the command
                }

                out.println("updated");
                updatedCounter++;
            } else {
                out.println("created");
                createdCounter++;
            }

            if ("base64".equals(contentType.getEncodingParameter())) {
                byte[] data = EncodeUtil.fromBase64ToBytes(node.get("value").textValue());
                out.saveByteArrayToFile(data, localPropertyFile);
            } else {
                byte[] data = node.get("value").textValue().getBytes(StandardCharsets.UTF_8);
                out.saveByteArrayToFile(data, localPropertyFile);
            }
            localPropertyFile.setLastModified(updated);

            publishService.add(fullLocalPath, updated);
        }

        publishService.save();

        out.println("Finished get of " + filesCounter + " files. " + createdCounter +
                " created. " + updatedCounter + " updated. " + skippedCounter + " skipped.");
    }

    public String getUsageHelp() {
        return "pi get <PROPERTY_KEY_PATTERN>\n" +
                "   Downloads all remote properties of the pattern into its local folder inside src.\n" +
                "   Examples:\n" +
                "     pi get global/app/myapp/** - Downloads all resources recursively.\n" +
                "     pi get global/app/myapp/* - Downloads all resources. Not recursively.\n" +
                "     pi get global/app/*/pipeline/* - Downloads all pipelines of all apps.\n" +
                "     pi get global/app/myapp/ - Short-cut of global/app/myapp/**.";
    }
}
