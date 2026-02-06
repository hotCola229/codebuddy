package com.codebuddy.backend.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 项目状态枚举
 */
public enum ProjectStatus {
    DRAFT(0, "草稿"),
    ACTIVE(1, "活跃"),
    ARCHIVED(2, "已归档");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    ProjectStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ProjectStatus fromCode(int code) {
        for (ProjectStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid project status code: " + code);
    }
}
