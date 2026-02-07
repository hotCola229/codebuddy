package com.codebuddy.backend.controller;

import com.codebuddy.backend.common.ApiResponse;
import com.codebuddy.backend.common.ErrorCode;
import com.codebuddy.backend.exception.BusinessException;
import com.codebuddy.backend.service.DictQueryService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * 字典查询控制器
 */
@RestController
@RequestMapping("/api/dict")
@Validated
public class DictQueryController {

    @Autowired
    private DictQueryService dictQueryService;

    /**
     * 字典查询接口
     *
     * @param pageNum   页码（必填，>= 1）
     * @param pageSize  每页大小（必填，1..100）
     * @param dictType  字典类型（必填，非空，长度 <= 50）
     * @param traceId   追踪ID（从请求头 X-Trace-Id 读取）
     * @return 透传第三方响应
     */
    @GetMapping("/query")
    public ApiResponse<Object> queryDict(
            @RequestParam @Min(value = 1, message = "pageNum 必须 >= 1") Integer pageNum,
            @RequestParam @Min(value = 1, message = "pageSize 必须 >= 1") @Max(value = 100, message = "pageSize 必须 <= 100") Integer pageSize,
            @RequestParam @NotBlank(message = "dictType 不能为空") @Size(max = 50, message = "dictType 长度不能超过 50") String dictType,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        try {
            // 设置 MDC
            if (traceId != null && !traceId.isEmpty()) {
                MDC.put("traceId", traceId);
            }

            // 调用服务
            Map<String, Object> result = dictQueryService.queryDict(pageNum, pageSize, dictType, traceId);

            // 透传第三方响应
            return ApiResponse.success(result);

        } finally {
            MDC.clear();
        }
    }
}
