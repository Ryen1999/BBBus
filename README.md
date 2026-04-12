# 微服务分仓 + API 契约示例

这个仓库演示你描述的模式：

- `stock-service-api`：只放接口契约（DTO + API 接口），打成 `jar` 发布到 Maven 私服。
- `stock-service`：库存服务实现方，依赖 `stock-service-api` 并实现接口。
- `order-service`：订单服务消费方，只依赖 `stock-service-api`，通过 Feign 调用库存服务。

> 真实团队协作时，通常是三个独立仓库。这里为了演示放在一个仓库目录下。

## 目录结构

```text
.
├── stock-service-api      # 契约仓库（会发布 jar）
├── stock-service          # 库存服务实现
└── order-service          # 订单服务消费
```

## 1) 发布 API 契约

在 `stock-service-api` 目录执行（发布到你们私服时改成对应 settings + deploy）：

```bash
mvn clean install
```

这一步会把 `stock-service-api` 安装到本地 Maven 仓库（私服场景则是 deploy）。

## 2) 启动 stock-service

```bash
cd stock-service
mvn spring-boot:run
```

默认端口 `8081`，提供：

- `GET /api/stocks/{skuCode}`
- `POST /api/stocks/deduct`

## 3) 启动 order-service

```bash
cd order-service
mvn spring-boot:run
```

默认端口 `8080`，调用库存服务接口：

- `POST /api/orders/place?skuCode=SKU-1&count=2`

## 4) 关键价值

- `order-service` 团队不需要 `stock-service` 的业务代码。
- 团队只需依赖 `stock-service-api` 的版本（例如 `1.0.0`）。
- 服务间通过 API 契约版本协作，而不是源码耦合。

## 5) 生产建议

- 给 `stock-service-api` 维护语义化版本（`1.0.0`, `1.1.0`）。
- 对破坏性变更升级大版本。
- 在 CI 里对 API 契约做兼容性校验（如 OpenAPI diff / contract test）。
