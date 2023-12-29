package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.cli.service.PublishCliService;
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

import java.io.File;
import java.util.List;

import static com.logabit.pipeforce.cli.uri.CliPipeforceURIResolver.Method.GET;
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
public class GetCliCommandTest extends BaseRepoAwareCliCommandTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @Mock
    private PublishCliService publishCliService;

    @Test
    public void testGet() throws Exception {

        CliConfig.Instance instance = new CliConfig.Instance();
        instance.setNamespace("enterprise");
        cliContext.setCurrentInstance(instance);

        systemInMock.provideLines("1"); // Do you want to delete? 1=yes

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

        when(resolver.resolveToObject(
                GET, "$uri:command:property.list?filter=global/app/myapp/pipeline/**", ArrayNode.class)).thenReturn(foundPropsNode);

        GetCliCommand getCmd = (GetCliCommand) cliContext.createCommandInstance("get");
        getCmd.call(new CommandArgs("global/app/myapp/pipeline/**"));

        verify(publishCliService, times(1)).load();
        verify(publishCliService, times(1)).save();

        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(outputService, times(2)).saveByteArrayToFile(dataCaptor.capture(), fileCaptor.capture());

        List<byte[]> allData = dataCaptor.getAllValues();
        Assert.assertEquals("someValue1ččč", new String(allData.get(0)));
        Assert.assertEquals("someValue2", new String(allData.get(1)));

        List<File> allFiles = fileCaptor.getAllValues();
        Assert.assertEquals(new File(repoHome, "properties/global/app/myapp/pipeline/prop1.pi.yaml"), allFiles.get(0));
        Assert.assertEquals(new File(repoHome, "properties/global/app/myapp/pipeline/prop2.pi.yaml"), allFiles.get(1));
    }
}
