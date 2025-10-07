# AI会议助手 MVP 实施计划

## 项目概述

一个基于AI的会议助手应用，能够独立于具体会议软件运行，实时捕获系统音频、转录为文字、识别说话人，并生成智能会议总结。

## MVP核心功能

1. **Android音频捕获** - 实时获取系统音频（捕获其他会议App的声音）
2. **语音转文字** - 使用AI大模型进行实时转录
3. **说话人区分** - 手动标记不同说话人身份
4. **AI总结** - 会议结束后自动生成结构化摘要
5. **历史记录** - 查看和管理过往会议记录

## 技术栈

### 移动端
- **框架**: React Native + TypeScript
- **平台**: Android（MVP阶段）
- **音频捕获**: Android AudioPlaybackCapture API
- **通信**: WebSocket客户端
- **状态管理**: React Context/Redux

### 后端
- **框架**: Spring Boot 3.x
- **AI框架**: Spring AI
- **AI模型**: OpenAI GPT-4o（语音转文字 + 文本总结）
- **数据库**: PostgreSQL
- **实时通信**: WebSocket (Spring WebSocket)
- **缓存**: Redis（可选）

### 架构设计原则
- **模型可插拔**: 使用接口抽象AI服务调用，预留多模型切换能力
- **本地模型支持**: 架构预留本地模型运行能力
- **微服务就绪**: 模块化设计，便于后续拆分服务

## 项目结构

```
MeetingAssistant/
├── docs/                        # 项目文档
│   ├── PLAN.md                 # 本文件 - 实施计划
│   ├── ARCHITECTURE.md         # 架构设计文档
│   ├── API.md                  # API接口文档
│   └── DATABASE.md             # 数据库设计
│
├── mobile-app/                  # React Native应用
│   ├── android/                # Android原生代码
│   │   └── app/src/main/java/com/meeting/assistant/
│   │       └── AudioCaptureModule.java
│   ├── ios/                    # iOS（未来支持）
│   ├── src/
│   │   ├── screens/            # 页面组件
│   │   │   ├── MeetingScreen.tsx
│   │   │   ├── TranscriptScreen.tsx
│   │   │   └── HistoryScreen.tsx
│   │   ├── services/           # 业务服务
│   │   │   ├── AudioService.ts
│   │   │   └── WebSocketService.ts
│   │   ├── components/         # UI组件
│   │   │   ├── AudioRecorder.tsx
│   │   │   ├── TranscriptView.tsx
│   │   │   └── SpeakerTag.tsx
│   │   ├── types/              # TypeScript类型定义
│   │   └── utils/              # 工具函数
│   ├── package.json
│   └── tsconfig.json
│
└── backend/                     # Spring Boot后端
    ├── pom.xml
    └── src/main/java/com/meeting/assistant/
        ├── ai/                 # AI服务模块
        │   ├── AIService.java              # AI服务接口（可插拔设计）
        │   └── OpenAIProvider.java         # GPT-4o实现
        ├── audio/              # 音频处理模块
        │   ├── AudioProcessor.java
        │   └── AudioSegment.java
        ├── websocket/          # WebSocket通信
        │   └── AudioStreamHandler.java
        ├── entity/             # 数据实体
        │   ├── Meeting.java
        │   ├── Transcript.java
        │   └── Speaker.java
        ├── service/            # 业务服务
        │   ├── TranscriptionService.java
        │   ├── SummaryService.java
        │   └── MeetingService.java
        ├── repository/         # 数据访问层
        │   ├── MeetingRepository.java
        │   ├── TranscriptRepository.java
        │   └── SpeakerRepository.java
        ├── controller/         # REST API
        │   └── MeetingController.java
        └── config/             # 配置类
            ├── SpringAIConfig.java
            └── WebSocketConfig.java
```

## MVP实施计划（8周）

### Week 1: 项目基础搭建
**目标**: 完成开发环境和项目骨架

- [x] 创建项目文档
- [ ] 初始化React Native项目（Android only）
- [ ] 创建Spring Boot项目
- [ ] 配置Spring AI + OpenAI API
- [ ] 设计数据库schema
- [ ] 搭建PostgreSQL数据库
- [ ] Git仓库初始化

