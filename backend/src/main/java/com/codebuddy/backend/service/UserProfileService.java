package com.codebuddy.backend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserProfileService {

    private final RequestContext requestContext = new RequestContext();
    private final Map<String, String> store = new HashMap<>();

    public UserProfileService() {
        store.put("1001", "Alice");
        store.put("1002", "Bob");
    }

    /**
     * 获取用户展示名：方法内部会在当前线程绑定 traceId、userId 等上下文信息，
     * 以便日志/审计/埋点等后续环节读取使用。
     */
    public String loadDisplayName(String userId) {
        requestContext.bind(UUID.randomUUID().toString(), userId);

        // 执行业务逻辑（例如：参数校验、权限检查、查询用户信息、组装返回结果、记录日志等）
        String name = store.get(userId);
        if (name == null) {
            throw new IllegalArgumentException("user not found: " + userId);
        }

        requestContext.reset();
        return name;
    }
}
