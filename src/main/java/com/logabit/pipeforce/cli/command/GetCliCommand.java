package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.command.stub.PropertyListParams;
import com.logabit.pipeforce.common.content.model.ContentType;
import com.logabit.pipeforce.common.content.service.MimeTypeService;
import com.logabit.pipeforce.common.net.ClientPipeforceURIResolver;
import com.logabit.pipeforce.common.property.IProperty;
import com.logabit.pipeforce.common.util.EncodeUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import com.logabit.pipeforce.common.util.VersionUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.logabit.pipeforce.common.util.VersionUtil.givenNewerOrEqualThanRequired;
import static com.logabit.pipeforce.common.util.VersionUtil.givenNewerThanRequired;

/**
 * Downloads a given set of properties.
 *
 * @author sniederm
 * @since 6.0
 */
public class GetCliCommand extends BaseCliCommand {

    private PublishCliService publishService;

    private ClientPipeforceURIResolver pi;

    private MimeTypeService mimeTypeService;

    private int rememberOverwriteAnswer = -1;

    private int filesCounter = 0;

    private int updatedCounter = 0;

    private int createdCounter = 0;

    private int skippedCounter = 0;

    private String pathPrefix;

    private int[] serverVersion;

    @Override
    public int call(CommandArgs args) {

        File targetFolder = getContext().getPropertiesHomeFolder();
        CliPathArg pathArg = getContext().createPathArg(args.getOptionKeyAt(0));

        boolean includeData = false;
        if (StringUtil.isEqual(args.getSwitches().get("includeData"), "true")) {
            includeData = true;
        }

        return execute(pathArg, targetFolder, includeData);
    }

    public int execute(CliPathArg pathArg, File targetFolder, boolean includeData) {

        String excludeDataPattern = "global/app/*/data/**";
        if (includeData) {
            excludeDataPattern = null;
        }

        publishService = getContext().getPublishService();
        publishService.load();
        pi = getContext().getResolver();
        mimeTypeService = getContext().getMimeTypeService();
        pathPrefix = PathUtil.path("/pipeforce", getContext().getCurrentInstance().getNamespace());
        serverVersion = getContext().getServerVersion();
        int[] requiredVersion = new int[]{10, 0, 2, 0};
        int offset = 0;
        ArrayNode list;

        int batchIndex = 1;
        do {

            list = pi.command(
                    new PropertyListParams()
                            .pattern(pathArg.getRemotePattern())
                            .excludePatterns(excludeDataPattern)
                            .offset(offset)
                            .limit(100),
                    ArrayNode.class
            );

            if (list == null) {
                break;
            }

            processReceivedProperties(list, targetFolder);
            offset = offset + list.size();

            if (list.size() > 0) {
                out.println("Batch: " + batchIndex + " | Fetched: " + list.size() + " | Overall: " + offset);
            }

            batchIndex = batchIndex + 1;

            // Server version < 10.0.2 -> No offset is supported
            if (!(givenNewerOrEqualThanRequired(serverVersion, requiredVersion))) {
                break;
            }

        } while (list.size() > 0);

        publishService.save();

        out.println("Finished get of " + filesCounter + " files. " + createdCounter +
                " created. " + updatedCounter + " updated. " + skippedCounter + " skipped.");

        return 0;
    }

    private void processReceivedProperties(ArrayNode list, File targetFolder) {

        for (JsonNode node : list) {

            // /pipeforce/NAMESPACE/global/app/...
            String path = node.get(IProperty.FIELD_PATH).textValue();

            String type = node.get("type").textValue();
            ContentType contentType = new ContentType(type);

            // /pipeforce/NAMESPACE/global/app... -> global/app...
            int prefixIndex = path.indexOf("global/app/");
            String relLocalPath = path.substring(prefixIndex);
            String ext = mimeTypeService.getFileExtensionForMimeType(type);
            relLocalPath = relLocalPath + ext;
            out.print("Get " + relLocalPath + " : ");

            // e.g. /Users/someUser/pipeforce/src/....
            String fullLocalPath = PathUtil.path(targetFolder.getAbsolutePath(), relLocalPath);

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
                String text = node.get("value").textValue();

                if (text != null) {
                    byte[] data = text.getBytes(StandardCharsets.UTF_8);
                    out.saveByteArrayToFile(data, localPropertyFile);
                }
            }
            localPropertyFile.setLastModified(updated);

            publishService.add(fullLocalPath, updated);
        }
    }

    public String getUsageHelp() {
        return "pi get [--includeData:true] <PROPERTY_PATH_PATTERN>\n" +
                "   Downloads all remote properties of the pattern into its local properties home folder.\n" +
                "   By default, app data is excluded since version >= 10.0.2.\n" +
                "   Examples:\n" +
                "     pi get global/app/myapp/** - Downloads all resources recursively.\n" +
                "     pi get global/app/myapp/* - Downloads all resources. Not recursively.\n" +
                "     pi get global/app/*/pipeline/* - Downloads all pipelines of all apps.\n" +
                "     pi get --includeData:true global/app/myapp/ - Short-cut of global/app/myapp/**.";
    }
}
