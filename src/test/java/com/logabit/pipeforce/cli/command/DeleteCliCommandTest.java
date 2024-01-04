package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.cli.uri.ClientPipeforceURIResolver;
import com.logabit.pipeforce.common.util.JsonUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static com.logabit.pipeforce.cli.uri.ClientPipeforceURIResolver.Method.GET;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link DeleteCliCommand}.
 *
 * @author sniederm
 * @since 2.7
 */
@RunWith(MockitoJUnitRunner.class)
public class DeleteCliCommandTest extends BaseRepoAwareCliCommandTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @Mock
    private PublishCliService publishCliService;

    @Test
    public void testDeleteProperty() throws Exception {

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        systemInMock.provideLines("1"); // Do you want to delete? 1=yes

        String foundProperties = "[\n" +
                "  {\n" +
                "    \"path\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop1\",\n" +
                "    \"uuid\": \"a656bc2d-9a2f-40b5-9eb7-fb0f7cc78b94\",\n" +
                "    \"value\": \"someValue1\",\n" +
                "    \"defaultValue\": null,\n" +
                "    \"type\": \"application/yaml; type=pipeline\",\n" +
                "    \"created\": 1613460723183,\n" +
                "    \"updated\": null,\n" +
                "    \"timeToLive\": null\n" +
                "  },\n" +
                "  {\n" +
                "    \"path\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop2\",\n" +
                "    \"uuid\": \"f9e714a1-dcaf-4da6-908b-2571b7dcd8c7\",\n" +
                "    \"value\": \"someValue2\",\n" +
                "    \"defaultValue\": null,\n" +
                "    \"type\": \"application/yaml; type=pipeline\",\n" +
                "    \"created\": 1613460723360,\n" +
                "    \"updated\": null,\n" +
                "    \"timeToLive\": null\n" +
                "  }\n" +
                "]";

        ArrayNode foundPropsNode = (ArrayNode) JsonUtil.jsonStringToJsonNode(foundProperties);
        when(resolver.resolveToObject(
                GET, "$uri:command:property.list?filter=global/app/myapp/pipeline/**", ArrayNode.class)).thenReturn(foundPropsNode);

        DeleteCliCommand deleteCmd = (DeleteCliCommand) cliContext.createCommandInstance("delete");
        deleteCmd.call(new CommandArgs("global/app/myapp/pipeline/**"));

        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(3)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals("$uri:command:property.list?filter=global/app/myapp/pipeline/**", values.get(0));
        Assert.assertEquals("$uri:command:property.schema.delete?pattern=/pipeforce/enterprise/global/app/myapp/pipeline/prop1", values.get(1));
        Assert.assertEquals("$uri:command:property.schema.delete?pattern=/pipeforce/enterprise/global/app/myapp/pipeline/prop2", values.get(2));

        verify(publishCliService, times(1)).load();
        verify(publishCliService, times(1)).save();
    }

    @Test
    public void testDeletePropertyNothingFound() throws Exception {

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        systemInMock.provideLines("1"); // Do you want to delete? 1=yes

        String foundProperties = "[]"; // No properties found

        ArrayNode foundPropsNode = (ArrayNode) JsonUtil.jsonStringToJsonNode(foundProperties);
        when(resolver.resolveToObject(GET, "$uri:command:property.list?filter=global/app/myapp/pipeline/mypipe", ArrayNode.class)).thenReturn(foundPropsNode);

        DeleteCliCommand deleteCmd = (DeleteCliCommand) cliContext.createCommandInstance("delete");
        deleteCmd.call(new CommandArgs("global/app/myapp/pipeline/mypipe.pi.yaml"));

        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("$uri:command:property.list?filter=global/app/myapp/pipeline/mypipe", values.get(0));
    }

    @Test
    public void testDeletePropertyAnswerNo() throws Exception {

        systemInMock.provideLines("0"); // Do you want to delete? 0=no

        DeleteCliCommand deleteCmd = (DeleteCliCommand) cliContext.createCommandInstance("delete");
        deleteCmd.call(new CommandArgs("properties/global/app/myapp/pipeline/**"));

        verify(resolver, times(0)).resolveToObject(any(), anyString(), any());
    }

    @Test
    public void testFolderSingleWildcardNoChange() throws Exception {

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        systemInMock.provideLines("1"); // Do you want to delete? 1=yes

        DeleteCliCommand deleteCmd = (DeleteCliCommand) cliContext.createCommandInstance("delete");
        deleteCmd.call(new CommandArgs("global/app/myapp/pipeline/*"));

        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("$uri:command:property.list?filter=global/app/myapp/pipeline/*", values.get(0));
    }

    @Test
    public void testFolderDoubleWildcardNoChange() throws Exception {

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        systemInMock.provideLines("1"); // Do you want to delete? 1=yes

        DeleteCliCommand deleteCmd = (DeleteCliCommand) cliContext.createCommandInstance("delete");
        deleteCmd.call(new CommandArgs("global/app/myapp/pipeline/**"));

        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("$uri:command:property.list?filter=global/app/myapp/pipeline/**", values.get(0));
    }

    @Test
    public void testFolderLeafNoChange() throws Exception {

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        systemInMock.provideLines("1"); // Do you want to delete? 1=yes

        DeleteCliCommand deleteCmd = (DeleteCliCommand) cliContext.createCommandInstance("delete");
        deleteCmd.call(new CommandArgs("global/app/myapp/pipeline/someleaf"));

        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("$uri:command:property.list?filter=global/app/myapp/pipeline/someleaf", values.get(0));
    }
}
