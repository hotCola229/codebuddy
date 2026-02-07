package com.codebuddy.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codebuddy.backend.common.ErrorCode;
import com.codebuddy.backend.config.RateLimitConfig;
import com.codebuddy.backend.config.ThirdPartyDictProperties;
import com.codebuddy.backend.entity.ExternalCallLog;
import com.codebuddy.backend.exception.BusinessException;
import com.codebuddy.backend.mapper.ExternalCallLogMapper;
import com.codebuddy.backend.util.ThirdPartySignatureUtil;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 字典查询服务
 */
@Service
public class DictQueryService {

    private static final Logger logger = LoggerFactory.getLogger(DictQueryService.class);
    private static final String SERVICE_NAME = "DICT_QUERY";
    private static final String THIRD_PARTY_PATH = "/api/v1/dataapi/execute/dict/query";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ThirdPartyDictProperties thirdPartyDictProperties;

    @Autowired
    private Bucket dictQueryBucket;

    @Autowired
    private ExternalCallLogMapper externalCallLogMapper;

    /**
     * 查询字典（包含限流、重试、日志记录）
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param dictType  字典类型
     * @param traceId   追踪ID
     * @return 第三方响应
     */
    public Map<String, Object> queryDict(Integer pageNum, Integer pageSize, String dictType, String traceId) {
        long startTime = System.currentTimeMillis();
        AtomicInteger attemptCount = new AtomicInteger(0);

        try {
            // 设置 MDC
            if (traceId != null && !traceId.isEmpty()) {
                MDC.put("traceId", traceId);
            } else {
                traceId = UUID.randomUUID().toString();
                MDC.put("traceId", traceId);
            }

            // 检查限流
            if (!dictQueryBucket.tryConsume(1)) {
                // 限流落库
                saveCallLog(traceId, null, null, null, null, 0, 0,
                        System.currentTimeMillis() - startTime, "RATE_LIMIT", "Rate limit exceeded");
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "服务繁忙，请稍后重试");
            }

            // 调用第三方接口（带重试）
            return callThirdPartyWithRetry(pageNum, pageSize, dictType, traceId, startTime, attemptCount);

        } finally {
            MDC.clear();
        }
    }

    /**
     * 调用第三方接口（带重试）
     */
    @Retryable(value = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000))
    private Map<String, Object> callThirdPartyWithRetry(Integer pageNum, Integer pageSize, String dictType,
                                                        String traceId, long startTime, AtomicInteger attemptCount) {
        int currentAttempt = attemptCount.incrementAndGet();
        long requestStartTime = System.currentTimeMillis();

        try {
            // 构建请求参数
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("pageNum", pageNum);
            queryParams.put("pageSize", pageSize);
            queryParams.put("dictType", dictType);

            // 生成签名
            String timestamp = ThirdPartySignatureUtil.generateTimestamp();
            String signature = ThirdPartySignatureUtil.generateSignature(
                    "GET", THIRD_PARTY_PATH, queryParams,
                    thirdPartyDictProperties.getAppKey(), thirdPartyDictProperties.getAppSecret(), timestamp
            );

            // 构建请求URL
            String queryString = buildQueryString(queryParams);
            String targetUrl = thirdPartyDictProperties.getBaseUrl() + THIRD_PARTY_PATH + "?" + queryString;

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("AppKey", thirdPartyDictProperties.getAppKey());
            headers.set("Signature", signature);
            headers.set("Timestamp", timestamp);

            // 发送请求
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // 成功
            saveCallLog(traceId, targetUrl, HttpMethod.GET.name(), queryString,
                    response.getStatusCodeValue(), 1, currentAttempt,
                    System.currentTimeMillis() - requestStartTime, null, null);

            return response.getBody();

        } catch (HttpServerErrorException e) {
            logger.error("第三方接口返回5xx错误", e);
            saveCallLog(traceId, null, HttpMethod.GET.name(), null,
                    e.getStatusCode().value(), 0, currentAttempt,
                    System.currentTimeMillis() - requestStartTime, "HTTP_5XX", e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("第三方接口超时或IO异常", e);
            saveCallLog(traceId, null, HttpMethod.GET.name(), null,
                    null, 0, currentAttempt,
                    System.currentTimeMillis() - requestStartTime, "TIMEOUT", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("第三方接口调用异常", e);
            saveCallLog(traceId, null, HttpMethod.GET.name(), null,
                    null, 0, currentAttempt,
                    System.currentTimeMillis() - requestStartTime, e.getClass().getSimpleName(), e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "调用第三方接口失败: " + e.getMessage());
        }
    }

    /**
     * 保存调用日志（异常不影响接口返回）
     */
    private void saveCallLog(String traceId, String targetUrl, String httpMethod, String queryString,
                            Integer httpStatus, Integer success, Integer attempt, Long durationMs,
                            String exceptionType, String exceptionMessage) {
        try {
            ExternalCallLog log = new ExternalCallLog();
            log.setTraceId(traceId);
            log.setService(SERVICE_NAME);
            log.setTargetUrl(targetUrl);
            log.setHttpMethod(httpMethod);
            log.setQueryString(queryString);
            log.setHttpStatus(httpStatus);
            log.setSuccess(success);
            log.setAttempt(attempt);
            log.setDurationMs(durationMs);
            log.setExceptionType(exceptionType);
            log.setExceptionMessage(exceptionMessage);
            log.setCreatedAt(new Date());

            externalCallLogMapper.insert(log);
        } catch (Exception e) {
            logger.error("保存调用日志失败", e);
        }
    }

    /**
     * 构建查询字符串
     */
    private String buildQueryString(Map<String, Object> params) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("构建查询字符串失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "构建查询字符串失败");
        }
    }
}
