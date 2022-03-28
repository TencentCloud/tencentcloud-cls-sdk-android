package com.tencentcloudapi.cls.android.producer.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


/**
 * @author farmerx
 */
public class QcloudClsSignature {
    public static final String LINE_SEPARATOR = "\n";
    public static final String Q_SIGN_ALGORITHM_KEY = "q-sign-algorithm";
    public static final String Q_SIGN_ALGORITHM_VALUE = "sha1";
    public static final String Q_AK = "q-ak";
    public static final String Q_SIGN_TIME = "q-sign-time";
    public static final String Q_KEY_TIME = "q-key-time";
    public static final String Q_HEADER_LIST = "q-header-list";
    public static final String Q_URL_PARAM_LIST = "q-url-param-list";
    public static final String Q_SIGNATURE = "q-signature";
    public static final String DEFAULT_ENCODING = "UTF-8";

    private static Map<String, String> buildSignHeaders(Map<String, String> originHeaders) {
        Map<String, String> signHeaders = new HashMap<>();
        for (Entry<String, String> headerEntry : originHeaders.entrySet()) {
            String key = headerEntry.getKey();
            if (key.equalsIgnoreCase("content-type") || key.equalsIgnoreCase("content-md5")
                    || key.equalsIgnoreCase("host") || key.startsWith("x") || key.startsWith("X")) {
                String lowerKey = key.toLowerCase();
                String value = headerEntry.getValue();
                signHeaders.put(lowerKey, value);
            }
        }

        return signHeaders;
    }

    private static String buildSignMemberStr(Map<String, String> signHeaders) {
        StringBuilder strBuilder = new StringBuilder();
        boolean seenOne = false;
        for (String key : signHeaders.keySet()) {
            if (!seenOne) {
                seenOne = true;
            } else {
                strBuilder.append(";");
            }
            strBuilder.append(key.toLowerCase());
        }
        return strBuilder.toString();
    }

    private static String formatMapToStr(Map<String, String> kVMap) throws UnsupportedEncodingException {
        StringBuilder strBuilder = new StringBuilder();
        boolean seeOne = false;
        for (Entry<String, String> entry : kVMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String lowerKey = key.toLowerCase();
            String encodeKey = URLEncoder.encode(lowerKey, DEFAULT_ENCODING).replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
            String encodedValue = "";
            if (value != null) {
                encodedValue = URLEncoder.encode(value, DEFAULT_ENCODING).replace("+", "%20").replace("*", "%2A")
                        .replace("%7E", "~");
            }
            if (!seeOne) {
                seeOne = true;
            } else {
                strBuilder.append("&");
            }
            strBuilder.append(encodeKey).append("=").append(encodedValue);
        }
        return strBuilder.toString();
    }

    private static String buildTimeStr(long expireMillsecond) {
        StringBuilder strBuilder = new StringBuilder();
        long startTime = System.currentTimeMillis() / 1000 - 60;
        long endTime = (System.currentTimeMillis() + expireMillsecond) / 1000;
        strBuilder.append(startTime).append(";").append(endTime);
        //return "1510109254;1510109314";
        return strBuilder.toString();
    }

    public static String buildSignature(String secretId, String secretKey, String method, String path,
                                        Map<String, String> paramMap, Map<String, String> headerMap, long expireMillsecond)
            throws UnsupportedEncodingException {

        Map<String, String> signHeaders = buildSignHeaders(headerMap);
        TreeMap<String, String> sortedSignHeaders = new TreeMap<>();
        TreeMap<String, String> sortedParams = new TreeMap<>();

        sortedSignHeaders.putAll(signHeaders);
        sortedParams.putAll(paramMap);

        String qHeaderListStr = buildSignMemberStr(sortedSignHeaders);
        String qUrlParamListStr = buildSignMemberStr(sortedParams);
        String qKeyTimeStr, qSignTimeStr;
        qKeyTimeStr = qSignTimeStr = buildTimeStr(expireMillsecond);
        String signKey = DigestUtils.hamcSha1( qKeyTimeStr.getBytes(), secretKey.getBytes());
        String formatMethod = method.toLowerCase();
        String formatParameters = formatMapToStr(sortedParams);
        String formatHeaders = formatMapToStr(sortedSignHeaders);

        String formatStr = formatMethod + LINE_SEPARATOR +
                path + LINE_SEPARATOR + formatParameters +
                LINE_SEPARATOR + formatHeaders + LINE_SEPARATOR;
        //System.out.println(formatStr + "\n");
        String hashFormatStr = DigestUtils.sha1(formatStr);
        String stringToSign = Q_SIGN_ALGORITHM_VALUE +
                LINE_SEPARATOR + qSignTimeStr + LINE_SEPARATOR +
                hashFormatStr + LINE_SEPARATOR;
        //System.out.println(stringToSign + "\n");
        String signature = DigestUtils.hamcSha1(stringToSign.getBytes(), signKey.getBytes());

        return Q_SIGN_ALGORITHM_KEY + "=" +
                Q_SIGN_ALGORITHM_VALUE + "&" + Q_AK + "=" +
                secretId + "&" + Q_SIGN_TIME + "=" +
                qSignTimeStr + "&" + Q_KEY_TIME + "=" + qKeyTimeStr +
                "&" + Q_HEADER_LIST + "=" + qHeaderListStr + "&" +
                Q_URL_PARAM_LIST + "=" + qUrlParamListStr + "&" +
                Q_SIGNATURE + "=" + signature;
    }



}