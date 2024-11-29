package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.common.command.ICommandParams;
import com.logabit.pipeforce.common.command.stub.PropertySchemaPutParams;
import com.logabit.pipeforce.common.net.Request;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.ThreadUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.logabit.pipeforce.common.property.IProperty.FIELD_PATH;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link PublishCliCommand}.
 *
 * @author sniederm
 * @since 2.0
 */
@RunWith(MockitoJUnitRunner.class)
public class PublishCliCommandTest extends BaseRepoAwareCliCommandTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @InjectMocks
    private PublishCliCommand publishCommand = new PublishCliCommand();


    @Test
    public void test_NewApp_NewPipeline_NewForm_NewSchema_Publish() throws Exception {

        cliContext.setCurrentWorkDir(super.repoHome);

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        JsonNode resultNode = JsonUtil.mapToJsonNode(ListUtil.asMap("result", "created"));
        Mockito.when(resolver.command(any(), any())).thenReturn(resultNode);

        System.out.println("");
        System.out.println("> new app");
        cliContext.setArgs("new", "app");
        systemInMock.provideLines("com.logabit.someapp", null, "someDescription", null);
        ICliCommand out = cliContext.callCommand();

        System.out.println("");
        System.out.println("> new pipeline");
        cliContext.setArgs("new", "pipeline");
        systemInMock.provideLines("com.logabit.someapp", "somepipeline");
        out = cliContext.callCommand();

        System.out.println("");
        System.out.println("> new form");
        cliContext.setArgs("new", "form");
        systemInMock.provideLines("com.logabit.someapp", "someform", "someformdesc", "1", "someobject");
        out = cliContext.callCommand();

        // Copy a binary file to the app for testing
        File targetFile = new File(super.repoHome, "properties/global/app/com.logabit.someapp/template/logo.png");
        FileUtils.copyFile(new File("src/test/resources/logo.png"), targetFile);

        systemInMock.provideLines("yes");
        PublishCliCommand publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("properties/global/app/com.logabit.someapp/**"));

//        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
//        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
//        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
//        ArgumentCaptor<Map> varsCaptor = ArgumentCaptor.forClass(Map.class);

        ArgumentCaptor<PropertySchemaPutParams> commandCaptor = ArgumentCaptor.forClass(PropertySchemaPutParams.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, atLeastOnce()).command((ICommandParams) commandCaptor.capture(), typeCaptor.capture());

        List<PropertySchemaPutParams> allNodes = commandCaptor.getAllValues();

        Assert.assertEquals(new File("global/app/com.logabit.someapp/config/app"),
                new File(allNodes.get(0).getParamsMap().get(FIELD_PATH) + ""));

        Assert.assertEquals("application/json", allNodes.get(1).getParamsMap().get("type"));

        Map<String, Object> node4 = allNodes.get(4).getParamsMap();

        Assert.assertEquals(new File("global/app/com.logabit.someapp/template/logo"),
                new File(allNodes.get(4).getParamsMap().get(FIELD_PATH) + ""));
        Assert.assertEquals("image/png;encoding=base64", node4.get("type"));

        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(5, publishCommand.getPublishedCounter());

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("global/app/com.logabit.someapp/**"));

        // No files have changed -> Nothing to upload
        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(0, publishCommand.getPublishedCounter());

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(CommandArgs.EMPTY); // All in src folder

        ThreadUtil.sleep(1000); // Make sure last modified of appConfig has changed in any case

        // Change appConfig
        final File appConfig = new File(PathUtil.path(cliContext.getRepoHome(), "properties/global/app/com.logabit.someapp/config/app.json"));
        String appConfigString = FileUtil.fileToString(appConfig);
        Map<String, Object> appConfigMap = JsonUtil.jsonStringToMap(appConfigString);
        appConfigMap.put("description", "someChangedDescription");
        appConfigString = JsonUtil.objectToJsonString(appConfigMap);
        FileUtil.saveStringToFile(appConfigString, appConfig);

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("properties/global/app/**")); // All in src folder

        // appConfig file have changed -> 1 to upload
        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(1, publishCommand.getPublishedCounter());
    }

    @Test
    public void testPreparePath() throws Exception {

        cliContext.setCurrentWorkDir(new File("/Users/some/pipeforce"));
        publishCommand.setContext(cliContext);

        Assert.assertEquals(new File("/Users/some/pipeforce/properties/global/app/myapp/**").toURI().toString(),
                publishCommand.prepareLocalPathPattern("myapp"));

        Assert.assertEquals(new File("/Users/some/pipeforce/properties/global/app/myapp/**").toURI().toString(),
                publishCommand.prepareLocalPathPattern("global/app/myapp/**"));

        Assert.assertEquals(new File("/Users/some/pipeforce/properties/global/app/myapp/pipeline/test").toURI().toString(),
                publishCommand.prepareLocalPathPattern("global/app/myapp/pipeline/test"));

        Assert.assertEquals(new File("/Users/some/pipeforce/properties/global/app/*/pipeline/test").toURI().toString(),
                publishCommand.prepareLocalPathPattern("global/app/*/pipeline/test"));
    }
}
