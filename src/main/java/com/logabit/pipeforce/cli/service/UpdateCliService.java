package com.logabit.pipeforce.cli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.util.IOUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
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

        JsonNode gitHubInfo = downloadGitHubLatest();

        String releaseName = gitHubInfo.get("name").textValue();

        String version = releaseName.substring(1);
        version = version.split("-")[0];
        String downloadUrl = "";

        ArrayNode assets = (ArrayNode) gitHubInfo.get("assets");

        for (JsonNode asset : assets) {

            if (asset.get("browser_download_url").textValue().endsWith("pipeforce-cli.jar")) {
                downloadUrl = asset.get("browser_download_url").textValue();
            }
        }

        String currentReleaseName = getContext().getConfigService().getInstalledReleaseName();
        VersionInfo latestInfo = new VersionInfo(currentReleaseName, releaseName, downloadUrl);
        return latestInfo;
    }

    /**
     * Downloads and updates the local version by the given latest version.
     *
     * @param versionInfo
     */
    public void downloadAndUpdateVersion(VersionInfo versionInfo) {

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

        String downloadedJarPath = PathUtil.path(config.getInstallationHome(), "pipeforce-cli", "bin", "pipeforce-cli-downloaded.jar");
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

        String installedJarPath = PathUtil.path(config.getInstallationHome(), "pipeforce-cli", "bin", "pipeforce-cli.jar");
        File installedJarFile = new File(installedJarPath);
        getContext().getOutputService().moveFile(downloadedJarFile, installedJarFile);
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

        private Boolean newer;

        private String latestReleaseName;
        private String latestVersion;
        private String latestDownloadUrl;

        private String currentReleaseName;
        private String currentVersion;

        public VersionInfo(String currentReleaseName, String latestReleaseName, String latestDownloadUrl) {
            this.currentReleaseName = currentReleaseName;
            this.latestReleaseName = latestReleaseName;
            this.latestDownloadUrl = latestDownloadUrl;
        }

        public String getLatestReleaseName() {
            return latestReleaseName;
        }

        public String getLatestVersion() {

            if (this.latestVersion != null) {
                return this.latestVersion;
            }

            String releaseName = getLatestReleaseName();
            this.latestVersion = toPlainVersion(releaseName);
            return this.latestVersion;
        }

        public String getLatestDownloadUrl() {
            return latestDownloadUrl;
        }

        public String getCurrentVersion() {

            if (this.currentVersion != null) {
                return this.currentVersion;
            }

            String releaseName = getCurrentReleaseName();
            this.currentVersion = toPlainVersion(releaseName);
            return this.currentVersion;
        }

        public String getCurrentReleaseName() {
            return this.currentReleaseName;
        }

        /**
         * Returns true in case this version is newer than the given one.
         *
         * @param givenVersion
         * @return
         */
        public boolean isNewer(String givenVersion) {

            String[] latestVersionSplit = getLatestVersion().split("\\.");
            String[] givenVersionSplit = givenVersion.split("\\.");

            int latestMajor = Integer.parseInt(latestVersionSplit[0]);
            int givenMajor = Integer.parseInt(givenVersionSplit[0]);
            if (latestMajor != givenMajor) {
                return (latestMajor > givenMajor);
            }

            int latestMinor = Integer.parseInt(latestVersionSplit[1]);
            int givenMinor = Integer.parseInt(givenVersionSplit[1]);
            if (latestMinor != givenMinor) {
                return (latestMinor > givenMinor);
            }

            int latestPatch = Integer.parseInt(latestVersionSplit[2]);
            int givenPatch = Integer.parseInt(givenVersionSplit[2]);
            if (latestPatch != givenPatch) {
                return (latestPatch > givenPatch);
            }

            return false;
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

            String version = toPlainVersion(this.currentReleaseName);
            this.newer = this.isNewer(version);
            return this.newer;
        }

        /**
         * Converts from prefixed/suffixed release name to plain version: v1.2.3-RELEASE -> 1.2.3
         *
         * @param releaseName
         * @return
         */
        private String toPlainVersion(String releaseName) {


            // Strip off v
            if (!StringUtil.isNumeric(releaseName.charAt(0) + "")) {
                releaseName = releaseName.substring(1);
            }

            // Strip off -RELEASE or similar
            if (releaseName.contains("-")) {
                releaseName = releaseName.split("-")[0];
            }

            return releaseName;
        }
    }
}
