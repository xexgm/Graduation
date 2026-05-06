# WSS 自签证书实现方案

## 1. 目标与当前项目现状

本方案用于把当前 Netty WebSocket 服务从普通 `ws://` 升级为 `wss://`，优先满足本地开发、毕业设计演示和简单部署场景。

当前项目结构中，实时通信服务由 `Graduation-Netty` 模块提供，核心启动类为：

- `Graduation-Netty/src/main/java/com/gm/graduation/netty/server/NettyServer.java`
- WebSocket 路径：`/ws`
- Netty 监听端口：`9999`
- 当前 Pipeline：`HttpServerCodec` -> `HttpObjectAggregator` -> `WebSocketServerProtocolHandler` -> 业务编解码/处理器

要实现 WSS，后端需要在 Pipeline 最前面加入 Netty 的 `SslHandler`。TLS 握手成功后，后面的 HTTP Upgrade 和 WebSocket 业务逻辑基本不用改。

升级前：

```text
客户端 -> ws://localhost:9999/ws -> Netty WebSocket
```

升级后：

```text
客户端 -> wss://localhost:9999/ws -> TLS -> Netty WebSocket
```

## 2. 整体实现思路

采用“Netty 进程内直接启用 TLS”的简单方案：

1. 使用 `keytool` 生成一个本地自签 PKCS12 证书。
2. 后端启动时读取证书、私钥和密码，构建 Netty `SslContext`。
3. 在 `NettyServer.initChannel(...)` 的 Pipeline 第一位添加 `SslHandler`。
4. 前端把 WebSocket 地址从 `ws://localhost:9999/ws` 改成 `wss://localhost:9999/ws`。
5. 浏览器或客户端需要信任自签证书，否则连接会被拦截。

这个方案不引入 Nginx、Caddy、ACME、证书自动续期，也不改变现有 WebSocket 消息协议，改动面比较小。

## 3. 后端实现方案

### 3.1 生成自签证书

建议开发阶段把证书放在 `IM-Bootstrap/src/main/resources/certs/local-wss.p12`，这样 Spring Boot 打包后也能从 classpath 读取。

先创建目录：

```bash
mkdir -p IM-Bootstrap/src/main/resources/certs
```

生成证书：

```bash
keytool -genkeypair \
  -alias graduation-im-wss \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -storetype PKCS12 \
  -keystore IM-Bootstrap/src/main/resources/certs/local-wss.p12 \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=localhost, OU=dev, O=graduation, L=dev, ST=dev, C=CN" \
  -ext SAN=dns:localhost,ip:127.0.0.1
```

说明：

- `CN=localhost` 和 `SAN=dns:localhost,ip:127.0.0.1` 很重要，浏览器会校验证书域名/IP 是否匹配。
- 本地访问 `wss://localhost:9999/ws` 时，证书中必须包含 `localhost`。
- 本地访问 `wss://127.0.0.1:9999/ws` 时，证书中必须包含 `127.0.0.1`。
- `changeit` 只是开发示例密码，正式环境不要使用。
- 自签证书和密码不要提交到公开仓库；如果只是毕设本地演示，可以提交示例证书，但要在文档里说明仅限开发。

### 3.2 增加 WSS 配置

当前 `NettyServer` 是在 `ImBootstrapApplication` 中手动 `new NettyServer()`，不是 Spring Bean，所以最简单的方式是让 Netty 侧直接读取环境变量或 JVM 启动参数。

建议新增一个配置类，例如：

```java
package com.gm.graduation.common.utils;

public class WssConfig {

    public static boolean enabled() {
        return Boolean.parseBoolean(getValue("NETTY_SSL_ENABLED", "netty.ssl.enabled", "false"));
    }

    public static String keyStorePath() {
        return getValue("NETTY_SSL_KEY_STORE", "netty.ssl.key-store", "classpath:certs/local-wss.p12");
    }

    public static String keyStorePassword() {
        return getValue("NETTY_SSL_KEY_STORE_PASSWORD", "netty.ssl.key-store-password", "changeit");
    }

    public static String keyStoreType() {
        return getValue("NETTY_SSL_KEY_STORE_TYPE", "netty.ssl.key-store-type", "PKCS12");
    }

    private static String getValue(String envKey, String propertyKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }
}
```

