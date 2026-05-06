# 1.启动方式

浏览器需要先信任证书

终端中，先导出证书：keytool -exportcert \
  -alias graduation-im-wss \
  -keystore IM-Bootstrap/src/main/resources/certs/local-wss.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -rfc \
  -file local-wss.crt

在 macOS 上加入系统信任：  sudo security add-trusted-cert \
  -d \
  -r trustRoot \
  -k /Library/Keychains/System.keychain \
  local-wss.crt

## 1.1 WSS 模式

添加vm options：

-Dnetty.ssl.enabled=true


## 1.2 WS 模式

vm options

-Dnetty.ssl.enabled=false
  
## 1.3 消息加密开启

生成32字节秘钥：openssl rand -base64 32

vm options

-Dmessage.crypto.enabled=true

-Dmessage.crypto.key=35ZeojmK50lRQ05RhFLkxaYztjy1gDAbVja/ZMpe+Qo=
