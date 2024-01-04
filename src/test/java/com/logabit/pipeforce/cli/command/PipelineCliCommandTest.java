package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.uri.ClientPipeforceURIResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link PipelineCliCommand}.
 *
 * @author sniederm
 * @since 2.7
 */
@RunWith(MockitoJUnitRunner.class)
public class PipelineCliCommandTest extends BaseRepoAwareCliCommandTest {

    @Test
    public void testRunRemote() throws Exception {

        cliContext.setArgs("pipeline", "global/app/myapp/pipeline/hello");
        cliContext.callCommand();

        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolveToObject(methodCaptor.capture(), uriCaptor.capture(), typeCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals("$uri:pipeline:global/app/myapp/pipeline/hello", values.get(0));
    }

    @Test
    public void testRunFile() throws Exception {
        //TODO expect this but system specific "/some/home/pipeforce/properties/global/app/myapp/pipeline/hello.pi.yaml"
        when(outputService.readFileToString(Mockito.anyString())).thenReturn("pipeline:");

        PipelineCliCommand localRun = (PipelineCliCommand) cliContext.createCommandInstance("pipeline");
        localRun.call(new CommandArgs("properties/global/app/myapp/pipeline/hello.pi.yaml"));

        ArgumentCaptor<ClientPipeforceURIResolver.Method> methodCaptor = ArgumentCaptor.forClass(ClientPipeforceURIResolver.Method.class);
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> varsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);

        verify(resolver, times(1)).resolveToObject(
                methodCaptor.capture(), uriCaptor.capture(), bodyCaptor.capture(),
                headersCaptor.capture(), varsCaptor.capture(), typeCaptor.capture());

        Assert.assertEquals(NullNode.getInstance(), bodyCaptor.getValue().get("pipeline"));
    }
}
