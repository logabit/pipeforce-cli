package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the published.log.json file inside of each app repo.
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

        String publishedString = FileUtil.fileToString(
                new File(getContext().getHiddenPipeforceFolder(), "published.log.json")
        );

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
        FileUtil.saveStringToFile(json, new File(getContext().getHiddenPipeforceFolder(), "published.log.json"));
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
            Long lastModifiedEntry = Long.valueOf(publishedMap.get(path) + "");
            if (lastModifiedEntry == lastModified) {
                return false;
            }
        }

        publishedMap.put(path, lastModified);
        return true;
    }

    /**
     * Removes concrete path entries.
     *
     * @param targetPath
     */
    public void remove(String targetPath) {

        if (publishedMap == null) {
            return;
        }

        publishedMap.remove(targetPath);
    }

    /**
     * Removes all entries of a given folder (= path prefix). So a targetPath of /my/path/ would remove everything
     * inside the folder /my/path/.
     */
    public void removeFolder(String targetPath) {

        publishedMap.keySet().removeIf(k -> (k + "").startsWith(targetPath));
    }

    public Map getPublishedMap() {
        return Collections.unmodifiableMap(publishedMap);
    }
}
