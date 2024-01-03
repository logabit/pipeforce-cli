package com.logabit.pipeforce.cli.uri;

import com.logabit.pipeforce.common.net.PipeforceURI;
import com.logabit.pipeforce.common.util.Assert;
import com.logabit.pipeforce.common.util.EncodeUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A PIPEFORCE URI resolver to be used at client side (CLI).
 * This can also be seen as a reference implementation for SDKs in other languages.
 *
 * @author sn
 * @since 10
 */
public class CliPipeforceURIResolver {

    private RestTemplate restTemplate;

    private String baseUrl;

    private String token;

    private String username;

    private String password;

    /**
     * HTTP Methods to be used to resolve a PIPEFORCE URI.
     */
    public enum Method {

        /**
         * Typically used to execute a pipeline or command.
         * Query params will become command params or pipeline vars.
         * No input body (null).
         */
        GET,

        /**
         * Typically used to execute a pipeline or command.
         * Query params will become command params or pipeline vars.
         * Request body will become input body for command or pipeline.
         */
        POST,

        /**
         * Typically used to execute a command.
         * Expect the command parameters in the body as url-encoded query string -> more secure.
         * Will overwrite any Content-Type header in the request to application/x-www-form-urlencoded.
         * Also see {@link com.logabit.pipeforce.common.util.UriUtil#getMapAsQuery(Map)}
         */
        POST_PARAMS_URLENCODED,

        /**
         * Typically used to upload a pipeline script or a property.
         */
        PUT,

        /**
         * Typically used to update a property.
         */
        PATCH,

        /**
         * Typically used to delete a property.
         */
        DELETE,

        CONNECT,
        TRACE,
        HEAD,
        OPTIONS
    }

    public CliPipeforceURIResolver(String baseUrl, String token, RestTemplate template) {
        this(baseUrl, null, null, template);
        this.token = token;
    }

    public CliPipeforceURIResolver(String baseUrl, String username, String password, RestTemplate template) {
        Assert.notNullOrEmpty(baseUrl, "Base URL may not be null or empty");
        Assert.notNull(template, "RestTemplate may not be null");

        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.restTemplate = template;
    }

    /**
     * Resolves the given PIPEFORCE URI at server side and returns the response from server
     * as required type.
     *
     * @param method       The HTTP method to use.
     * @param uri          The PIPEFORCE URI.
     * @param body         The optional body to send.
     * @param headers      The optional headers to send.
     * @param variables    The optional variables to replace in the uri.
     * @param requiredType The required Return type.
     * @param <T>          The return type.
     * @return
     */
    public <T> T resolveToObject(Method method, String uri, Object body, Map<String, Object> headers,
                                 Map<String, String> variables, Class<T> requiredType) {

        ResponseEntity<T> entity = resolveToEntity(method, uri, body, headers, variables, requiredType);

        if (entity == null) {
            return null;
        }

        return entity.getBody();
    }

    public <T> T resolveToObject(Method method, String uri, Class<T> requiredType) {
        return resolveToObject(method, uri, null, null, null, requiredType);
    }

    public <T> T resolveToObject(Method method, String uri, Map<String, Object> paramVars, Class<T> requiredType) {

        uri = StringUtil.replaceVariables(uri, paramVars);
        return resolveToObject(method, uri, null, null, null, requiredType);
    }


    /**
     * Same as {@link #resolveToObject(Method, String, Object, Map, Map, Class)} but returns the response entity
     * in order to be able to read response headers and additional information from the response.
     *
     * @param method       The HTTP method to use.
     * @param uri          The PIPEFORCE URI.
     * @param body         The optional body to send.
     * @param headers      The optional headers to send.
     * @param variables    The optional variables to replace in the uri.
     * @param requiredType The required Return type.
     * @param <T>          The return type.
     * @return
     */
    public <T> ResponseEntity<T> resolveToEntity(Method method, String uri, Object body, Map<String, Object> headers,
                                                 Map<String, String> variables, Class<T> requiredType) {


        PipeforceURI pipeforceURI = new PipeforceURI(uri);

        if (headers == null) {
            headers = new HashMap<>();
        }

        if (variables == null) {
            variables = new HashMap<>();
        }

        String methodName = method.name();
        if (method.equals(Method.POST_PARAMS_URLENCODED)) {
            methodName = "POST";
            headers.put("Content-Type", "application/x-www-form-urlencoded");
        }

        HttpEntity entity = new HttpEntity(body, mapToHttpHeaders(headers));

        ResponseEntity<T> r = this.restTemplate.exchange(this.baseUrl + pipeforceURI.getUriString(false),
                HttpMethod.valueOf(methodName), entity, requiredType, variables);

        return r;
    }

    public <T> ResponseEntity<T> resolveToEntity(Method method, String uri, Class<T> requiredType) {

        return resolveToEntity(method, uri, null, null, null, requiredType);
    }

    private HttpHeaders mapToHttpHeaders(Map<String, Object> map) {

        if (map == null) {
            map = new HashMap<>();
        }

        if ((!map.containsKey("Authorization"))) {
            if (this.token != null) {
                map.put("Authorization", "refresh " + this.token);
            } else if (this.username != null) {
                map.put("Authorization", "Basic " + EncodeUtil.toBase64(username + ":" + password));
            }
        }

        HttpHeaders headers = new HttpHeaders();
        for (String key : map.keySet()) {

            Object value = map.get(key);

            if (value instanceof Collection<?>) {
                headers.put(key, new ArrayList<>((Collection) value));
                continue;
            }

            if (value == null) {
                headers.add(key, null);
                continue;
            }

            headers.add(key, value + "");
        }

        return headers;
    }


}
