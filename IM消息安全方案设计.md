# IM 消息安全方案设计

## 1. 背景

当前项目的消息存储逻辑主要位于 `IM-Bootstrap/src/main/java/com/gm/imbootstrap/mapper` 及对应的 Service 层，核心消息实体如下：

- 私聊消息：`Graduation-Common/src/main/java/com/gm/graduation/common/domain/PrivateMessage.java`
- 聊天室消息：`Graduation-Common/src/main/java/com/gm/graduation/common/domain/ChatRoomMessage.java`
- WebSocket 传输消息：`Graduation-Common/src/main/java/com/gm/graduation/common/domain/CompleteMessage.java`

当前实现中，`PrivateMessage.content` 和 `ChatRoomMessage.content` 均为明文存储；Netty WebSocket 侧也尚未启用 TLS，`CompleteMessage.encryption` 字段暂未真正参与加密流程。因此，消息在“传输链路”和“落库存储”两个环节都存在被窃听、被拖库后直接泄露的风险。

本文档分两部分：

1. 先给出当前项目适合采用的常见可落地安全方案
2. 再重点细化“方案一：消息加密存储”和“方案二：消息传输加密”的具体实现设计

---

## 2. 常见且可落地的消息安全方案

### 2.1 消息加密存储

**目标**：即使数据库泄露，攻击者也无法直接看到明文消息。

**适用位置**：

- `PrivateMessageService.saveMessage(...)`
- 后续若聊天室消息也有写入逻辑，同样在入库前进行加密

**推荐方式**：

- 使用 `AES-256-GCM` 对消息内容加密
- 数据库存密文，业务查询时解密
- 密钥不写死在代码中，通过环境变量或配置中心注入

**特点**：

- 改造成本低
- 对现有业务侵入可控
- 对数据库泄露场景防护明显
- 服务端仍可解密，因此不属于严格意义上的端到端加密

---

### 2.2 消息传输加密

**目标**：防止消息在客户端与服务端之间传输时被中间人窃听或篡改。

**推荐方式**：

- 网络层使用 `WSS`（WebSocket over TLS）
- 应用层保留 `CompleteMessage.encryption` 字段，用于扩展消息体加密协议

**特点**：

- 直接提升链路安全性
- 对外暴露服务时是基础必选项
- 与存储加密可以叠加使用

---

### 2.3 消息内容过滤与 XSS 防护

**目标**：避免用户发送恶意脚本、危险标签或注入型内容。

**建议做法**：

- 入库前对消息内容做 HTML 清洗或转义
- 对消息长度做限制，例如单条消息不超过 `2000` 或 `5000` 字符
- 对异常控制字符、空消息、超长消息做统一校验

---

### 2.4 消息访问权限控制

**目标**：防止越权查看聊天记录。

**当前项目重点**：

- 私聊历史查询必须校验当前登录用户是否为对话参与者
- 聊天室历史查询应校验用户是否有该聊天室访问权限

---

### 2.5 消息签名与防篡改

**目标**：防止消息内容在链路中被篡改。

**建议做法**：

- 给消息附加签名字段，例如 `HMAC-SHA256`
- 签名内容可包含：`uid + toId + content + timeStamp`
- 服务端验证签名后再进入业务处理

---

### 2.6 消息生命周期控制

**目标**：降低敏感消息的长期暴露风险。

**建议做法**：

- 支持阅后即焚
- 支持消息过期时间
- 支持定时物理删除或逻辑失效

---

## 3. 总体落地建议

如果按优先级推进，建议顺序如下：

1. **先做方案二中的 TLS/WSS**：先把链路安全补上
2. **再做方案一中的加密存储**：补上数据库泄露防护
3. **然后做访问权限控制和内容过滤**：堵住越权与 XSS 风险
4. **最后再扩展签名、防篡改、消息过期等增强能力**

如果你的目标是毕业设计阶段尽快做出“有说服力、可演示、可答辩”的安全增强版本，推荐最低可交付组合如下：

- `WSS + AES-GCM 加密存储 + 密钥外置配置 + 历史消息解密展示`

