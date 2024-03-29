package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.common.model.WorkspaceConfig;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PathUtil;

import java.io.File;

/**
 * Service to init given folders as PIPEFORCE app repo folders.
 */
public class InitCliService extends BaseCliContextAware {

    public static final String FOLDER_NAME_PIPEFORCE = ".pipeforce";

    public static final String FOLDER_NAME_GLOBAL = "global";

    public static final String FOLDER_NAME_APP = "app";

    public void init(String path) {

        FileUtil.createFolders(PathUtil.path(path, getContext().getConfigService().getWorkspaceConfig().getPropertiesHome(),
                FOLDER_NAME_GLOBAL, FOLDER_NAME_APP));
        FileUtil.createFolders(PathUtil.path(path, FOLDER_NAME_PIPEFORCE));
        createVSCodeWorkspaceFile(path);
        createPipeforceConfigYamlFile(path);
    }

    /**
     * Checks if current work dir is inside a valid workspace folder.
     *
     * @return
     */
    public boolean isWorkDirInsideWorkspace() {

        // Make sure publish is executed always inside init folder for security reasons
        File workDir = getContext().getCurrentWorkDir();
        File pipeforceFolder = new File(workDir, ".pipeforce");
        File srcFolder = new File(workDir, getContext().getConfigService().getWorkspaceConfig().getPropertiesHome());
        return (pipeforceFolder.exists() && srcFolder.exists());
    }

    public void createVSCodeWorkspaceFile(String path) {

        String apiUrl = getContext().getCurrentInstance().getHubApiUrl("command");

        String vsCodeWorkspace = "" +
                "{\n" +
                "        \"folders\": [\n" +
                "           {\n" +
                "               \"path\":\".\"\n" +
                "           }\n" +
                "        ],\n" +
                "\n" +
                "        \"settings\": {\n" +
                "\n" +
                "            \"yaml.schemas\": {\n" +
                "               \"" + apiUrl + "/pipeline.schema.get\": [\"/*.pi.yaml\"]\n" +
                "            },\n" +
                "\n" +
                "           \"files.exclude\": {\n" +
                "               \"**/PIPEFORCE.code-workspace\": true\n" +
                "           },\n" +
                "\n" +
                "           \"files.encoding\": \"utf8\"\n" +
                "        }\n" +
                "}\n";

        String vsCodeConfig = PathUtil.path(path, "PIPEFORCE.code-workspace");
        FileUtil.saveStringToFile(vsCodeWorkspace, FileUtil.getOrCreateFile(vsCodeConfig));
    }

    public void createPipeforceConfigYamlFile(String path) {

        WorkspaceConfig config = new WorkspaceConfig();
        String configPath = PathUtil.path(path, WorkspaceConfig.DEFAULT_LOCATION);
        FileUtil.saveStringToFile(JsonUtil.objectToJsonString(config), FileUtil.getOrCreateFile(configPath));
    }
}
