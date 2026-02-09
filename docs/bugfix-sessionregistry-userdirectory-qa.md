# UserProfileService 缺陷修复 QA 文档

## 问题是什么/影响范围

### Q: 具体的问题是什么？
A: `UserProfileService` 类存在严重的 ThreadLocal 内存泄漏问题，主要表现在以下三个方面：

1. **RequestContext.reset() 清理不彻底**：只清空了 Map 中的键值对，但没有调用 `ThreadLocal.remove()`，导致 ThreadLocal 引用的对象无法被垃圾回收。
2. **异常情况下资源未清理**：当 `loadDisplayName` 方法抛出异常时，`requestContext.reset()` 不会被调用，导致 ThreadLocal 中残留大量数据（包括 2MB 的 payload）。
3. **并发场景下内存累积**：在多线程高并发场景下，每次调用 `bind()` 都会创建新的 2MB byte 数组，如果发生异常，这些数据会残留在 ThreadLocal 中，导致内存泄漏。

### Q: 影响范围有多大？
A: 
- **直接影响**：所有调用 `UserProfileService.loadDisplayName()` 的代码，特别是在并发场景下
- **内存影响**：每次异常调用会残留约 2MB 数据，在服务器长期运行和高并发场景下可能导致 OOM
- **业务影响**：如果线程池被重复使用，ThreadLocal 残留的数据可能导致用户信息错乱（traceId、userId 等上下文信息）

## 如何复现（命令）

### Q: 如何稳定复现这个问题？
A: 提供以下两种复现方式：

#### 方式1：运行新增的回归测试

```bash
cd backend
mvn test -Dtest=UserProfileServiceTest#testThreadLocalLeakOnException
```

#### 方式2：编写简单复现脚本

```java
import com.codebuddy.backend.service.UserProfileService;

public class ReproduceBug {
    public static void main(String[] args) {
        UserProfileService service = new UserProfileService();
        
        // 模拟异常调用，不会调用 reset()
        for (int i = 0; i < 1000; i++) {
            try {
                service.loadDisplayName("9999"); // 不存在的用户，抛异常
            } catch (IllegalArgumentException e) {
                // 预期异常，但 reset() 未被调用
            }
        }
        
        // 此时 ThreadLocal 中残留了大量 2MB 数据
        System.out.println("如果观察 JVM 堆内存，会发现明显的内存占用");
    }
}
```

#### 方式3：观察日志输出

运行测试后，可以看到以下输出：
```
⚠️  缺陷：reset() 只 clear() 了 Map，但没有调用 ThreadLocal.remove()
❌ 缺陷复现：异常时 ThreadLocal 未清理，残留 2MB 数据
```

## 根因是什么（指向代码）

### Q: 根因定位在哪里？
A: 根因涉及两个类和三个关键位置：

#### 1. **UserProfileService.java 第 21-31 行**

```java
public String loadDisplayName(String userId) {
    requestContext.bind(UUID.randomUUID().toString(), userId);

    // 执行业务逻辑
    String name = store.get(userId);
    if (name == null) {
        throw new IllegalArgumentException("user not found: " + userId);
    }

    requestContext.reset();  // ❌ 异常时不会执行到这里
    return name;
}
```

**问题**：当抛出异常时，`requestContext.reset()` 不会被调用，ThreadLocal 未清理。

#### 2. **RequestContext.java 第 26-31 行**

```java
public void reset() {
    Map<String, Object> ctx = LOCAL.get();
    if (ctx != null) {
        ctx.clear();  // ❌ 只清空 Map，没有调用 LOCAL.remove()
    }
}
```

**问题**：只清空了 Map 中的键值对，但 ThreadLocal 本身还在，引用的 Map 对象无法被 GC 回收。

#### 3. **RequestContext.java 第 18 行**

```java
ctx.put("payload", new byte[2 * 1024 * 1024]);  // ⚠️ 每次 bind 都创建 2MB 数组
```

**问题**：每次调用都分配 2MB，如果未正确清理，会造成严重的内存泄漏。

## 怎么修（改动点）

### Q: 修复方案是什么？
A: 采用最小修复原则，改动两处代码：

#### 改动1：UserProfileService.java - 使用 try-finally 确保清理

**文件**：`backend/src/main/java/com/codebuddy/backend/service/UserProfileService.java`

**改动前**（第 21-32 行）：
```java
public String loadDisplayName(String userId) {
    requestContext.bind(UUID.randomUUID().toString(), userId);

    // 执行业务逻辑
    String name = store.get(userId);
    if (name == null) {
        throw new IllegalArgumentException("user not found: " + userId);
    }

    requestContext.reset();
    return name;
}
```

**改动后**：
```java
public String loadDisplayName(String userId) {
    requestContext.bind(UUID.randomUUID().toString(), userId);

    try {
        // 执行业务逻辑
        String name = store.get(userId);
        if (name == null) {
            throw new IllegalArgumentException("user not found: " + userId);
        }
        return name;
    } finally {
        // 确保无论是否抛出异常都清理 ThreadLocal
        requestContext.reset();
    }
}
```

#### 改动2：RequestContext.java - 彻底清理 ThreadLocal

**文件**：`backend/src/main/java/com/codebuddy/backend/service/RequestContext.java`

**改动前**（第 26-31 行）：
```java
public void reset() {
    Map<String, Object> ctx = LOCAL.get();
    if (ctx != null) {
        ctx.clear();
    }
}
```

**改动后**：
```java
public void reset() {
    // 彻底清理 ThreadLocal，防止内存泄漏
    LOCAL.remove();
}
```

