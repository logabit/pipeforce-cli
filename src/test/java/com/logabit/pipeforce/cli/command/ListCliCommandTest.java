package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.service.InstallCliService;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.pipeline.PipelineRunner;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
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

import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;
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
    private OutputCliService outputService;

    @Mock
    private PipelineRunner pipelineRunner;

    @Test
    public void testList() throws Exception {

        when(configService.getNamespace()).thenReturn("enterprise");

        String foundProperties = "[\n" +
                "  {\n" +
                "    \"key\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop1\",\n" +
                "    \"uuid\": \"a656bc2d-9a2f-40b5-9eb7-fb0f7cc78b94\",\n" +
                "    \"value\": \"someValue1ččč\",\n" +
                "    \"defaultValue\": null,\n" +
                "    \"type\": \"application/yaml; type=pipeline\",\n" +
                "    \"created\": 1613460723183,\n" +
                "    \"updated\": null,\n" +
                "    \"timeToLive\": null\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop2\",\n" +
                "    \"uuid\": \"f9e714a1-dcaf-4da6-908b-2571b7dcd8c7\",\n" +
                "    \"value\": \"someValue2\",\n" +
                "    \"defaultValue\": null,\n" +
                "    \"type\": \"application/yaml; type=pipeline\",\n" +
                "    \"created\": 1613460723360,\n" +
                "    \"updated\": null,\n" +
                "    \"timeToLive\": null\n" +
                "  }\n" +
                "]";

        JsonNode foundPropsNode = JsonUtil.jsonStringToJsonNode(foundProperties);

        when(pipelineRunner.executePipelineUri(Mockito.anyString())).thenReturn(foundPropsNode);

        ListCliCommand getCmd = (ListCliCommand) cliContext.createCommandInstance("list");
        getCmd.call(new CommandArgs("global/app/myapp/"));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(pipelineRunner, times(1)).executePipelineUri(uriCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals("property.list?filter=global/app/myapp/**", ListUtil.lastElement(values));

        // Converts global/*/myapp/** -> global/*/myapp/**

        getCmd = (ListCliCommand) cliContext.createCommandInstance("list");
        getCmd.call(new CommandArgs("global/*/myapp/**"));

        uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(pipelineRunner, times(2)).executePipelineUri(uriCaptor.capture());

        values = uriCaptor.getAllValues();
        Assert.assertEquals("property.list?filter=global/*/myapp/**", ListUtil.lastElement(values));

        // Converts global/app/myapp -> global/app/myapp

        getCmd = (ListCliCommand) cliContext.createCommandInstance("list");
        getCmd.call(new CommandArgs("global/app/myapp"));

        uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(pipelineRunner, times(3)).executePipelineUri(uriCaptor.capture());

        values = uriCaptor.getAllValues();
        Assert.assertEquals("property.list?filter=global/app/myapp", ListUtil.lastElement(values));

    }
}
