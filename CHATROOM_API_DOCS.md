# 聊天室 API 文档

## 基础信息
- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- 认证方式: Bearer Token（所有接口均需携带 Token；创建/下线/删除 需要管理员权限）

## 统一响应格式
```typescript
interface ApiResponse<T> {
  code: number;        // 200=成功, 400=失败, 401=未授权, 403=无权限
  message: string;     // 消息
  data: T | null;      // 数据
  timestamp: number;   // 毫秒时间戳
}
```

## 数据模型
```typescript
// 聊天室类型
export type ChatRoomType = 'PUBLIC_ROOM' | 'PRIVATE_ROOM';

// 聊天室状态（服务端枚举序列化为字符串返回）
export type ChatRoomStatus = 'ACTIVE' | 'DISBANDED' | 'DELETED';

// 聊天室实体（服务端返回）
interface ChatRoom {
  roomId: number;            // 聊天室ID
  roomName: string;          // 名称
  description?: string;      // 公告/描述
  ownerId: number;           // 创建者用户ID
  roomType: ChatRoomType;    // 类型：PUBLIC_ROOM/PRIVATE_ROOM
  createTimeStamp: number;   // 创建时间（毫秒）
  status: ChatRoomStatus;    // 状态：ACTIVE/DISBANDED/DELETED
}

// 创建聊天室请求体
interface CreateChatRoomRequest {
  roomName: string;          // 必填
  description?: string;      // 可选
  roomType?: ChatRoomType;   // 可选，不传默认 PUBLIC_ROOM
}
```

权限约束
- 管理员判定：用户的 `role == 1` 才能执行创建、下线、删除操作。
- Token 要求：所有接口需携带有效 Token（`Authorization: Bearer {token}`）。

---

## 1. 创建聊天室
POST `/chatroom/create`

- 权限：管理员（role == 1）
- Header：`Authorization: Bearer {token}`
- Body：`CreateChatRoomRequest`
- 返回：`ApiResponse<ChatRoom>`

示例
```bash
curl -X POST 'http://localhost:8080/chatroom/create' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "roomName": "公告群",
    "description": "系统公告",
    "roomType": "PUBLIC_ROOM"
  }'
```

成功响应 200
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "roomId": 1,
    "roomName": "公告群",
    "description": "系统公告",
    "ownerId": 10001,
    "roomType": "PUBLIC_ROOM",
    "createTimeStamp": 1738412345678,
    "status": "ACTIVE"
  },
  "timestamp": 1738412345678
}
```

失败响应示例
- 401 未登录/Token 无效
```json
{ "code": 401, "message": "未登录或凭证无效", "data": null, "timestamp": 0 }
```
- 403 非管理员
```json
{ "code": 403, "message": "仅管理员可操作", "data": null, "timestamp": 0 }
```
- 400 参数错误（如 roomName 为空）
```json
{ "code": 400, "message": "聊天室名称不能为空", "data": null, "timestamp": 0 }
```

---

## 2. 下线聊天室
POST `/chatroom/{roomId}/offline`

- 权限：管理员（role == 1）
- Header：`Authorization: Bearer {token}`
- Path 参数：`roomId`（number）
- 返回：`ApiResponse<null>`

示例
```bash
curl -X POST 'http://localhost:8080/chatroom/1/offline' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN'
```

成功响应 200
```json
{ "code": 200, "message": "下线成功", "data": null, "timestamp": 1738412345678 }
```

失败响应
- 401 未登录/Token 无效
- 403 非管理员
- 400 参数错误 或 "聊天室不存在或更新失败"

---

## 3. 删除聊天室（软删）
DELETE `/chatroom/{roomId}`

- 权限：管理员（role == 1）
- Header：`Authorization: Bearer {token}`
- Path 参数：`roomId`（number）
- 返回：`ApiResponse<null>`

说明
- 软删除：仅将 `status` 更新为 `DELETED`，数据仍保留在库中。

示例
```bash
curl -X DELETE 'http://localhost:8080/chatroom/1' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN'
```

成功响应 200
```json
{ "code": 200, "message": "删除成功", "data": null, "timestamp": 1738412345678 }
```

失败响应
- 401 未登录/Token 无效
- 403 非管理员
- 400 参数错误 或 "聊天室不存在或更新失败"

---

## 4. 查询聊天室列表（未删除）
GET `/chatroom/list`

- 权限：需登录（任意已登录用户可访问）
- Header：`Authorization: Bearer {token}`
- 返回：`ApiResponse<ChatRoom[]>`

示例
```bash
curl -X GET 'http://localhost:8080/chatroom/list' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN'
```

成功响应 200
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "roomId": 1,
      "roomName": "公告群",
      "description": "系统公告",
      "ownerId": 10001,
      "roomType": "PUBLIC_ROOM",
      "createTimeStamp": 1738412345678,
      "status": "ACTIVE"
    },
    {
      "roomId": 2,
      "roomName": "内部群",
      "description": "仅内部成员",
      "ownerId": 10002,
      "roomType": "PRIVATE_ROOM",
      "createTimeStamp": 1738412345678,
      "status": "DISBANDED"
    }
  ],
  "timestamp": 1738412345678
}
```

失败响应
- 401 未登录/Token 无效

---

## 错误码说明
- 200：成功
- 400：参数错误/业务失败（见 message）
- 401：未授权（Token 缺失或无效）
- 403：无权限（非管理员尝试进行需要管理员权限的操作）

## 备注
- 创建、下线、删除操作均依赖管理员判定：后端根据当前登录用户 `role == 1` 判定。
- `status` 字段只读：由后端在不同接口中设置为 `ACTIVE`/`DISBANDED`/`DELETED`。
- `createTimeStamp` 为毫秒时间戳，前端可按需格式化展示。
