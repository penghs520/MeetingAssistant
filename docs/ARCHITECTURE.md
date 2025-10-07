# Meeting Assistant 架构设计文档

## 系统架构概览

Meeting Assistant采用前后端分离的架构设计，移动端使用React Native，后端使用Spring Boot，通过WebSocket进行实时音频传输和转录推送。

```
┌─────────────────────────────────────────────────────────────┐
│                     移动端 (React Native)                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 音频捕获模块  │  │   UI组件      │  │  WebSocket   │      │
│  │ (Android原生) │  │              │  │   客户端      │      │
│  └──────┬───────┘  └──────────────┘  └──────┬───────┘      │
│         │                                    │               │
└─────────┼────────────────────────────────────┼───────────────┘
          │                                    │
          │ 音频流                              │ 转录结果
          ▼                                    ▼
┌─────────────────────────────────────────────────────────────┐
│                  后端服务 (Spring Boot)                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  WebSocket   │  │  AI服务层     │  │  业务服务     │      │
│  │   Handler    │──┤ (可插拔)      │──┤   层          │      │
│  └──────────────┘  └──────┬───────┘  └──────┬───────┘      │
│                           │                  │               │
│                    ┌──────▼───────┐   ┌──────▼───────┐     │
│                    │ OpenAI       │   │ JPA          │     │
│                    │ Provider     │   │ Repository   │     │
│                    └──────────────┘   └──────┬───────┘     │
│                                               │              │
└───────────────────────────────────────────────┼──────────────┘
                                                │
                                         ┌──────▼───────┐
                                         │ PostgreSQL   │
                                         │  Database    │
                                         └──────────────┘
```

## 核心模块设计

### 1. 移动端架构

#### 1.1 音频捕获模块 (Android Native)

**职责**:
- 使用AudioPlaybackCapture捕获系统音频
- 音频格式转换和编码
- 音频数据分段缓冲

**技术实现**:
```java
// Android AudioPlaybackCapture
MediaProjection → AudioRecord → 音频Buffer → React Native Bridge
```

**关键类**:
- `AudioCaptureModule.java` - React Native原生模块
- `AudioRecordService` - 前台服务保持录音

#### 1.2 WebSocket客户端

**职责**:
- 建立与后端的WebSocket连接
- 发送音频数据流
- 接收转录结果推送
- 断线重连机制

**数据流**:
```
音频捕获 → 分段(3秒) → Base64编码 → WebSocket发送
WebSocket接收 → JSON解析 → UI更新
```

#### 1.3 UI组件层

**主要组件**:
- `MeetingScreen` - 会议主界面
- `TranscriptView` - 实时转录显示
- `SpeakerTag` - 说话人标记组件
- `SummaryView` - AI总结展示

### 2. 后端架构

#### 2.1 三层架构

```
Controller层 (REST API)
    ↓
Service层 (业务逻辑)
    ↓
Repository层 (数据访问)
    ↓
Database (PostgreSQL)
```

#### 2.2 WebSocket处理流程

```
客户端连接
    ↓
AudioStreamHandler.afterConnectionEstablished()
    ↓ 创建/获取Meeting
    ↓
接收音频数据 (handleBinaryMessage)
    ↓
AIService.transcribe() - 异步转录
    ↓
TranscriptionService.save() - 保存转录
    ↓
WebSocket推送结果给客户端
```

**关键类**:
- `AudioStreamHandler` - WebSocket消息处理
- `WebSocketConfig` - WebSocket配置

#### 2.3 AI服务层（可插拔设计）

**接口定义**:
```java
public interface AIService {
    String transcribe(byte[] audioData);
    String summarize(String transcript, List<Speaker> speakers);
    String getModelName();
}
```

**实现类**:
- `OpenAIProvider` - GPT-4o实现（当前）
- `GeminiProvider` - Google Gemini（未来）
- `LocalWhisperProvider` - 本地Whisper模型（未来）

**设计优势**:
- 通过接口抽象，易于切换AI模型
- 支持运行时动态配置
- 便于A/B测试不同模型

#### 2.4 业务服务层

**MeetingService**:
- 会议生命周期管理（创建、完成、删除）
- 触发AI总结生成

**TranscriptionService**:
- 保存转录文本
- 管理转录序列号
- 关联说话人

**SpeakerService**:
- 说话人CRUD操作
- 说话人标记管理

#### 2.5 数据访问层

**Repository接口**:
- `MeetingRepository` - 会议数据访问
- `TranscriptRepository` - 转录数据访问
- `SpeakerRepository` - 说话人数据访问

**使用Spring Data JPA**:
- 自动生成CRUD方法
- 支持自定义查询
- 事务管理

### 3. 数据模型

#### 3.1 实体关系

```
Meeting (1) ───┬──< (N) Transcript
               └──< (N) Speaker
                         │
                         └──< (N) Transcript
```

#### 3.2 核心实体

**Meeting** - 会议实体
- 基本信息（标题、时间）
- 状态管理（RECORDING/COMPLETED/FAILED）
- AI总结字段

**Transcript** - 转录实体
- 转录文本内容
- 时间戳和序列号
- 关联说话人（可选）

