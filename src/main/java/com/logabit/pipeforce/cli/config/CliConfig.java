package com.logabit.pipeforce.cli.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.logabit.pipeforce.cli.service.InstallCliService;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;

import java.io.InputStream;

/**
 * Main configuration for the pi tool.
 *
 * @author sniederm
 * @since 7.0
 */
public class CliConfig {

    /**
     * The home folder of the PIPEFORCE workspace defaults to $USER_HOME/pipeforce.
     */
    private String home = PathUtil.path(System.getProperty("user.home"), "pipeforce");

    /**
     * The namespace of the instance to connect to.
     */
    private String namespace;

    /**
     * The hostname of the instance to connect to. Default is: pipeforce.net.
     */
    private String host = "pipeforce.net";

    /**
     * The port to connect to. Default is: 443.
     */
    private int port = 443;

    /**
     * The protocol to connect with. Default is: https.
     */
    private String protocol = "https";

    /**
     * The API path to be used. Default is: /api/v3.
     */
    private String apiPath = "/api/v3";

    /**
     * The username to connect with.
     */
    private String username;

    /**
     * The apitoken to use for authentication.
     */
    private String apiToken;

    /**
     * The date and time when this apitoken was created / updated last.
     */
    private String apiTokenCreated;

    /**
     * The date and time this configuration was created.
     */
    private String configCreated;

    /**
     * The date and time this configuration was updated last.
     */
    private String configUpdated;

    private String selectedApp;

    /**
     * Checks regularly for updates.
     */
    private boolean updateCheck = true;

    /**
     * The base url to check for new updates.
     */
    private String updateCheckUrl = "https://downloads.pipeforce.io";

    /**
     * The timestamp in milliseconds when the last check for a new version was made.
     */
    private long updateCheckLast;

    /**
     * The installed version of pipeforce-cli
     */
    private String installedVersion;

    private String serverVersion;
    private int serverVersionMajor;
    private int serverVersionMinor;
    private int serverVersionBugfix;
    private String serverEdition;
    private String serverStage;

    public String getSelectedApp() {
        return selectedApp;
    }

    public void setSelectedApp(String selectedApp) {
        this.selectedApp = selectedApp;
    }

    public String getHome() {
        return home;
    }

    @JsonIgnore
    public String getAppHome(String appName) {
        return PathUtil.path(getHome(), "src", "global", "app", appName);
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void setApiTokenCreated(String apiTokenCreated) {
        this.apiTokenCreated = apiTokenCreated;
    }

    public String getApiTokenCreated() {
        return apiTokenCreated;
    }

    public String getConfigCreated() {
        return configCreated;
    }

    public void setConfigCreated(String configCreated) {
        this.configCreated = configCreated;
    }

    public String getConfigUpdated() {
        return configUpdated;
    }

    public void setConfigUpdated(String configUpdated) {
        this.configUpdated = configUpdated;
    }

    public String getUpdateCheckUrl() {
        return updateCheckUrl;
    }

    public void setUpdateCheckUrl(String updateCheckUrl) {
        this.updateCheckUrl = updateCheckUrl;
    }

    public long getUpdateCheckLast() {
        return updateCheckLast;
    }

    public void setUpdateCheckLast(long updateCheckLast) {
        this.updateCheckLast = updateCheckLast;
    }

    public boolean isUpdateCheck() {
        return updateCheck;
    }

    public void setUpdateCheck(boolean updateCheck) {
        this.updateCheck = updateCheck;
    }

    public String getInstalledVersion() {

        if (!StringUtil.isEmpty(installedVersion)) {
            return installedVersion;
        }

        // This version is set by Maven build, so load it and write it to the CLI config for easier access
        InputStream is = InstallCliService.class.getClassLoader().getResourceAsStream("version.txt");

        if (is == null) {
            throw new IllegalStateException("Could not read from classpath:version.txt file!");
        }

        installedVersion = StringUtil.fromInputStream(is).trim();

        return installedVersion;
    }

    /**
     * Returns the installedVersion as an array of: major.minor.bugfix.
     * For example 1.0.3 would return the array [0]=1, [1]=0, [2]=3.
     *
     * @return
     */
    @JsonIgnore
    public int[] getInstalledVersionArray() {

        int[] versionArray = new int[]{0, 0, 0};
        String installedVersion = getInstalledVersion();
        if (installedVersion.equals("${project.version}")) {
            return new int[]{0, 0}; // We are running in DEV mode
        }

        String[] split = installedVersion.split("\\.");
        try {
            int counter = 0;
            for (String number : split) {

                versionArray[counter] = Integer.parseInt(number);
                counter++;
            }
            return versionArray;
        } catch (Exception e) {
            throw new RuntimeException("Cannot extract major version from 'installedVersion: " + installedVersion +
                    ": " + e.getMessage(), e);
        }
    }

    public void setInstalledVersion(String installedVersion) {
        this.installedVersion = installedVersion;
    }
}