**交付物**:
- 可运行的空React Native App
- 可运行的Spring Boot应用
- 完整的项目文档

### Week 2: Android音频捕获
**目标**: 实现系统音频实时捕获

- [ ] 创建Android Native Module
- [ ] 实现MediaProjection权限申请流程
- [ ] 使用AudioPlaybackCapture捕获系统音频
- [ ] 实现前台服务（保持后台运行）
- [ ] 音频格式转换（PCM → Opus/AAC）
- [ ] 音频数据桥接到React Native

**技术要点**:
```java
// AudioPlaybackCapture核心实现
AudioPlaybackCaptureConfiguration config =
    new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
        .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
        .build();
```

**交付物**:
- 能够捕获其他App音频的Android模块
- 前台服务实现

### Week 3: WebSocket实时通信
**目标**: 建立移动端与后端的实时音频传输

- [ ] Spring Boot WebSocket服务器配置
- [ ] 设计音频流传输协议
- [ ] React Native WebSocket客户端实现
- [ ] 音频分段策略（3-5秒/段）
- [ ] 断线重连机制
- [ ] 音频缓冲队列管理

**数据协议**:
```json
{
  "type": "audio_chunk",
  "meetingId": "uuid",
  "timestamp": 1234567890,
  "audioData": "base64_encoded_audio",
  "format": "opus",
  "sampleRate": 16000
}
```

**交付物**:
- 稳定的WebSocket通信链路
- 音频实时传输能力

### Week 4: AI语音转文字
**目标**: 实现实时语音转录功能

- [ ] Spring AI集成OpenAI GPT-4o
- [ ] 实现音频转文字服务
- [ ] 转录结果时间戳记录
- [ ] WebSocket实时推送转录文本
- [ ] 移动端实时显示转录内容
- [ ] 错误处理和重试机制

**核心代码**:
```java
@Service
public class OpenAIProvider implements AIService {
    private final ChatClient chatClient;

    @Override
    public String transcribe(byte[] audio) {
        return chatClient.prompt()
            .user(u -> u.media(MimeTypeUtils.parseMimeType("audio/wav"), audio))
            .call()
            .content();
    }
}
```

**交付物**:
- 实时语音转文字功能
- 转录文本实时显示

### Week 5: 说话人标记功能
**目标**: 支持手动标记和区分不同说话人

- [ ] 基础说话人检测（音频特征变化）
- [ ] 移动端说话人标记UI
- [ ] 说话人信息管理
- [ ] 转录文本与说话人关联
- [ ] 不同说话人颜色/头像区分显示
- [ ] 说话人列表管理

**UI设计**:
```
[张三] 14:30:15
大家好，今天我们讨论一下项目进度...

[李四] 14:30:45
好的，我先汇报一下我的部分...

[+ 标记新说话人]
```

**交付物**:
- 说话人标记功能
- 说话人区分显示

### Week 6: AI会议总结
**目标**: 实现会议结束后的智能总结生成

- [ ] 会议结束触发总结生成
- [ ] 使用GPT-4o生成会议纪要
- [ ] Prompt工程优化
  - 会议摘要
  - 关键要点
  - 行动项(Action Items)
  - 按说话人分组发言
- [ ] 移动端总结展示页面
- [ ] 总结导出功能

**Prompt模板**:
```
请根据以下会议转录内容生成结构化总结：

会议时间：{datetime}
参会人员：{speakers}
会议时长：{duration}

转录内容：
{transcript}

请按以下格式输出：
1. 会议摘要（3-5句话）
2. 关键讨论点（分点列出）
3. 决策事项
4. 待办任务（标注负责人）
5. 各参会者主要发言内容
```

**交付物**:
- AI总结生成功能
- 总结展示页面

### Week 7: 数据持久化与历史记录
**目标**: 实现会议数据的完整存储和查询

- [ ] 实现Meeting/Transcript/Speaker实体
- [ ] JPA Repository配置
- [ ] 会议数据保存到PostgreSQL
- [ ] 音频文件上传云存储（可选）
- [ ] 历史会议列表页面
- [ ] 查看历史转录和总结
- [ ] 删除/编辑/导出功能
- [ ] 数据分页加载

