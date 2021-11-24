package com.tencentcloud.cls.producer.request;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The base request of all sls request
 * @author farmerx
 *
 */
public class Request implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5830692390140453699L;
	private Map<String, String> mParams = new HashMap<String, String>();

	/**
	 * Construct the base request
	 */
	public Request() { }

	/**
	 * Get the value of given key in the request
	 * @param key key name
	 * @return value of the key
	 */
	public String GetParam(String key) {
		if (mParams.containsKey(key)) {
			return mParams.get(key);
		} else {
			return new String();
		}
	}

	/**
	 * Set a key/value pair into the request
	 * @param key key name
	 * @param value value of the key
	 */
	public void SetParam(String key, String value) {
		if (value == null)
		{
			mParams.put(key, "");
		}
		else
		{
			mParams.put(key, value);
		}

	}

	/**
	 * Get all the parameter in the request
	 * @return all parameter
	 */
	public Map<String, String> GetAllParams() {
		return mParams;
	}

}