这样既有网络安全，又有数据存储安全，而且改造范围清晰、容易展示。

---

## 4. 方案一：消息加密存储详细设计

## 4.1 目标

对数据库中的消息正文进行加密，避免以下风险：

- 数据库账号泄露后，攻击者直接读到明文消息
- 开发、运维、测试人员直接访问库表时看到用户隐私
- 备份文件泄露导致历史消息明文暴露

这里推荐做“**服务端透明加密存储**”：

- 发送消息时：业务层加密后入库
- 查询消息时：业务层解密后返回给前端

---

## 4.2 推荐算法

建议使用：`AES-256-GCM`

原因：

- 对称加密，性能适合 IM 高频消息写入
- `GCM` 模式同时提供机密性和完整性校验
- Java 原生 `javax.crypto` 支持较好

不建议：

- 直接使用 `AES/ECB/PKCS5Padding`，因为 ECB 模式不安全
- 直接自己拼接“加密 + MD5”，容易设计出伪安全方案

---

## 4.3 当前项目中的改造点

### 4.3.1 需要重点改造的类

1. `IM-Bootstrap/src/main/java/com/gm/imbootstrap/service/PrivateMessageService.java`
2. 聊天室消息保存逻辑所在的 Service 或 Processor
3. `Graduation-Common/src/main/java/com/gm/graduation/common/domain/PrivateMessage.java`
4. `Graduation-Common/src/main/java/com/gm/graduation/common/domain/ChatRoomMessage.java`
5. `IM-Bootstrap/src/main/resources/application.yml`

### 4.3.2 建议新增的类

建议在 `IM-Bootstrap` 模块新增：

- `com.gm.imbootstrap.util.MessageCryptoUtil`
- `com.gm.imbootstrap.config.MessageSecurityProperties`
- `com.gm.imbootstrap.service.MessageCryptoService`

职责建议如下：

- `MessageSecurityProperties`：读取加密配置
- `MessageCryptoUtil`：封装底层 AES-GCM 加解密逻辑
- `MessageCryptoService`：封装业务可直接调用的“加密/解密/密钥版本处理”能力

---

## 4.4 数据库设计建议

当前表结构中的 `content` 字段可以继续保留，但建议至少增加以下字段：

### 4.4.1 私聊消息表 `graduation_private_message`

新增字段建议：

- `content_cipher`：密文内容
- `content_iv`：AES-GCM 随机 IV
- `encrypt_version`：密钥版本号
- `content_plain`：迁移期临时字段，可选，不建议长期保留

### 4.4.2 聊天室消息表 `graduation_chatroom_message`

新增字段建议：

- `content_cipher`
- `content_iv`
- `encrypt_version`

### 4.4.3 为什么不建议只复用 `content`

可以直接把 `content` 字段替换成 Base64 密文，但不如拆分字段清晰，原因如下：

- 后续排查更直观
- 方便做密钥轮换
- 方便做兼容迁移
- IV 单独存储更规范

如果你想控制改造量，也可以采用简化版：

- `content`：直接存 Base64 密文
- `encrypt_version`：记录密钥版本
- IV 拼接进密文串里，例如 `Base64(iv):Base64(cipherText)`

对于当前项目，**建议先做简化版**，这样数据库改动最小。

---

## 4.5 配置设计

建议在 `application.yml` 中增加：

```yaml
message:
  security:
    storage-encrypt-enabled: true
    algorithm: AES/GCM/NoPadding
    key-version: v1
    aes-key: ${MESSAGE_AES_KEY:}
```

注意：

- `aes-key` 必须通过环境变量注入，不要硬编码到仓库
- 建议使用 32 字节密钥，对应 `AES-256`
- 生产环境可接入配置中心或 KMS

---

## 4.6 代码实现建议

### 4.6.1 工具类设计

建议 `MessageCryptoUtil` 提供以下接口：

```java
String encrypt(String plainText, String base64Key);
String decrypt(String cipherPayload, String base64Key);
```

其中 `cipherPayload` 建议采用统一格式：

