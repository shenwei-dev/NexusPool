# FamilyChatApp（Android 客户端）

NexusPool 的 Android 客户端，Java + Gradle 单模块工程，与 `backend/` 的 Spring Boot 服务配套使用。

## 技术栈

- Java 11、AGP 8.13.0 / Gradle 8.13、compileSdk 36 / minSdk 24
- OkHttp + Retrofit（Gson / Protobuf）、OkHttp WebSocket、Room、Glide、uCrop
- `io.agora.rtc:lite-sdk` 音视频通话、`protobuf-javalite` 消息帧
- 消息二进制帧使用 Protobuf `MsgFrame`，定义见 `proto/MessageProto.proto`，需与后端的 `MessageProto.java` 保持字段编号一致

## 环境要求

- Android Studio（自带 JDK 17）与 Android SDK 36
- 后端服务及其依赖（MySQL / Redis / RabbitMQ）已启动，见 [`../backend/README.md`](../backend/README.md)
- 无需单独安装 Gradle，使用仓库自带的 `gradlew` / `gradlew.bat`

## 配置

```bash
cp local.properties.example local.properties   # Windows: Copy-Item local.properties.example local.properties
```

`local.properties` 由 Android Studio 生成 `sdk.dir`，再追加以下自定义项（**该文件不会入库**）：

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `server.host` | `10.0.2.2` | 模拟器访问宿主机；真机填局域网 IP 或域名 |
| `server.http.port` | `8080` | 后端 REST 端口 |
| `server.ws.port` | `8081` | 后端 Netty WSS 端口 |
| `amap.web.key` | 空 | 高德静态地图 WebKey |
| `agora.app.id` | 空 | 声网 AppID |

这些值在构建时通过 `buildConfigField` 注入 `BuildConfig`，由 `AppConfig` 等类读取。**源码、资源、Manifest 中不得再写入真实地址或第三方密钥**；未配置密钥时对应功能不可用，但应用仍可构建运行。

> 若此前曾在源码或已编译的 APK 中写入过高德 WebKey / 声网 AppID，请到对应开放平台**轮换密钥**，并清理或妥善保管历史 APK。

## 构建

```bash
./gradlew assembleDebug        # Windows: .\gradlew.bat assembleDebug
./gradlew installDebug         # 安装到已连接设备
```

Release 构建当前复用 debug 签名（`app/build.gradle`），正式发布前须替换为自有 keystore，且 keystore 永不入库。

## 与后端的契约

| 项 | 值 |
| --- | --- |
| REST | `http://<server.host>:8080`，路径前缀 `/api/auth`、`/api/user`、`/api/friends`、`/api/friend_request`、`/api/chat`、`/api/chat/session`、`/api/oss` |
| WebSocket | `wss://<server.host>:8081/ws?token=<accessToken>` |
| 消息编码 | Protobuf `MsgFrame`；文本帧为 JSON 信令，应用层心跳 type=99 / 30s |

## 已知安全待办

当前实现面向开发调试，公开部署前建议逐项整改：

- 明文 HTTP 与 `usesCleartextTraffic="true"`，应改用 HTTPS；
- WebSocket 客户端信任所有证书且未校验主机名，应改为证书固定（Certificate Pinning）；
- JWT 放在 WS URL query 中、明文存 SharedPreferences 且 `allowBackup="true"`，应改为首帧/Header 传递并加密存储、关闭备份；
- 缺少离线推送通道，进程被杀后仅靠 WS 长连接。

## 目录结构

```
android/
├── app/src/main/java/com/family/chat/   # api / network / ui / repository / viewmodel ...
├── app/src/main/res/                    # 布局、图标、资源
├── proto/MessageProto.proto             # 与后端共享的消息协议
├── gradle/libs.versions.toml            # 版本目录
└── build.gradle / settings.gradle       # 构建脚本
```
