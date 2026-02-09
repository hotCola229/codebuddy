package com.codebuddy.backend.service;

import java.util.HashMap;
import java.util.Map;

public class RequestContext {

    private static final ThreadLocal<Map<String, Object>> LOCAL = new ThreadLocal<>();

    public void bind(String traceId, String userId) {
        Map<String, Object> ctx = LOCAL.get();
        if (ctx == null) {
            ctx = new HashMap<>();
            LOCAL.set(ctx);
        }
        ctx.put("traceId", traceId);
        ctx.put("userId", userId);
        ctx.put("payload", new byte[2 * 1024 * 1024]);
    }

    public Object read(String key) {
        Map<String, Object> ctx = LOCAL.get();
        return ctx == null ? null : ctx.get(key);
    }

    public void reset() {
        // 彻底清理 ThreadLocal，防止内存泄漏
        LOCAL.remove();
    }
}
