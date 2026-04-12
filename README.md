# BBBus 内容服务项目

基于 Spring Cloud + Spring Cloud Alibaba 的微服务架构项目，专注于内容分享与订单管理业务。

## 📋 项目简介

本项目是一个内容服务平台，提供分享管理、订单处理、用户激励等核心功能。采用微服务架构，集成 Nacos、Sentinel、RocketMQ 等中间件，实现高可用、可扩展的业务系统。

### 核心特性

- ✅ **服务注册发现**：基于 Nacos 实现服务治理
- ✅ **声明式 HTTP 客户端**：使用 OpenFeign 进行服务调用
- ✅ **流量防护**：集成 Sentinel 实现熔断限流
- ✅ **消息队列**：RocketMQ 支持异步消息和事务消息
- ✅ **数据持久化**：MyBatis + MySQL
- ✅ **分享审核**：完整的内容审核流程

## 🏗️ 项目结构

```
BBBus/
├── content-service/            # 内容服务（核心业务）
│   ├── src/main/java/
│   │   └── com/bbbus/contentservice/
│   │       ├── ContentServiceApplication.java    # 应用启动类
│   │       ├── client/                 # Feign 客户端（调用外部服务）
│   │       ├── controller/             # REST 控制器
│   │       │   ├── OrderController.java        # 订单管理
│   │       │   ├── ShareAdminController.java   # 分享审核（管理员）
│   │       │   └── TestController.java         # 测试接口
│   │       ├── service/                # 业务逻辑层
│   │       ├── dao/                    # 数据访问层
│   │       ├── domain/                 # 领域实体
│   │       ├── dto/                    # 数据传输对象
│   │       ├── rocketmq/               # RocketMQ 消息处理
│   │       │   ├── AddBonusSource.java           # 消息源定义
│   │       │   ├── AddBonusStreamListener.java   # 消息监听器
│   │       │   └── AddBonusTransactionListener.java  # 事务监听器
│   │       ├── fallback/               # Feign 降级处理
│   │       └── fallbackfactory/        # Feign 降级工厂
│   ├── src/main/resources/
│   │   ├── application.yml             # 应用配置
│   │   └── mapper/                     # MyBatis 映射文件
│   │       ├── RocketMQTransactionLogMapper.xml
│   │       └── ShareMapper.xml
│   └── pom.xml
│
└── sentinel-dashboard-1.8.6.jar  # Sentinel 控制台
```

> ⚠️ **注意**：`stock-service-api` 和 `stock-service` 目录为历史遗留代码，已废弃，请勿使用。

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 | 开发语言 |
| Spring Boot | 2.7.18 | 应用框架 |
| Spring Cloud | 2021.0.5 | 微服务框架 |
| Spring Cloud Alibaba | 2021.0.5.0 | 阿里微服务组件 |
| Nacos | - | 服务注册与配置中心 |
| OpenFeign | - | 声明式 HTTP 客户端 |
| Sentinel | - | 流量控制与熔断降级 |
| RocketMQ | 2.0.3 | 消息中间件 |
| MyBatis | 1.3.0 | ORM 框架 |
| MySQL | 8.0.33 | 关系型数据库 |
| Maven | - | 项目构建工具 |

## 🚀 快速开始

### 前置要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Nacos Server（用于服务注册发现）
- RocketMQ（用于消息队列）

### 1️⃣ 启动 Sentinel 控制台（可选）

```bash
java -jar sentinel-dashboard-1.8.6.jar
```

默认访问地址：`http://localhost:8080`

### 2️⃣ 启动内容服务 (content-service)

```bash
cd content-service
mvn spring-boot:run
```

- **服务端口**：8080
- **主要功能**：
  - 订单管理
  - 分享审核
  - 用户积分奖励（通过 RocketMQ 消息）
  - 调用外部库存服务（通过 Feign）

## 📡 API 接口说明

### 内容服务 (content-service : 8080)

#### 创建订单
```http
POST /api/orders/place?skuCode=SKU-001&count=2
```

#### 分享审核（管理员）
```http
POST /api/shares/audit
Content-Type: application/json

{
  "shareId": 1,
  "auditStatus": "PASS"
}
```

> 💡 **提示**：具体的接口路径和参数请参考 `controller` 包下的代码实现。

## 🔑 核心设计理念

### 微服务架构

```
┌──────────────────────┐
│   content-service    │
│   (内容服务)          │
│                      │
│  ├─ 订单管理         │
│  ├─ 分享审核         │
│  ├─ 用户积分         │
│  └─ Feign 客户端     │
└──────────────────────┘
         │
         │ 调用外部服务
         ▼
┌──────────────────────┐
│  外部库存服务等       │
│  (通过 Feign 调用)    │
└──────────────────────┘
```

### 关键特性

1. **模块化设计**：清晰的分层架构（Controller → Service → DAO）
2. **消息驱动**：使用 RocketMQ 实现异步处理和最终一致性
3. **容错机制**：Feign 降级 + Sentinel 熔断保护
4. **事务消息**：RocketMQ 事务消息保证数据一致性

## ⚙️ 配置说明

### Nacos 配置

在各服务的 `application.yml` 中配置 Nacos 地址：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

### RocketMQ 配置

```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    group: bbbus-producer-group
```

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bbbus?useUnicode=true&characterEncoding=utf-8
    username: root
    password: your_password
```

## 🧪 测试验证

### 1. 验证服务注册

访问 Nacos 控制台 `http://localhost:8848/nacos`，确认以下服务已注册：
- `content-service`

### 2. 测试创建订单

```bash
curl -X POST "http://localhost:8080/api/orders/place?skuCode=SKU-001&count=2"
```

### 3. 测试分享审核

```bash
curl -X POST http://localhost:8080/api/shares/audit \
  -H "Content-Type: application/json" \
  -d '{"shareId": 1, "auditStatus": "PASS"}'
```

## 📝 开发规范

### 分支策略

- `main`：主分支，保持稳定可发布状态
- `feature/*`：功能开发分支
- `hotfix/*`：紧急修复分支

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 统一的异常处理和返回格式

## 🐛 常见问题

### Q1: Feign 调用失败

**原因**：服务未正确注册到 Nacos 或负载均衡器未配置

**解决**：
1. 检查 Nacos 控制台确认服务已注册
2. 确保引入了 `spring-cloud-starter-loadbalancer`
3. 检查服务名称是否正确

### Q2: Sentinel 规则不生效

**原因**：未正确配置 Sentinel Dashboard 地址

**解决**：
```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080
```

### Q3: RocketMQ 消息发送失败

**原因**：NameServer 地址配置错误或 Broker 未启动

**解决**：
1. 确认 RocketMQ 服务正常运行
2. 检查 `rocketmq.name-server` 配置
3. 查看生产者日志排查具体错误

## 📄 许可证

本项目仅供学习和参考使用。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

**项目地址**：[https://github.com/Ryen1999/BBBus](https://github.com/Ryen1999/BBBus)
