package com.logabit.pipeforce.cli.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.common.util.DateTimeUtil;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import org.springframework.beans.BeanUtils;

import java.io.File;

/**
 * Manages the CLI configuration.
 *
 * @author sniederm
 * @since 6.0
 */
public class ConfigCliService extends CliConfig {

    /**
     * Loads the existing configuration file. If no such file exists so far, nothing happens.
     */
    public void loadConfiguration() {
        String path = getConfigFilePath();
        File configFile = new File(path);

        if (!configFile.exists()) {
            return;
        }

        try {
            String json = FileUtil.fileToString(configFile);
            CliConfig loadedConfig = JsonUtil.jsonStringToObject(json, CliConfig.class);

            // TODO BeanUtils takes about 200ms -> too much. replace by other approach
            BeanUtils.copyProperties(loadedConfig, this);

        } catch (Exception e) {
            throw new RuntimeException("Could not load configuration: " + path, e);
        }
    }

    @JsonIgnore
    public boolean isConfigExists() {
        return this.getConfigCreated() != null;
    }

    public void saveConfiguration() {

        if (getConfigCreated() == null) {
            setConfigCreated(DateTimeUtil.currentDateTimeAsIso8061());
        } else {
            setConfigUpdated(DateTimeUtil.currentDateTimeAsIso8061());
        }

        String json = JsonUtil.objectToJsonNode(this).toPrettyString();

        File configFile = null;
        configFile = FileUtil.getOrCreateFile(getConfigFilePath());
        FileUtil.saveStringToFile(json, configFile);
    }

    @JsonIgnore
    public String getConfigFilePath() {
        return PathUtil.path(System.getProperty("user.home"), "pipeforce/pipeforce-cli/conf/cli.config.json");
    }
}
