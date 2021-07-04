package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.service.InstallCliService;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.cli.service.UpdateCliService;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link PublishCliCommand}.
 *
 * @author sniederm
 * @since 7.0
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateCliCommandTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @InjectMocks
    private final CliContext cliContext = new CliContext();

    @Mock
    private ConfigCliService configService;

    @Mock
    private InstallCliService installService;

    @Mock
    private OutputCliService outputService;

    @Mock
    private HttpClient httpClient;

    private UpdateCliService updateCliService;

    @Before
    public void onStartUp() {

        updateCliService = Mockito.spy(UpdateCliService.class);
        updateCliService.setContext(cliContext);
    }

    @Test
    public void testNoNewVersionAvailable() throws Exception {

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(StringUtil.toInputStream(
                "pipeforce-cli-1.0.0.jar\n" +
                        "pipeforce-cli-2.0.1-QA-123-efghs.jar\n" +
                        "pipeforce-cli-3.0.0-QA-124-cbgss.jar\n" +
                        "pipeforce-cli-3.0.0.jar"));
        when(httpResponse.getEntity()).thenReturn(entity);

        when(configService.getInstalledVersion()).thenReturn("3.0.0"); // Already latest version
        when(configService.getUpdateCheckUrl()).thenReturn("https://download.someBaseUrl");

        UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
        update.call(CommandArgs.EMPTY);

        ArgumentCaptor<HttpGet> httpHeadArgumentCaptor = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient, times(1)).execute(httpHeadArgumentCaptor.capture());

        Assert.assertEquals("https://download.someBaseUrl/pipeforce-cli/files.txt", httpHeadArgumentCaptor.getValue().getURI() + "");
    }

    @Test
    public void testNewVersionAvailable_butUserSelectsNo() throws Exception {

        systemInMock.provideLines(
                "0" // User is asked to update -> Selects no
        );

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(StringUtil.toInputStream(
                "pipeforce-cli-1.0.0.jar\n" +
                        "pipeforce-cli-2.0.1-QA-123-efghs.jar\n" +
                        "pipeforce-cli-3.0.0-QA-124-cbgss.jar\n" +
                        "pipeforce-cli-3.0.0.jar"));
        when(httpResponse.getEntity()).thenReturn(entity);

        when(configService.getInstalledVersion()).thenReturn("1.0.0"); // Old version, next expected is 3.0.0
        when(configService.getUpdateCheckUrl()).thenReturn("https://download.someBaseUrl");

        UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
        update.call(CommandArgs.EMPTY);

        ArgumentCaptor<HttpGet> httpGetArgumentCaptor = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient, times(1)).execute(httpGetArgumentCaptor.capture());

        Assert.assertEquals("https://download.someBaseUrl/pipeforce-cli/files.txt", httpGetArgumentCaptor.getValue().getURI() + "");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(outputService, times(1)).println(messageCaptor.capture());
        Assert.assertEquals("Current version is: 1.0.0. A newer version of PIPEFORCE CLI has been detected: 3.0.0. Download and install?",
                messageCaptor.getValue());
    }

    @Test
    public void testNewVersionAvailable_userSelectsYes() throws Exception {

        systemInMock.provideLines(
                "1" // User is asked to update -> Selects yes
        );

        HttpResponse httpFilesResponse = mock(HttpResponse.class);
        HttpResponse downloadResponse = mock(HttpResponse.class);

        when(httpClient.execute(any(HttpGet.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                HttpGet httpGet = invocation.getArgument(0);

                if (httpGet.getURI().toString().equals("https://download.someBaseUrl/pipeforce-cli/files.txt")) {
                    return httpFilesResponse;
                } else {
                    return downloadResponse;
                }
            }
        });

        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpFilesResponse.getStatusLine()).thenReturn(statusLine);

        HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(StringUtil.toInputStream(
                "pipeforce-cli-1.0.0.jar\n" +
                        "pipeforce-cli-2.0.1-QA-123-efghs.jar\n" +
                        "pipeforce-cli-3.0.0-QA-124-cbgss.jar\n" +
                        "pipeforce-cli-3.0.0.jar"));
        when(httpFilesResponse.getEntity()).thenReturn(entity);


        // Second is download request
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(downloadResponse.getEntity()).thenReturn(httpEntity);
        HttpGet downloadRequest = new HttpGet("https://download.someBaseUrl/pipeforce-cli/pipeforce-cli-3.0.0.jar");

        StatusLine downloadResponseStatusLine = mock(StatusLine.class);
        when(downloadResponseStatusLine.getStatusCode()).thenReturn(200);
        when(downloadResponse.getStatusLine()).thenReturn(downloadResponseStatusLine);

        FileOutputStream fosMock = mock(FileOutputStream.class);
        when(outputService.createOutputStream(any(File.class))).thenReturn(fosMock);

        when(configService.getInstalledVersion()).thenReturn("1.0.0"); // Old version, next expected is 3.0.0
        when(configService.getUpdateCheckUrl()).thenReturn("https://download.someBaseUrl");
        when(configService.getHome()).thenReturn("/user/home/pipeforce/");

        UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
        update.call(CommandArgs.EMPTY);

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(outputService, times(1)).createOutputStream(fileCaptor.capture());
        Assert.assertEquals(new File("/user/home/pipeforce/tool/pipeforce-cli-3.0.0.jar"),
                fileCaptor.getValue());

        ArgumentCaptor<String> installedVersionCaptor = ArgumentCaptor.forClass(String.class);
        verify(configService, times(1)).setInstalledVersion(installedVersionCaptor.capture());
        Assert.assertEquals("3.0.0", installedVersionCaptor.getValue());

        verify(installService, times(1)).createPiScript();
    }

    @Test
    public void testUpdateToExactVersion() throws Exception {

        // Mock download request
        HttpResponse downloadResponse = mock(HttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(downloadResponse.getEntity()).thenReturn(httpEntity);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(downloadResponse);

        StatusLine downloadResponseStatusLine = mock(StatusLine.class);
        when(downloadResponseStatusLine.getStatusCode()).thenReturn(200);
        when(downloadResponse.getStatusLine()).thenReturn(downloadResponseStatusLine);

        FileOutputStream fosMock = mock(FileOutputStream.class);
        when(outputService.createOutputStream(any(File.class))).thenReturn(fosMock);

        when(configService.getUpdateCheckUrl()).thenReturn("https://download.someBaseUrl");
        when(configService.getHome()).thenReturn("/user/home/pipeforce/");

        UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
        update.call(new CommandArgs("5.3"));

        // Check the requested URL
        ArgumentCaptor<HttpRequestBase> httpRequestCaptor = ArgumentCaptor.forClass(HttpRequestBase.class);
        verify(httpClient, times(1)).execute(httpRequestCaptor.capture());
        Assert.assertEquals("https://download.someBaseUrl/pipeforce-cli/pipeforce-cli-5.3.jar", httpRequestCaptor.getAllValues().get(0).getURI() + "");

        // Check the path of the downloaded file
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(outputService, times(1)).createOutputStream(fileCaptor.capture());
        Assert.assertEquals(new File("/user/home/pipeforce/tool/pipeforce-cli-5.3.jar"),
                fileCaptor.getValue());

        // Check the correct installedVersion is set
        ArgumentCaptor<String> installedVersionCaptor = ArgumentCaptor.forClass(String.class);
        verify(configService, times(1)).setInstalledVersion(installedVersionCaptor.capture());
        Assert.assertEquals("5.3", installedVersionCaptor.getValue());

        // Update pi script was called
        verify(installService, times(1)).createPiScript();
    }

    @Test
    public void testDownloadIsNotReachable() throws Exception {

        when(httpClient.execute(any(HttpGet.class))).thenThrow(new IOException("Dummy connection exception for testing"));

        try {
            UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
            update.call(new CommandArgs("5.3"));
            Assert.fail("CliException expected but not thrown.");
        } catch (CliException e) {

        }
    }

    @Test
    public void testIsNewerVersionAvailable() {

        UpdateCliService updateCliService = Mockito.spy(UpdateCliService.class);
        updateCliService.setContext(cliContext);

        Mockito.doReturn(
                ListUtil.asList(
                        "pipeforce-cli-1.0.0.jar",
                        "pipeforce-cli-2.0.0-QA-123-efghs.jar",
                        "pipeforce-cli-2.0.0-QA-124-cbgss.jar",
                        "pipeforce-cli-2.0.0-MS1-124-cbgss.jar",
                        "pipeforce-cli-2.0.0-MS2-124-hshss.jar",
                        "pipeforce-cli-2.1.0.jar",
                        "pipeforce-cli-3.0.1.jar",
                        "pipeforce-cli-4.0.2.jar"
                )).when(updateCliService).downloadFilesList();

        String newerVersion = updateCliService.isNewerVersionAvailable("1.0");
        Assert.assertEquals("4.0.2", newerVersion);
    }
}
