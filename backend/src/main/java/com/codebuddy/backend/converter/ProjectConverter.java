package com.codebuddy.backend.converter;

import com.codebuddy.backend.dto.ProjectCreateRequest;
import com.codebuddy.backend.dto.ProjectUpdateRequest;
import com.codebuddy.backend.entity.Project;
import com.codebuddy.backend.vo.ProjectResponseVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Project 实体与 DTO/VO 转换工具类
 */
public class ProjectConverter {

    /**
     * CreateRequest 转 Entity
     */
    public static Project toEntity(ProjectCreateRequest request) {
        if (request == null) {
            return null;
        }
        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        return project;
    }

    /**
     * UpdateRequest 转 Entity
     */
    public static Project toEntity(ProjectUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        return project;
    }

    /**
     * Entity 转 ResponseVO
     */
    public static ProjectResponseVO toVO(Project entity) {
        if (entity == null) {
            return null;
        }
        ProjectResponseVO vo = new ProjectResponseVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * Entity 列表转 ResponseVO 列表
     */
    public static List<ProjectResponseVO> toVOList(List<Project> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ProjectConverter::toVO)
                .collect(Collectors.toList());
    }
}
