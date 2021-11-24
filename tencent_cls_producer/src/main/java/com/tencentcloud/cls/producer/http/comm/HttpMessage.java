package com.tencentcloud.cls.producer.http.comm;

import com.tencentcloud.cls.producer.http.utils.CaseInsensitiveMap;
import java.util.Map;

/**
 * The base class for message of HTTP request and response.
 * @author xiaoming.yin
 *
 */
public abstract class HttpMessage {
	
	private Map<String, String> headers = new CaseInsensitiveMap<String>();
    private byte[] content;
    private long contentLength;

    protected HttpMessage() {
        super();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers){
        assert (headers != null);
        this.headers = headers;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

}
