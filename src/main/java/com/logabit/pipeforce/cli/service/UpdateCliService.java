package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages update checks, downloads and installations.
 *
 * @author sniederm
 * @since 7.0
 */
public class UpdateCliService extends BaseCliContextAware {

    /**
     * Downloads the files.txt from the configured server url.
     *
     * @return
     */
    public List<String> downloadFilesList() {

        ConfigCliService config = getContext().getConfigService();

        String filesListUrl = PathUtil.path(config.getUpdateCheckUrl(), "pipeforce-cli", "files.txt");

        // Download the latest files.txt
        HttpResponse response = null;
        try {
            HttpGet downloadRequest = new HttpGet(filesListUrl);
            response = getContext().getHttpClient().execute(downloadRequest);
        } catch (Exception e) {
            throw new CliException("Download files list from [" + filesListUrl + "] failed: " + e.getMessage(), e);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new CliException("Download files list failed. Url not reachable [statusCode: " + statusCode + "]: " + filesListUrl);
        }

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                String versions = StringUtil.fromInputStream(entity.getContent());
                return StringUtil.splitToList(versions, "\n");
            } catch (Exception e) {
                throw new CliException("Could not read files list from " + filesListUrl + " : " + e.getMessage(), e);
            }
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Checks if a never version for the given version is available.
     *
     * @param currentVersion
     * @return The newer available version or null in case no newer version is available
     */
    public String isNewerVersionAvailable(String currentVersion) {

        List<String> files = downloadFilesList();
        List<ComparableVersion> versionsList = new ArrayList<>();

        for (String filename : files) {
            String v = filename.substring("pipeforce-cli-".length(), filename.length() - ".jar".length());

            if (v.contains("-")) {
                continue; // Ignore snapshot versions for auto-updates (e.g.: 1.0-MS1)
            }

            ComparableVersion version = new ComparableVersion(v);
            versionsList.add(version);
        }

        List<ComparableVersion> sortedVersions = versionsList.stream().sorted().collect(Collectors.toList());
        ComparableVersion thisVersion = new ComparableVersion(currentVersion);
        ComparableVersion latestVersion = ListUtil.lastElement(sortedVersions);

        if (latestVersion.compareTo(thisVersion) > 0) {
            return latestVersion.toString(); // Latest version
        }

        return null; // No new version found
    }

    /**
     * Constructs the download url for the given artifact version.
     *
     * @param version
     * @return
     */
    public String getDownloadUrl(String version) {

        ConfigCliService config = getContext().getConfigService();

        // Example URL: https://download.pipeforce.io/pipeforce-cli/pipeforce-cli-8.0.jar
        return PathUtil.path(config.getUpdateCheckUrl(), "pipeforce-cli", "pipeforce-cli-" + version + ".jar");
    }

    /**
     * Downloads and installs the exact given version.
     *
     * @param version
     */
    public void downloadAndInstallVersion(String version) {

        // Download the latest jar to the $USER_HOME/pipeforce/tools folder
        String downloadUrl = getDownloadUrl(version);
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

        File updateJarFile = new File(PathUtil.path(config.getHome(), "tool",
                "pipeforce-cli-" + version + ".jar"));

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (OutputStream os = getContext().getOutputService().createOutputStream(updateJarFile)) {
                entity.writeTo(os);
            } catch (Exception e) {
                throw new CliException("Could not save download from [" + downloadUrl + "] to file [" +
                        updateJarFile + "]: " + e.getMessage(), e);
            }
        }

        config.setInstalledVersion(version);
        config.saveConfiguration();
        getContext().getInstallService().createPiScript(); // Let the pi script point to the latest version
    }
}
