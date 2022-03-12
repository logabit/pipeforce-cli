package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.service.InstallCliService;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.cli.service.UpdateCliService;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.IOUtil;
import com.logabit.pipeforce.common.util.ReflectionUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
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

    @Mock
    private ConfigCliService configService;

    @Mock
    private InstallCliService installService;

    @Mock
    private OutputCliService outputService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private UpdateCliService updateCliService;

    @InjectMocks
    private final CliContext cliContext = new CliContext();


    @Test
    public void testNewVersionAvailable() throws Exception {

        UpdateCliService.VersionInfo versionInfo = new UpdateCliService.VersionInfo(
                "v7.0.0-RC12", "v8.5.5-RELEASE", "versionInfo");

        when(updateCliService.getVersionInfo()).thenReturn(versionInfo);
        when(configService.getReleaseTagFromJar()).thenReturn("v7.0.0-RC12");

        UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
        update.call(CommandArgs.EMPTY);

        CommandResult result = update.getResult();
        Assert.assertEquals(UpdateCliCommand.MSG_SUCCESS, result.getMessage());
    }

    @Test
    public void testSameVersionAvailable() throws Exception {

        UpdateCliService.VersionInfo versionInfo = new UpdateCliService.VersionInfo(
                "v8.5.5-RELEASE", "v8.5.5-RELEASE", "versionInfo");

        UpdateCliService updateService = mock(UpdateCliService.class);
        ReflectionUtil.setFieldValue(cliContext, "updateService", updateService);
        when(updateService.getVersionInfo()).thenReturn(versionInfo);

        UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
        update.call(CommandArgs.EMPTY);

        CommandResult result = update.getResult();
        Assert.assertEquals(UpdateCliCommand.MSG_REJECTED_LATEST_INSTALLED, result.getMessage());
    }

    @Test
    public void testOlderVersionAvailable() throws Exception {

        UpdateCliService.VersionInfo versionInfo = new UpdateCliService.VersionInfo(
                "v8.5.5-RELEASE", "v7.5.5-RELEASE", "versionInfo");

        UpdateCliService updateService = mock(UpdateCliService.class);
        ReflectionUtil.setFieldValue(cliContext, "updateService", updateService);
        when(updateService.getVersionInfo()).thenReturn(versionInfo);

        UpdateCliCommand update = (UpdateCliCommand) cliContext.createCommandInstance("update");
        update.call(CommandArgs.EMPTY);

        CommandResult result = update.getResult();
        Assert.assertEquals(UpdateCliCommand.MSG_REJECTED_LATEST_INSTALLED, result.getMessage());
    }
}
