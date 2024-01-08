package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.net.ClientPipeforceURIResolver;
import com.logabit.pipeforce.common.net.Request;
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
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolve(requestCaptor.capture(), typeCaptor.capture());

        Request request = requestCaptor.getValue();
        Assert.assertEquals("$uri:pipeline:global/app/myapp/pipeline/hello", request.getUri());
    }

    @Test
    public void testRunFile() throws Exception {
        //TODO expect this but system specific "/some/home/pipeforce/properties/global/app/myapp/pipeline/hello.pi.yaml"
        when(outputService.readFileToString(Mockito.anyString())).thenReturn("pipeline:");

        PipelineCliCommand localRun = (PipelineCliCommand) cliContext.createCommandInstance("pipeline");
        localRun.call(new CommandArgs("properties/global/app/myapp/pipeline/hello.pi.yaml"));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolve(requestCaptor.capture(), typeCaptor.capture());

        Request request = requestCaptor.getValue();
        Assert.assertEquals(NullNode.getInstance(), ((JsonNode) request.getBody()).get("pipeline"));
    }
}
