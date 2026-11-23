# family-chat-backend（后端）

> 仓库总览见 [../README.md](../README.md)；Android 客户端见 [../android/README.md](../android/README.md)。

基于 Spring Boot 3 与 Netty 的即时通讯（IM）后端，提供 REST API 与 WebSocket 长连接两套通道，支持单聊、好友关系、AI 助手、对象存储直传，以及完整的指标与日志可观测性。

## 功能特性

- **账号**：注册、登录、刷新令牌、登出；JWT 鉴权 + Spring Security。
- **关系**：账号搜索、用户资料、好友列表与详情、好友请求的发送与处理。
- **会话与消息**：会话列表、历史消息（Protobuf 编码）、未读清除、置顶 / 隐藏 / 删除。
- **实时通道**：独立 Netty WebSocket 服务（WSS，运行时生成自签名证书），配合 RabbitMQ 广播与 Redis 路由实现多实例消息投递。
- **AI 助手**：接入 OpenAI 兼容大模型，通过工具调用查询好友、会话、历史消息并代发消息。
- **文件**：S3 兼容对象存储（Backblaze B2 / OSS 等）预签名直传，头像与图片走对象 key 持久化。
- **稳定性**：Redis 滑动窗口限流（用户 / IP / 接口 / 全局）、TraceId 链路追踪、统一响应与全局异常处理。
- **可观测性**：Actuator + Prometheus 指标、Loki 日志推送、Grafana RED 大盘与告警规则。

## 技术栈

| 层次 | 选型 |
| --- | --- |
| 基础 | Java 17、Spring Boot 3.5.6、Maven Wrapper |
| Web / 安全 | Spring MVC、Spring Security、JWT (jjwt)、Validation、Spring AOP |
| 存储 | MySQL 8 + MyBatis、Redis (Lettuce / Redisson) |
| 消息 | RabbitMQ、Netty WebSocket、Protobuf |
| 外部服务 | S3 兼容对象存储 (AWS SDK)、OpenAI 兼容大模型接口 (OkHttp) |
| 监控 | Micrometer Prometheus、Logback + loki4j、Loki / Prometheus / Grafana |

## 环境要求

- JDK 17+
- MySQL 8（数据库名 `liteim`）、Redis、RabbitMQ
- 可选：Docker（监控栈）、S3 兼容对象存储、OpenAI 兼容大模型服务
- 无需单独安装 Maven，仓库自带 `mvnw` / `mvnw.cmd`（首次运行自动下载 Maven 3.9.11）

## 快速开始

### 1. 准备数据库

创建 `liteim` 数据库并初始化业务表结构。

> 本目录（`backend/`）下的 `db-migrate.sql`、`db-optimize-index.sql`、`db-migrate-avatar-key.sql` 是针对**已有库结构**的增量迁移与索引优化脚本，不是初始化建表脚本，请按需执行。执行 `db-migrate-avatar-key.sql` 前需先把 `@oss_public_base` 改成你自己的对象存储公网地址。

### 2. 配置环境变量

```bash
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
```

编辑 `.env` 填写真实值。以下四项**没有默认值，缺失会导致启动失败**：

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | MySQL 密码 |
| `JWT_SECRET_KEY` | 至少 32 字节的随机字符串，可用 `openssl rand -base64 32` 生成 |
| `AI_API_KEY` | 大模型接口密钥 |
| `AI_BASE_URL` | OpenAI 兼容接口地址 |

其它变量（数据库地址、Redis、RabbitMQ、对象存储、Grafana 密码等）均有默认值或可留空，完整清单见 `.env.example`。

应用通过 `spring.config.import` 从运行目录（通常为 `backend/`）读取 `.env`；也可以直接使用系统环境变量（优先级更高）。**`.env` 已被 `.gitignore` 忽略，请勿提交；请一并忽略运行日志 `logs/`。**

### 3. 启动依赖

确保 MySQL、Redis、RabbitMQ 可访问。监控组件可选启动：

```bash
docker compose up -d          # Loki + Prometheus + Grafana
```

Grafana 默认账号 `admin`，密码由 `.env` 中的 `GRAFANA_ADMIN_PASSWORD` 指定（未配置时回退为 `admin`，上线前务必修改）。

### 4. 启动应用

```bash
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

或构建后运行：

```bash
./mvnw clean package -DskipTests
java -jar target/chat-0.0.1-SNAPSHOT.jar
```

## 端口

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| HTTP API | 8080 | REST 接口、`/actuator/prometheus` |
| WebSocket | 8081 | Netty WSS 长连接 |
| Grafana | 3000 | 容器，可视化大盘 |
| Prometheus | 9090 | 容器，指标抓取 |
| Loki | 3100 | 容器，日志接收 |
| Logstash | 5044 | 独立配置，容器编排中不包含 |

## 主要接口

| 前缀 | 说明 |
| --- | --- |
| `/api/auth` | 注册、登录、刷新令牌、登出 |
| `/api/user` | 用户资料、账号搜索 |
| `/api/friends` | 好友列表、好友详情 |
| `/api/friend_request` | 好友请求发送与列表 |
| `/api/chat` | 消息收发（Protobuf） |
| `/api/chat/session` | 会话列表、未读清除、置顶、隐藏、删除 |
| `/api/oss` | 头像 / 图片上传预签名 |

## 监控与日志

- `logback-spring.xml` 默认将日志异步推送到 `http://127.0.0.1:3100/loki/api/v1/push`，未部署 Loki 时请按环境调整该地址。
- `monitoring/prometheus/prometheus.yml` 抓取 `host.docker.internal:8080/actuator/prometheus`；Grafana 数据源与 RED 大盘通过 `monitoring/grafana/provisioning` 自动预置。
- `logstash.conf` 是可选的 Logstash → Elasticsearch 配置示例，需要自行准备这两个服务。

## 上线前安全提示

- `SecurityConfig` 当前为 `anyRequest().permitAll()` 且关闭 CSRF，`application.yml` 中 `management.endpoint.health.show-details` 为 `always`，公开部署前请按实际鉴权需求收紧。
- Netty 的 WSS 使用运行时生成的自签名证书，仅适用于开发调试，生产环境请替换为正式证书。
- 默认口令（MySQL `root`、RabbitMQ `guest/guest`、Grafana `admin`）仅用于本地开发，生产环境必须替换。

## 目录结构

```
src/main/java/com/family/chat/
├── ai/             # AI Agent 与工具调用
├── config/         # Bean 与属性配置
├── controller/     # REST 接口
├── netty/          # WebSocket 服务与会话管理
├── service/impl/   # 业务逻辑
├── mapper/         # MyBatis 数据访问
├── util/           # JWT、限流、账号生成等工具
└── protobuf/       # 消息 Protobuf 定义
src/main/resources/ # application.yml、logback-spring.xml
monitoring/         # Loki / Prometheus / Grafana 配置
```
