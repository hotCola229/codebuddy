package com.codebuddy.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codebuddy.backend.entity.Project;
import com.codebuddy.backend.exception.BusinessException;
import com.codebuddy.backend.mapper.ProjectMapper;
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
    public Project create(Project project) {
        save(project);
        return project;
    }

    /**
     * 根据ID获取项目
     */
    public Project getById(Long id) {
        Project project = super.getById(id);
        if (project == null) {
            throw new BusinessException(40401, "项目不存在");
        }
        return project;
    }

    /**
     * 分页查询项目列表
     */
    public IPage<Project> listProjects(Integer page, Integer size, String keyword) {
        Page<Project> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Project::getName, keyword)
                    .or()
                    .like(Project::getOwner, keyword)
            );
        }

        return page(pageParam, queryWrapper);
    }

    /**
     * 更新项目
     */
    public Project update(Long id, Project project) {
        Project existing = getById(id);
        existing.setName(project.getName());
        existing.setOwner(project.getOwner());
        existing.setStatus(project.getStatus());
        updateById(existing);
        return existing;
    }

    /**
     * 删除项目（逻辑删除）
     */
    public void delete(Long id) {
        Project project = getById(id);
        removeById(id);
    }
}
