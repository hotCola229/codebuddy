package com.codebuddy.backend.controller;

import com.codebuddy.backend.BackendApplication;
import com.codebuddy.backend.entity.ExternalCallLog;
import com.codebuddy.backend.mapper.ExternalCallLogMapper;
import com.codebuddy.backend.service.DictQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 字典查询控制器测试（使用 @MockBean）
 */
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DictQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExternalCallLogMapper externalCallLogMapper;

    @MockBean
    private DictQueryService dictQueryService;

    @BeforeEach
    void setup() {
        // 清理 external_call_log 表
        externalCallLogMapper.delete(null);
    }

    @Test
    @Order(1)
    @DisplayName("测试成功场景 - 200响应")
    void testSuccessScenario() throws Exception {
        // Mock 返回成功响应
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("total", 5);

        Map<String, String> dataItem = new HashMap<>();
        dataItem.put("code", "1");
        dataItem.put("value", "java类");

        mockResponse.put("data", java.util.Collections.singletonList(dataItem));
        mockResponse.put("totalPage", 1);
        mockResponse.put("currentPageNum", 1);
        mockResponse.put("pageSize", 10);

        when(dictQueryService.queryDict(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // 发起请求
        MvcResult result = mockMvc.perform(get("/api/dict/query")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("dictType", "job_type")
                        .header("X-Trace-Id", "test-trace-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        // 验证 Service 被调用
        verify(dictQueryService, times(1)).queryDict(1, 10, "job_type", "test-trace-001");

        System.out.println("✓ 成功场景测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("测试失败场景 - Service抛出异常")
    void testFailureScenario() throws Exception {
        // Mock 抛出异常
        when(dictQueryService.queryDict(anyInt(), anyInt(), anyString(), anyString()))
                .thenThrow(new RuntimeException("第三方服务异常"));

        // 发起请求
        mockMvc.perform(get("/api/dict/query")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("dictType", "job_type")
                        .header("X-Trace-Id", "test-trace-002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        // 验证 Service 被调用
        verify(dictQueryService, times(1)).queryDict(1, 10, "job_type", "test-trace-002");

        System.out.println("✓ 失败场景测试通过");
    }

    @Test
    @Order(3)
    @DisplayName("测试参数校验 - pageNum 小于 1")
    void testValidationPageNum() throws Exception {
        mockMvc.perform(get("/api/dict/query")
                        .param("pageNum", "0")
                        .param("pageSize", "10")
                        .param("dictType", "job_type")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 验证 Service 不被调用
        verify(dictQueryService, never()).queryDict(anyInt(), anyInt(), anyString(), anyString());

        System.out.println("✓ pageNum 参数校验测试通过");
    }

    @Test
    @Order(4)
    @DisplayName("测试参数校验 - pageSize 超出范围")
    void testValidationPageSize() throws Exception {
        mockMvc.perform(get("/api/dict/query")
                        .param("pageNum", "1")
                        .param("pageSize", "101")
                        .param("dictType", "job_type")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 验证 Service 不被调用
        verify(dictQueryService, never()).queryDict(anyInt(), anyInt(), anyString(), anyString());

        System.out.println("✓ pageSize 参数校验测试通过");
    }

    @Test
    @Order(5)
    @DisplayName("测试参数校验 - dictType 为空")
    void testValidationDictType() throws Exception {
        mockMvc.perform(get("/api/dict/query")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("dictType", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 验证 Service 不被调用
        verify(dictQueryService, never()).queryDict(anyInt(), anyInt(), anyString(), anyString());

        System.out.println("✓ dictType 参数校验测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("测试参数校验 - dictType 超过长度限制")
    void testValidationDictTypeLength() throws Exception {
        String longDictType = org.apache.commons.lang3.StringUtils.repeat("a", 51);
        mockMvc.perform(get("/api/dict/query")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("dictType", longDictType)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 验证 Service 不被调用
        verify(dictQueryService, never()).queryDict(anyInt(), anyInt(), anyString(), anyString());

        System.out.println("✓ dictType 长度校验测试通过");
    }
}
