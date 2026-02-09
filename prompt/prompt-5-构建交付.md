为当前 Spring Boot 微服务补齐“可交付能力”：完善 README、提供基于 **OpenJDK 8** 的 Docker 镜像构建与运行方式、提供一键脚本完成 **编译→测试→打包→构建镜像→Docker 运行→health 验证**。启动应用必须仅通过 Docker 方式，不提供本地 `mvn spring-boot:run` 启动步骤。不允许引入 docker compose。若容器需要访问 MySQL，则通过加入 Docker 网络 **`datawings`** 来访问已运行的 MySQL8 容器服务。

------

### 1) README 要求（必须）

README 必须包含可直接执行的章节：

- 环境要求：JDK8、Maven、Docker
- 运行测试：`mvn test` 命令 + 期望结果
- 构建镜像：`docker build` 命令 + 期望结果（镜像名/tag 固定并说明）
- Docker 启动应用：只给 Docker 运行方式（不写本地启动方式）
- 依赖说明（仅命令，不用 compose）：
  - 如果需要 MySQL：说明容器需加入网络 `datawings`，并在 `docker run` 中使用 `--network datawings`
  - 同时说明 DB 连接地址如何配置（例如使用 `MYSQL_HOST=mysql8` 或容器名/服务名）
- Health 验证：给出 curl 命令 + 期望结果（如 `/actuator/health` 返回 UP）
- FAQ：至少 3 条（端口占用、网络 datawings 不存在、配置未生效/连接失败等）

------

### 2) Docker 交付（必须）

- 提供 `Dockerfile`：运行时基于 **OpenJDK 8**
- 容器启动后服务端口可访问，并可通过 health 接口验证
- 必须外置第三方插件/配置：以挂载方式暴露（必须做到）
  - 宿主机 `./config` 挂载到容器 `/app/config`
  - Spring Boot 启动必须加载该目录配置（例如 `--spring.config.additional-location=/app/config/`）
  - README 和脚本必须说明修改挂载配置即可生效

------

### 3) 一键脚本（必须，不用 compose）

提供可执行脚本（例如 `scripts/onekey.sh`），必须实现：

1. `mvn clean test package`
2. `docker build` 构建镜像（镜像名/tag 固定）
3. `docker run` 启动容器（固定容器名，可重复执行自动覆盖）
   - 端口映射
   - 配置挂载：`-v $(pwd)/config:/app/config`
   - 若需要 MySQL：`--network datawings`（固定使用该网络名）
4. 自动 health 检查（curl 检查 health 返回 UP/ok），失败脚本退出非 0

脚本要求：

- 支持通过环境变量/参数控制：端口、镜像 tag、是否加入 datawings 网络、DB 连接信息（但必须给默认值）
- 重复执行不报错：已存在容器需先 stop/rm 再 run

------

### 4) CI（必须）

新增 CI 配置（按仓库现有体系；若无则新增 GitHub Actions）：

- 必须包含：checkout → setup JDK8 → `mvn clean test package`

------

### 验收（必须全部满足）

- README 完整且内容可执行
- 一键脚本可用：完成编译/测试/打包/构建镜像/运行容器/health 验证
- `mvn test` 通过

------

### 输出（按顺序）

1. 文件清单：新增/修改文件路径列表
2. 最短启动步骤：命令列表 + 期望结果（包含一键脚本与单独 docker build/run + health 验证命令）