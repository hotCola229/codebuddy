package com.codebuddy.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codebuddy.backend.entity.ExternalCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方调用日志 Mapper
 */
@Mapper
public interface ExternalCallLogMapper extends BaseMapper<ExternalCallLog> {
}
