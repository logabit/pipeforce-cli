package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.common.net.ClientPipeforceURIResolver;
import com.logabit.pipeforce.common.net.Request;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.UriUtil;
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
    public void test_NewApp_NewPipeline_NewForm_NewObject_Publish() throws Exception {

        cliContext.setCurrentWorkDir(super.repoHome);

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        JsonNode resultNode = JsonUtil.mapToJsonNode(ListUtil.asMap("result", "created"));
        Mockito.when(resolver.resolve(any(), any())).thenReturn(resultNode);

        systemInMock.provideLines("com.logabit.someapp", null, "someDescription", null);
        cliContext.setArgs("new", "app");
        cliContext.callCommand();

        systemInMock.provideLines("com.logabit.someapp", "somepipeline");
        cliContext.setArgs("new", "pipeline");
        cliContext.callCommand();

        systemInMock.provideLines("com.logabit.someapp", "someform", "someformdesc", "1", "someobject");
        cliContext.setArgs("new", "form");
        cliContext.callCommand();

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

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, atLeastOnce()).resolve(requestCaptor.capture(), typeCaptor.capture());

        List<Request> allNodes = requestCaptor.getAllValues();

        Assert.assertEquals(new File("global/app/com.logabit.someapp/config/app"),
                new File(allNodes.get(0).getParams().get(FIELD_PATH) + ""));

        Assert.assertEquals("application/json", allNodes.get(1).getParams().get("type"));

        Map<String, String> node4 = allNodes.get(4).getParams();

        Assert.assertEquals(new File("global/app/com.logabit.someapp/template/logo"),
                new File(allNodes.get(4).getParams().get(FIELD_PATH) + ""));
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

        // Test that lower case values of "show" attribute in app config will be converted to upper case correctly
        final File appConfig = new File(PathUtil.path(cliContext.getRepoHome(), "properties/global/app/com.logabit.someapp/config/app.json"));
        String appConfigString = FileUtil.fileToString(appConfig);
        Map<String, Object> appConfigMap = JsonUtil.jsonStringToMap(appConfigString);
        String showValue = (String) appConfigMap.get("show");
        appConfigMap.put("show", showValue.toLowerCase());
        appConfigString = JsonUtil.objectToJsonString(appConfigMap);
        FileUtil.saveStringToFile(appConfigString, appConfig);

        systemInMock.provideLines("yes");
        publishCommand = (PublishCliCommand) cliContext.createCommandInstance("publish");
        publishCommand.call(new CommandArgs("properties/global/app/**")); // All in src folder

        // appConfig file have changed -> 1 to upload
        Assert.assertEquals(5, publishCommand.getFilesCounter());
        Assert.assertEquals(1, publishCommand.getPublishedCounter());

        // Make sure appConfig's show attribute was converted to upper case
        appConfigString = FileUtil.fileToString(appConfig);
        appConfigMap = JsonUtil.jsonStringToMap(appConfigString);
        Assert.assertEquals("CAN_APP_COM.LOGABIT.SOMEAPP", appConfigMap.get("show"));
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
