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
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;

import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link GetCliCommand}.
 *
 * @author sniederm
 * @since 2.7
 */
@RunWith(MockitoJUnitRunner.class)
public class GetCliCommandTest {

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
    private PublishCliService publishCliService;

    @Mock
    private PipelineRunner pipelineRunner;

    @Before
    public void setUp() {

        cliContext.setCurrentWorkDir(new File("/some/home/pipeforce"));
        when(configService.getHome()).thenReturn("/some/home/pipeforce");
    }

    @Test
    public void testGet() throws Exception {

        when(configService.getNamespace()).thenReturn("enterprise");

        systemInMock.provideLines("1"); // Do you want to delete? 1=yes

        String foundProperties = "[\n" +
                "  {\n" +
                "    \"key\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop1\",\n" +
                "    \"uuid\": \"a656bc2d-9a2f-40b5-9eb7-fb0f7cc78b94\",\n" +
                "    \"value\": \"someValue1\",\n" +
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

        when(pipelineRunner.executePipelineUri("property.list?filter=global/app/myapp/pipeline/**")).thenReturn(foundPropsNode);

        GetCliCommand getCmd = (GetCliCommand) cliContext.createCommandInstance("get");
        getCmd.call(new CommandArgs("global/app/myapp/pipeline/**"));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(pipelineRunner, times(1)).executePipelineUri(uriCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals("property.list?filter=global/app/myapp/pipeline/**", values.get(0));

        verify(publishCliService, times(1)).load();
        verify(publishCliService, times(1)).save();

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(outputService, times(2)).saveStringToFile(dataCaptor.capture(), fileCaptor.capture());

        List<String> allData = dataCaptor.getAllValues();
        Assert.assertEquals("someValue1", allData.get(0));
        Assert.assertEquals("someValue2", allData.get(1));

        List<File> allFiles = fileCaptor.getAllValues();
        Assert.assertEquals("/some/home/pipeforce/src/global/app/myapp/pipeline/prop1.pi.yaml", allFiles.get(0).getPath());
        Assert.assertEquals("/some/home/pipeforce/src/global/app/myapp/pipeline/prop2.pi.yaml", allFiles.get(1).getPath());
    }
}
