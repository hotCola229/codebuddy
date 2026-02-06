# 项目初始化任务清单

## 已完成任务

- [x] 创建根 pom.xml（Maven 父项目配置）
- [x] 创建 backend 模块 pom.xml（Spring Boot 2.7.x + Actuator）
- [x] 创建 BackendApplication 启动类
- [x] 创建 HealthController 提供 /health 接口
- [x] 创建 application.yml 配置文件
- [x] 创建 HealthControllerTest 测试类

## 待验证任务

- [ ] 运行 `mvn test` 验证测试通过
  ```bash
  mvn test
  ```
  期望结果：测试全部通过

- [ ] 启动应用
  ```bash
  mvn -pl backend spring-boot:run
  ```
  期望结果：应用在 8080 端口启动成功

- [ ] 验证 /health 接口
  ```bash
  curl http://localhost:8080/health
  ```
  期望结果：`{"status":"UP"}`

- [ ] 验证 /actuator/health 接口
  ```bash
  curl http://localhost:8080/actuator/health
  ```
  期望结果：返回包含 `{"status":"UP"}` 的 JSON
