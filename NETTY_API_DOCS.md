# Netty WebSocket API 文档

## 1. 概述

本文档定义了客户端与 `Graduation-Netty` 服务端之间通过 WebSocket 进行实时通信的接口协议。客户端首先需要通过 WebSocket 连接到服务器，在连接建立时进行身份验证，然后通过发送和接收序列化后的 `CompleteMessage` JSON 对象进行交互。

## 2. 建立连接

客户端通过标准的 WebSocket 协议连接到 Netty 服务端。

- **连接 URL**:
  ```
  ws://<your-server-host>:<port>/ws?token=<user-token>
  ```
- **参数说明**:
  - `<your-server-host>:<port>`: Netty 服务端的地址和端口。
  - `token`: 必需参数。用户登录后获取的身份令牌，用于在建立连接时进行身份验证。如果 `token` 无效或缺失，连接将被拒绝。

## 3. 消息格式

所有在客户端和服务端之间传递的消息都是一个完整的JSON对象，该对象是 `CompleteMessage` 类的序列化形式。

- **`CompleteMessage` 结构**:
  ```json
  {
    "appId": "integer",
    "uid": "long",
    "token": "string",
    "compression": "integer",
    "encryption": "integer",
    "messageType": "integer",
    "toId": "long",
    "content": "string",
    "timeStamp": "long"
  }
  ```
- **字段说明**:
  - `appId`: **业务线标识**。用于区分不同的业务模块。
  - `uid`: **用户ID**。发送方的用户ID。
  - `token`: **用户令牌**。用于消息级别的认证。
  - `compression`: **是否压缩** (暂未使用)。
  - `encryption`: **是否加密** (暂未使用)。
  - `messageType`: **消息类型**。定义在特定业务线下的具体操作。
  - `toId`: **接收方ID**。根据业务不同，可以是用户ID，也可以是聊天室ID (`chatRoomId`)。
  - `content`: **消息内容**。消息的主要载体，可以是文本或序列化后的数据。
  - `timeStamp`: **发送时间戳**。消息发送时的客户端时间。

## 4. 业务线 (appId)

| `appId` | 业务线     | 描述                     |
| :------ | :--------- | :----------------------- |
| `0`     | Link       | 基础连接，如心跳维持等。 |
| `1`     | ChatRoom   | 聊天室业务，如收发消息等。 |

---

## 5. 接口详解

### 5.1 Link 业务 (`appId: 0`)

#### 5.1.1 心跳 (Heartbeat)

- **`messageType`**: `3`
- **描述**: 用于客户端和服务器之间维持长连接。
- **方向**: 客户端 -> 服务端

**请求消息**:
```json
{
  "appId": 0,
  "uid": 12345,
  "token": "user-token-string",
  "messageType": 3,
  "content": "ping",
  "timeStamp": 1678886400000
}
```

- **方向**: 服务端 -> 客户端

**响应消息 (`pong`)**:
```json
{
  "appId": 0,
  "messageType": 3,
  "content": "pong",
  "timeStamp": 1678886400001
}
```

---

### 5.2 ChatRoom 业务 (`appId: 1`)

#### 5.2.1 进入聊天室 (Enter Chat Room)

- **`messageType`**: `0`
- **描述**: 用户加入一个指定的聊天室。
- **方向**: 客户端 -> 服务端

**请求消息**:
- `toId` 字段为目标聊天室的 `chatRoomId`。
```json
{
  "appId": 1,
  "uid": 12345,
  "token": "user-token-string",
  "messageType": 0,
  "toId": 1001,
  "content": null,
  "timeStamp": 1678886400000
}
```

#### 5.2.2 发送聊天室消息 (Send Chat Room Message)

- **`messageType`**: `1`
- **描述**: 用户在指定的聊天室中发送一条消息。
- **方向**: 客户端 -> 服务端

**请求消息**:
- `toId` 字段为目标聊天室的 `chatRoomId`。
- `content` 字段为消息文本。
```json
{
  "appId": 1,
  "uid": 12345,
  "token": "user-token-string",
  "messageType": 1,
  "toId": 1001,
  "content": "大家好！",
  "timeStamp": 1678886400000
}
```

#### 5.2.3 退出聊天室 (Exit Chat Room)

- **`messageType`**: `2`
- **描述**: 用户离开一个指定的聊天室。
- **方向**: 客户端 -> 服务端

**请求消息**:
- `toId` 字段为目标聊天室的 `chatRoomId`。
```json
{
  "appId": 1,
  "uid": 12345,
  "token": "user-token-string",
  "messageType": 2,
  "toId": 1001,
  "content": null,
  "timeStamp": 1678886400000
}
```