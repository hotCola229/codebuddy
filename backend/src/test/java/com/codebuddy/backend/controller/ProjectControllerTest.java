package com.codebuddy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 项目 Controller 测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // 清理测试数据
        // 删除所有测试创建的项目（通过多次尝试）
        for (long i = 1; i <= 100; i++) {
            try {
                mockMvc.perform(delete("/api/projects/" + i));
            } catch (Exception e) {
                // 忽略不存在的项目
            }
        }
    }

    /**
     * 测试创建项目成功
     */
    @Test
    void testCreateProjectSuccess() throws Exception {
        String requestBody = "{\"name\":\"测试项目\",\"owner\":\"张三\",\"status\":0}";

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.name").value("测试项目"))
                .andExpect(jsonPath("$.data.owner").value("张三"))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    /**
     * 测试参数校验失败 - status 值无效
     */
    @Test
    void testCreateProjectValidationFailure() throws Exception {
        String requestBody = "{\"name\":\"测试项目\",\"owner\":\"张三\",\"status\":9}";

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * 测试查询不存在的项目
     */
    @Test
    void testGetProjectNotFound() throws Exception {
        mockMvc.perform(get("/api/projects/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("项目不存在"));
    }

    /**
     * 测试逻辑删除后再查询返回不存在
     */
    @Test
    void testDeleteProjectThenQueryNotFound() throws Exception {
        // 1. 创建项目
        String requestBody = "{\"name\":\"待删除项目\",\"owner\":\"李四\",\"status\":1}";
        MvcResult createResult = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        // 获取创建的项目ID
        String response = createResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        Long projectId = jsonNode.get("data").get("id").asLong();

        // 2. 删除项目
        mockMvc.perform(delete("/api/projects/" + projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 3. 再次查询，应该返回 40401
        mockMvc.perform(get("/api/projects/" + projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("项目不存在"));
    }

    /**
     * 测试分页查询
     */
    @Test
    void testListProjects() throws Exception {
        // 先创建几个项目
        for (int i = 1; i <= 5; i++) {
            String requestBody = String.format("{\"name\":\"项目%d\",\"owner\":\"用户%d\",\"status\":0}", i, i);
            mockMvc.perform(post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));
        }

        // 分页查询
        mockMvc.perform(get("/api/projects?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(5)));
    }

    /**
     * 测试更新项目
     */
    @Test
    void testUpdateProject() throws Exception {
        // 1. 创建项目
        String requestBody = "{\"name\":\"原项目名\",\"owner\":\"原负责人\",\"status\":0}";
        MvcResult createResult = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        // 获取项目ID
        String response = createResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        Long projectId = jsonNode.get("data").get("id").asLong();

        // 2. 更新项目
        String updateBody = "{\"name\":\"新项目名\",\"owner\":\"新负责人\",\"status\":1}";
        mockMvc.perform(put("/api/projects/" + projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("新项目名"))
                .andExpect(jsonPath("$.data.owner").value("新负责人"))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    /**
     * 测试关键词搜索
     */
    @Test
    void testListProjectsWithKeyword() throws Exception {
        // 创建特定名称的项目
        String requestBody = "{\"name\":\"搜索测试项目\",\"owner\":\"测试用户\",\"status\":0}";
        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // 搜索
        mockMvc.perform(get("/api/projects?page=1&size=10&keyword=搜索"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }
}
