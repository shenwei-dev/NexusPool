# NexusPool

即时通讯（IM）系统，包含 Spring Boot 后端与 Android 客户端。两端通过 REST + WebSocket 通信，消息帧使用 Protobuf 编码。

## 仓库结构

```text
NexusPool/
├── backend/    # Spring Boot 3 后端：REST API、Netty WSS、MySQL/Redis/RabbitMQ、AI Agent、可观测性
│   ├── src/  pom.xml  mvnw  monitoring/  db-*.sql
│   └── docker-compose.yml  .env.example  README.md
├── android/    # Android 客户端（Java）：Retrofit/OkHttp、Room、声网音视频、Protobuf
│   ├── app/src/  proto/  gradle/  settings.gradle
│   └── local.properties.example  README.md
├── .gitignore / .gitattributes
└── LICENSE     # MIT
```

## 技术栈

| 端 | 主要选型 |
| --- | --- |
| `backend/` | Java 17 / Spring Boot 3.5.6 / Maven、MyBatis + MySQL、Redis + Redisson、RabbitMQ、Netty WebSocket、JWT、S3 兼容对象存储 |
| `android/` | Java 11 / AGP 8.13 / Gradle 8.13、OkHttp + Retrofit、Room、Glide、Agora Lite SDK、protobuf-javalite |

## 快速开始

1. **启动后端**：见 [`backend/README.md`](backend/README.md)。在 `backend/` 下把 `.env.example` 复制为 `.env` 并填值，准备好 MySQL / Redis / RabbitMQ 后执行 `./mvnw spring-boot:run`（Windows 用 `.\mvnw.cmd`）。
2. **运行客户端**：见 [`android/README.md`](android/README.md)。在 `android/` 下把 `local.properties.example` 复制为 `local.properties` 并填值，执行 `./gradlew installDebug`（Windows 用 `.\gradlew.bat`）。

可选监控栈：在 `backend/` 下执行 `docker compose up -d`（Loki / Prometheus / Grafana）。

## 端口与协议

| 项 | 值 |
| --- | --- |
| REST | `http://<host>:8080`，路径前缀 `/api/*` |
| WebSocket | `wss://<host>:8081/ws` |
| 消息编码 | Protobuf `MsgFrame`，定义见 `android/proto/MessageProto.proto`，后端实现见 `backend/src/main/java/com/family/chat/protobuf/MessageProto.java` |

## 配置与密钥

- 后端：`backend/.env.example` → `backend/.env`
- 客户端：`android/local.properties.example` → `android/local.properties`

两个目标文件均已被忽略。**真实密码、内网地址、第三方 API Key / AppID 一律不得写入源码或提交**；构建产物（`target/`、`build/`、APK）、运行日志与 IDE 配置同样不会入库。

> 若历史版本或已编译的 APK 中曾包含真实密钥，请到对应开放平台轮换，并清理或妥善保管历史产物。

## 许可

MIT，见 [LICENSE](LICENSE)。
