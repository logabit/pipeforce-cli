package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.uri.CliPipeforceURIResolver;
import com.logabit.pipeforce.common.model.WorkspaceConfig;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static com.logabit.pipeforce.cli.uri.CliPipeforceURIResolver.Method.GET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link ListCliCommand}.
 *
 * @author sniederm
 * @since 2.20
 */
@RunWith(MockitoJUnitRunner.class)
public class ListCliCommandTest {

    @InjectMocks
    private final CliContext cliContext = new CliContext();

    @Mock
    private ConfigCliService configService;

    @Mock
    protected CliPipeforceURIResolver resolver;

    @Before
    public void setUp() {

        WorkspaceConfig config = new WorkspaceConfig();
        config.setPropertiesHome("src");
        Mockito.when(configService.getWorkspaceConfig()).thenReturn(config);
    }

    @Test
    public void testList() throws Exception {

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        String foundProperties = "[\n" +
                "  {\n" +
                "    \"path\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop1\",\n" +
                "    \"uuid\": \"a656bc2d-9a2f-40b5-9eb7-fb0f7cc78b94\",\n" +
                "    \"value\": \"someValue1ččč\",\n" +
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

        when(resolver.resolveToObject(any(), any(), any())).thenReturn(foundPropsNode);

        ListCliCommand getCmd = (ListCliCommand) cliContext.createCommandInstance("list");
        getCmd.call(new CommandArgs("global/app/myapp/"));

        ArgumentCaptor<CliPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(CliPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals("$uri:command:property.list?pattern=global/app/myapp/**", ListUtil.lastElement(values));

        // Converts global/*/myapp/** -> global/*/myapp/**

        getCmd = (ListCliCommand) cliContext.createCommandInstance("list");
        getCmd.call(new CommandArgs("global/*/myapp/**"));

        methodCaptor = ArgumentCaptor.forClass(CliPipeforceURIResolver.Method.class);
        uriCaptor = ArgumentCaptor.forClass(String.class);
        typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(2)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        values = uriCaptor.getAllValues();
        Assert.assertEquals("$uri:command:property.list?pattern=global/*/myapp/**", ListUtil.lastElement(values));

        // Converts global/app/myapp -> global/app/myapp

        getCmd = (ListCliCommand) cliContext.createCommandInstance("list");
        getCmd.call(new CommandArgs("global/app/myapp"));

        methodCaptor = ArgumentCaptor.forClass(CliPipeforceURIResolver.Method.class);
        uriCaptor = ArgumentCaptor.forClass(String.class);
        typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(3)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        values = uriCaptor.getAllValues();
        Assert.assertEquals("$uri:command:property.list?pattern=global/app/myapp", ListUtil.lastElement(values));

    }
}
