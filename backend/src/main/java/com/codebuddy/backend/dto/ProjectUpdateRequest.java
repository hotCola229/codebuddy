package com.codebuddy.backend.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 更新项目请求
 */
@Data
public class ProjectUpdateRequest {

    @NotNull(message = "项目名称不能为空")
    @Size(min = 1, max = 50, message = "项目名称长度必须在1-50之间")
    private String name;

    @Size(max = 50, message = "项目负责人长度不能超过50")
    private String owner;

    @NotNull(message = "项目状态不能为空")
    @Min(value = 0, message = "项目状态只能是0、1、2")
    @Max(value = 2, message = "项目状态只能是0、1、2")
    private Integer status;
}
