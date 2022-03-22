package com.tencentcloudapi.cls.android.producer.http.comm;

import com.tencentcloudapi.cls.android.producer.util.Args;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author farmerx
 */
public class RequestMessage extends HttpMessage {
    private HttpMethod method = HttpMethod.GET; // HTTP Method. default GET.
    private URI endpoint;
    private String resourcePath;
    private Map<String, String> parameters = new HashMap<String, String>();

    public RequestMessage() {
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    /**
     * @return the endpoint
     */
    public URI getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return the resourcePath
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * @param resourcePath the resourcePath to set
     */
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(Map<String, String> parameters) {
        Args.notNull(parameters, "parameters");
        this.parameters = parameters;
    }
}
