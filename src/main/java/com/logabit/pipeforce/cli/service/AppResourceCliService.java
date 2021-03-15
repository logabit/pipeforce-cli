package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.common.util.FileUtil;

public class AppResourceCliService extends BaseCliContextAware {

    /**
     * Returns true in case an app folder with given name exists inside the app folder.
     *
     * @param appName
     * @return
     */
    public boolean isAppExists(String appName) {
        return FileUtil.isFileExists(getContext().getConfigService().getHome(), "app", appName);
    }
}
