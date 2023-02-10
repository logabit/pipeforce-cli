package com.logabit.pipeforce.cli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.util.IOUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import com.logabit.pipeforce.common.util.VersionUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.OutputStream;

/**
 * Manages update checks, downloads and installations.
 *
 * @author sniederm
 * @since 7.0
 */
public class UpdateCliService extends BaseCliContextAware {

    /**
     * Returns the information of current and latest version.
     */
    public VersionInfo getVersionInfo() {

        /*
           For testing, set this VM property to the version to the latest "fake" version.
           In this case the version info is not downloaded from GitHub.
         */
        String releaseName = System.getProperty("LATEST_VERSION_TAG");
        String downloadUrl = "https://github.com/logabit/pipeforce-cli/releases/download/" + releaseName + "/pipeforce-cli.jar";

        if (StringUtil.isEmpty(releaseName)) {
            JsonNode gitHubInfo = downloadGitHubLatest();

            releaseName = gitHubInfo.get("name").textValue();

            String version = releaseName.substring(1);
            version = version.split("-")[0];

            ArrayNode assets = (ArrayNode) gitHubInfo.get("assets");

            for (JsonNode asset : assets) {

                if (asset.get("browser_download_url").textValue().endsWith("pipeforce-cli.jar")) {
                    downloadUrl = asset.get("browser_download_url").textValue();
                }
            }
        }

        String currentReleaseName = getContext().getConfigService().getReleaseTagFromJar();
        VersionInfo latestInfo = new VersionInfo(currentReleaseName, releaseName, downloadUrl);
        return latestInfo;
    }

    /**
     * Downloads and updates the local version by the given latest version.
     *
     * @param versionInfo
     */
    public void downloadAndUpdateVersion(VersionInfo versionInfo) {

        if (versionInfo.getCurrentReleaseTag().equals(versionInfo.getLatestReleaseTag())) {
            return; // Nothing to update
        }

        // Download the latest jar to the $USER_HOME/pipeforce/bin folder
        String downloadUrl = versionInfo.getLatestDownloadUrl();
        HttpResponse response = null;
        try {
            HttpGet downloadRequest = new HttpGet(downloadUrl);
            response = getContext().getHttpClient().execute(downloadRequest);
        } catch (Exception e) {
            throw new CliException("Download from [" + downloadUrl + "] failed: " + e.getMessage(), e);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new CliException("Download url not reachable [statusCode: " + statusCode + "]: " + downloadUrl);
        }

        ConfigCliService config = getContext().getConfigService();

        String downloadedJarPath = PathUtil.path(config.getInstallationHome(), "bin",
                "pipeforce-cli-" + versionInfo.getLatestReleaseTag() + ".jar");
        File downloadedJarFile = new File(downloadedJarPath);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (OutputStream os = getContext().getOutputService().createOutputStream(downloadedJarFile)) {
                IOUtil.copy(entity.getContent(), os);
            } catch (Exception e) {
                throw new CliException("Could not save download from [" + downloadUrl + "] to file [" +
                        downloadedJarFile + "]: " + e.getMessage(), e);
            }
        }

        getContext().getConfigService().setInstalledReleaseTag(versionInfo.getLatestReleaseTag());
        getContext().getConfigService().saveConfiguration();
        getContext().getInstallService().createPiScript();
    }

    protected JsonNode downloadGitHubLatest() {

        // Download the latest files.txt
        HttpResponse response = null;
        try {
            HttpGet downloadRequest = new HttpGet("https://api.github.com/repos/logabit/pipeforce-cli/releases/latest");
            response = getContext().getHttpClient().execute(downloadRequest);
        } catch (Exception e) {
            throw new CliException("Could not download latest info from GitHub: " + e.getMessage(), e);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new CliException("Could not download latest info from GitHub: Status code: " + statusCode);
        }

        HttpEntity entity = response.getEntity();
        try {
            String content = StringUtil.fromInputStream(entity.getContent());
            return JsonUtil.jsonStringToJsonNode(content);
        } catch (Exception e) {
            throw new CliException("Could not read latest info from response : " + e.getMessage(), e);
        }
    }

    /**
     * Represents current and latest version information.
     *
     * @author sniederm
     * @since 8.0
     */
    public static class VersionInfo {

        private final int[] currentVersion;
        private final int[] latestVersion;
        private Boolean newer;

        private String latestReleaseTag;
        private String latestDownloadUrl;
        private String currentReleaseTag;

        public VersionInfo(String currentReleaseNameTag, String latestReleaseTag, String latestDownloadUrl) {
            this.currentReleaseTag = currentReleaseNameTag;
            this.currentVersion = VersionUtil.toVersionArray(this.currentReleaseTag);
            this.latestReleaseTag = latestReleaseTag;
            this.latestVersion = VersionUtil.toVersionArray(this.latestReleaseTag);
            this.latestDownloadUrl = latestDownloadUrl;
        }

        public String getLatestReleaseTag() {
            return latestReleaseTag;
        }

        public int[] getLatestVersion() {
            return this.latestVersion;
        }

        public String getLatestDownloadUrl() {
            return latestDownloadUrl;
        }

        public int[] getCurrentVersion() {
            return this.currentVersion;
        }

        public String getCurrentReleaseTag() {
            return this.currentReleaseTag;
        }

        /**
         * Returns true in case this version is newer than the installed / current one.
         *
         * @return
         */
        public boolean isNewerVersionAvailable() {

            if (this.newer != null) {
                return this.newer;
            }

            int[] currentVersion = VersionUtil.toVersionArray(this.currentReleaseTag);
            int[] latestVersion = VersionUtil.toVersionArray(this.latestReleaseTag);
            this.newer = VersionUtil.givenNewerThanRequired(latestVersion, currentVersion);
            return this.newer;
        }
    }
}
