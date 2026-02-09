package com.codebuddy.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目响应 VO
 */
@Data
public class ProjectResponseVO {

    private Long id;

    private String name;

    private String owner;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