**Speaker** - 说话人实体
- 姓名和显示颜色
- 所属会议

## 技术决策

### 1. 为什么选择WebSocket而非HTTP轮询？

**原因**:
- 实时性要求高，WebSocket延迟更低
- 双向通信，服务器可主动推送
- 减少网络开销和服务器压力

### 2. 为什么音频分段处理而非完整上传？

**原因**:
- 实时转录需求，不能等会议结束
- 降低单次请求大小，提高成功率
- 便于断点续传和错误恢复

### 3. 为什么使用可插拔AI服务接口？

**原因**:
- 支持多模型对比和切换
- 预留本地模型运行能力
- 降低对单一服务商依赖
- 便于成本优化

### 4. 为什么选择PostgreSQL而非MySQL？

**原因**:
- 更好的JSON支持（未来可能存储结构化总结）
- 全文搜索功能
- 更强的并发性能
- 开源且功能强大

## 数据流详解

### 完整音频转录流程

```
1. Android捕获系统音频
   ↓
2. 音频Buffer缓冲（3秒）
   ↓
3. 转换为WAV/PCM格式
   ↓
4. 通过WebSocket发送到后端
   ↓
5. 后端AudioStreamHandler接收
   ↓
6. 异步调用AIService.transcribe()
   ↓
7. OpenAI GPT-4o处理音频
   ↓
8. 返回转录文本
   ↓
9. TranscriptionService保存到数据库
   ↓
10. 通过WebSocket推送给客户端
   ↓
11. 移动端UI实时显示
```

### 会议总结生成流程

```
1. 用户点击"完成会议"
   ↓
2. MeetingService.completeMeeting()
   ↓
3. 从数据库查询所有转录记录
   ↓
4. 拼接成完整文本（含说话人标记）
   ↓
5. 调用AIService.summarize()
   ↓
6. GPT-4o生成结构化总结
   ↓
7. 保存到Meeting.summary字段
   ↓
8. 返回给客户端展示
```

## 性能优化策略

### 1. 异步处理

- 音频转录使用`CompletableFuture.runAsync()`
- 避免阻塞WebSocket连接
- 提高并发处理能力

### 2. 数据库优化

- 添加合适的索引（见DATABASE.md）
- 使用连接池
- 查询结果分页

### 3. 音频传输优化

- 音频压缩（Opus编码）
- 合理的分段大小（3-5秒）
- 客户端缓冲队列

### 4. 缓存策略（未来）

- Redis缓存会议信息
- 转录结果本地缓存
- AI模型响应缓存

## 扩展性设计

### 1. 水平扩展

- 无状态后端服务，支持多实例部署
- WebSocket使用Session管理，可配合负载均衡
- 数据库读写分离

### 2. 模块化扩展

**添加新AI模型**:
```java
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiProvider implements AIService {
    // 实现接口方法
}
```

**添加新功能模块**:
- 实时翻译模块
- 情感分析模块
- 关键词提取模块

### 3. 存储扩展

- 音频文件云存储（S3/OSS）
- 分布式文件系统
- 对象存储服务

## 安全性考虑

### 1. 认证与授权（未来）

- JWT Token认证
- 用户会议权限控制
- API访问限流

### 2. 数据安全

- 音频数据加密传输
- 敏感信息脱敏
- 定期数据备份

### 3. API密钥保护

- 环境变量存储
- 密钥轮换机制
- 访问日志审计

## 监控与运维

### 1. 日志管理

- 使用SLF4J + Logback
- 分级日志（ERROR/WARN/INFO/DEBUG）
- 日志收集（ELK Stack）

### 2. 指标监控

- Spring Actuator健康检查
- WebSocket连接数监控
- AI API调用统计
- 数据库连接池监控

### 3. 错误处理

- 全局异常处理器
- WebSocket错误重连
- AI服务降级策略

## 部署架构（生产环境）

```
                    ┌──────────────┐
                    │ Load Balancer│
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
         ┌────▼────┐  ┌────▼────┐  ┌───▼─────┐
         │ Backend │  │ Backend │  │ Backend │
         │ Instance│  │ Instance│  │ Instance│
         └────┬────┘  └────┬────┘  └───┬─────┘
              │            │            │
              └────────────┼────────────┘
                           │
                    ┌──────▼───────┐
                    │ PostgreSQL   │
                    │   Primary    │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ PostgreSQL   │
                    │   Replica    │
                    └──────────────┘
```

## 技术债务与改进方向

### 短期
- [ ] 添加单元测试和集成测试
- [ ] 完善错误处理和日志
- [ ] 添加API文档（Swagger）
- [ ] 性能基准测试

### 中期
- [ ] 用户认证与多租户支持
- [ ] 音频质量检测和降噪
- [ ] 实时监控仪表板
- [ ] CI/CD自动化部署

### 长期
- [ ] 微服务拆分（音频服务、AI服务、业务服务）
- [ ] 服务网格（Istio）
- [ ] 事件驱动架构（Kafka）
- [ ] 全球CDN加速

---

**文档版本**: v1.0
**最后更新**: 2025-10-07
**作者**: 架构团队
