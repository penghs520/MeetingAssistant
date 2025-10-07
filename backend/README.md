# Meeting Assistant Backend

Spring Boot后端服务，提供WebSocket音频流处理、AI转录和会议管理功能。

## 技术栈

- Spring Boot 3.3.5
- Spring AI 1.0.0-M4
- PostgreSQL
- WebSocket
- OpenAI GPT-4o

## 快速开始

### 1. 配置数据库

```bash
psql -U postgres
CREATE DATABASE meeting_assistant;
\q
```

### 2. 设置环境变量

```bash
export OPENAI_API_KEY=your_openai_api_key_here
```

### 3. 修改配置（可选）

编辑 `src/main/resources/application.yml`

### 4. 运行应用

```bash
./mvnw spring-boot:run
```

服务将在 http://localhost:8080 启动

## API端点

- `POST /api/meetings` - 创建新会议
- `GET /api/meetings` - 获取所有会议
- `GET /api/meetings/{id}` - 获取会议详情
- `POST /api/meetings/{id}/complete` - 完成会议并生成总结
- `GET /api/meetings/{id}/transcripts` - 获取会议转录
- `POST /api/meetings/{id}/speakers` - 添加说话人

## WebSocket端点

- `ws://localhost:8080/ws/audio-stream?meetingId={id}`

## 项目结构

```
src/main/java/com/meeting/assistant/
├── ai/                  # AI服务（可插拔）
│   ├── AIService.java
│   └── OpenAIProvider.java
├── audio/               # 音频处理
├── websocket/           # WebSocket处理
│   └── AudioStreamHandler.java
├── entity/              # JPA实体
│   ├── Meeting.java
│   ├── Speaker.java
│   └── Transcript.java
├── service/             # 业务�务
├── repository/          # 数据访问
├── controller/          # REST控制器
└── config/              # 配置类
```

## 开发

### 构建

```bash
./mvnw clean package
```

### 测试

```bash
./mvnw test
```

### 运行

```bash
java -jar target/assistant-1.0.0-SNAPSHOT.jar
```

## 环境变量

| 变量名 | 说明 | 必需 |
|--------|------|------|
| OPENAI_API_KEY | OpenAI API密钥 | 是 |
| DATABASE_URL | 数据库连接URL | 否 |
| DATABASE_USERNAME | 数据库用户名 | 否 |
| DATABASE_PASSWORD | 数据库密码 | 否 |
