在现有 Spring Boot 2.7.x（Java 8）项目中新增“字典查询”封装能力：对外提供 GET `/api/dict/query`，仅做参数校验；内部调用第三方 GET 接口并自动补全签名 Header；对第三方调用实现可配置超时、重试与限流；每次外部调用必须把日志写入数据库表；测试使用 WireMock 覆盖成功与失败场景，`mvn test` 必须通过。实现过程中可参考我提供的第三方接口说明文档。

------

### 0) 第三方固定配置（必须写入 application.yml，可用环境变量覆盖但必须有默认值）

- baseUrl：`http://172.20.4.32:18022/`
- appKey：`28dc15bb-2e2c-45e4-b435-525853f69173`
- appSecret：`0e27fdf2820802cdea8e0eb22b695c93`

------

### 1) 对外接口（固定）

- `GET /api/dict/query`
- Query 参数（必填）：`pageNum`、`pageSize`、`dictType`
- 校验规则：
  - pageNum >= 1
  - pageSize 1..100
  - dictType 非空，长度 <= 50
- 返回：成功时透传第三方响应 body；失败按项目统一异常/返回体规则（若项目已有则沿用）

------

### 2) 第三方调用（固定）

- 第三方请求：`GET {baseUrl}/api/v1/dataapi/execute/dict/query`
- 第三方 Header 必须携带：`AppKey`、`Signature`、`Timestamp`
- Timestamp：格式 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`
- AppKey/AppSecret/baseUrl 从 `application.yml` 读取（默认值见第 0 条）

------

### 3) 签名生成必须封装为工具类（新增工具类）

必须把第三方文档中的“生成签名”步骤封装为独立工具类（例如 `ThirdPartySignatureUtil`），并在第三方调用处使用该工具类生成 Signature。

工具类必须提供：

- 生成 Timestamp（按格式与时区）
- specialUrlEncode（按文档规则：URLEncode 后 `+ -> %20`，`* -> %2A`，`%7E -> ~`）
- 构造待签名串：包含业务参数 + appKey + timestamp，按 key 排序拼接 queryString；待签名串为 `HTTPMethod&specialUrlEncode(url)&specialUrlEncode(sortedQueryString)`
- 计算签名：HmacSHA1 + Base64（UTF-8），签名密钥为 `appSecret + "&"`
- 主入口方法：输入（HTTPMethod、path、queryParams、appKey、appSecret、timestamp）输出 signature

------

### 4) 超时 / 重试 / 限流（确定技术选型，必须可配置）

HTTP 客户端固定使用 Spring `RestTemplate`。

**超时**

- 连接超时、读取超时：`application.yml` 可配置并注入 RestTemplate

**重试（固定 spring-retry）**

- 仅对第三方返回 5xx、以及超时/IO 异常重试
- 重试 2 次（最多 3 次尝试）
- 指数退避：initialDelay、multiplier、maxDelay 全部可配置
- 必须能获取 attempt 次数用于日志入库（attempt 从 1 开始）

**限流（固定 Bucket4j）**

- 限流对象：第三方调用（不是对外接口）
- 超限时不调用第三方，直接返回失败（走统一异常结构），并写入日志（exceptionType=RATE_LIMIT）

------

### 5) 调用日志必须落库（每次尝试都插入）

每次调用第三方（含每次重试、成功/失败/异常/被限流）都必须写 1 条日志到 MySQL `test` 库表 `external_call_log`。日志写入失败不得影响接口返回。

- 在项目根目录 `./db.sql` 中新增/更新 `external_call_log` 表（字段至少包含：trace_id/request_id/service/target_url/http_method/query_string/http_status/success/attempt/duration_ms/exception_type/exception_message/created_at + 必要索引）。
- traceId 规则：从请求头 `X-Trace-Id` 读取；没有则生成 UUID，写入 MDC 并贯穿本次请求。
- service 固定值：`DICT_QUERY`
- 被限流也必须落库：attempt=1，httpStatus 为空，success=0，exceptionType=RATE_LIMIT

------

### 6) 测试（固定 WireMock，断言重试次数 + 日志落库条数）

- 测试中启动 WireMock 作为第三方服务（不要直接调用真实 baseUrl）
- 至少覆盖：
  1. 成功：200 + JSON → `/api/dict/query` 透传成功；日志表插入 1 条（attempt=1, success=1, http_status=200）
  2. 失败(500)：连续 500 → 触发重试 2 次（共 3 次请求）；日志表插入 3 条（attempt=1..3, success=0, http_status=500）
- 测试连接本地 MySQL `test`（root/root），测试前清理 `external_call_log`，保证可重复
- `mvn test` 必须通过

------

### 7) 输出（按顺序）

1. `application.yml` 配置项说明（给出 key 与示例值）：baseUrl/appKey/appSecret、连接/读取超时、spring-retry 退避参数、Bucket4j 限流参数
2. 验证步骤：curl 成功/失败示例 + 期望结果；查询日志表 SQL 示例（按 trace_id）
3. 关键文件清单（新增/修改路径）