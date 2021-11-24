package com.tencentcloud.cls.producer.response;

import com.tencentcloud.cls.producer.common.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base response class of all sls response
 * 
 * @author farmerx
 * 
 */
public class Response implements Serializable {
	private static final long serialVersionUID = 7331835262124313824L;
	private Map<String, List<String>> mHeaders = new HashMap<String, List<String>>();
	private Integer httpStatusCode = 0;

	/**
	 * Construct the base response body with http headers
	 * 
	 * @param headers  http headers
	 *
	 */
	public Response(Map<String, List<String>> headers) {
		SetAllHeaders(headers);
	}

	/**
	 * Get the request id of the response
	 * 
	 * @return request id
	 */
	public String GetRequestId() {
		return GetHeader(Constants.CONST_X_SLS_REQUESTID).get(0);
	}

	/**
	 * Get the value of a key in the http response header, if the key is not
	 * found, it will return empty
	 * 
	 * @param key key name
	 *
	 * @return the value of the key
	 */
	public List<String> GetHeader(String key) {
		if (mHeaders.containsKey(key)) {
			return mHeaders.get(key);
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 *  设置http状态码
	 * @param code
	 */
	public void SetHttpStatusCode(Integer code) {
		this.httpStatusCode = code;
	}

	/**
	 *  获取http返回的状态吗
	 * @return Integer
	 */
	public Integer GetHttpStatusCode() {
		return this.httpStatusCode;
	}

	/**
	 * Set http headers
	 *
	 * @param headers http headers
	 *
	 */
	private void SetAllHeaders(Map<String, List<String>> headers) {
		mHeaders = new HashMap<String, List<String>>(headers);
	}

	/**
	 * Get all http headers
	 * 
	 * @return http headers
	 */
	public Map<String, List<String>> GetAllHeaders() {
		return mHeaders;
	}

}
