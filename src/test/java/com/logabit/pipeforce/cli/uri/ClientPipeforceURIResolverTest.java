package com.logabit.pipeforce.cli.uri;

import com.logabit.pipeforce.common.util.EncodeUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.logabit.pipeforce.cli.uri.ClientPipeforceURIResolver.Method.GET;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ClientPipeforceURIResolverTest {

    @Test
    public void testPipeforceURIFromCli_GET() {

        RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);

        ClientPipeforceURIResolver resolver = new ClientPipeforceURIResolver("http://localhost:8080/api/v3/",
                "someUsername", "somePassword", restTemplateMock);

        resolver.resolveToEntity(GET, "$uri:pipeline:global/app/io.pipeforce.sniederm/pipeline/hello", Map.class);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpMethod> methodCaptor = ArgumentCaptor.forClass(HttpMethod.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<Map> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplateMock, times(1)).exchange(uriCaptor.capture(), methodCaptor.capture(), entityCaptor.capture(), typeCaptor.capture(), varsCaptor.capture());

        Assert.assertEquals("http://localhost:8080/api/v3/pipeline:global/app/io.pipeforce.sniederm/pipeline/hello", uriCaptor.getValue());
        Assert.assertEquals(HttpMethod.GET, methodCaptor.getValue());
        Assert.assertEquals(Map.class, typeCaptor.getValue());
        Assert.assertEquals("Basic " + EncodeUtil.toBase64("someUsername:somePassword"),
                entityCaptor.getValue().getHeaders().get("Authorization").get(0));
    }
}