package com.codebuddy.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方接口配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party.dict")
public class ThirdPartyDictProperties {

    /**
     * 第三方接口baseUrl
     */
    private String baseUrl = "http://172.20.4.32:18022/";

    /**
     * AppKey
     */
    private String appKey = "28dc15bb-2e2c-45e4-b435-525853f69173";

    /**
     * AppSecret
     */
    private String appSecret = "0e27fdf2820802cdea8e0eb22b695c93";
}
