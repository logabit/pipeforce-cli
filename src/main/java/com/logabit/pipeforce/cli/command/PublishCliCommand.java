package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.content.model.ContentType;
import com.logabit.pipeforce.common.content.service.MimeTypeService;
import com.logabit.pipeforce.common.net.Request;
import com.logabit.pipeforce.common.util.EncodeUtil;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.logabit.pipeforce.common.property.IProperty.FIELD_PATH;

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

        if (!getContext().getInitService().isWorkDirInsideWorkspace()) {
            getContext().getOutputService().println("Publish must be executed in the root of a workspace folder.");
            return -1;
        }

        String path = args.getOptionKeyAt(0);

        // pi publish
        if (StringUtil.isEmpty(path)) {
            path = config.getWorkspaceConfig().getPropertiesHome() + "/**";
        }

        // pi publish myapp
        if (!path.contains("/") && !path.contains("\\")) {
            path = config.getWorkspaceConfig().getPropertiesHome() + "/global/app/" + path + "/**";
        }

        // In all other cases we expect: pi publish properties/global/...

        CliPathArg pathArg = getContext().createPathArg(path);

        out.println("Publish " + pathArg.getLocalPattern() + " ?");
        Integer choose = in.choose(ListUtil.asList("no", "yes"), "yes", null);
        if (choose == 0) {
            return 0;
        }

        // Publish even if already uploaded before?
        String forceString = args.getSwitch("force");
        boolean force = "true".equals(forceString);

        publish(pathArg, force);
        out.println("See your changes here: " + getContext().getCurrentInstance().getPortalUrl());

        return 0;
    }

    public void publish(CliPathArg pathArg, boolean force) throws IOException {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("file:" + pathArg.getLocalPattern());

        List<File> files = Arrays.stream(resources).map(r -> {
                    try {
                        return r.getFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).collect(Collectors.toList());

        publish(files.toArray(new File[]{}), force);
    }

    public void publish(File[] files, boolean force) throws IOException {

        PublishCliService publishService = getContext().getPublishService();
        MimeTypeService mimeTypeService = getContext().getMimeTypeService();

        String srcHome = PathUtil.path(context.getPropertiesHomeFolder());

        publishService.load();
        filesCounter = 0;
        publishedCounter = 0;
        updatedCounter = 0;
        createdCounter = 0;
        boolean deployWithExtension = config.getWorkspaceConfig().isDeployWithExtension();

        for (File file : files) {

            if ((!file.exists()) || file.isDirectory() || file.getName().startsWith(".")) {
                continue;
            }

            filesCounter++;
            String absoluteFilePath = file.getAbsolutePath();
            String propertyKey = file.getAbsolutePath().substring(srcHome.length());
            propertyKey = propertyKey.substring(1);

            // Replace any backslash to forward slash \ -> / (for windows)
            propertyKey = propertyKey.replaceAll("\\\\", "/");

            if (!deployWithExtension) {
                propertyKey = PathUtil.removeExtensions(propertyKey);
            }

            boolean appConfigValid = true;
            if (isAppConfigProperty(propertyKey)) {
                appConfigValid = validateAppConfig(file);
            }

            long lastModified = Files.getLastModifiedTime(file.toPath()).toMillis();
            if (!publishService.add(absoluteFilePath, lastModified) && appConfigValid) {
                if (!force) {
                    continue; // Ignore this resource since already in publish registry
                }
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

            Map schemaPutArgs = new HashMap();
            schemaPutArgs.put(FIELD_PATH, propertyKey);
            schemaPutArgs.put("type", type.toString());
            schemaPutArgs.put("existStrategy", "update");
            schemaPutArgs.put("evalValue", "false");
            schemaPutArgs.put("value", propertyValue);


            JsonNode node = getContext().getResolver().resolve(
                    Request.postParamsUrlEncoded()
                            .uri("$uri:command:property.schema.put").
                            params(schemaPutArgs),
                    JsonNode.class);


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
     * Is it a config property with path global/app/MYAPP/config/...
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
            appConfig = JsonUtil.jsonStringToMap(FileUtil.fileToString(appConfigFile));
        } catch (Exception e) {
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

        return "pi publish [--force:true|false] <PATH_PATTERN>\n" +
                "   Publishes all locally created/modified resources from inside properties to the server.\n" +
                "   <PATH_PATTERN> must point to resources inside the properties folder.\n" +
                "   Examples: \n" +
                "     pi publish - Publishes all resources inside properties folder.\n" +
                "     pi publish myapp - Publishes all app resources inside properties/global/app/myapp/**.\n" +
                "     pi publish properties/global/app/myapp/** - Publishes resources of myapp recursively.\n" +
                "     pi publish properties/global/app/myapp/ - Same as properties/global/app/myapp/**.\n" +
                "     pi publish properties/global/app/*/pipeline/* - Publishes all pipelines of all apps.";
    }

    private static class FileAndKey {

        private File file;

        private String key;
    }
}
