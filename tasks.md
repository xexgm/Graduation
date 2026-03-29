# Task Breakdown: 用户私聊与好友管理 (chat-private-message)

## 1. Setup & Infrastructure (Phase 1)
**Goal**: Initialize common models, enums, and database mappers required for the entire feature.
- [x] **T001**: **创建好友相关的枚举与实体类**  
  在 `Graduation-Common/src/main/java/com/gm/graduation/common` 下：
  1. 创建枚举 `FriendStatusEnum`（0-正常，1-拉黑），增加 `@EnumValue` 注解。
  2. 创建实体类 `FriendRelation`（映射 `graduation_friend_relation` 表）。
- [x] **T002**: **创建消息相关的实体类**  
  在 `Graduation-Common/src/main/java/com/gm/graduation/common/domain` 下：
  1. 创建实体类 `PrivateMessage`（映射 `graduation_private_message` 表）。
  2. 创建实体类 `ChatRoomMessage`（映射 `graduation_chatroom_message` 表）。
- [x] **T003**: **扩展 AppEnum 协议字典**  
  修改 `Graduation-Common/.../enums/AppEnum.java`，增加 `PRIVATE_CHAT(2)` 业务线。
- [x] **T004**: **[P] 创建基础 Mapper 接口**  
  在 `IM-Bootstrap/src/main/java/com/gm/imbootstrap/mapper` 中创建 `FriendRelationMapper`、`PrivateMessageMapper` 和 `ChatRoomMessageMapper`，继承 MyBatis-Plus 的 `BaseMapper`。

---

## 2. User Story 1: 用户好友管理 (Phase 2)
**Goal**: 实现基于 SpringBoot REST API 的好友关系维护功能。
- [x] **T005**: **创建 FriendService 和 DTO**  
  在 `IM-Bootstrap/.../service` 创建 `FriendService`，包含：添加好友、删除好友、拉取好友列表。并在 `dto` 包下创建相关请求和响应类。
- [x] **T006**: **创建 FriendController**  
  在 `IM-Bootstrap/.../controller` 创建 `FriendController`，提供 API：
  - `POST /friend/add`
  - `DELETE /friend/remove/{friendId}`
  - `GET /friend/list`

---

## 3. User Story 2: 历史消息查询 API (Phase 3)
**Goal**: 提供 REST API 供前端拉取历史聊天记录（私聊与聊天室）。
- [x] **T007**: **[P] 创建 PrivateMessageService**  
  实现根据 `senderId` 和 `receiverId` 分页查询 `graduation_private_message` 表记录的逻辑。
- [x] **T008**: **[P] 创建 ChatRoomMessageService**  
  （此前已部分建立，如果没有，则补充完整）实现根据 `roomId` 分页查询 `graduation_chatroom_message` 历史记录的逻辑。
- [x] **T009**: **创建 MessageController**  
  新增 `MessageController.java`，提供 API：
  - `GET /message/private/history`
  - `GET /message/chatroom/history`

---

## 4. User Story 3: 私聊实时通信扩展 (Phase 4)
**Goal**: 通过 Netty 接收私聊消息，实现在线用户的实时消息推送及离线消息落库。
- [x] **T010**: **实现 PrivateChatProcessor**  
  在 `Graduation-Netty/.../processor/` 新建 `PrivateChatProcessor`，继承 `AbstractMessageProcessor`：
  - 拦截 appId=2 的私聊消息。
  - 获取 `toId`（接收者），并判断对方是否在线（调用 `UserLinkManager`）。
  - 若在线，调用 `writeAndFlush` 下发消息。
  - 最后（无论在线与否）将消息内容通过 Mapper 持久化到 `graduation_private_message` 表。
- [x] **T011**: **注册 PrivateChatProcessor 到处理链路**  
  修改 `Graduation-Netty/.../processor/ProcessorFactory.java`，在 `getProcessor` 方法的 switch 中增加 `case PRIVATE_CHAT -> PrivateChatProcessor.getInstance();`。

---

## 5. Polish & Documentation (Final Phase)
**Goal**: 同步更新前后端对接文档，确保接口完全透明。
- [x] **T012**: **更新 NETTY_API_DOCS.md**  
  增加关于 `appId=2` (私聊功能) 的消息体示例、发送规则、和响应规则说明。
- [x] **T013**: **更新 FRONTEND_API_DOCS.md**  
  添加 `/friend` 系列接口以及 `/message` 系列接口的 HTTP 请求/响应说明。

---

## Implementation Strategy & Execution Order
1. 优先完成 **Phase 1**，因为后面的 Controller 和 Netty 处理都需要依赖公共的实体和枚举。
2. 然后并行开发 **Phase 2 (好友关系)** 和 **Phase 3 (历史消息)**（这两个 API 模块是正交的，可以并行）。
3. 接着完成 **Phase 4 (Netty 实时转发)**，这是核心通讯层，需要连接之前的组件。
4. 最后完成文档的更新与联调整理。