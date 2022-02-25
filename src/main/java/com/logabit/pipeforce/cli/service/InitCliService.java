package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.PathUtil;

/**
 * Service to init given folders as PIPEFORCE app repo folders.
 */
public class InitCliService extends BaseCliContextAware {

    public void init(String path) {

        FileUtil.createFolders(PathUtil.path(path, "pipeforce", "src"));
        createVSCodeWorkspaceFile(path);
    }

    public void createVSCodeWorkspaceFile(String path) {

        String apiUrl = getContext().getConfigService().getHubApiUrl("command");

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
                "               \"" + apiUrl + "/schema.pipeline\": [\"/*.pi.yaml\"]\n" +
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
}
