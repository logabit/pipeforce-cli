package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.common.util.FileUtil;

/**
 * Service to handle app resources.
 */
public class AppResourceCliService extends BaseCliContextAware {

    /**
     * Returns true in case an app folder with given name exists inside the app folder.
     *
     * @param appName
     * @return
     */
    public boolean isAppExists(String appName) {
        return FileUtil.isFileExists(getContext().getPropertiesHomeFolder().getAbsolutePath(), "global", "app", appName);
    }
}
