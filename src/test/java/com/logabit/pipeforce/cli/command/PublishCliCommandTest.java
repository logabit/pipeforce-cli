package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.common.pipeline.PipelineRunner;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.ReflectionUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.commons.io.FileUtils;
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
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

/**
 * Tests the {@link PublishCliCommand}.
 *
 * @author sniederm
 * @since 2.0
 */
@RunWith(MockitoJUnitRunner.class)
public class PublishCliCommandTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @Mock
    private ConfigCliService configService;

    @Mock
    private PipelineRunner pipelineRunner;

    @InjectMocks
    private CliContext cliContext = new CliContext();

    private String workspaceHome;

    @After
    public void tearDown() {

        FileUtil.delete(workspaceHome);
    }

    @Before
    public void setUp() {

        workspaceHome = getTestWorkspace();
        Mockito.when(configService.getHome()).thenReturn(workspaceHome);
    }

    @Test
    public void test_NewApp_NewPipeline_NewForm_NewObject_Publish() throws Exception {

        cliContext.setCurrentWorkDir(new File(PathUtil.path(workspaceHome)));
        Mockito.when(configService.getAppHome("someapp")).thenReturn(PathUtil.path(workspaceHome, "src", "global", "app", "someapp"));

        JsonNode resultNode = JsonUtil.mapToJsonNode(ListUtil.asMap("result", "created"));
        Mockito.when(pipelineRunner.executePipelineJsonNode(Mockito.any(JsonNode.class))).thenReturn(resultNode);

        systemInMock.provideLines("someapp", null, "someDescription", null);
        cliContext.setArgs("new", "app");
        cliContext.callCommand();

        systemInMock.provideLines("someapp", "somepipeline");
        cliContext.setArgs("new", "pipeline");
        cliContext.callCommand();

        systemInMock.provideLines("someapp", "someform", "someformdesc", "1", "someobject");
        cliContext.setArgs("new", "form");
        cliContext.callCommand();

        // Copy a binary file to the app for testing
        FileUtils.copyFile(new File("src/test/resources/logo.png"), new File(PathUtil.path(configService.getHome(), "src/global/app/someapp/template/logo.png")));

        systemInMock.provideLines("yes");
        PublishCliCommand publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("src/global/app/someapp/**"));

        ArgumentCaptor<JsonNode> publishNode = ArgumentCaptor.forClass(JsonNode.class);
        Mockito.verify(pipelineRunner, Mockito.atLeastOnce()).executePipelineJsonNode(publishNode.capture());

        List<JsonNode> allNodes = publishNode.getAllValues();

        Assert.assertEquals(new File("global/app/someapp/config/someapp"),
                new File(allNodes.get(0).get("pipeline").get(0).get("property.schema.put").get("key").textValue()));
        Assert.assertEquals("application/json", allNodes.get(1).get("pipeline").get(0).get("property.schema.put").get("type").textValue());

        Assert.assertEquals(new File("global/app/someapp/template/logo"),
                new File(allNodes.get(4).get("pipeline").get(0).get("property.schema.put").get("key").textValue()));
        Assert.assertEquals("image/png;encoding=base64", allNodes.get(4).get("pipeline").get(0).get("property.schema.put").get("type").textValue());

        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(5, publishCommand.getPublishedCounter());

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("src/global/app/someapp/**"));

        // No files have changed -> Nothing to upload
        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(0, publishCommand.getPublishedCounter());

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(CommandArgs.EMPTY); // All in src folder

        // Test that lower case values of "show" attribute in app config will be converted to upper case correctly
        final File appConfig = new File(PathUtil.path(configService.getHome(), "src/global/app/someapp/config/someapp.json"));
        String appConfigString = FileUtil.readFileToString(appConfig);
        Map<String, Object> appConfigMap = JsonUtil.jsonStringToMap(appConfigString);
        String showValue = (String) appConfigMap.get("show");
        appConfigMap.put("show", showValue.toLowerCase());
        appConfigString = JsonUtil.objectToJsonString(appConfigMap);
        FileUtil.saveStringToFile(appConfigString, appConfig);

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(CommandArgs.EMPTY); // All in src folder

        // appConfig file have changed -> 1 to upload
        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(1, publishCommand.getPublishedCounter());

        // Make sure appConfig's show attribute was converted to upper case
        appConfigString = FileUtil.readFileToString(appConfig);
        appConfigMap = JsonUtil.jsonStringToMap(appConfigString);
        Assert.assertEquals("CAN_APP_SOMEAPP", appConfigMap.get("show"));
    }

    @Test
    public void testMigrateToNewAppConfig() throws Exception {

        cliContext.setCurrentWorkDir(new File(PathUtil.path(workspaceHome)));
        JsonNode resultNode = JsonUtil.mapToJsonNode(ListUtil.asMap("result", "created"));
        Mockito.when(pipelineRunner.executePipelineJsonNode(Mockito.any(JsonNode.class))).thenReturn(resultNode);

        systemInMock.provideLines(
                "someapp", null, "someDescription", null // new app
        );

        cliContext.setArgs("new", "app");
        cliContext.callCommand();

        systemInMock.provideLines(
                "yes" // Yes, publish
        );

        cliContext.setArgs("publish");
        cliContext.callCommand();

        // Now switch server version to 7
        ReflectionUtil.setFieldValue(cliContext, "serverVersionMajor", 7);

        systemInMock.provideLines(
                "yes", // Yes, publish
                "yes" // Yes migrate to app.json
        );

        cliContext.setArgs("publish");
        cliContext.callCommand();

        ArgumentCaptor<JsonNode> publishNode = ArgumentCaptor.forClass(JsonNode.class);
        Mockito.verify(pipelineRunner, Mockito.atLeastOnce()).executePipelineJsonNode(publishNode.capture());
        List<JsonNode> allValues = publishNode.getAllValues();
        System.out.println(allValues);

        Assert.assertEquals(new File("global/app/someapp/config/someapp"),
                new File(allValues.get(0).get("pipeline").get(0).get("property.schema.put").get("key").textValue()));

        Assert.assertEquals(new File("global/app/someapp/config/app"),
                new File(allValues.get(1).get("pipeline").get(0).get("property.schema.put").get("key").textValue()));
    }

    private String getTestWorkspace() {
        return PathUtil.path(System.getProperty("user.home"), "PIPEFORCE_TEST_" + StringUtil.randomString(5));
    }
}
