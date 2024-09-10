package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.command.stub.PropertyImportParams;
import com.logabit.pipeforce.common.content.model.ContentType;
import com.logabit.pipeforce.common.content.service.MimeTypeService;
import com.logabit.pipeforce.common.util.DateTimeUtil;
import com.logabit.pipeforce.common.util.EncodeUtil;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.FilenameUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import com.logabit.pipeforce.common.util.ThreadUtil;
import org.apache.commons.collections4.map.LinkedMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.logabit.pipeforce.common.property.IProperty.FIELD_PATH;
import static com.logabit.pipeforce.common.property.IProperty.FIELD_TYPE;
import static com.logabit.pipeforce.common.property.IProperty.FIELD_UUID;
import static com.logabit.pipeforce.common.property.IProperty.FIELD_VALUE;

/**
 * Imports a huge amount if properties.
 *
 * @author sniederm
 * @since 10.0.2
 */
public class ImportCliCommand extends BaseCliCommand {

    public static final String SWITCH_RECURSE = "recurse";

    public static final String SWITCH_STRATEGY = "strategy";

    public static final String SWITCH_REMOVE_UUID_FIELD = "removeUuidField";

    public static final String SWITCH_UUID_LOCATION = "uuidLocation"; // one of field, filename or null

    public static final String SWITCH_WAIT_BETWEEN = "waitBetween";

    public static final String SWITCH_BATCH_SIZE = "batchSize";

    @Override
    public int call(CommandArgs args) throws Exception {

        if (!getContext().getInitService().isWorkDirInsideWorkspace()) {
            getContext().getOutputService().println("Import must be executed in the root of a workspace folder.");
            return -1;
        }

        String existStrategy = "update";
        boolean recurse = false;

        LinkedMap<String, String> switches = args.getSwitches();
        if (switches.containsKey(SWITCH_STRATEGY)) {
            existStrategy = switches.get(SWITCH_STRATEGY);
        }

        if (switches.containsKey(SWITCH_RECURSE)) {
            recurse = switches.get(SWITCH_RECURSE).equals("true");
        }

        int waitBetween = 50;
        if (switches.containsKey(SWITCH_WAIT_BETWEEN)) {
            waitBetween = Integer.parseInt(switches.get(SWITCH_WAIT_BETWEEN));
        }

        int batchSize = 50;
        if (switches.containsKey(SWITCH_BATCH_SIZE)) {
            batchSize = Integer.parseInt(switches.get(SWITCH_BATCH_SIZE));
        }

        // Where is the uuid located? filename (without suffix)? For example: 00ac3c0a-40c1-4551-aeff-368ba449c51a.json
        // or field uuid?
        String uuidLocation = null;
        if (switches.containsKey(SWITCH_UUID_LOCATION)) {
            uuidLocation = switches.get(SWITCH_UUID_LOCATION);
        }

        // If file is a JSON and contains an uuid field -> Remove it before import?
        boolean removeUuidField = false;
        if (switches.containsKey(SWITCH_REMOVE_UUID_FIELD)) {
            removeUuidField = switches.get(SWITCH_REMOVE_UUID_FIELD).equals("true");
        }

        String path = args.getOptionKeyAt(0);

        // pi import
        if (StringUtil.isEmpty(path)) {
            path = config.getWorkspaceConfig().getPropertiesHome() + "/";
        }

        if (path.contains("*")) {
            getContext().getOutputService().println("Import path doesnt support wildcards.");
            return -1;
        }

        // pi import myapp
        if (!path.contains("/") && !path.contains("\\")) {
            path = config.getWorkspaceConfig().getPropertiesHome() + "/global/app/" + path + "/";
        }

        // In all other cases we expect: pi import properties/global/...

        CliPathArg pathArg = getContext().createPathArg(path);

        Path folderPath = Paths.get(pathArg.getLocalFile().getAbsolutePath());
        long overallFilesCounter = FileUtil.getNumberOfFilesInFolder(folderPath.toFile(), recurse);
        out.println("Import " + overallFilesCounter + " files from " + pathArg.getLocalFile() + "?");
        out.println("Config: strategy:" + existStrategy + ", uuidLocation:" + uuidLocation +
                ", removeUuidField:" + removeUuidField + ", recurse:" + recurse + ", waitBetween:" + waitBetween +
                ", batchSize:" + batchSize);
        out.println("Note: Import wont create any jobs or listeners!");
        Integer choose = in.choose(ListUtil.asList("no", "yes"), "yes", null);
        if (choose == 0) {
            return 0;
        }

        DateTimeUtil.Timer timer = DateTimeUtil.startTimer();
        processFilesInBatches(folderPath,
                batchSize, 0, overallFilesCounter, waitBetween, existStrategy, recurse, uuidLocation,
                removeUuidField);
        timer.stop();

        out.println("Done. Duration: " + timer.getTimeElapsed() + ". Overall files: " + overallFilesCounter + ".");

        return 0;
    }