```text
v1:Base64(iv):Base64(cipherText)
```

优点：

- 一个字段即可完整存储
- 兼容未来密钥轮换
- 读取时可按版本解析

### 4.6.2 保存私聊消息时的处理流程

当前 `PrivateMessageService.saveMessage(...)` 只有直接插库：

1. 校验 `message` 非空
2. 读取 `message.content`
3. 使用 `MessageCryptoService.encryptForStorage(content)` 加密
4. 将密文重新写回 `message.content`
5. 执行 `privateMessageMapper.insert(message)`

伪代码如下：

```java
public void saveMessage(PrivateMessage message) {
    if (message == null) {
        return;
    }
    String plainText = message.getContent();
    String cipherText = messageCryptoService.encryptForStorage(plainText);
    message.setContent(cipherText);
    privateMessageMapper.insert(message);
}
```

### 4.6.3 查询历史消息时的处理流程

当前 `getHistory(...)` 是直接从数据库分页查询后返回。改造建议：

1. 查询出分页结果
2. 遍历 `records`
3. 对每条消息 `content` 做解密
4. 将明文再写回返回对象

伪代码如下：

```java
Page<PrivateMessage> pageResult = privateMessageMapper.selectPage(page, qw);
for (PrivateMessage item : pageResult.getRecords()) {
    item.setContent(messageCryptoService.decryptFromStorage(item.getContent()));
}
return pageResult;
```

聊天室历史消息查询也采用同样思路。

---

## 4.7 兼容老数据的迁移方案

如果你的数据库中已经有明文历史消息，建议按“两阶段迁移”处理。

### 第一阶段：兼容读

解密逻辑增加兼容判断：

- 若 `content` 以 `v1:` 开头，按密文解析
- 若不以 `v1:` 开头，则视为旧明文，直接返回

这样做的好处是：

- 老数据不需要立刻全量迁移
- 新消息可以立即启用密文存储

### 第二阶段：离线回写

编写一次性迁移脚本：

1. 扫描明文数据
2. 批量加密
3. 回写到 `content`
4. 标记已迁移版本

这样答辩时也能解释你的系统具备平滑升级能力。

---

## 4.8 密钥管理建议

### 4.8.1 最低要求

- 不要把 AES 密钥写死在 Java 代码中
- 不要直接提交到 `application.yml`
- 使用环境变量，例如 `MESSAGE_AES_KEY`

### 4.8.2 进阶方案

- 使用配置中心下发
- 使用 KMS 管理主密钥
- 采用“数据密钥 + 主密钥”模式

### 4.8.3 密钥轮换建议

增加 `key-version` 概念：

- 新写入消息使用最新版本密钥
- 旧消息按自身版本解密
- 可逐步将旧数据重新加密到新版本

---

## 4.9 方案一的优缺点

### 优点

- 对数据库泄露场景防护明显
- 对现有项目改造量适中
- 与当前 Mapper/Service 架构高度兼容

### 缺点

- 服务端仍能解密，不属于严格端到端加密
- 查询历史消息时需要解密，会有少量 CPU 开销
- 密钥管理若做不好，会削弱整体效果

---

## 5. 方案二：消息传输加密详细设计

## 5.1 目标

防止消息在客户端和服务端之间通过 WebSocket 传输时被：

- 被中间人窃听
- 被代理工具抓包读取
- 被恶意节点篡改

当前项目的 WebSocket 服务启动类位于：

- `Graduation-Netty/src/main/java/com/gm/graduation/netty/server/NettyServer.java`

目前 pipeline 中只有：

- `HttpServerCodec`
- `HttpObjectAggregator`
- `WebSocketServerProtocolHandler`
- 自定义编解码器和业务处理器

尚未看到 `SslHandler`，因此当前是普通 `ws://`，不是 `wss://`。

---

## 5.2 方案二的推荐拆分

传输加密建议分为两层：

1. **基础必做层：TLS/WSS**
2. **可选增强层：应用层消息体加密**

对于当前项目，建议先完成第 1 层，再看时间决定是否补第 2 层。