推荐配置项：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `NETTY_SSL_ENABLED` / `netty.ssl.enabled` | `false` | 是否启用 WSS |
| `NETTY_SSL_KEY_STORE` / `netty.ssl.key-store` | `classpath:certs/local-wss.p12` | 证书位置 |
| `NETTY_SSL_KEY_STORE_PASSWORD` / `netty.ssl.key-store-password` | `changeit` | 证书密码 |
| `NETTY_SSL_KEY_STORE_TYPE` / `netty.ssl.key-store-type` | `PKCS12` | 证书类型 |

这样可以保留本地普通 `ws://` 启动能力，需要 WSS 时再开启开关。

### 3.3 创建 SslContext 工厂

建议在 `Graduation-Netty` 模块中增加一个工具类，例如：

```java
package com.gm.graduation.netty.server;

import com.gm.graduation.common.utils.WssConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;

public class NettySslContextFactory {

    public static SslContext buildServerSslContext() throws Exception {
        String keyStorePath = WssConfig.keyStorePath();
        char[] password = WssConfig.keyStorePassword().toCharArray();

        KeyStore keyStore = KeyStore.getInstance(WssConfig.keyStoreType());
        try (InputStream inputStream = openKeyStore(keyStorePath)) {
            keyStore.load(inputStream, password);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        return SslContextBuilder.forServer(keyManagerFactory)
            .protocols("TLSv1.3", "TLSv1.2")
            .build();
    }

    private static InputStream openKeyStore(String keyStorePath) throws Exception {
        if (keyStorePath.startsWith("classpath:")) {
            String resourcePath = keyStorePath.substring("classpath:".length());
            InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IllegalArgumentException("KeyStore not found in classpath: " + resourcePath);
            }
            return inputStream;
        }
        return new FileInputStream(keyStorePath);
    }
}
```

说明：

- 使用 PKCS12 的好处是证书链和私钥放在一个文件里，配置简单。
- `classpath:` 方便本地开发和打包运行。
- 外部文件路径方便部署时替换证书，例如 `/opt/graduation/certs/wss.p12`。

### 3.4 修改 NettyServer Pipeline

在 `NettyServer.start()` 中构建一个可选的 `SslContext`：

```java
final SslContext sslContext = WssConfig.enabled()
    ? NettySslContextFactory.buildServerSslContext()
    : null;
```

然后在 `initChannel(SocketChannel ch)` 中，把 `SslHandler` 放到 Pipeline 第一位：

```java
protected void initChannel(SocketChannel ch) throws Exception {
    if (sslContext != null) {
        ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
    }

    ch.pipeline()
        .addLast(new HttpServerCodec())
        .addLast(new HttpObjectAggregator(65536))
        .addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536, false, true))
        .addLast(new WebSocketFrameToMessageDecoder())
        .addLast(new MessageToWebSocketFrameEncoder())
        .addLast(new BusinessHandler());
}
```

完整顺序应为：

```text
SslHandler
HttpServerCodec
HttpObjectAggregator
WebSocketServerProtocolHandler("/ws", ...)
WebSocketFrameToMessageDecoder
MessageToWebSocketFrameEncoder
BusinessHandler
```

为什么 `SslHandler` 必须放最前面：

- TLS 握手发生在 HTTP/WebSocket 协议之前。
- 如果先放 `HttpServerCodec`，Netty 会把 TLS 握手数据当成 HTTP 数据解析，连接会失败。

### 3.5 启动方式

使用 JVM 参数开启：

```bash
mvn -pl IM-Bootstrap -am spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dnetty.ssl.enabled=true -Dnetty.ssl.key-store=classpath:certs/local-wss.p12 -Dnetty.ssl.key-store-password=changeit"
```

或者使用环境变量：

```bash
export NETTY_SSL_ENABLED=true
export NETTY_SSL_KEY_STORE=classpath:certs/local-wss.p12
export NETTY_SSL_KEY_STORE_PASSWORD=changeit
mvn -pl IM-Bootstrap -am spring-boot:run
```

启动成功后，日志可以补充打印当前协议：

```java
log.info("[NettyServer started] 初始化完成，监听端口: {}, protocol: {}", LISTENING_PORT,
    WssConfig.enabled() ? "wss" : "ws");
```

### 3.6 后端验证方式

可以先用 `openssl` 看 TLS 是否可用：

```bash
openssl s_client -connect localhost:9999 -servername localhost
```

看到证书信息和 TLS 协议版本，说明 TLS 层已经起来。

也可以用 `wscat` 测试。自签证书下需要允许不安全证书，仅限本地测试：

```bash
npx wscat -c wss://localhost:9999/ws --no-check
```

