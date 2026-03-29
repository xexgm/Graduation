# IM 聊天系统前端接口文档

## 📋 目录
- [1. SpringBoot REST API 接口](#1-springboot-rest-api-接口)
- [2. Netty WebSocket 接口](#2-netty-websocket-接口)
- [3. 前端集成示例](#3-前端集成示例)

---

## 1. SpringBoot REST API 接口

### 🌐 基础信息
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **认证方式**: Bearer Token (除登录注册外)

### 📦 统一响应格式
```typescript
interface ApiResponse<T> {
  code: number;        // 状态码: 200=成功, 400=失败, 401=未授权
  message: string;     // 响应消息
  data: T | null;      // 响应数据
  timestamp: number;   // 时间戳
}
```

---

### 👤 用户管理接口

#### 1.1 用户注册
```http
POST /user/register
```

**请求体:**
```typescript
interface RegisterRequest {
  username: string;    // 用户名
  password: string;    // 密码 (最少8位,含数字和字母)
  nickname?: string;   // 昵称 (可选)
}
```

**响应:**
```typescript
interface RegisterResponse {
  userId: number;
  username: string;
  nickname: string;
  status: number;
  createTime: number;
  updateTime: number;
  // password 字段不会返回
}
```

**示例:**
```javascript
// 请求
const response = await fetch('/user/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'testuser',
    password: 'password123'
  })
});

// 响应 200
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "testuser",
    "nickname": "testuser",
    "status": 1,
    "createTime": 1633536000000,
    "updateTime": 1633536000000
  },
  "timestamp": 1633536000000
}
```

---

#### 1.2 用户登录
```http
POST /user/login
```

**请求体:**
```typescript
interface LoginRequest {
  username: string;    // 用户名
  password: string;    // 密码
}
```

**响应:**
```typescript
interface LoginResponse {
  user: {
    userId: number;
    username: string;
    nickname: string;
    avatarUrl?: string;
    signature?: string;
    status: number;
    createTime: number;
    updateTime: number;
  };
  token: string;           // JWT Token
  tokenExpireTime: number; // Token过期时间戳
}
```

**示例:**
```javascript
// 请求
const response = await fetch('/user/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'testuser',
    password: 'password123'
  })
});

// 响应 200
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "user": {
      "userId": 1,
      "username": "testuser",
      "nickname": "testuser",
      "status": 1,
      "createTime": 1633536000000,
      "updateTime": 1633536000000
    },
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenExpireTime": 1634140800000
  },
  "timestamp": 1633536000000
}
```

---

#### 1.3 用户登出
```http
POST /user/logout
Authorization: Bearer {token}
```

**请求体:**
```typescript
interface LogoutRequest {
  userId: number;    // 用户ID
  token?: string;    // Token (可选)
}
```

**响应:**
```typescript
// 成功: code=200, data=null
// 失败: code=400, message="错误信息"
```

---

#### 1.4 获取用户信息
```http
GET /user/{userId}
Authorization: Bearer {token}
```

**响应:**
```typescript
interface UserInfo {
  userId: number;
  username: string;
  nickname: string;
  avatarUrl?: string;
  signature?: string;
  status: number;
  createTime: number;
  updateTime: number;
}
```

---

#### 1.5 验证Token
```http
POST /user/validate-token
```

**请求体:**
```typescript
interface TokenRequest {
  token: string;    // 要验证的Token
}
```

**响应:**
```typescript
// 成功: 返回用户信息
// 失败: code=400, message="Token无效或已过期"
```

---

#### 1.6 修改密码
```http
POST /user/change-password
Authorization: Bearer {token}
```

**请求体:**
```typescript
interface ChangePasswordRequest {
  userId: number;      // 用户ID
  oldPassword: string; // 旧密码
  newPassword: string; // 新密码
}
```

**响应:**
```typescript
// 成功: code=200, message="密码修改成功，请重新登录"
// 失败: code=400, message="错误信息"
```

---

### 👥 好友管理接口

#### 1.7 添加好友
```http
POST /friend/add
Authorization: Bearer {token}
```

**请求参数:**
- `friendId` (Long) - 好友用户ID
- `userId` (Long) - 当前用户ID (可以由拦截器自动注入)

**请求示例 (Query):**
```
POST /friend/add?userId=1&friendId=2
```

**响应:**
```typescript
// 成功: code=200, message="添加成功"
// 失败: code=500, message="错误信息"
```

---

#### 1.8 删除好友
```http
DELETE /friend/remove/{friendId}
Authorization: Bearer {token}
```

**请求参数:**
- `friendId` (Long, 路径参数) - 要删除的好友ID
- `userId` (Long) - 当前用户ID (可以由拦截器自动注入)

**请求示例:**
```
DELETE /friend/remove/2?userId=1
```

**响应:**
```typescript
// 成功: code=200, message="删除成功"
// 失败: code=500, message="错误信息"
```

---

#### 1.9 获取好友列表
```http
GET /friend/list
Authorization: Bearer {token}
```

**请求参数:**
- `userId` (Long) - 当前用户ID (可以由拦截器自动注入)

**响应:**
```typescript
interface FriendListResponse {
  code: number;
  message: string;
  data: FriendResponse[];
}

interface FriendResponse {
  userId: number;         // 好友用户ID
  username: string;       // 用户名
  nickname: string;       // 昵称
  avatarUrl?: string;     // 头像
  signature?: string;     // 个性签名
  status: number;         // 用户状态: 0=离线, 1=在线
  relationStatus: number; // 好友关系状态: 0=正常, 1=拉黑
}
```

---

### 🏠 聊天室管理接口

#### 1.10 创建聊天室
```http
POST /chatroom/create
Authorization: Bearer {token}
```

**请求体:**
```typescript
interface CreateChatRoomRequest {
  roomName: string;    // 聊天室名称 (必填)
  description?: string;// 聊天室描述
  roomType?: string;   // 聊天室类型: PUBLIC_ROOM 或 PRIVATE_ROOM (默认 PUBLIC_ROOM)
}
```

**响应:**
```typescript
interface ChatRoomResponse {
  roomId: number;
  roomName: string;
  description: string;
  ownerId: number;
  roomType: string;
  status: string;
  createTime: string;
}

// 成功: code=200, message="创建成功", data返回聊天室信息
// 失败: code=400, message="创建失败原因"
```

---

#### 1.11 获取所有活跃聊天室列表
```http
GET /chatroom/list
Authorization: Bearer {token}
```

**响应:**
```typescript
// 成功: code=200, message="查询成功", data返回 ChatRoomResponse 数组
interface ChatRoomListResponse {
  code: number;
  message: string;
  data: ChatRoomResponse[];
}
```

---

#### 1.12 下线聊天室
```http
POST /chatroom/{roomId}/offline
Authorization: Bearer {token}
```

**说明:**
仅聊天室的**创建者**有权限执行此操作。

**响应:**
```typescript
// 成功: code=200, message="下线成功"
// 失败: code=403, message="仅聊天室创建者可操作"
```

---

#### 1.13 删除聊天室
```http
DELETE /chatroom/{roomId}
Authorization: Bearer {token}
```

**说明:**
仅聊天室的**创建者**有权限执行此操作，此操作为软删除。

**响应:**
```typescript
// 成功: code=200, message="删除成功"
// 失败: code=403, message="仅聊天室创建者可操作"
```

---

#### 1.14 获取聊天室在线人数
```http
GET /chatroom/{roomId}/count
Authorization: Bearer {token}
```

**响应:**
```typescript
// 成功: code=200, message="查询成功", data返回在线人数(number)
```

---

### 💬 消息历史记录接口

#### 1.15 获取私聊历史记录
```http
GET /message/private/history
Authorization: Bearer {token}
```

**请求参数:**
- `userId` (Long) - 当前用户ID
- `friendId` (Long) - 好友用户ID
- `current` (int, 默认1) - 当前页码
- `size` (int, 默认20) - 每页数量

**请求示例:**
```
GET /message/private/history?userId=1&friendId=2&current=1&size=20
```

**响应:**
```typescript
interface PrivateMessageHistoryResponse {
  code: number;
  message: string;
  data: {
    records: PrivateMessage[];
    total: number;
    size: number;
    current: number;
    pages: number;
  }
}

interface PrivateMessage {
  msgId: number;       // 消息ID
  senderId: number;    // 发送者ID
  receiverId: number;  // 接收者ID
  content: string;     // 消息内容
  isRead: number;      // 是否已读: 0=未读, 1=已读
  createTime: string;  // 创建时间
}
```

---

#### 1.16 获取群聊历史记录
```http
GET /message/chatroom/history
Authorization: Bearer {token}
```

**请求参数:**
- `roomId` (Long) - 聊天室ID
- `current` (int, 默认1) - 当前页码
- `size` (int, 默认20) - 每页数量

**请求示例:**
```
GET /message/chatroom/history?roomId=1001&current=1&size=20
```

**响应:**
```typescript
interface ChatRoomMessageHistoryResponse {
  code: number;
  message: string;
  data: {
    records: ChatRoomMessage[];
    total: number;
    size: number;
    current: number;
    pages: number;
  }
}

interface ChatRoomMessage {
  msgId: number;       // 消息ID
  roomId: number;      // 聊天室ID
  senderId: number;    // 发送者ID
  content: string;     // 消息内容
  createTime: string;  // 创建时间
}
```

---

## 2. Netty WebSocket 接口

### 🌐 连接信息
- **WebSocket URL**: `ws://localhost:9999/ws`
- **协议**: WebSocket
- **消息格式**: JSON

### 📦 消息结构
```typescript
interface CompleteMessage {
  appId: number;        // 业务线ID
  uid: number;          // 用户ID
  token: string;        // 用户Token
  compression?: number; // 是否压缩
  encryption?: number;  // 是否加密
  messageType: number;  // 消息类型
  toId: number;         // 接收方ID (聊天室ID或用户ID)
  content: string;      // 消息内容
  timeStamp: number;    // 发送时间戳
}
```

---

### 🔗 连接管理 (appId=0)

#### 2.1 建立连接 (messageType=0)
```typescript
const connectMessage: CompleteMessage = {
  appId: 0,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 0,
  toId: 0,              // 建连时可为0
  content: "",
  timeStamp: Date.now()
};

// 发送
websocket.send(JSON.stringify(connectMessage));

// 响应
{
  "appId": 0,
  "uid": 123,
  "messageType": 0,
  "content": "连接建立成功",
  "timeStamp": 1633536000000
}
```

#### 2.2 断开连接 (messageType=1)
```typescript
const disconnectMessage: CompleteMessage = {
  appId: 0,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 1,
  toId: 0,
  content: "",
  timeStamp: Date.now()
};

// 发送
websocket.send(JSON.stringify(disconnectMessage));

// 响应
{
  "appId": 0,
  "uid": 123,
  "messageType": 1,
  "content": "连接已断开",
  "timeStamp": 1633536000000
}
```

#### 2.3 心跳保活 (messageType=2)
```typescript
const heartbeatMessage: CompleteMessage = {
  appId: 0,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 2,
  toId: 0,
  content: "ping",
  timeStamp: Date.now()
};

// 发送 (建议每30秒发送一次)
websocket.send(JSON.stringify(heartbeatMessage));

// 响应
{
  "appId": 0,
  "uid": 123,
  "messageType": 2,
  "content": "pong",
  "timeStamp": 1633536000000
}
```

#### 2.2 断开连接 (messageType=1)
```typescript
const disconnectMessage: CompleteMessage = {
  appId: 1,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 1,
  toId: 0,
  content: "",
  timeStamp: Date.now()
};

// 发送后连接会被关闭
```

#### 2.3 心跳保活 (messageType=2)
```typescript
const heartbeatMessage: CompleteMessage = {
  appId: 1,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 2,
  toId: 0,
  content: "ping",
  timeStamp: Date.now()
};

// 响应
{
  "appId": 1,
  "uid": 123,
  "messageType": 2,
  "content": "pong",
  "timeStamp": 1633536000000
}
```

---

### 💬 聊天室管理 (appId=1)

#### 2.4 进入聊天室 (messageType=0)
```typescript
const joinRoomMessage: CompleteMessage = {
  appId: 1,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 0,
  toId: 1001,           // 聊天室ID
  content: "",
  timeStamp: Date.now()
};

// 响应
{
  "appId": 1,
  "uid": 123,
  "messageType": 0,
  "toId": 1001,
  "content": "成功进入聊天室: 1001",
  "timeStamp": 1633536000000
}
```

#### 2.5 发送聊天室消息 (messageType=1)
```typescript
const chatMessage: CompleteMessage = {
  appId: 1,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 1,
  toId: 1001,           // 聊天室ID
  content: "Hello everyone!",
  timeStamp: Date.now()
};

// 聊天室内所有用户都会收到此消息
```

#### 2.6 退出聊天室 (messageType=2)
```typescript
const leaveRoomMessage: CompleteMessage = {
  appId: 1,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 2,
  toId: 1001,           // 聊天室ID
  content: "",
  timeStamp: Date.now()
};

// 响应
{
  "appId": 1,
  "uid": 123,
  "messageType": 2,
  "toId": 1001,
  "content": "成功退出聊天室: 1001",
  "timeStamp": 1633536000000
}
```

---

### 👤 私聊管理 (appId=2)

#### 2.7 发送私聊消息 (messageType=1)
```typescript
const privateMessage: CompleteMessage = {
  appId: 2,
  uid: 123,               // 发送者ID
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 1,         // 私聊消息类型
  toId: 456,              // 接收者(好友)的userID
  content: "你好，最近怎么样？",
  timeStamp: Date.now()
};

// 目标用户(如果在线)将会收到格式相同的推送消息
// 消息会由服务器自动持久化，如果对方不在线也会保存为离线未读消息
```

---

## 3. 前端集成示例

### 🚀 Vue 3 + TypeScript 实现

#### 3.1 HTTP 客户端封装
```typescript
// api/client.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

class ApiClient {
  private token: string = '';

  constructor() {
    // 从localStorage获取token
    this.token = localStorage.getItem('auth_token') || '';
  }

  // 设置token
  setToken(token: string) {
    this.token = token;
    localStorage.setItem('auth_token', token);
  }

  // 清除token
  clearToken() {
    this.token = '';
    localStorage.removeItem('auth_token');
  }

  // 通用请求方法
  private async request<T>(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    data?: any
  ): Promise<ApiResponse<T>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }

    try {
      const response = await axios({
        method,
        url: `${API_BASE_URL}${url}`,
        headers,
        data,
      });

      return response.data;
    } catch (error: any) {
      if (error.response?.status === 401) {
        this.clearToken();
        // 可以触发重新登录
      }
      throw error.response?.data || error;
    }
  }

  // 用户API
  async register(data: RegisterRequest): Promise<ApiResponse<RegisterResponse>> {
    return this.request('POST', '/user/register', data);
  }

  async login(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    const response = await this.request<LoginResponse>('POST', '/user/login', data);
    if (response.code === 200 && response.data?.token) {
      this.setToken(response.data.token);
    }
    return response;
  }

  async logout(data: LogoutRequest): Promise<ApiResponse<string>> {
    const response = await this.request<string>('POST', '/user/logout', data);
    this.clearToken();
    return response;
  }

  async getUserInfo(userId: number): Promise<ApiResponse<UserInfo>> {
    return this.request('GET', `/user/${userId}`);
  }

  async validateToken(token: string): Promise<ApiResponse<UserInfo>> {
    return this.request('POST', '/user/validate-token', { token });
  }

  async changePassword(data: ChangePasswordRequest): Promise<ApiResponse<string>> {
    return this.request('POST', '/user/change-password', data);
  }
}

export const apiClient = new ApiClient();
```

#### 3.2 WebSocket 客户端封装
```typescript
// websocket/client.ts
class WebSocketClient {
  private ws: WebSocket | null = null;
  private url: string = 'ws://localhost:9999/ws';
  private token: string = '';
  private userId: number = 0;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 5;
  private heartbeatInterval: NodeJS.Timeout | null = null;

  // 事件监听器
  private listeners: Map<string, Function[]> = new Map();

  constructor() {
    this.setupEventListeners();
  }

  // 连接WebSocket
  connect(token: string, userId: number): Promise<boolean> {
    return new Promise((resolve, reject) => {
      this.token = token;
      this.userId = userId;

      try {
        this.ws = new WebSocket(this.url);

        this.ws.onopen = () => {
          console.log('WebSocket连接已建立');
          this.reconnectAttempts = 0;
          
          // 发送建连消息
          this.sendConnectMessage();
          
          // 启动心跳
          this.startHeartbeat();
          
          this.emit('connected');
          resolve(true);
        };

        this.ws.onmessage = (event) => {
          try {
            const message: CompleteMessage = JSON.parse(event.data);
            this.handleMessage(message);
          } catch (error) {
            console.error('解析消息失败:', error);
          }
        };

        this.ws.onclose = (event) => {
          console.log('WebSocket连接已关闭', event.code, event.reason);
          this.stopHeartbeat();
          this.emit('disconnected', event);
          
          // 自动重连
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            setTimeout(() => {
              this.reconnectAttempts++;
              this.connect(this.token, this.userId);
            }, 3000);
          }
        };

        this.ws.onerror = (error) => {
          console.error('WebSocket错误:', error);
          this.emit('error', error);
          reject(error);
        };

      } catch (error) {
        reject(error);
      }
    });
  }

  // 断开连接
  disconnect() {
    if (this.ws) {
      // 发送断连消息
      this.sendDisconnectMessage();
      
      // 关闭连接
      this.ws.close();
      this.ws = null;
    }
    this.stopHeartbeat();
  }

  // 发送消息
  private send(message: CompleteMessage) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.error('WebSocket未连接');
    }
  }

  // 发送建连消息
  private sendConnectMessage() {
    this.send({
      appId: 0,
      uid: this.userId,
      token: this.token,
      messageType: 0,
      toId: 0,
      content: '',
      timeStamp: Date.now()
    });
  }

  // 发送断连消息
  private sendDisconnectMessage() {
    this.send({
      appId: 0,
      uid: this.userId,
      token: this.token,
      messageType: 1,
      toId: 0,
      content: '',
      timeStamp: Date.now()
    });
  }

  // 发送心跳
  private sendHeartbeat() {
    this.send({
      appId: 0,
      uid: this.userId,
      token: this.token,
      messageType: 2,
      toId: 0,
      content: 'ping',
      timeStamp: Date.now()
    });
  }

  // 启动心跳
  private startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat();
    }, 30000); // 30秒心跳
  }

  // 停止心跳
  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  // 聊天室操作
  joinChatRoom(roomId: number) {
    this.send({
      appId: 1,
      uid: this.userId,
      token: this.token,
      messageType: 0,
      toId: roomId,
      content: '',
      timeStamp: Date.now()
    });
  }

  sendChatMessage(roomId: number, content: string) {
    this.send({
      appId: 1,
      uid: this.userId,
      token: this.token,
      messageType: 1,
      toId: roomId,
      content,
      timeStamp: Date.now()
    });
  }

  leaveChatRoom(roomId: number) {
    this.send({
      appId: 1,
      uid: this.userId,
      token: this.token,
      messageType: 2,
      toId: roomId,
      content: '',
      timeStamp: Date.now()
    });
  }

  // 私聊操作
  sendPrivateMessage(friendId: number, content: string) {
    this.send({
      appId: 2,
      uid: this.userId,
      token: this.token,
      messageType: 1, // 可自定义私聊消息的具体type
      toId: friendId,
      content,
      timeStamp: Date.now()
    });
  }

  // 处理收到的消息
  private handleMessage(message: CompleteMessage) {
    console.log('收到消息:', message);

    if (message.appId === 0) {
      // 连接管理消息
      this.emit('connection-message', message);
    } else if (message.appId === 1) {
      // 聊天室消息
      if (message.messageType === 1) {
        // 聊天消息
        this.emit('chat-message', message);
      } else {
        // 聊天室系统消息
        this.emit('room-message', message);
      }
    } else if (message.appId === 2) {
      // 收到私聊消息
      this.emit('private-message', message);
    }

    // 触发通用消息事件
    this.emit('message', message);
  }

  // 事件监听
  on(event: string, callback: Function) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event)!.push(callback);
  }

  // 移除事件监听
  off(event: string, callback?: Function) {
    if (!this.listeners.has(event)) return;
    
    if (callback) {
      const callbacks = this.listeners.get(event)!;
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    } else {
      this.listeners.delete(event);
    }
  }

  // 触发事件
  private emit(event: string, ...args: any[]) {
    if (this.listeners.has(event)) {
      this.listeners.get(event)!.forEach(callback => {
        callback(...args);
      });
    }
  }

  // 设置事件监听器
  private setupEventListeners() {
    // 连接相关事件
    this.on('connected', () => {
      console.log('WebSocket已连接');
    });

    this.on('disconnected', (event: CloseEvent) => {
      console.log('WebSocket已断开连接');
    });

    this.on('error', (error: Event) => {
      console.error('WebSocket错误:', error);
    });

    // 消息事件
    this.on('connection-message', (message: CompleteMessage) => {
      if (message.messageType === 0) {
        console.log('连接建立成功:', message.content);
      } else if (message.messageType === 2) {
        console.log('心跳响应:', message.content);
      }
    });
  }
}

export const wsClient = new WebSocketClient();
```

#### 3.3 Vue 组件使用示例
```vue
<!-- LoginForm.vue -->
<template>
  <div class="login-form">
    <el-form @submit.prevent="handleLogin">
      <el-form-item>
        <el-input v-model="form.username" placeholder="用户名" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.password" type="password" placeholder="密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleLogin" :loading="loading">
          登录
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { apiClient } from '@/api/client';
import { wsClient } from '@/websocket/client';

const form = ref({
  username: '',
  password: ''
});

const loading = ref(false);

const handleLogin = async () => {
  loading.value = true;
  
  try {
    const response = await apiClient.login(form.value);
    
    if (response.code === 200 && response.data) {
      const { user, token } = response.data;
      
      // 连接WebSocket
      await wsClient.connect(token, user.userId);
      
      // 跳转到聊天页面
      // router.push('/chat');
      
      console.log('登录成功');
    } else {
      console.error('登录失败:', response.message);
    }
  } catch (error) {
    console.error('登录错误:', error);
  } finally {
    loading.value = false;
  }
};
</script>
```

---

### 🔧 错误处理

#### HTTP API 错误码
- `200`: 请求成功
- `400`: 请求参数错误
- `401`: 未授权(Token无效/过期)
- `404`: 资源不存在
- `500`: 服务器内部错误

#### WebSocket 错误处理
- 连接失败自动重试
- 心跳超时检测
- 消息发送失败处理
- 异常断线重连

---

### 📝 使用注意事项

1. **Token管理**: 
   - 登录后保存Token到localStorage
   - 每次HTTP请求携带Token
   - WebSocket建连时验证Token

2. **心跳机制**:
   - 每30秒发送一次心跳
   - 防止连接被服务器断开

3. **错误处理**:
   - HTTP 401错误时清除Token并重新登录
   - WebSocket断线时自动重连

4. **消息去重**:
   - 使用timeStamp字段防止重复消息

这份文档涵盖了所有接口的详细使用方法，您可以直接复制到前端项目中使用！