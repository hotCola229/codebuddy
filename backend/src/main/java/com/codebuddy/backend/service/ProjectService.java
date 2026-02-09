package com.codebuddy.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codebuddy.backend.common.ErrorCode;
import com.codebuddy.backend.converter.ProjectConverter;
import com.codebuddy.backend.dto.ProjectCreateRequest;
import com.codebuddy.backend.dto.ProjectUpdateRequest;
import com.codebuddy.backend.entity.Project;
import com.codebuddy.backend.exception.BusinessException;
import com.codebuddy.backend.mapper.ProjectMapper;
import com.codebuddy.backend.vo.ProjectResponseVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 项目 Service
 */
@Service
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {

    /**
     * 创建项目
     */
    public ProjectResponseVO create(ProjectCreateRequest request) {
        Project project = ProjectConverter.toEntity(request);
        save(project);
        return ProjectConverter.toVO(project);
    }

    /**
     * 根据ID获取项目
     */
    public ProjectResponseVO getById(Long id) {
        Project project = getEntityById(id);
        return ProjectConverter.toVO(project);
    }

    /**
     * 分页查询项目列表
     */
    public IPage<ProjectResponseVO> listProjects(Integer page, Integer size, String keyword) {
        Page<Project> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Project::getName, keyword)
                    .or()
                    .like(Project::getOwner, keyword)
            );
        }

        IPage<Project> entityPage = page(pageParam, queryWrapper);
        return entityPage.convert(ProjectConverter::toVO);
    }

    /**
     * 更新项目
     */
    public ProjectResponseVO update(Long id, ProjectUpdateRequest request) {
        Project existing = getEntityById(id);
        Project updateEntity = ProjectConverter.toEntity(request);
        existing.setName(updateEntity.getName());
        existing.setOwner(updateEntity.getOwner());
        existing.setStatus(updateEntity.getStatus());
        updateById(existing);
        return ProjectConverter.toVO(existing);
    }

    /**
     * 删除项目（逻辑删除）
     */
    public void delete(Long id) {
        getEntityById(id); // 验证项目存在
        removeById(id);
    }

    /**
     * 获取 Entity（内部使用）
     */
    private Project getEntityById(Long id) {
        Project project = super.getById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }
}
