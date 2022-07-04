package com.tencentcloudapi.cls.android.producer.response;

import java.util.List;
import java.util.Map;

public class SearchLogResponse extends Response{

    private String result = "";
    /**
     * Construct the base response body with http headers
     *
     * @param headers http headers
     */
    public SearchLogResponse(Map<String, List<String>> headers) {
        super(headers);
    }

    /**
     *  设置检索返回结果
     * @param result search log result
     */
    public void SetResult(String result) {
        this.result = result;
    }

    /**
     *  获取检索接错
     * @return String
     */
    public String GetResult() {
        return this.result;
    }
}