    public long processFilesInBatches(Path folderPath, int batchSize, long alreadyProcessed, long overallFiles,
                                      int waitBetween, String existStrategy, boolean recurse, String uuidLocation,
                                      boolean removeUuidField) throws IOException {

        List<Path> foldersFound = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folderPath)) {
            List<File> batch = new ArrayList<>(batchSize);

            for (Path path : directoryStream) {
                if (Files.isRegularFile(path)) {  // Check if it's a file
                    batch.add(path.toFile());

                    // When the batch size is reached, process the batch
                    if (batch.size() == batchSize) {
                        alreadyProcessed = importProperties(batch, alreadyProcessed, overallFiles, existStrategy,
                                uuidLocation, removeUuidField);
                        batch.clear(); // Clear the batch for the next set

                        out.println("Imported " + alreadyProcessed + " files.");

                        if (waitBetween > 0) {
                            ThreadUtil.sleep(waitBetween); // Do not over-attack server
                        }
                    }
                } else if (Files.isDirectory(path)) {
                    foldersFound.add(path);
                }
            }

            // Process any remaining files in the last batch
            if (!batch.isEmpty()) {
                alreadyProcessed = importProperties(batch, alreadyProcessed, overallFiles, existStrategy,
                        uuidLocation, removeUuidField);
            }
        }

        // Process sub folders recursively
        if (recurse) {
            for (Path path : foldersFound) {
                alreadyProcessed = processFilesInBatches(path, batchSize, alreadyProcessed, overallFiles,
                        waitBetween, existStrategy, recurse, uuidLocation, removeUuidField);
            }
        }

        return alreadyProcessed;
    }

    public long importProperties(List<File> files, long alreadyProcessed, long overallFiles, String existStrategy,
                                 String uuidLocation, boolean removeUuidField) throws IOException {

        MimeTypeService mimeTypeService = getContext().getMimeTypeService();

        String srcHome = PathUtil.path(context.getPropertiesHomeFolder());

        boolean deployWithExtension = config.getWorkspaceConfig().isDeployWithExtension();

        List<Map> propertiesList = new ArrayList<>();

        for (File file : files) {

            if (!FileUtil.isRegularFile(file)) {
                continue;
            }

            String propertyPath = file.getAbsolutePath().substring(srcHome.length());
            propertyPath = propertyPath.substring(1);

            // Replace any backslash to forward slash \ -> / (for windows)
            propertyPath = propertyPath.replaceAll("\\\\", "/");

            if (!deployWithExtension) {
                propertyPath = PathUtil.removeExtensions(propertyPath);
            }

            String fileName = file.getName();
            String propertyType = mimeTypeService.detectMimeType(fileName);

            String propertyValue;
            ContentType type;

            if (mimeTypeService.isBinary(propertyType)) {
                type = new ContentType(propertyType + ";encoding=base64");
                propertyValue = EncodeUtil.toBase64(file);
            } else {
                type = new ContentType(propertyType);
                propertyValue = StringUtil.fromFile(file);
            }

            String uuid = null;
            if (uuidLocation != null) {

                if (uuidLocation.equals("filename")) {
                    uuid = FilenameUtil.removePathAndSuffix(fileName);
                }
            }

            if (propertyType.startsWith("application/json")) {

                JsonNode node = JsonUtil.jsonStringToJsonNode(propertyValue);

                if ((uuidLocation != null) && uuidLocation.equals("field")) {
                    uuid = node.get("uuid").textValue();
                }

                if (removeUuidField) {

                    ObjectNode objectNode = (ObjectNode) node;
                    objectNode.remove("uuid");
                    propertyValue = objectNode.toPrettyString();
                }
            }

            Map propertyParams = new HashMap();
            propertyParams.put(FIELD_PATH, propertyPath);
            propertyParams.put(FIELD_TYPE, type.toString());
            propertyParams.put(FIELD_VALUE, propertyValue);

            if (uuid != null) {
                propertyParams.put(FIELD_UUID, uuid);
            }

            propertiesList.add(propertyParams);

            alreadyProcessed = alreadyProcessed + 1;
            out.println("Importing " + alreadyProcessed + "/" + overallFiles + ": " + propertyPath);
        }

        if (propertiesList.size() > 0) {
            getContext().getResolver().command(
                    new PropertyImportParams()
                            .strategy(existStrategy)
                            .setBody(propertiesList));
        }

        return alreadyProcessed;
    }

    public String getUsageHelp() {

        return "pi import [-existStrategy:update|skip|error] [-uuidLocation:filename|field] [-removeUuidField:true] " +
                "[-batchSize:50] [-waitBetween:50] <PATH>\n" +
                "   Imports all files at given path as properties, recursively.\n" +
                "   Default exist strategy is update.\n" +
                "   This command is for batch import of huge amount of files.\n" +
                "   At server side no events will be fired on import = no jobs or listeners will be created.\n" +
                "   <PATH> is optional and must point to files inside the properties folder.\n" +
                "   Examples: \n" +
                "     pi import - Imports all files inside properties folder recursively.\n" +
                "     pi import -existStrategy:skip myapp - Imports all app files, skips existing files.\n" +
                "     pi import properties/global/app/myapp/data/ - Imports all data files.";
    }
}
