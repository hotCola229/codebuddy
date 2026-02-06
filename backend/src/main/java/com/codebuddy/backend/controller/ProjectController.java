package com.codebuddy.backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codebuddy.backend.common.ApiResponse;
import com.codebuddy.backend.dto.ProjectCreateRequest;
import com.codebuddy.backend.dto.ProjectUpdateRequest;
import com.codebuddy.backend.entity.Project;
import com.codebuddy.backend.service.ProjectService;
import org.springframework.beans.BeanUtils;
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
    public ApiResponse<Project> create(@Valid @RequestBody ProjectCreateRequest request) {
        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        Project created = projectService.create(project);
        return ApiResponse.success(created);
    }

    /**
     * 根据ID获取项目
     */
    @GetMapping("/{id}")
    public ApiResponse<Project> getById(@PathVariable Long id) {
        Project project = projectService.getById(id);
        return ApiResponse.success(project);
    }

    /**
     * 分页查询项目列表
     */
    @GetMapping
    public ApiResponse<IPage<Project>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页数量必须大于0") @Max(value = 100, message = "每页数量不能超过100") Integer size,
            @RequestParam(required = false) String keyword) {
        IPage<Project> result = projectService.listProjects(page, size, keyword);
        return ApiResponse.success(result);
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public ApiResponse<Project> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        Project updated = projectService.update(id, project);
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
