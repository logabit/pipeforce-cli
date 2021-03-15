package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PathUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the .published file inside of each app.
 *
 * @author sniederm
 * @since 6.0
 */
public class PublishCliService extends BaseCliContextAware {


    private Map publishedMap = new HashMap<>();

    /**
     * Loads the published map from the home folder.
     */
    public void load() {

        String publishedString = FileUtil.readFileToString(
                getContext().getConfigService().getHome(),
                ".published");

        this.publishedMap = JsonUtil.jsonStringToMap(publishedString);
    }

    /**
     * Saves the published map into the home folder.
     */
    public void save() {

        if (publishedMap == null) {
            return;
        }

        String json = JsonUtil.objectToJsonString(publishedMap);
        ConfigCliService configService = getContext().getConfigService();
        FileUtil.saveStringToFile(json, new File(PathUtil.path(configService.getHome(), ".published")));
    }

    /**
     * Adds a new entry to the published map. If such an entry already exists, doesnt add and returns false.
     *
     * @param path
     * @param lastModified in milliseconds since 1970
     * @return False in case the given entry already exists.
     */
    public boolean add(String path, long lastModified) {

        lastModified = lastModified / 1000;
        if (publishedMap.containsKey(path)) {
            Integer lastModifiedEntry = (Integer) publishedMap.get(path);
            if (lastModifiedEntry == lastModified) {
                return false;
            }
        }

        publishedMap.put(path, lastModified);
        return true;
    }

    public void remove(String targetPath) {

        if (publishedMap == null) {
            return;
        }

        publishedMap.remove(targetPath);
    }
}
