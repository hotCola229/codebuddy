package com.codebuddy.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.codebuddy.backend.enums.ProjectStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体
 */
@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("owner")
    private String owner;

    @TableField("status")
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public ProjectStatus getStatusEnum() {
        return ProjectStatus.fromCode(status);
    }

    public void setStatusEnum(ProjectStatus statusEnum) {
        this.status = statusEnum.getCode();
    }
}