### Q: 为什么这样修复？
A: 
1. **使用 try-finally**：这是 Java 资源清理的标准模式，确保无论是否发生异常，`reset()` 都会被调用。
2. **调用 ThreadLocal.remove()**：这是 ThreadLocal 的正确清理方式，会从当前线程彻底删除 ThreadLocal 引用，允许 GC 回收对象。
3. **不修改 bind() 方法**：虽然 2MB 的 payload 较大，但只要正确清理，就不会造成累积泄漏。如果需要优化，可以单独作为性能优化任务。

## 如何验证/回归

### Q: 如何验证修复有效？
A: 通过以下步骤验证修复：

#### 步骤1：运行新增的回归测试

```bash
cd backend
mvn test -Dtest=UserProfileServiceTest
```

**期望结果**：
```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

测试输出应该包含：
```
✓ 正常场景测试通过：多次调用无异常，验证无内存泄漏
✓ RequestContext.reset() 测试通过：ThreadLocal 被彻底清理
✓ try-finally 测试通过：无论是否异常，reset 都会被调用
✓ 异常场景测试通过：异常后 ThreadLocal 被正确清理，可继续正常调用
✓ 并发测试通过：2000 次成功，2000 次异常，无内存泄漏
```

#### 步骤2：运行完整测试套件

```bash
cd backend
mvn test
```

**期望结果**：所有 19 个测试通过，无新增失败。

#### 步骤3：修复前后对比

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| 正常调用 | ThreadLocal 部分清理 | ThreadLocal 彻底清理 ✓ |
| 异常调用 | ThreadLocal 残留 2MB ✓ | ThreadLocal 彻底清理 ✓ |
| 并发调用（1000 次异常） | 内存累积约 2GB | 内存正常释放 ✓ |
| 后续调用 | 可能读取到旧数据 | 每次都是全新数据 ✓ |

#### 步骤4：内存监控（生产环境验证）

在生产环境中，可以通过以下方式验证：

1. **JVM 堆内存监控**：观察 GC 前后内存使用情况
2. **ThreadLocal 跟踪**：使用工具如 VisualVM、JProfiler 监控 ThreadLocal 大小
3. **压测验证**：执行包含异常场景的压测，验证内存稳定性

**命令示例**：
```bash
# 使用 jstat 监控 GC
jstat -gc <pid> 1000

# 使用 jmap 导出堆转储分析
jmap -dump:live,format=b,file=heap.hprof <pid>
```

## 风险与监控建议

### Q: 修复有什么风险？
A: 风险评估：

| 风险项 | 风险等级 | 说明 | 缓解措施 |
|--------|----------|------|----------|
| 行为变更 | 低 | reset() 行为从 clear() 变为 remove()，但对调用方无感知 | 新增回归测试验证 |
| 性能影响 | 低 | remove() 比 clear() 更轻量，无性能风险 | 性能测试验证 |
| 兼容性 | 无 | ThreadLocal 是实现细节，不影响接口契约 | 无需处理 |
| 线程安全 | 无 | ThreadLocal 本身是线程安全的 | 无需处理 |

**总体风险**：✅ 低风险，可安全发布

### Q: 如何在生产环境监控？
A: 建议以下监控策略：

#### 1. **JVM 内存监控**
```
指标：堆内存使用率、GC 频率、GC 耗时
阈值：堆内存 > 80% 告警，GC 频率 > 5次/分钟 告警
工具：Prometheus + Grafana、JMX
```

#### 2. **业务指标监控**
```
指标：
- loadDisplayName 调用次数
- loadDisplayName 异常率
- loadDisplayName 平均耗时

告警：
- 异常率 > 5% 告警
- 平均耗时 > 100ms 告警
```

#### 3. **ThreadLocal 专项监控**
```
通过 AOP 或日志记录：
- ThreadLocal bind 次数
- ThreadLocal reset 次数
- 差值应该接近 0（允许短暂波动）

示例代码：
@Around("execution(* *..UserProfileService.loadDisplayName(..))")
public Object monitor(ProceedingJoinPoint pjp) {
    log.info("Before: bind count = {}", RequestContext.getBindCount());
    Object result = pjp.proceed();
    log.info("After: reset count = {}", RequestContext.getResetCount());
    return result;
}
```

#### 4. **灰度发布策略**
```
阶段1：发布到测试环境，运行完整回归测试
阶段2：发布到预发布环境，执行压测（包含异常场景）
阶段3：生产环境 10% 流量灰度，观察 24 小时
阶段4：全量发布
```

### Q: 长期优化建议
A:
1. **优化 payload 大小**：评估 2MB 的 payload 是否必要，考虑使用对象池或延迟加载
2. **增加单元测试覆盖率**：确保 ThreadLocal 相关逻辑有充分测试
3. **代码审查机制**：建立 ThreadLocal 使用规范和审查清单
4. **定期内存分析**：建立定期的内存泄漏扫描机制

---

## 附录

### 变更文件清单
1. `backend/src/main/java/com/codebuddy/backend/service/UserProfileService.java` - 添加 try-finally
2. `backend/src/main/java/com/codebuddy/backend/service/RequestContext.java` - 改为 remove()
3. `backend/src/test/java/com/codebuddy/backend/service/UserProfileServiceTest.java` - 新增 5 个回归测试

### 相关链接
- ThreadLocal 官方文档：https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html
- 内存泄漏排查指南：[内部知识库]

### 联系人
- 修复人：AI Assistant
- 审核人：待定
- 影响范围：后端服务、用户模块
