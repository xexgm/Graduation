# 私聊与好友功能需求文档 (Feature Spec)

## 1. 功能概述

**功能名称**: 用户私聊与好友管理 (chat-private-message)
**目标**: 在现有的聊天室功能基础上，扩展系统的即时通讯能力，允许用户互相添加为好友，并通过 Netty 模块进行点对点的实时私聊。消息的存储需独立于聊天室消息，并预留后续加密存储的扩展能力。

## 2. 核心场景与流程设计

### 2.1 用户好友管理场景 (基于 SpringBoot REST API)
- **添加好友**: 用户 A 发起添加用户 B 为好友的请求，成功后双方建立好友关系。
- **删除好友**: 用户 A 解除与用户 B 的好友关系，解除后双方无法再发送私聊消息。
- **查看好友列表**: 用户可以查询自己当前的所有好友。

### 2.2 用户私聊通信场景 (基于 Netty WebSocket)
- **发送私聊消息**: 用户 A 在 WebSocket 中组装私聊消息体发送给后端 Netty。
- **路由与转发**: 后端 Netty 中的 `PrivateChatProcessor` 接收到消息，判断接收方用户 B 的在线状态。
  - **若 B 在线**: 通过 Netty 缓存的 Channel 实时将消息推送给 B。
  - **若 B 不在线**: 消息仅做持久化落库（后续用户 B 登录时可通过历史消息接口拉取）。
- **消息持久化**: 发送成功或失败的消息，都将写入独立的私聊消息数据表。

## 3. 数据库层面设计 (Database Design)

为了支持此功能，数据库需要新增三张表：

### 3.1 好友关系表 (`graduation_friend_relation`)
用于记录用户之间的好友绑定关系。
- `id` (主键)
- `user_id` (用户 A 的 ID)
- `friend_id` (用户 B 的 ID)
- `status` (好友状态，预留：0-正常, 1-拉黑等)
- `create_time` (添加时间)

### 3.2 私聊消息表 (`graduation_private_message`)
独立于聊天室消息表，记录点对点的通信数据。
- `msg_id` (消息主键)
- `sender_id` (发送者 ID)
- `receiver_id` (接收者 ID)
- `content` (消息内容。**注：当前明文存储，后续可在此字段级别实现对称/非对称加密方案扩展**)
- `is_read` (阅读状态：0-未读, 1-已读)
- `create_time` (消息发送时间)

### 3.3 聊天室消息表 (`graduation_chatroom_message`) (补充梳理)
作为对比和业务隔离，需确认该表独立存储：
- `msg_id`
- `room_id`
- `sender_id`
- `content`
- `create_time`

## 4. 技术实现方案细节 (Technical Implementation)

### 4.1 控制器层 (IM-Bootstrap)
新增 `FriendController.java` 和对应的 Service、Mapper，提供以下接口：
1. `POST /friend/add`: 添加好友（传入目标用户ID）。
2. `DELETE /friend/remove/{friendId}`: 删除好友。
3. `GET /friend/list`: 获取当前登录用户的好友列表。
4. `GET /message/private/history`: 拉取与某位好友的私聊历史记录（支持分页）。

### 4.2 Netty 协议扩展 (Graduation-Netty)
基于现有 `CompleteMessage` 协议体系进行扩展：
1. **AppEnum 枚举新增业务线**: 
   - `LINK(0)`: 连接管理
   - `CHAT_ROOM(1)`: 聊天室业务
   - `PRIVATE_CHAT(2)`: **新增**，私聊业务。

2. **新增处理器 `PrivateChatProcessor`**:
   在 `ProcessorFactory` 中注册 `AppEnum.PRIVATE_CHAT` 对应的处理器：
   - 拦截 `appId = 2` 的消息。
   - 校验发送方和接收方是否为好友关系（可选：可调用 SpringBoot 层的服务验证）。
   - 将消息存入 `graduation_private_message` 表。
   - 通过 `UserLinkManager` 查找 `toId`（接收者）的 `ChannelHandlerContext`，如果存在则进行 `writeAndFlush`。