如果项目连接时要求 `token` query 参数，则使用：

```bash
npx wscat -c "wss://localhost:9999/ws?token=你的token" --no-check
```

连接建立后，仍然需要按现有协议发送 `messageType=0` 的应用层建连消息，否则服务端不会把用户 Channel 放入缓存。

## 4. 前端配合方案

### 4.1 修改 WebSocket 地址

当前前端文档中的地址是：

```typescript
private url: string = 'ws://localhost:9999/ws';
```

启用 WSS 后改成：

```typescript
private url: string = 'wss://localhost:9999/ws';
```

如果连接时带 token：

```typescript
const wsUrl = `wss://localhost:9999/ws?token=${encodeURIComponent(token)}`;
const ws = new WebSocket(wsUrl);
```

建议前端用环境变量区分开发和生产：

```typescript
const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'wss://localhost:9999/ws';
```

`.env.development` 示例：

```env
VITE_WS_URL=wss://localhost:9999/ws
```

如果是 Vue CLI 或其他脚手架，变量名按框架要求调整，例如 `VUE_APP_WS_URL`。

### 4.2 浏览器信任自签证书

自签证书默认不被浏览器信任，因此前端直接连接 `wss://localhost:9999/ws` 可能会失败，常见表现是：

- `WebSocket connection failed`
- `net::ERR_CERT_AUTHORITY_INVALID`
- `onclose` 触发，状态码可能不是业务状态码

本地开发有两种处理方式。

方式一：浏览器临时信任证书。

1. 先在浏览器打开 `https://localhost:9999/ws`。
2. 浏览器会提示证书不安全。
3. 选择继续访问或高级 -> 继续。
4. 再回到前端页面连接 `wss://localhost:9999/ws`。

注意：`https://localhost:9999/ws` 不是普通 HTTP 页面，可能显示 400/404/Upgrade 相关错误，这是正常的。关键是让浏览器先接受该 host:port 的自签证书。

方式二：把证书导入系统或浏览器信任区。

先导出证书：

```bash
keytool -exportcert \
  -alias graduation-im-wss \
  -keystore IM-Bootstrap/src/main/resources/certs/local-wss.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -rfc \
  -file local-wss.crt
```

macOS 可导入钥匙串并设为始终信任。Chrome/Safari 通常会使用系统钥匙串；Firefox 可能需要单独导入。

### 4.3 HTTPS 页面与 WSS 的关系

浏览器有混合内容限制：

- 页面是 `http://localhost:5173` 时，可以连接 `wss://localhost:9999/ws`。
- 页面是 `https://example.com` 时，不能连接 `ws://example.com:9999/ws`，必须使用 `wss://`。
- 如果页面是 HTTPS，但 WSS 证书不受信任，浏览器仍然会阻止连接。

因此，一旦前端页面通过 HTTPS 提供，WebSocket 也必须改为 WSS。

### 4.4 前端业务协议不需要变化

WSS 只改变传输层，不改变消息 JSON 结构。现有逻辑仍保持：

1. 登录接口获取 token。
2. 建立 `wss://.../ws?token=...` 连接。
3. `onopen` 后立即发送 `messageType=0` 的应用层建连消息。
4. 聊天室消息、私聊消息、心跳消息沿用现有 `CompleteMessage` 格式。

示例：

```typescript
ws.onopen = () => {
  ws.send(JSON.stringify({
    appId: 0,
    uid: userId,
    token,
    messageType: 0,
    content: '请求建立连接',
    timeStamp: Date.now()
  }));
};
```

## 5. 推荐落地步骤

### 第一步：只在本地启用 WSS

1. 生成 `local-wss.p12`。
2. 增加 `WssConfig`。
3. 增加 `NettySslContextFactory`。
4. 修改 `NettyServer`，在 Pipeline 第一位加 `SslHandler`。
5. 使用 `-Dnetty.ssl.enabled=true` 启动。
6. 前端改为 `wss://localhost:9999/ws`。
7. 浏览器信任自签证书后验证收发消息。

### 第二步：保留 ws/wss 开关

开发调试时可能还需要普通 WS，因此建议保留：

```bash
-Dnetty.ssl.enabled=false
```

或不配置 `NETTY_SSL_ENABLED` 时默认关闭。

这样项目可以同时支持：

```text
netty.ssl.enabled=false -> ws://localhost:9999/ws
netty.ssl.enabled=true  -> wss://localhost:9999/ws
```

