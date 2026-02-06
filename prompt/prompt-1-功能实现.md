在当前 Spring Boot 2.7.x（Java 8）项目中新增 Project 管理功能：CRUD + 分页 + 参数校验 + 统一返回体 + 全局异常处理。数据访问使用 MyBatis-Plus，连接本地 MySQL（库名 `test`，用户名/密码 `root/root`）。需要在 Maven 中自行补齐相关依赖（MyBatis-Plus、mysql-connector-j、validation、测试依赖等）。

### 数据库与 SQL

- 建表 SQL 放项目根目录：`./db.sql`
- `db.sql` 必须包含：创建库 `test`（如不存在）+ 创建表 `project`
- 表结构（必须一致）：
  - `id` BIGINT AUTO_INCREMENT PK
  - `name` VARCHAR(50) NOT NULL
  - `owner` VARCHAR(50) NULL
  - `status` INT NOT NULL（取值固定：0=DRAFT，1=ACTIVE，2=ARCHIVED）
  - `created_at` DATETIME NOT NULL
  - `updated_at` DATETIME NOT NULL
  - `deleted` TINYINT NOT NULL DEFAULT 0（逻辑删除：0/1）
- 删除必须为逻辑删除：DELETE 接口只把 `deleted=1`，并更新 `updated_at`；所有查询只返回 `deleted=0`。

### 接口（固定）

Base：`/api/projects`

1. `POST /api/projects`
2. `GET /api/projects/{id}`（已删除视为不存在）
3. `GET /api/projects?page=&size=&keyword=`
   - page 从 1 开始，默认 1
   - size 默认 10，范围 1-100
   - keyword 可选，LIKE 匹配 name 或 owner
4. `PUT /api/projects/{id}`（不存在/已删除返回不存在）
5. `DELETE /api/projects/{id}`（逻辑删除，不物理删）

### 校验（必须）

- name：必填，长度 1-50
- status：必填且只能是 0/1/2
- owner：可选，长度 <= 50
- page/size：按上面范围校验（默认值也要实现）

### 统一返回体（所有成功）

```
{ "code": 0, "message": "ok", "data": ... }
```

分页 data 必须包含：`records,page,size,total`

### 全局异常（统一结构 + 中文 message + 固定 code）

失败统一：

```
{ "code": <非0>, "message": "<中文>", "data": <可选> }
```

错误码固定：

- 40001 参数校验失败（中文 message）
- 40002 请求参数格式/类型错误（中文 message）
- 40401 项目不存在（中文 message）
- 50000 服务内部错误（中文 message）
   用 `@RestControllerAdvice` 覆盖校验异常、类型转换异常、资源不存在、自定义业务异常、兜底异常。错误码集中定义（枚举或常量类）。

### MyBatis-Plus 要求

- 使用 BaseMapper/ServiceImpl/Page 实现 CRUD + 分页
- status 在 Java 侧用 enum 表达（0/1/2），DB 存 INT；需实现类型映射（MyBatis-Plus/TypeHandler 方式均可），并保证返回/入参使用整数
- created_at/updated_at 用 MetaObjectHandler 自动填充（创建/更新/删除都要更新 updated_at）
- 启用 MyBatis-Plus 逻辑删除，字段为 `deleted`

### 配置

- `application.yml` 写 MySQL 连接（默认 test 库 root/root，可用环境变量覆盖但要有默认值）

### 测试（必须连接 MySQL）

- 新增至少 3 个 MockMvc 测试，必须连本地 MySQL（test 库），不允许 H2
- 测试需可重复执行：测试前清理 project 表（或每条用唯一数据并清理）
- 至少覆盖：
  1. 创建成功返回 code=0
  2. 参数校验失败返回 40001 且 message 中文（例如 status=9）
  3. 不存在/已删除查询返回 40401 且 message 中文

### 输出（按顺序）

1. `./db.sql` 完整内容
2. 关键文件清单（新增/修改路径）
3. 所有新增/修改文件完整内容（按路径分组）
4. curl 验证命令 + 期望结果（覆盖 create/get/list/update/delete，且包含 1 个失败示例；展示逻辑删除后再查返回 40401）