---

## 5.3 第一层：TLS/WSS 改造

## 5.3.1 实现目标

将 WebSocket 地址从：

- `ws://host:port/ws`

升级为：

- `wss://host:port/ws`

客户端与服务端之间的 HTTP 握手、WebSocket 帧、消息体内容都通过 TLS 加密传输。

---

## 5.3.2 当前项目中的改造点

主要改造类：

- `Graduation-Netty/src/main/java/com/gm/graduation/netty/server/NettyServer.java`

建议新增类：

- `Graduation-Netty/src/main/java/com/gm/graduation/netty/config/NettySslConfig.java`
- 或在 `Graduation-Common` 中新增 SSL 配置读取类

建议新增配置：

- 证书路径
- 私钥路径
- 是否启用 TLS
- 证书密码

---

## 5.3.3 配置建议

可以在 `application.yml` 中增加类似配置：

```yaml
netty:
  ssl:
    enabled: true
    cert-path: ${NETTY_SSL_CERT_PATH:}
    private-key-path: ${NETTY_SSL_KEY_PATH:}
```
或者如果使用 `PKCS12`：

```yaml
netty:
  ssl:
    enabled: true
    key-store-path: ${NETTY_SSL_KEYSTORE_PATH:}
    key-store-password: ${NETTY_SSL_KEYSTORE_PASSWORD:}
```

毕业设计环境如果只是本地演示，可以先使用自签证书。

---

## 5.3.4 Netty 侧实现方式

核心思路是在 `ChannelPipeline` 最前面添加 `SslHandler`。

典型流程：

1. 应用启动时加载证书与私钥
2. 构建 `SslContext`
3. 在 `initChannel(...)` 中首先 `addLast(sslContext.newHandler(...))`
4. 后续再接 HTTP 编解码与 WebSocket 协议处理器

伪代码如下：

```java
SslContext sslContext = SslContextBuilder.forServer(certFile, keyFile).build();

ch.pipeline()
    .addLast(sslContext.newHandler(ch.alloc()))
    .addLast(new HttpServerCodec())
    .addLast(new HttpObjectAggregator(65536))
    .addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536, false, true))
    .addLast(new WebSocketFrameToMessageDecoder())
    .addLast(new MessageToWebSocketFrameEncoder())
    .addLast(new BusinessHandler());
```

这样客户端只要改为使用 `wss://` 即可完成加密链路。

---

## 5.3.5 客户端配套改造

客户端需要同步修改：

- WebSocket 连接地址改为 `wss://`
- 信任对应证书
- 若是自签证书，开发环境需要手动信任或放宽校验

如果前端是浏览器页面，还需要注意：

- 页面若通过 `https://` 打开，则 WebSocket 也应使用 `wss://`
- 否则会出现混合内容限制

---

## 5.3.6 证书部署建议

### 本地开发/演示

- 生成自签证书
- 放在本地受控目录
- 通过环境变量配置路径

### 线上/正式环境

- 使用正规 CA 证书
- 或将 TLS 终止放在 Nginx、网关层，再转发到 Netty

如果后续采用 Nginx 反向代理，你的 Netty 服务也可以保持内部明文，只让外部访问经过 `https/wss` 网关。这是更常见的工程做法。

---

## 5.4 第二层：应用层消息体加密

## 5.4.1 为什么还需要应用层加密

即使已经启用了 TLS，以下场景仍可能需要进一步增强：

- 服务端内部某些节点不完全可信
- 希望在网关、代理、日志链路中尽量不出现明文消息
- 需要突出“安全 IM”的设计深度

当前 `CompleteMessage` 已经有 `encryption` 字段，可以直接利用。

---

## 5.4.2 建议的数据协议扩展

当前 `CompleteMessage` 字段如下：

- `uid`
- `token`
- `encryption`
- `toId`
- `content`
- `timeStamp`

建议扩展为：

- `encryption`：加密算法标识
- `keyVersion`：密钥版本
- `nonce`：随机数/IV
- `signature`：签名摘要，可选

