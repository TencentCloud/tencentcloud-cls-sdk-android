package com.tencentcloudapi.cls.android.producer.request;

import com.tencentcloudapi.cls.android.producer.common.Constants;

public class SearchLogRequest extends Request {

    /**
     *
     * @param topicId
     * @param logsetId
     * @param from
     * @param to
     * @param queryString
     * @param limit
     */
    public SearchLogRequest(String topicId, String logsetId, String from, String to, String queryString, Integer limit) {
        super();
        SetLogSet(logsetId);
        SetTopic(topicId);
        SetQueryString(queryString);
        SetStartTime(from);
        SetEndTime(to);
        SetLimit(limit);
    }

    /**
     *
     * @param topicId
     * @param logsetId
     * @param from
     * @param to
     * @param queryString
     * @param limit
     * @param sort
     */
    public SearchLogRequest(String topicId, String logsetId, String from, String to, String queryString,  Integer limit, String sort) {
        super();
        SetLogSet(logsetId);
        SetTopic(topicId);
        SetQueryString(queryString);
        SetStartTime(from);
        SetEndTime(to);
        SetSort(sort);
        SetLimit(limit);
    }

    /**
     *
     * @param topicId
     * @param logsetId
     * @param from
     * @param to
     * @param queryString
     * @param limit
     * @param sort
     * @param context
     */
    public SearchLogRequest(String topicId, String logsetId, String from, String to, String queryString,  Integer limit,  String sort, String context) {
        super();
        SetLogSet(logsetId);
        SetTopic(topicId);
        SetQueryString(queryString);
        SetStartTime(from);
        SetEndTime(to);
        SetSort(sort);
        SetContext(context);
        SetLimit(limit);
    }

    public void SetContext(String context) {
        SetParam(Constants.CONST_CONTEXT, context);
    }

    public String GetContext() {
        return GetParam(Constants.CONST_CONTEXT);
    }

    public void SetSort(String sort) {
        SetParam(Constants.CONST_SORT, sort);
    }

    public String GetSort() {
        return GetParam(Constants.CONST_SORT);
    }

    public void SetLogSet(String logset) {
        SetParam(Constants.CONST_LOGSET_ID, logset);
    }

    public String GetLogSet() {
        return GetParam(Constants.CONST_LOGSET_ID);
    }

    public void SetTopic(String topic) {
        SetParam(Constants.CONST_TOPIC_IDS, topic);
    }

    public String GetTopic() {
        return GetParam(Constants.CONST_TOPIC_IDS);
    }

    public void SetQueryString(String query) {
        SetParam(Constants.CONST_QUERY_STRING, query);
    }

    public String GetQuery() {
        return GetParam(Constants.CONST_QUERY_STRING);
    }

    public String GetStartTime() {
        return GetParam(Constants.CONST_START_TIME);
    }

    public void SetStartTime(String from) {
        SetParam(Constants.CONST_START_TIME, from);
    }

    public String GetEndTime() {
        return GetParam(Constants.CONST_END_TIME);
    }

    public void SetEndTime(String to) { SetParam(Constants.CONST_END_TIME, to); }

    public void SetLimit(Integer limit) {
        SetParam(Constants.CONST_LIMIT, limit.toString());
    }

    public Integer GetLimit() {
        String limit =GetParam(Constants.CONST_LIMIT);
        if (limit.isEmpty()) {
            return 0;
        } else {
            return Integer.parseInt(limit);
        }
    }

}
