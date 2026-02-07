package com.codebuddy.backend.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * 第三方接口签名工具类
 */
public class ThirdPartySignatureUtil {

    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";
    private static final String TIME_ZONE = "Asia/Shanghai";
    private static final String ALGORITHM_HMAC_SHA1 = "HmacSHA1";
    private static final String CHARSET = "UTF-8";

    /**
     * 生成当前时间戳
     *
     * @return 格式化的时间戳字符串
     */
    public static String generateTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT_STRING);
        df.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        return df.format(new Date());
    }

    /**
     * POP特殊的URL编码规则
     * 在一般的URLEncode后再增加三种字符替换：加号（+）替换成 %20、星号（*）替换成 %2A、%7E 替换回波浪号（~）
     *
     * @param value 需要编码的值
     * @return 编码后的字符串
     */
    public static String specialUrlEncode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, CHARSET).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    /**
     * 构造生成签名的请求字符串
     *
     * @param path        请求路径（不包含baseUrl）
     * @param httpMethod  HTTP方法
     * @param appKey      AppKey
     * @param timestamp   时间戳
     * @param queryParams 查询参数
     * @return 待签名的字符串
     */
    public static String buildSignString(String path, String httpMethod, String appKey,
                                          String timestamp, Map<String, Object> queryParams) throws UnsupportedEncodingException {
        // 创建参数map的副本
        Map<String, Object> parasMap = new HashMap<>(queryParams);
        // 加入appKey
        parasMap.put("appKey", appKey);
        // 加入timestamp时间参数
        parasMap.put("timestamp", timestamp);

        // 根据参数Key排序
        TreeMap<String, Object> sortParasMap = new TreeMap<>(parasMap);

        // 把排序后的参数顺序拼接成如下格式：specialUrlEncode(参数Key) + "=" + specialUrlEncode(参数值)
        StringBuilder sortQueryStringTmp = new StringBuilder();
        for (String key : sortParasMap.keySet()) {
            sortQueryStringTmp.append("&").append(specialUrlEncode(key))
                    .append("=").append(specialUrlEncode(sortParasMap.get(key).toString()));
        }
        String sortedQueryString = sortQueryStringTmp.substring(1); // 去除第一个多余的&符号

        // 按POP的签名规则拼接成最终的待签名串
        // 规则如下：HTTPMethod + "&" + specialUrlEncode(url) + "&" + specialUrlEncode(sortedQueryString)
        return httpMethod.toUpperCase() + "&" + specialUrlEncode(path) + "&" + specialUrlEncode(sortedQueryString);
    }

    /**
     * 计算签名
     * 签名采用HmacSHA1算法 + Base64，编码采用UTF-8
     *
     * @param appSecret     AppSecret（需要在后面多加一个"&"字符）
     * @param stringToSign  待签名的字符串
     * @return Base64编码的签名
     */
    public static String sign(String appSecret, String stringToSign)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String signingKey = appSecret + "&";
        SecretKeySpec keySpec = new SecretKeySpec(signingKey.getBytes(CHARSET), ALGORITHM_HMAC_SHA1);
        Mac mac = Mac.getInstance(ALGORITHM_HMAC_SHA1);
        mac.init(keySpec);
        byte[] signData = mac.doFinal(stringToSign.getBytes(CHARSET));
        return java.util.Base64.getEncoder().encodeToString(signData);
    }

    /**
     * 主入口方法：生成签名
     *
     * @param httpMethod  HTTP方法
     * @param path        请求路径
     * @param queryParams 查询参数
     * @param appKey      AppKey
     * @param appSecret   AppSecret
     * @param timestamp   时间戳
     * @return 签名字符串
     */
    public static String generateSignature(String httpMethod, String path, Map<String, Object> queryParams,
                                           String appKey, String appSecret, String timestamp)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String stringToSign = buildSignString(path, httpMethod, appKey, timestamp, queryParams);
        return sign(appSecret, stringToSign);
    }
}
