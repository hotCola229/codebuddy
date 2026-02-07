package com.codebuddy.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 第三方调用日志表
 */
@Data
@TableName("external_call_log")
public class ExternalCallLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 追踪ID（从请求头 X-Trace-Id 读取，没有则生成 UUID）
     */
    private String traceId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 服务名称（固定值：DICT_QUERY）
     */
    private String service;

    /**
     * 目标URL
     */
    private String targetUrl;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 查询参数
     */
    private String queryString;

    /**
     * HTTP状态码
     */
    private Integer httpStatus;

    /**
     * 是否成功（0=失败，1=成功）
     */
    private Integer success;

    /**
     * 尝试次数（从1开始）
     */
    private Integer attempt;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 异常类型（例如：TIMEOUT, IO_EXCEPTION, RATE_LIMIT等）
     */
    private String exceptionType;

    /**
     * 异常消息
     */
    private String exceptionMessage;

    /**
     * 创建时间
     */
    private Date createdAt;
}
