package com.codebuddy.backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.codebuddy.backend.common.ApiResponse;
import com.codebuddy.backend.dto.ProjectCreateRequest;
import com.codebuddy.backend.dto.ProjectUpdateRequest;
import com.codebuddy.backend.service.ProjectService;
import com.codebuddy.backend.vo.ProjectResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 项目 Controller
 */
@RestController
@RequestMapping("/api/projects")
@Validated
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * 创建项目
     */
    @PostMapping
    public ApiResponse<ProjectResponseVO> create(@Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponseVO created = projectService.create(request);
        return ApiResponse.success(created);
    }

    /**
     * 根据ID获取项目
     */
    @GetMapping("/{id}")
    public ApiResponse<ProjectResponseVO> getById(@PathVariable Long id) {
        ProjectResponseVO project = projectService.getById(id);
        return ApiResponse.success(project);
    }

    /**
     * 分页查询项目列表
     */
    @GetMapping
    public ApiResponse<IPage<ProjectResponseVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页数量必须大于0") @Max(value = 100, message = "每页数量不能超过100") Integer size,
            @RequestParam(required = false) String keyword) {
        IPage<ProjectResponseVO> result = projectService.listProjects(page, size, keyword);
        return ApiResponse.success(result);
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public ApiResponse<ProjectResponseVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        ProjectResponseVO updated = projectService.update(id, request);
        return ApiResponse.success(updated);
    }

    /**
     * 删除项目（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success();
    }
}