**数据库Schema**:
```sql
CREATE TABLE meetings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(50),
    summary TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE speakers (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT REFERENCES meetings(id),
    name VARCHAR(100),
    color VARCHAR(20)
);

CREATE TABLE transcripts (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT REFERENCES meetings(id),
    speaker_id BIGINT REFERENCES speakers(id),
    content TEXT,
    timestamp TIMESTAMP,
    sequence_order INT
);
```

**交付物**:
- 完整的数据持久化
- 历史记录管理功能

### Week 8: 测试、优化与发布
**目标**: 完成测试和优化，准备发布

- [ ] 功能测试（端到端）
- [ ] 性能优化
  - 内存使用优化
  - 电量消耗优化
  - 网络流量优化
- [ ] 音频质量优化（降噪）
- [ ] UI/UX改进
- [ ] 错误处理完善
- [ ] 用户引导和帮助文档
- [ ] 打包Android APK
- [ ] 准备应用商店资料

**交付物**:
- 可发布的Android APK
- 用户使用文档

## 核心技术实现

### 1. Android音频捕获

```java
public class AudioCaptureModule extends ReactContextBaseJavaModule {

    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;

    @ReactMethod
    public void startCapture(Promise promise) {
        // 1. 请求MediaProjection权限
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        // 用户授权后...

        // 2. 配置AudioPlaybackCapture
        AudioPlaybackCaptureConfiguration config =
            new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build();

        // 3. 创建AudioRecord
        AudioFormat format = new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(16000)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build();

        audioRecord = new AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .build();

        // 4. 开始录制
        audioRecord.startRecording();

        // 5. 读取音频数据并发送到React Native
        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, bufferSize);
                sendAudioToReactNative(buffer);
            }
        }).start();
    }
}
```

### 2. WebSocket音频传输

**后端WebSocket Handler**:
```java
@Component
public class AudioStreamHandler extends TextWebSocketHandler {

    @Autowired
    private AIService aiService;

    @Autowired
    private TranscriptionService transcriptionService;

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] audioData = message.getPayload().array();
        String meetingId = (String) session.getAttributes().get("meetingId");

        // 异步处理音频转录
        CompletableFuture.runAsync(() -> {
            try {
                // 调用AI转录
                String text = aiService.transcribe(audioData);

                // 保存转录结果
                Transcript transcript = transcriptionService.save(meetingId, text);

                // 推送给客户端
                session.sendMessage(new TextMessage(
                    new ObjectMapper().writeValueAsString(transcript)
                ));
            } catch (Exception e) {
                // 错误处理
            }
        });
    }
}
```

**移动端WebSocket客户端**:
```typescript
class WebSocketService {
    private ws: WebSocket;

    connect(meetingId: string) {
        this.ws = new WebSocket(`ws://server/audio-stream?meetingId=${meetingId}`);

        this.ws.onmessage = (event) => {
            const transcript = JSON.parse(event.data);
            // 更新UI显示转录文本
            this.onTranscriptReceived(transcript);
        };
    }

    sendAudio(audioData: ArrayBuffer) {
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(audioData);
        }
    }
}
```

### 3. Spring AI集成

**配置类**:
```java
@Configuration
public class SpringAIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultOptions(ChatOptionsBuilder.builder()
                .withModel("gpt-4o")
                .withTemperature(0.7)
                .build())
            .build();
    }
}
```

**AI服务接口（可插拔设计）**:
```java
public interface AIService {
    /**
     * 语音转文字
     * @param audioData 音频数据
     * @return 转录文本
     */
    String transcribe(byte[] audioData);

    /**
     * 生成会议总结
     * @param transcript 完整转录文本
     * @return 结构化总结
     */
    String summarize(String transcript, List<Speaker> speakers);
}
```

**OpenAI实现**:
```java
@Service
public class OpenAIProvider implements AIService {

    private final ChatClient chatClient;

