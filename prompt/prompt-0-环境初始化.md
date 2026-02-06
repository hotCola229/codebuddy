**目标**
 以当前目录为根目录，初始化一个可运行的 Spring Boot 2.7.x（Java 8）项目，使用 Maven 多模块构建（当前仅需 1 个后端服务模块），集成 Spring Boot Actuator，并提供最小可运行与可测试的健康检查接口。

------

### 验收（必须全部满足）

1. `mvn test` 通过（**不依赖外部服务**，测试可在纯本机环境直接跑通）
2. 应用可启动（`mvn -pl backend spring-boot:run` 或等价方式），并提供：
   - `GET /actuator/health` 返回 UP（Actuator）
   - 额外提供 `GET /health` 返回 `{"status":"UP"}`（或同等含义 JSON）
3. `README.md` 写清：
   - 如何构建与测试（命令 + 期望结果）
   - 如何启动应用（命令 + 期望结果）
   - 如何验证 health（curl 命令 + 期望结果）

------

### 实现要求（必须）

- Maven 多模块：根 `pom.xml`（packaging=pom） + `backend` 模块
- 依赖尽量轻量常见：`spring-boot-starter-web`、`spring-boot-starter-actuator`、测试依赖
- `application.yml` 必须存在（可以很轻量），至少包含：
  - server port（可默认 8080）
  - actuator health 暴露配置（建议只暴露 health/info）
- 提供最小可验证代码：
  - 启动类 `BackendApplication`
  - `HealthController`（实现 `/health`）
  - 至少 1 个测试类（建议 `@SpringBootTest` + `MockMvc` 或 `TestRestTemplate`）验证 `/health` 返回 UP

------

### 默认参数

- 服务端口：`8080`

------

### 输出要求（按顺序输出，不能缺）

1. **TASKS.md 文件内容**：可勾选待办清单（含执行命令）
2. **变更文件列表**：新增/修改的文件路径清单
3. **所有新增/修改文件的完整内容**：按文件路径分组输出（可直接复制落盘）
4. **启动与验证步骤**：包含命令 + 期望结果（含 `mvn test`、启动、curl 验证）

------

### 结构目标（必须符合）

```
.
├── pom.xml
├── README.md
├── TASKS.md
└── backend
    ├── pom.xml
    └── src
        ├── main
        │   ├── java/.../BackendApplication.java
        │   ├── java/.../controller/HealthController.java
        │   └── resources/application.yml
        └── test
            └── java/.../HealthControllerTest.java
```