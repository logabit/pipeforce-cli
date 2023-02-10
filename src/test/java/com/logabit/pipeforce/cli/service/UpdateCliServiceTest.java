package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCliServiceTest {

    @InjectMocks
    private final CliContext cliContext = new CliContext();

    @Mock
    private ConfigCliService configService;

    @Mock
    private OutputCliService outputService;

    @Mock
    private HttpClient httpClient;

    @Test
    public void testLatestVersionInfo() {

        UpdateCliService.VersionInfo version = new UpdateCliService.VersionInfo(
                "v7.0.0-RC12", "v8.5.5-RELEASE", "http://someUrl");

        Assert.assertArrayEquals(new int[]{8, 5, 5, 0}, version.getLatestVersion());
        Assert.assertArrayEquals(new int[]{7, 0, 0, 0}, version.getCurrentVersion());
        Assert.assertTrue(version.isNewerVersionAvailable());

        version = new UpdateCliService.VersionInfo(
                "v7.0.0-b42-RC12", "v8.5.5-RELEASE", "http://someUrl");

        Assert.assertArrayEquals(new int[]{8, 5, 5, 0}, version.getLatestVersion());
        Assert.assertArrayEquals(new int[]{7, 0, 0, 42}, version.getCurrentVersion());
        Assert.assertTrue(version.isNewerVersionAvailable());

        version = new UpdateCliService.VersionInfo(
                "v9.0.0-RC12", "v8.5.5-RELEASE", "http://someUrl");

        Assert.assertArrayEquals(new int[]{8, 5, 5, 0}, version.getLatestVersion());
        Assert.assertArrayEquals(new int[]{9, 0, 0, 0}, version.getCurrentVersion());
        Assert.assertFalse(version.isNewerVersionAvailable());
    }

    @Test
    public void testGetVersionInfo() throws IOException {

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(FileUtil.fileToInputStream(new File("src/test/resources/github.latest.test.json")));
        when(httpResponse.getEntity()).thenReturn(entity);

        when(configService.getReleaseTagFromJar()).thenReturn("v7.9.9-RC1"); // This is the currently installed version

        UpdateCliService updateCliService = new UpdateCliService();
        updateCliService.setContext(cliContext);

        UpdateCliService.VersionInfo versionInfo = updateCliService.getVersionInfo();
        Assert.assertNotNull(versionInfo);
        Assert.assertEquals("v7.9.9-RC1", versionInfo.getCurrentReleaseTag());
        Assert.assertArrayEquals(new int[]{7, 9, 9, 0}, versionInfo.getCurrentVersion());
        Assert.assertEquals("v8.0.0-RC37", versionInfo.getLatestReleaseTag());
        Assert.assertArrayEquals(new int[]{8, 0, 0, 0}, versionInfo.getLatestVersion());
        Assert.assertEquals(true, versionInfo.isNewerVersionAvailable());
        Assert.assertEquals("https://github.com/logabit/pipeforce-cli/releases/download/v8.0.0-RC37/pipeforce-cli.jar", versionInfo.getLatestDownloadUrl());
    }

    @Test
    public void testDownloadAndInstallVersion() throws IOException {

        // https://www.baeldung.com/mockito-mock-static-methods
        try (MockedStatic<FileUtil> fileUtil = Mockito.mockStatic(FileUtil.class)) {

            when(configService.getInstallationHome()).thenReturn("/some/home/path");
            when(configService.getInstalledJarPath()).thenReturn("/some/home/path/pipeforce-cli/bin/pipeforce-cli-8.5.5.jar");
            UpdateCliService updateCliService = new UpdateCliService();
            updateCliService.setContext(cliContext);

            // Mock download response

            HttpResponse downloadResponse = mock(HttpResponse.class);
            HttpEntity httpEntity = mock(HttpEntity.class);
            when(httpEntity.getContent()).thenReturn(StringUtil.toInputStream("downloadedData"));
            when(downloadResponse.getEntity()).thenReturn(httpEntity);

            StatusLine downloadResponseStatusLine = mock(StatusLine.class);
            when(downloadResponseStatusLine.getStatusCode()).thenReturn(200);
            when(downloadResponse.getStatusLine()).thenReturn(downloadResponseStatusLine);
            when(httpClient.execute(any(HttpGet.class))).thenReturn(downloadResponse);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            when(outputService.createOutputStream(any(File.class))).thenReturn(bos);

            UpdateCliService.VersionInfo version = new UpdateCliService.VersionInfo(
                    "v7.0.0-RC12", "v8.5.5-RELEASE", "http://someUrl");

            updateCliService.downloadAndUpdateVersion(version);

            Assert.assertEquals("downloadedData", new String(bos.toByteArray()));

            ArgumentCaptor<String> installedReleaseTag = ArgumentCaptor.forClass(String.class);

            verify(configService, times(1)).setInstalledReleaseTag(installedReleaseTag.capture());

            Assert.assertEquals("v8.5.5-RELEASE", installedReleaseTag.getValue());
        }
    }
}
