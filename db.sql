-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE test;

-- 创建项目表
CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '项目名称',
    owner VARCHAR(50) NULL COMMENT '项目负责人',
    status INT NOT NULL COMMENT '状态：0=DRAFT，1=ACTIVE，2=ARCHIVED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    INDEX idx_name_owner (name, owner),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- 创建第三方调用日志表
CREATE TABLE IF NOT EXISTS external_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL COMMENT '追踪ID（从请求头 X-Trace-Id 读取，没有则生成 UUID）',
    request_id VARCHAR(64) NULL COMMENT '请求ID',
    service VARCHAR(50) NOT NULL COMMENT '服务名称（固定值：DICT_QUERY）',
    target_url VARCHAR(500) NULL COMMENT '目标URL',
    http_method VARCHAR(10) NULL COMMENT 'HTTP方法',
    query_string TEXT NULL COMMENT '查询参数',
    http_status INT NULL COMMENT 'HTTP状态码',
    success TINYINT NOT NULL COMMENT '是否成功（0=失败，1=成功）',
    attempt INT NOT NULL COMMENT '尝试次数（从1开始）',
    duration_ms BIGINT NULL COMMENT '耗时（毫秒）',
    exception_type VARCHAR(50) NULL COMMENT '异常类型（例如：TIMEOUT, IO_EXCEPTION, RATE_LIMIT等）',
    exception_message TEXT NULL COMMENT '异常消息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_trace_id (trace_id),
    INDEX idx_service (service),
    INDEX idx_created_at (created_at),
    INDEX idx_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方调用日志表';