注意：同一个 Netty 端口不能同时既处理 `ws://` 又处理 `wss://`，除非额外做端口拆分或协议识别。简单方案下建议一个端口只跑一种协议。

### 第三步：部署时替换证书

如果部署到服务器，例如使用 `chat.example.com`，需要重新生成包含域名的证书：

```bash
keytool -genkeypair \
  -alias graduation-im-wss \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -storetype PKCS12 \
  -keystore wss.p12 \
  -storepass 你的密码 \
  -keypass 你的密码 \
  -dname "CN=chat.example.com, OU=dev, O=graduation, L=dev, ST=dev, C=CN" \
  -ext SAN=dns:chat.example.com
```

启动时指定外部文件：

```bash
export NETTY_SSL_ENABLED=true
export NETTY_SSL_KEY_STORE=/opt/graduation/certs/wss.p12
export NETTY_SSL_KEY_STORE_PASSWORD=你的密码
java -jar IM-Bootstrap/target/IM-Bootstrap-1.0.0.jar
```

前端地址改为：

```env
VITE_WS_URL=wss://chat.example.com:9999/ws
```

如果后续要正式上线，建议换成 CA 签发证书，或者改用 Nginx/Caddy 统一管理证书。

## 6. 常见问题排查

### 6.1 连接报 `ERR_CERT_AUTHORITY_INVALID`

原因：浏览器不信任自签证书。

处理：

- 先访问 `https://localhost:9999/ws` 并手动继续访问。
- 或将导出的 `local-wss.crt` 加入系统信任。
- 或使用测试工具时加 `--no-check`，仅限开发测试。

### 6.2 连接报 `wrong version number` 或 `not an SSL/TLS record`

常见原因：

- 服务端启用了 WSS，但客户端仍连 `ws://`。
- 服务端未启用 WSS，但客户端连 `wss://`。

处理：确认 `NETTY_SSL_ENABLED` 和前端 URL 协议一致。

### 6.3 WebSocket 握手失败

常见原因：

- 连接路径不是 `/ws`。
- token query 参数缺失或无效。
- 前端没有在 `onopen` 后发送 `messageType=0` 建连消息。

处理：

- 确认地址为 `wss://localhost:9999/ws?token=xxx`。
- 确认后端 `WebSocketServerProtocolHandler("/ws", ...)` 未改路径。
- 查看 `BusinessHandler` 和 `LinkProcessor` 日志。

### 6.4 前端 HTTPS 页面无法连接 `ws://`

原因：浏览器混合内容限制。

处理：前端页面是 HTTPS 时，WebSocket 必须使用 `wss://`。

### 6.5 证书域名不匹配

例如证书只包含 `localhost`，但前端连接 `wss://127.0.0.1:9999/ws`，浏览器仍可能拒绝。

处理：

- 本地证书同时加入 `SAN=dns:localhost,ip:127.0.0.1`。
- 前端连接地址和证书 SAN 保持一致。

## 7. 最小改造清单

后端：

- 新增 `WssConfig`，读取是否启用 WSS、证书路径和密码。
- 新增 `NettySslContextFactory`，从 PKCS12 证书构建 `SslContext`。
- 修改 `NettyServer`，在 Pipeline 第一位加入 `SslHandler`。
- 启动时增加 `NETTY_SSL_ENABLED=true` 或 `-Dnetty.ssl.enabled=true`。

前端：

- WebSocket 地址从 `ws://localhost:9999/ws` 改成 `wss://localhost:9999/ws`。
- 如果连接时使用 token，继续保留 `?token=xxx`。
- `onopen` 后仍发送原来的 `messageType=0` 建连消息。
- 本地开发需要信任自签证书。

推荐最终本地开发配置：

```text
后端 Netty: wss://localhost:9999/ws
后端 REST:  http://localhost:8080
前端页面:   http://localhost:5173 或 http://localhost:8081
证书:       IM-Bootstrap/src/main/resources/certs/local-wss.p12
```

## 8. 后续扩展

当前方案只解决传输加密。后续如果继续实现消息加密存储，可以在此基础上增加 `AES-256-GCM`：

- WSS 保护客户端到服务端的传输链路。
- AES-256-GCM 保护数据库中的消息内容。
- 两者可以叠加使用，互不冲突。

如果要正式生产部署，更推荐使用公网 CA 证书，或者使用 Nginx/Caddy 做 TLS 终结；但对当前项目的本地演示和简单部署来说，Netty 直接 WSS + 自签证书已经足够。
