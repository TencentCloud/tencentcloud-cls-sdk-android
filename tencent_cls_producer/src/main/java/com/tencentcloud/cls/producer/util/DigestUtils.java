package com.tencentcloud.cls.producer.util;

import com.tencentcloud.cls.producer.common.Constants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author farmerx
 */
public final class DigestUtils {

    private DigestUtils() {
    }

    public static String md5Crypt(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.CONST_MD5);
            String res = new BigInteger(1, md.digest(bytes)).toString(16).toUpperCase();

            StringBuilder zeros = new StringBuilder();
            for (int i = 0; i + res.length() < 32; i++) {
                zeros.append("0");
            }
            return zeros.toString() + res;
        } catch (NoSuchAlgorithmException e) {
            // never happen
            throw new RuntimeException("Not Supported signature method "
                    + Constants.CONST_MD5, e);
        }
    }

    public static String sha1(String data) {
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA");
        } catch (Exception ignore) {
        }

        byte[] byteArray = new byte[0];
        try {
            byteArray = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignore) {
        }
        byte[] md5Bytes = sha.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static String hamcSha1(byte[] data, byte[] key) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            return byte2hex(mac.doFinal(data));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String byte2hex(byte[] b) {
        StringBuilder hs = new StringBuilder();
        String stmp;
        for (int n = 0; b != null && n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1)
                hs.append('0');
            hs.append(stmp);
        }
        return hs.toString();
    }

}