### 4.3 预留扩展点：消息加密
对于后续需要扩展的加密存储需求：
- **方案预研**: 可以在写入 `graduation_private_message.content` 字段之前，在 Service 层使用 AES 算法对明文进行加密，数据库只落盘密文；读取时再进行解密返回。
- 当前阶段不做加密逻辑的硬编码，保持原样传递。

## 5. 验收标准与成功指标 (Success Criteria)

1. **好友管理**: 能够通过 API 成功完成添加、删除、列表查询的闭环，数据库关系记录正确。
2. **私聊送达**: 
   - 在线的用户 A 向 在线的用户 B 发送私聊消息，用户 B 能够在 `500ms` 内收到 WebSocket 推送。
   - 私聊消息不会被错误地广播给其他无关用户。
3. **数据隔离**: 私聊消息和聊天室消息在数据库中完美分离，互不干扰。
## 6. 实现计划 (Implementation Plan)

### 6.1 技术上下文与原则检查
**Constitution Check:**
- 遵守 **Architecture Principle**: HTTP API 放在 `IM-Bootstrap`，实时消息放在 `Graduation-Netty`，实体定义放在 `Graduation-Common`。
- 遵守 **Communication Principle**: 私聊消息也是 `CompleteMessage` 的标准 JSON，增加 `appId: 2` 的路由。
- 遵守 **Documentation Alignment**: 接口完成后需要同步更新 API 文档。

### 6.2 Phase 0: 实体与数据库设计
1. **定义模型 (Graduation-Common)**
   - 创建 `FriendRelation` 实体类，映射 `graduation_friend_relation` 表。
   - 创建 `PrivateMessage` 实体类，映射 `graduation_private_message` 表。
   - 创建 `ChatRoomMessage` 实体类，映射 `graduation_chatroom_message` 表。
   - 增加好友状态枚举 (如 `FriendStatusEnum`)，遵循 Mybatis-Plus `@EnumValue` 规范。
2. **生成 SQL 并建表**
   - 编写三张新表的 MySQL DDL 脚本。
3. **增加枚举定义 (Graduation-Common)**
   - `AppEnum` 增加 `PRIVATE_CHAT(2)`。

### 6.3 Phase 1: Controller 层开发 (HTTP API)
**模块**: `IM-Bootstrap`
1. **Mapper & Service**:
   - 创建 `FriendRelationMapper`, `PrivateMessageMapper`, `ChatRoomMessageMapper`。
   - 创建 `FriendService` 实现添加、删除、列表查询逻辑。
   - 创建 `PrivateMessageService` 实现历史消息分页查询逻辑。
   - 创建 `ChatRoomMessageService` 实现聊天室历史消息分页查询逻辑。
2. **Controller 接口开发**:
   - `POST /friend/add`
   - `DELETE /friend/remove/{friendId}`
   - `GET /friend/list`
   - `GET /message/private/history`
   - `GET /message/chatroom/history`

### 6.4 Phase 2: Netty 层开发 (WebSocket 实时通信)
**模块**: `Graduation-Netty`
1. **处理器工厂适配**:
   - `ProcessorFactory` 中增加 `AppEnum.PRIVATE_CHAT -> PrivateChatProcessor.getInstance()` 的路由分支。
2. **开发 PrivateChatProcessor**:
   - 继承 `AbstractMessageProcessor`。
   - 实现**拦截私聊消息**，提取 `uid` (发送方) 和 `toId` (接收方)。
   - **离线落库**: 调用 Spring 容器中的 Bean 或使用预设的 MyBatis-Plus Mapper 进行消息存储。
   - **在线推送**: 从 `UserLinkManager` 中获取 `toId` 的 Channel，如果处于 Active 状态，调用 `writeAndFlush` 进行实时下发。

### 6.5 Phase 3: 文档与测试
1. 同步修改 `NETTY_API_DOCS.md`，增加关于 appId=2 (私聊功能) 的交互报文示例。
2. 同步修改 `FRONTEND_API_DOCS.md`，添加好友管理等 Controller 层的 API 文档说明。
3. 执行单元测试与联调测试，确保私聊不影响现有的聊天室和连接存活机制。