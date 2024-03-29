package com.logabit.pipeforce.cli.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.logabit.pipeforce.cli.service.InstallCliService;
import com.logabit.pipeforce.cli.service.UpdateCliService;
import com.logabit.pipeforce.common.model.WorkspaceConfig;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Main configuration for the pi tool.
 *
 * @author sniederm
 * @since 7.0
 */
@JsonIgnoreProperties
public class CliConfig {

    /**
     * The home folder of the PIPEFORCE installation, defaults to $USER_HOME/pipeforce/pipeforce-cli.
     */
    private String installationHome = PathUtil.path(System.getProperty("user.home"), "pipeforce", "pipeforce-cli");

    /**
     * The date and time this configuration was created.
     */
    private String configCreated;

    /**
     * The date and time this configuration was updated last.
     */
    private String configUpdated;

    /**
     * Checks regularly for updates.
     */
    private boolean updateCheck = true;

    /**
     * The timestamp in milliseconds when the last check for a new version was made.
     */
    private long updateCheckLast;

    /**
     * The available namespace instances. Key = namespace.host
     */
    private List<Instance> instances = new ArrayList<>();

    /**
     * Contains the url of the default instance.
     */
    private String defaultInstance;

    private String installedReleaseTag;

    private String releaseTagFromJar;

    /**
     * Contains all workspace related configurations parsed from file .pipeforce/config.json
     */
    private WorkspaceConfig workspaceConfig = new WorkspaceConfig();

    /**
     * The default instance key as: namespace.host
     *
     * @return
     */
    public String getDefaultInstance() {
        return defaultInstance;
    }

    public void setDefaultInstance(String defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public String getInstallationHome() {
        return installationHome;
    }

    public void setInstallationHome(String home) {
        this.installationHome = home;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    /**
     * Expects the fully qualified instance name like namespace.domain.tld (for example dev.pipeforce.org)
     * and returns the instance config for this if exists. Otherwise, returns null.
     *
     * @param name
     * @return
     */
    @JsonIgnore
    public Instance getInstanceByQualifiedName(String name) {

        List<Instance> instances = getInstances();

        for (Instance instance : instances) {
            if (instance.getName().equals(name)) {
                return instance;
            }
        }

        return null;
    }

    /**
     * Returns the instance configuration with given namespace and host.
     * If no such instance exists, returns null.
     *
     * @param namespace
     * @param host
     * @return
     */
    @JsonIgnore
    public Instance getInstanceByNamespaceAndHost(String namespace, String host) {

        List<Instance> instances = getInstances();

        for (Instance instance : instances) {
            if (instance.getNamespace().equals(namespace) && instance.getHost().equals(host)) {
                return instance;
            }
        }

        return null;
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

    @JsonIgnore
    public String getReleaseTagFromJar() {

        // For testing purposes only
        if (this.releaseTagFromJar != null) {
            return this.releaseTagFromJar;
        }

        // For testing purposes. Set for example -DCURRENT_VERSION_TAG=v.1.2.3-RELEASE
        String currentVersion = System.getProperty("CURRENT_VERSION_TAG");
        if (!StringUtil.isEmpty(currentVersion)) {
            return currentVersion;
        }

        // This version is set by Maven build, so load it and write it to the CLI config for easier access
        InputStream is = InstallCliService.class.getClassLoader().getResourceAsStream("version.txt");

        if (is == null) {
            throw new IllegalStateException("Could not read from classpath:version.txt file!");
        }

        return StringUtil.fromInputStream(is).trim();
    }

    @JsonIgnore
    public String getInstalledJarPath() {
        return PathUtil.path(getInstallationHome(), "bin", "pipeforce-cli-" + getInstalledReleaseTag() + ".jar");
    }

    /**
     * @param installedReleaseTag
     */
    public void setInstalledReleaseTag(String installedReleaseTag) {
        this.installedReleaseTag = installedReleaseTag;
    }

    public String getInstalledReleaseTag() {

        if (this.installedReleaseTag != null) {
            return this.installedReleaseTag;
        }

        UpdateCliService.VersionInfo versionInfo = new UpdateCliService.VersionInfo(
                getReleaseTagFromJar(), null, null);

        return versionInfo.getCurrentReleaseTag();
    }

    public void setWorkspaceConfig(WorkspaceConfig workspaceConfig) {
        this.workspaceConfig = workspaceConfig;
    }

    public WorkspaceConfig getWorkspaceConfig() {
        return workspaceConfig;
    }

    /**
     * Represents a single instance config entry.
     */
    public static class Instance {

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

        public String getApiTokenCreated() {
            return apiTokenCreated;
        }

        public void setApiTokenCreated(String apiTokenCreated) {
            this.apiTokenCreated = apiTokenCreated;
        }

        @JsonIgnore
        public String getName() {
            return this.namespace + "." + this.host;
        }

        @JsonIgnore
        public String getPortalUrl() {
            return PathUtil.path("https://" + getNamespace() + "." + getHost());
        }

        @JsonIgnore
        public String getAppUrl(String appName) {
            return PathUtil.path(getPortalUrl(), "#/app?name=" + appName);
        }

        @JsonIgnore
        public String getHubApiUrl(String path) {
            return PathUtil.path(getProtocol() + "://hub-" + getNamespace() + "." + getHost() + ":" + getPort(), getApiPath(), path);
        }

        @Override
        public String toString() {
            return getName() + " [protocol: " + getProtocol() + ", port: " + getPort() + ", username: " + getUsername() + "]";
        }
    }
}
