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

        Assert.assertEquals("8.5.5", version.getLatestVersion());

        Assert.assertTrue(version.isNewer("7.0.0"));
        Assert.assertTrue(version.isNewer("8.1.0"));
        Assert.assertTrue(version.isNewer("8.5.1"));

        Assert.assertFalse(version.isNewer("9.0.0"));
        Assert.assertFalse(version.isNewer("8.6.0"));
        Assert.assertFalse(version.isNewer("8.5.6"));
    }

    @Test
    public void testGetVersionInfo() throws IOException {

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(FileUtil.readFileToInputStream(new File("src/test/resources/github.latest.test.json")));
        when(httpResponse.getEntity()).thenReturn(entity);

        when(configService.getInstalledReleaseName()).thenReturn("v7.9.9-RC1"); // This is the currently installed version

        UpdateCliService updateCliService = new UpdateCliService();
        updateCliService.setContext(cliContext);

        UpdateCliService.VersionInfo versionInfo = updateCliService.getVersionInfo();
        Assert.assertNotNull(versionInfo);
        Assert.assertEquals("v7.9.9-RC1", versionInfo.getCurrentReleaseName());
        Assert.assertEquals("7.9.9", versionInfo.getCurrentVersion());
        Assert.assertEquals("v8.0.0-RC37", versionInfo.getLatestReleaseName());
        Assert.assertEquals("8.0.0", versionInfo.getLatestVersion());
        Assert.assertEquals(true, versionInfo.isNewerVersionAvailable());
        Assert.assertEquals("https://github.com/logabit/pipeforce-cli/releases/download/v8.0.0-RC37/pipeforce-cli.jar", versionInfo.getLatestDownloadUrl());
    }

    @Test
    public void testDownloadAndInstallVersion() throws IOException {

        when(configService.getInstallationHome()).thenReturn("/some/home/path");
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

        ArgumentCaptor<File> currentJar = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<File> newJar = ArgumentCaptor.forClass(File.class);
        verify(outputService, times(1)).moveFile(currentJar.capture(), newJar.capture());

        Assert.assertEquals("/some/home/path/bin/pipeforce-cli.jar", currentJar.getValue().getAbsolutePath());
        Assert.assertEquals("/some/home/path/bin/pipeforce-cli.jar", newJar.getValue().getAbsolutePath());

    }
}
