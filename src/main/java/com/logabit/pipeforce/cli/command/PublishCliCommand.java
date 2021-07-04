package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.content.model.ContentType;
import com.logabit.pipeforce.common.content.service.MimeTypeService;
import com.logabit.pipeforce.common.util.EncodeUtil;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * Publishes all resources of a given app to the server.
 *
 * @author sniederm
 * @since 7.0
 */
public class PublishCliCommand extends BaseCliCommand {

    private int filesCounter = 0;
    private int publishedCounter = 0;
    private int updatedCounter = 0;
    private int createdCounter = 0;

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String path = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(path)) {
            path = "**";
        }

        CliPathArg pathArg = getContext().createPathArg(path);

        out.println("Publish " + pathArg.getLocalPattern() + " ?");
        Integer choose = in.choose(ListUtil.asList("no", "yes"), "yes", null);
        if (choose == 0) {
            return 0;
        }

        PublishCliService publishService = getContext().getPublishService();
        MimeTypeService mimeTypeService = getContext().getMimeTypeService();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("file:" + pathArg.getLocalPattern());

        String srcHome = PathUtil.path(config.getHome(), "src");

        publishService.load();
        int serverVersionMajor = getContext().getSeverVersionMajor();
        filesCounter = 0;
        publishedCounter = 0;
        updatedCounter = 0;
        createdCounter = 0;

        for (Resource resource : resources) {

            File file = resource.getFile();

            if ((!file.exists()) || file.isDirectory() || file.getName().startsWith(".")) {
                continue;
            }

            filesCounter++;
            String absoluteFilePath = file.getAbsolutePath();
            String propertyKey = file.getAbsolutePath().substring(srcHome.length());
            propertyKey = propertyKey.substring(1);
            propertyKey = PathUtil.removeExtensions(propertyKey);
            // Replace any backslash to forward slash \ -> / (for windows)
            propertyKey = propertyKey.replaceAll("\\\\", "/");

            boolean appConfigValid = true;
            FileAndKey fileAndKey = null;

            if (isAppConfigProperty(propertyKey)) {
                fileAndKey = checkAppConfig(propertyKey, file, serverVersionMajor);
                if (fileAndKey != null) {
                    // Property file and key has changed to global/app/APPNAME/config/app.json
                    propertyKey = fileAndKey.key;
                    file = fileAndKey.file;
                }

                appConfigValid = validateAppConfig(file);
            }

            long lastModified = Files.getLastModifiedTime(file.toPath()).toMillis();
            if (!publishService.add(absoluteFilePath, lastModified) && fileAndKey == null && appConfigValid) {
                continue;
            }

            String propertyType = mimeTypeService.detectMimeType(file.getName());

            out.print("Publishing " + propertyKey + " : ");

            String propertyValue;
            ContentType type;

            if (mimeTypeService.isBinary(propertyType)) {
                type = new ContentType(propertyType + ";encoding=base64");
                propertyValue = EncodeUtil.toBase64(file);
            } else {
                type = new ContentType(propertyType);
                propertyValue = StringUtil.fromFile(file);
            }

            ObjectNode pipelineArgs = JsonUtil.createObjectNode();
            pipelineArgs.put("key", propertyKey);
            pipelineArgs.put("type", type.toString());
            pipelineArgs.put("existStrategy", "update");
            pipelineArgs.put("evalValue", "false");
            pipelineArgs.put("value", propertyValue);

            ObjectNode schemaPutCommand = JsonUtil.createObjectNode();
            schemaPutCommand.set("property.schema.put", pipelineArgs);

            ArrayNode pipelineArray = JsonUtil.createArrayNode();
            pipelineArray.add(schemaPutCommand);

            ObjectNode pipelineJson = JsonUtil.createObjectNode();
            pipelineJson.set("pipeline", pipelineArray);

            JsonNode node = (JsonNode) getContext().getPipelineRunner().executePipelineJsonNode(pipelineJson);

            String action = node.get("result").textValue();

            if (action.equals("create")) {
                createdCounter++;
            } else {
                updatedCounter++;
            }

            publishedCounter++;

            out.println(action);
        }

        publishService.save();

        out.println("Found " + filesCounter + " files. " + publishedCounter +
                " published. " + updatedCounter + " updated. " + createdCounter + " created.");
        out.println("See your changes here: " + config.getPortalUrl());

        return 0;
    }

    public int getFilesCounter() {
        return filesCounter;
    }

    public int getPublishedCounter() {
        return publishedCounter;
    }

    public int getUpdatedCounter() {
        return updatedCounter;
    }

    public int getCreatedCounter() {
        return createdCounter;
    }

    /**
     * Since in version 7.0 the app config has changed from global/app/APPNAME/config/APPNAME.json
     * to global/app/APPNAME/config/app.json we migrate these here in case the target server is >= 7.0.
     */
    private FileAndKey checkAppConfig(String propertyKey, File propertyFile, int serverVersionMajor) {

        if (!isAppConfigProperty(propertyKey)) {
            return null;
        }

        propertyKey = StringUtil.removePrefix("/", propertyKey);
        String[] split = StringUtil.split(propertyKey, "/");
        String appName = split[2];

        if (serverVersionMajor <= 6) {
            if (!propertyKey.endsWith(appName)) {
                throw new CliException("Folder conf may not contain any other files except the app config " +
                        "global/app/" + appName + "/config/" + appName + ".json but contains: " + propertyKey +
                        ". Please move this file to another location and try again to publish.");
            }

            return null; // All OK. We are on server version 6.0 and this is the app config file as expected
        }

        File newPropertyFile = new File(propertyFile.getParentFile(), "app.json");

        if (newPropertyFile.exists()) {
            return null;
        }

        out.println("In server version >= 7.0, the default app config file was renamed to app.json. " +
                "Should I rename " + propertyFile.getAbsolutePath() + " to " + newPropertyFile.getAbsolutePath() +
                " for you? Note: If you choose 'no', your app might not work correctly!");

        Integer choose = in.choose(ListUtil.asList("no", "yes"), "yes", null);

        if (choose == 1) {
            try {
                FileUtils.moveFile(propertyFile, newPropertyFile);
            } catch (IOException e) {
                throw new CliException(String.format("failed to move: %s to: %s", propertyFile, newPropertyFile), e);
            }

            FileAndKey fileAndKey = new FileAndKey();
            fileAndKey.file = newPropertyFile;
            fileAndKey.key = "global/app/" + appName + "/config/app";
            return fileAndKey;
        }

        return null;
    }

    /**
     * Is it a config property with key path global/app/MYAPP/config/...
     *
     * @param propertyKey
     * @return
     */
    private boolean isAppConfigProperty(String propertyKey) {

        propertyKey = StringUtil.removePrefix("/", propertyKey);
        String[] split = StringUtil.split(propertyKey, "/");

        // global/app/APPNAME/config/CONFIGNAME = 5
        if (split.length < 5) {
            return false;
        }

        if (!"config".equals(split[3])) {
            return false; // Not a config property
        }

        return true;
    }

    /**
     * Validates the app config before it gets published.
     * Checks if the value of "show" attribute in the config contains upper case.
     * If not, converts to upper case.
     * This is the right place to add additional validation / update rules.
     *
     * @param appConfigFile
     * @return True if the file is valid and nothing has changed. If false is returned, something could have changed.
     */
    private boolean validateAppConfig(File appConfigFile) {

        Map appConfig = null;
        try {
            appConfig = JsonUtil.jsonStringToMap(FileUtil.readFileToString(appConfigFile));
        } catch (IOException e) {
            throw new CliException("Could not read file: " + appConfigFile + ": " + e.getMessage(), e);
        }

        String show = (String) appConfig.get("show");

        if (StringUtil.isEmpty(show)) {
            // Config contains no show entry -> valid, since attribute could be removed in future
            return true;
        }

        String showUpper = show.toUpperCase();

        if (showUpper.equals(show)) {
            // Do not change file in case nothing has changed -> Not dirty
            return true;
        }

        appConfig.put("show", showUpper);

        String data = JsonUtil.objectToJsonString(appConfig);
        FileUtil.saveStringToFile(data, appConfigFile);
        return false;
    }

    public String getUsageHelp() {

        return "pi publish <PATH_PATTERN>\n" +
                "   Publishes all locally created/modified resources from inside src to the server.\n" +
                "   <PATH_PATTERN> must be relative to src folder. Absolute path not allowed.\n" +
                "   Examples: \n" +
                "     pi publish global/app/myapp/** - Publishes content of myapp recursively.\n" +
                "     pi publish global/app/myapp/ - Short-cut of global/app/myapp/**.\n" +
                "     pi publish global/app/*/pipeline/* - Publishes all pipelines of all apps.";
    }

    private static class FileAndKey {
        private File file;
        private String key;
    }
}
