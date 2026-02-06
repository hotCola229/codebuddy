# CodeBuddy Project

Spring Boot 2.7.x 多模块项目，集成 Actuator 健康检查。

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Maven
- Spring Boot Actuator

## 项目结构

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

## 构建与测试

### 构建项目

```bash
mvn clean install
```

### 运行测试

```bash
mvn test
```

**期望结果**：所有测试通过，包括：
- HealthControllerTest.testHealthEndpoint
- HealthControllerTest.testActuatorHealthEndpoint

## 启动应用

```bash
mvn -pl backend spring-boot:run
```

**期望结果**：应用启动成功，在控制台看到类似输出：
```
Started BackendApplication in X.XXX seconds
```

## 验证健康检查

### 验证自定义健康检查接口

```bash
curl http://localhost:8080/health
```

**期望结果**：
```json
{"status":"UP"}
```

### 验证 Actuator 健康检查接口

```bash
curl http://localhost:8080/actuator/health
```

**期望结果**：
```json
{
  "status": "UP"
}
```

## 快速验证步骤

1. **运行测试**：
   ```bash
   mvn test
   ```
   应该看到所有测试通过

2. **启动应用**：
   ```bash
   mvn -pl backend spring-boot:run
   ```
   应用在 8080 端口启动

3. **验证健康接口**：
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/actuator/health
   ```
   两个接口都应返回 `{"status":"UP"}`