示例约定：

- `0`：明文
- `1`：AES-GCM
- `2`：RSA + AES 会话密钥模式

---

## 5.4.3 应用层加密推荐做法

### 简化版

适合当前项目先实现：

- 登录后服务端下发一个会话密钥给客户端
- 客户端发送消息时，用该会话密钥加密 `content`
- 服务端收到后先解密，再进入业务处理与入库

### 更合理版

- 客户端生成随机 AES 会话密钥
- 用服务端公钥加密该会话密钥
- 服务端私钥解出后，仅在当前连接上下文使用

这种方式更适合展示“非对称协商 + 对称传输”的完整设计。

---

## 5.4.4 当前项目中的落地点

重点位置：

- `Graduation-Netty/src/main/java/com/gm/graduation/netty/handler/WebSocketFrameToMessageDecoder.java`
- `Graduation-Netty/src/main/java/com/gm/graduation/netty/handler/MessageToWebSocketFrameEncoder.java`
- `Graduation-Netty/src/main/java/com/gm/graduation/netty/handler/BusinessHandler.java`
- `Graduation-Netty/src/main/java/com/gm/graduation/netty/processor/PrivateChatProcessor.java`
- `Graduation-Common/src/main/java/com/gm/graduation/common/domain/CompleteMessage.java`

典型处理顺序建议为：

1. WebSocket 文本帧进入解码器
2. 转成 `CompleteMessage`
3. 根据 `encryption` 字段判断是否需要解密
4. 解密出明文 `content`
5. 进入 `BusinessHandler` / `Processor`
6. 业务处理后，回包时再根据协议进行加密

---

## 5.4.5 应用层加密的注意事项

- 不要复用固定 IV
- 不要把密钥直接放在消息体明文中
- 消息中最好带 `timeStamp`，可做重放攻击校验
- 可以增加 5 分钟时间窗口，超时消息直接丢弃

---

## 5.5 方案二的优缺点

### 优点

- 先启用 TLS 就能快速明显提升安全性
- 与当前 Netty 架构适配度高
- 应用层加密可复用现有 `CompleteMessage.encryption` 字段

### 缺点

- TLS 需要证书与部署管理
- 应用层加密会增加协议复杂度
- 客户端也必须同步改造

---

## 6. 推荐实施路线

## 第一阶段：最低可交付版本

建议优先完成以下内容：

1. Netty 支持 `WSS`
2. `PrivateMessage.content` 入库前 AES-GCM 加密
3. 私聊历史消息查询时自动解密
4. `MESSAGE_AES_KEY` 改为环境变量注入

这一阶段完成后，你已经可以在论文或答辩中展示：

- 传输加密
- 存储加密
- 密钥外置
- 历史消息可透明解密

---

## 第二阶段：增强版本

在第一阶段基础上继续补充：

1. 聊天室消息也启用同样的加密存储
2. `CompleteMessage` 扩展 `keyVersion`、`nonce`、`signature`
3. 增加应用层消息体加密
4. 增加时间戳防重放校验

---

## 第三阶段：工程化完善

如果还想进一步提升完整度，可继续做：

1. 密钥轮换机制
2. 历史明文数据迁移脚本
3. KMS 或配置中心接入
4. 敏感词过滤与权限校验补齐

---

## 7. 最终建议

结合当前项目规模、代码结构和毕业设计场景，最推荐的落地方式是：

### 必做组合

- **方案二的 TLS/WSS**：解决链路明文问题
- **方案一的 AES-GCM 加密存储**：解决数据库泄露问题

### 建议同步补强

- 历史消息查询权限校验
- 消息长度限制与内容过滤
- 密钥通过环境变量或配置中心管理

### 不建议一开始就做得过重

不建议上来就直接实现完整端到端加密，因为：

- 需要复杂的密钥交换和设备管理
- 需要客户端配套大量改造
- 对当前项目而言实现成本明显偏高

对于当前这套 IM 项目，先把“**WSS + 服务端透明加密存储**”做完整，就是一套既务实又能体现安全设计深度的方案。