    @Autowired
    public OpenAIProvider(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String transcribe(byte[] audioData) {
        return chatClient.prompt()
            .user(u -> u
                .text("请将这段音频转录为文字")
                .media(MimeTypeUtils.parseMimeType("audio/wav"), audioData)
            )
            .call()
            .content();
    }

    @Override
    public String summarize(String transcript, List<Speaker> speakers) {
        String prompt = buildSummaryPrompt(transcript, speakers);
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    private String buildSummaryPrompt(String transcript, List<Speaker> speakers) {
        return String.format("""
            请根据以下会议转录内容生成结构化总结：

            参会人员：%s

            转录内容：
            %s

            请按以下格式输出：
            1. 会议摘要（3-5句话）
            2. 关键讨论点（分点列出）
            3. 决策事项
            4. 待办任务（标注负责人）
            5. 各参会者主要发言内容
            """,
            speakers.stream().map(Speaker::getName).collect(Collectors.joining(", ")),
            transcript
        );
    }
}
```

## 数据库设计

### 实体关系
```
Meeting (1) ----< (N) Transcript
Meeting (1) ----< (N) Speaker
Speaker (1) ----< (N) Transcript
```

### 详细Schema

```sql
-- 会议表
CREATE TABLE meetings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL, -- RECORDING, COMPLETED, FAILED
    summary TEXT,
    audio_file_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_meetings_start_time ON meetings(start_time DESC);
CREATE INDEX idx_meetings_status ON meetings(status);

-- 说话人表
CREATE TABLE speakers (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_speakers_meeting_id ON speakers(meeting_id);

-- 转录表
CREATE TABLE transcripts (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    speaker_id BIGINT REFERENCES speakers(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sequence_order INT NOT NULL,
    confidence DECIMAL(5,2), -- 置信度
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_transcripts_meeting_id ON transcripts(meeting_id);
CREATE INDEX idx_transcripts_timestamp ON transcripts(timestamp);
CREATE INDEX idx_transcripts_sequence ON transcripts(meeting_id, sequence_order);
```

## MVP功能清单

### 必须实现 ✅
- [x] Android系统音频捕获
- [ ] 实时WebSocket音频传输
- [ ] GPT-4o语音转文字
- [ ] 实时转录文本显示
- [ ] 手动说话人标记
- [ ] AI会议总结生成
- [ ] 历史会议记录
- [ ] 基础导出功能（文本/Markdown）

### 暂不实现 ❌
- iOS版本
- 多模型切换UI
- 声纹自动识别
- 离线模式
- 说话人自动分离（音频特征分析）
- 多语言支持
- 实时翻译
- 会议协作功能

### 架构预留 🔧
- AI服务接口抽象（便于后续切换模型）
- 模型Provider模式（可扩展到本地模型）
- 说话人分离服务接口（预留自动识别能力）
- 多语言i18n支持（代码层面预留）

## 用户使用流程

### 首次使用
1. 安装MeetingAssistant APK
2. 授予麦克风和媒体投影权限
3. 配置后端服务器地址（或使用默认云服务）
4. 创建用户账号（可选）

### 日常使用
1. 打开其他会议App（钉钉/腾讯会议/飞书等）
2. 启动MeetingAssistant
3. 点击"开始新会议"
4. 授权屏幕录制权限（仅首次）
5. 输入会议标题
6. 点击"开始捕获音频"
7. **实时查看转录文字滚动显示**
8. **点击说话人头像标记**："这是张三"
9. 会议进行中可随时暂停/继续
10. 会议结束点击"停止并生成总结"
11. **查看AI生成的会议纪要**
12. 导出或分享会议记录

### 查看历史
1. 打开MeetingAssistant
2. 进入"历史记录"标签
3. 选择任意会议查看
4. 查看完整转录和总结
5. 重新编辑/导出/删除

## 技术风险与应对

### 风险1: GPT-4o音频转录成本高
**影响**: 高频使用导致API成本激增

**应对策略**:
- 音频压缩（Opus编码）
- 3-5秒分段处理（避免过短）
- 实现音频静音检测，跳过无效段
- 提供用户配额管理
- 未来支持本地Whisper模型

### 风险2: 实时性延迟问题
**影响**: 转录文字显示滞后影响体验

**应对策略**:
- WebSocket保持长连接
- 音频分段策略优化（平衡实时性和准确性）
- 后端异步处理
- 客户端乐观更新UI（显示"转录中..."）

### 风险3: Android权限被拒绝
**影响**: 无法捕获系统音频

**应对策略**:
- 清晰的权限说明和引导界面
- 提供权限设置跳转
- Fallback到麦克风录音模式
- 详细的帮助文档

### 风险4: 电量消耗过大
**影响**: 用户体验差，弃用率高

**应对策略**:
- 优化音频采集频率
- 音频数据压缩传输
- 后台低功耗模式
- 提供省电模式选项
- 电量警告提示

### 风险5: 网络不稳定导致数据丢失
**影响**: 部分转录内容缺失

**应对策略**:
- 本地音频缓冲队列
- 断线重连机制
- 本地持久化音频文件
- 离线转录队列（后续上传）

## 性能指标目标

### 实时性
- 音频捕获延迟: < 100ms
- WebSocket传输延迟: < 200ms
- AI转录响应时间: < 3s（3秒音频）
- 端到端延迟: < 5s

### 资源消耗
- 内存占用: < 200MB
- 电量消耗: < 10%/小时
- 网络流量: < 50MB/小时

### 准确性
- 转录准确率: > 90%（标准普通话）
- 说话人识别准确率: > 85%（手动标记后）

## 后续迭代方向

### V2.0 计划
- iOS版本支持
- 声纹自动识别
- 多模型切换UI
- 本地Whisper模型支持
- 离线模式

### V3.0 计划
- 多语言实时翻译
- 会议协作（多人同时标注）
- 智能提醒（待办事项通知）
- 会议模板和自定义Prompt
- 数据分析（会议时长统计、发言占比等）

## 开发规范

### 代码规范
- Java: Google Java Style Guide
- TypeScript: Airbnb JavaScript Style Guide
- 代码审查必须通过

### Git工作流
- 主分支: `main`
- 开发分支: `develop`
- 功能分支: `feature/xxx`
- 修复分支: `fix/xxx`

### 提交规范
```
feat: 添加音频捕获功能
fix: 修复WebSocket断线问题
docs: 更新API文档
refactor: 重构AI服务接口
test: 添加单元测试
```

### 文档要求
- 每个模块必须有README
- 公共API必须有注释
- 复杂算法必须有说明
- 数据库变更必须有migration脚本

## 测试策略

### 单元测试
- Java: JUnit 5 + Mockito
- TypeScript: Jest + React Native Testing Library
- 覆盖率目标: > 70%

### 集成测试
- Spring Boot Test
- WebSocket测试
- 数据库测试

### 端到端测试
- 模拟真实会议场景
- 多说话人测试
- 长时间运行测试（2小时+）
- 弱网环境测试

## 部署方案

### 后端部署
- 容器化: Docker
- 编排: Kubernetes（生产环境）
- 数据库: PostgreSQL (RDS)
- 缓存: Redis
- 文件存储: S3/OSS

### 移动端发布
- Android: Google Play / 国内应用商店
- iOS: App Store（V2.0）

### 监控与日志
- 应用监控: Prometheus + Grafana
- 日志收集: ELK Stack
- 错误追踪: Sentry

## 预算估算

### 开发成本
- 开发周期: 8周
- 开发人员: 2人（全栈）

### 运营成本（月）
- 服务器: $100（初期）
- OpenAI API: $200（1000小时会议）
- 云存储: $50
- 总计: ~$350/月

### 优化方向
- 使用本地模型降低API成本
- 用户付费订阅模式
- 企业版私有部署

## 成功指标

### MVP验证指标
- 成功完成10场真实会议录制
- 转录准确率 > 85%
- 用户满意度 > 4.0/5.0
- 无重大bug

### 产品指标（3个月后）
- 注册用户: 1000+
- 月活用户: 300+
- 平均会议时长: 45分钟
- 用户留存率: > 40%

---

**文档版本**: v1.0
**创建日期**: 2025-10-07
**最后更新**: 2025-10-07
**负责人**: 项目团队
