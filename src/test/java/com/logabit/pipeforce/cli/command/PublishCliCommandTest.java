package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.common.pipeline.PipelineRunner;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
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

    private File appRepoHome;

    @InjectMocks
    private PublishCliCommand publishCommand = new PublishCliCommand();


    @Before
    public void setUp() {

        appRepoHome = createTestAppRepoHome();
    }

    @After
    public void tearDown() {
//        FileUtil.delete(appRepoHome);
    }

    @Test
    public void test_NewApp_NewPipeline_NewForm_NewObject_Publish() throws Exception {

        cliContext.setCurrentWorkDir(appRepoHome);

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

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
        File targetFile = new File(appRepoHome, "src/global/app/someapp/template/logo.png");
        FileUtils.copyFile(new File("src/test/resources/logo.png"), targetFile);

        systemInMock.provideLines("yes");
        PublishCliCommand publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("src/global/app/someapp/**"));

        ArgumentCaptor<JsonNode> publishNode = ArgumentCaptor.forClass(JsonNode.class);
        Mockito.verify(pipelineRunner, Mockito.atLeastOnce()).executePipelineJsonNode(publishNode.capture());

        List<JsonNode> allNodes = publishNode.getAllValues();

        Assert.assertEquals(new File("global/app/someapp/config/app"),
                new File(allNodes.get(0).get("pipeline").get(0).get("property.schema.put").get("key").textValue()));
        Assert.assertEquals("application/json", allNodes.get(1).get("pipeline").get(0).get("property.schema.put").get("type").textValue());

        Assert.assertEquals(new File("global/app/someapp/template/logo"),
                new File(allNodes.get(4).get("pipeline").get(0).get("property.schema.put").get("key").textValue()));
        Assert.assertEquals("image/png;encoding=base64", allNodes.get(4).get("pipeline").get(0).get("property.schema.put").get("type").textValue());

        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(5, publishCommand.getPublishedCounter());

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("global/app/someapp/**"));

        // No files have changed -> Nothing to upload
        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(0, publishCommand.getPublishedCounter());

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(CommandArgs.EMPTY); // All in src folder

        // Test that lower case values of "show" attribute in app config will be converted to upper case correctly
        final File appConfig = new File(PathUtil.path(cliContext.getRepoHome(), "src/global/app/someapp/config/app.json"));
        String appConfigString = FileUtil.readFileToString(appConfig);
        Map<String, Object> appConfigMap = JsonUtil.jsonStringToMap(appConfigString);
        String showValue = (String) appConfigMap.get("show");
        appConfigMap.put("show", showValue.toLowerCase());
        appConfigString = JsonUtil.objectToJsonString(appConfigMap);
        FileUtil.saveStringToFile(appConfigString, appConfig);

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("src/global/app/**")); // All in src folder

        // appConfig file have changed -> 1 to upload
        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(1, publishCommand.getPublishedCounter());

        // Make sure appConfig's show attribute was converted to upper case
        appConfigString = FileUtil.readFileToString(appConfig);
        appConfigMap = JsonUtil.jsonStringToMap(appConfigString);
        Assert.assertEquals("CAN_APP_SOMEAPP", appConfigMap.get("show"));
    }

    @Test
    public void testPreparePath() throws Exception {

        Mockito.when(configService.getHome()).thenReturn("/Users/some/pipeforce");

        Assert.assertEquals(new File("/Users/some/pipeforce/src/global/app/myapp/**").toURI().toString(),
                publishCommand.prepareLocalPathPattern("myapp"));

        Assert.assertEquals(new File("/Users/some/pipeforce/src/global/app/myapp/**").toURI().toString(),
                publishCommand.prepareLocalPathPattern("global/app/myapp/**"));

        Assert.assertEquals(new File("/Users/some/pipeforce/src/global/app/myapp/pipeline/test").toURI().toString(),
                publishCommand.prepareLocalPathPattern("global/app/myapp/pipeline/test"));

        Assert.assertEquals(new File("/Users/some/pipeforce/src/global/app/*/pipeline/test").toURI().toString(),
                publishCommand.prepareLocalPathPattern("global/app/*/pipeline/test"));
    }

    private File createTestAppRepoHome() {

        File testRepo = new File(System.getProperty("user.home"), "PIPEFORCE_TEST_" + StringUtil.randomString(5));

        File srcFolder = new File(testRepo, "src");
        FileUtil.createFolders(srcFolder);

        File pipeforceFolder = new File(testRepo, ".pipeforce");
        FileUtil.createFolders(pipeforceFolder);

        return testRepo;
    }
}
