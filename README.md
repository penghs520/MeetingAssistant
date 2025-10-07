# Meeting Assistant - AI会议助手

一个基于AI的智能会议助手应用，能够实时捕获系统音频、转录为文字、识别说话人，并生成智能会议总结。

## 项目特点

- 🎤 **系统音频捕获** - 实时捕获其他会议App（钉钉、腾讯会议等）的声音
- 🤖 **AI智能转录** - 使用GPT-4o等大模型进行语音转文字
- 👥 **说话人识别** - 手动标记并区分不同说话人
- 📝 **智能总结** - 自动生成会议摘要、关键要点和待办事项
- 📱 **跨平台支持** - React Native开发，支持Android（iOS后续支持）
- 🔌 **可插拔架构** - 支持切换不同AI模型（预留本地模型能力）

## 技术栈

### 移动端
- React Native 0.81.4 + TypeScript
- Android AudioPlaybackCapture API
- WebSocket客户端

### 后端
- Spring Boot 3.3.5
- Spring AI 1.0.0-M4
- PostgreSQL 16+
- WebSocket (Spring WebSocket)

### AI服务
- OpenAI GPT-4o（转录+总结）
- 预留多模型切换能力

## 项目结构

```
MeetingAssistant/
├── docs/                    # 项目文档
│   ├── PLAN.md             # 实施计划
│   ├── DATABASE.md         # 数据库设计
│   └── API.md              # API文档（待补充）
├── mobile-app/             # React Native应用
│   ├── android/            # Android原生代码
│   ├── ios/                # iOS（未来）
│   └── src/                # TypeScript源码
└── backend/                # Spring Boot后端
    ├── pom.xml
    └── src/main/java/com/meeting/assistant/
        ├── ai/             # AI服务（可插拔）
        ├── audio/          # 音频处理
        ├── websocket/      # WebSocket
        ├── entity/         # 数据实体
        ├── service/        # 业务服务
        ├── repository/     # 数据访问
        └── controller/     # REST API
```

## 快速开始

### 前置要求

- Java 17+
- Node.js 20.19.4+
- PostgreSQL 16+
- Android Studio（移动端开发）
- OpenAI API Key

### 后端启动

1. **创建数据库**

```bash
psql -U postgres
CREATE DATABASE meeting_assistant;
```

2. **配置环境变量**

```bash
export OPENAI_API_KEY=your_openai_api_key
```

3. **修改配置文件**

编辑 `backend/src/main/resources/application.yml`，配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/meeting_assistant
    username: your_username
    password: your_password
```

4. **启动应用**

```bash
cd backend
./mvnw spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 移动端启动

1. **安装依赖**

```bash
cd mobile-app
npm install
```

2. **配置后端地址**

编辑 `mobile-app/src/config.ts`，设置后端WebSocket地址：

```typescript
export const WEBSOCKET_URL = 'ws://your-server:8080/ws/audio-stream';
```

3. **运行Android应用**

```bash
npx react-native run-android
```

## 使用指南

### 用户流程

1. 打开其他会议App（钉钉、腾讯会议等）
2. 启动Meeting Assistant
3. 点击"开始新会议"
4. 授予音频录制权限
5. 实时查看转录文字
6. 标记说话人："这是张三"
7. 会议结束后点击"停止并生成总结"
8. 查看AI生成的会议纪要
9. 导出或分享

### API接口

#### 创建会议

```http
POST /api/meetings
Content-Type: application/json

{
  "title": "产品评审会议"
}
```

#### 获取会议列表

```http
GET /api/meetings
```

#### 获取会议转录

```http
GET /api/meetings/{id}/transcripts
```

#### 完成会议并生成总结

```http
POST /api/meetings/{id}/complete
```

更多API文档见 [docs/API.md](docs/API.md)

### WebSocket协议

#### 连接

```
ws://localhost:8080/ws/audio-stream?meetingId=1
```

#### 发送音频数据

```javascript
// 发送二进制音频数据（WAV格式）
websocket.send(audioArrayBuffer);
```

#### 接收转录结果

```json
{
  "type": "transcript",
  "id": 123,
  "content": "大家好，今天我们讨论一下项目进度",
  "timestamp": "2025-10-07T14:30:15",
  "speakerId": 5
}
```

## 开发指南

### 添加新的AI模型

1. 实现 `AIService` 接口：

```java
@Service
public class CustomModelProvider implements AIService {
    @Override
    public String transcribe(byte[] audioData) {
        // 实现音频转录
    }

    @Override
    public String summarize(String transcript, List<Speaker> speakers) {
        // 实现文本总结
    }
}
```

2. 在 `application.yml` 配置切换策略（未来）

### Android原生模块开发

音频捕获模块位于：`mobile-app/android/app/src/main/java/AudioCaptureModule.java`

使用AudioPlaybackCapture API捕获系统音频。

### 数据库迁移

使用Hibernate自动建表（开发环境）：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

生产环境建议使用SQL脚本手动管理（见 `docs/DATABASE.md`）。

## 部署

### Docker部署（推荐）

```bash
# 构建后端镜像
cd backend
docker build -t meeting-assistant-backend .

# 启动服务
docker-compose up -d
```

### 传统部署

```bash
# 打包后端
cd backend
./mvnw clean package

# 运行
java -jar target/assistant-1.0.0-SNAPSHOT.jar
```

### Android APK打包

```bash
cd mobile-app/android
./gradlew assembleRelease
```

APK位置：`mobile-app/android/app/build/outputs/apk/release/app-release.apk`

## 路线图

### MVP (Week 1-8)
- [x] 项目基础搭建
- [x] 数据库设计
- [x] Spring Boot后端架构
- [ ] Android音频捕获
- [ ] WebSocket通信
- [ ] AI转录功能
- [ ] 说话人标记
- [ ] AI总结生成
- [ ] 移动端UI

### V2.0
- [ ] iOS版本支持
- [ ] 声纹自动识别
- [ ] 多模型切换UI
- [ ] 本地Whisper模型
- [ ] 离线模式

### V3.0
- [ ] 多语言实时翻译
- [ ] 会议协作功能
- [ ] 智能提醒
- [ ] 数据分析仪表板

## 贡献指南

欢迎提交Issue和Pull Request！

### 开发规范

- 代码风格：Java遵循Google Java Style Guide
- 提交规范：使用Conventional Commits
- 分支策略：Feature Branch Workflow

### 提交格式

```
feat: 添加音频捕获功能
fix: 修复WebSocket断线问题
docs: 更新API文档
refactor: 重构AI服务接口
```

## 许可证

MIT License

## 联系方式

- 项目主页：https://github.com/your-username/MeetingAssistant
- 问题反馈：https://github.com/your-username/MeetingAssistant/issues

---

**当前版本**: v1.0.0-SNAPSHOT
**最后更新**: 2025-10-07
