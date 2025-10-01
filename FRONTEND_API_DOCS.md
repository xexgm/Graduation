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

### 🔗 连接管理 (appId=1)

#### 2.1 建立连接 (messageType=0)
```typescript
const connectMessage: CompleteMessage = {
  appId: 1,
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
  "appId": 1,
  "uid": 123,
  "messageType": 0,
  "content": "连接建立成功",
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

### 💬 聊天室管理 (appId=2)

#### 2.4 进入聊天室 (messageType=0)
```typescript
const joinRoomMessage: CompleteMessage = {
  appId: 2,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 0,
  toId: 1001,           // 聊天室ID
  content: "",
  timeStamp: Date.now()
};

// 响应
{
  "appId": 2,
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
  appId: 2,
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
  appId: 2,
  uid: 123,
  token: "eyJhbGciOiJIUzUxMiJ9...",
  messageType: 2,
  toId: 1001,           // 聊天室ID
  content: "",
  timeStamp: Date.now()
};

// 响应
{
  "appId": 2,
  "uid": 123,
  "messageType": 2,
  "toId": 1001,
  "content": "成功退出聊天室: 1001",
  "timeStamp": 1633536000000
}
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
      appId: 1,
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
      appId: 1,
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
      appId: 1,
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
      appId: 2,
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
      appId: 2,
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
      appId: 2,
      uid: this.userId,
      token: this.token,
      messageType: 2,
      toId: roomId,
      content: '',
      timeStamp: Date.now()
    });
  }

  // 处理收到的消息
  private handleMessage(message: CompleteMessage) {
    console.log('收到消息:', message);

    if (message.appId === 1) {
      // 连接管理消息
      this.emit('connection-message', message);
    } else if (message.appId === 2) {
      // 聊天室消息
      if (message.messageType === 1) {
        // 聊天消息
        this.emit('chat-message', message);
      } else {
        // 聊天室系统消息
        this.emit('room-message', message);
      }
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