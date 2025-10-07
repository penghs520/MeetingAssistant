# Meeting Assistant - 快速开始指南

## 项目概述

AI会议助手，支持实时系统音频捕获、AI转录和智能总结。

## 架构

```
┌─────────────┐                ┌─────────────┐
│  Android App│ ─WebSocket───> │   Backend   │
│ (音频捕获)   │ <──转录结果──  │  (Spring AI)│
└─────────────┘                └──────┬──────┘
                                      │
                               ┌──────▼──────┐
                               │ PostgreSQL  │
                               └─────────────┘
```

## 前置要求

- ✅ Java 17+
- ✅ Node.js 20.19+
- ✅ PostgreSQL 16+
- ✅ Android Studio
- ✅ OpenAI API Key

## 30分钟快速部署

### Step 1: 启动数据库 (5分钟)

```bash
# 安装PostgreSQL (如未安装)
brew install postgresql@16  # macOS
# 或 sudo apt install postgresql-16  # Linux

# 启动PostgreSQL
brew services start postgresql@16

# 创建数据库
createdb meeting_assistant
```

### Step 2: 启动后端 (10分钟)

```bash
cd backend

# 设置OpenAI API Key
export OPENAI_API_KEY=sk-your-api-key-here

# 修改数据库配置（如需要）
# 编辑 src/main/resources/application.yml

# 运行后端
./mvnw spring-boot:run

# 验证：访问 http://localhost:8080
```

### Step 3: 构建移动端 (15分钟)

```bash
cd mobile-app

# 安装依赖
npm install

# 配置后端地址
# 编辑 src/services/MeetingService.ts
# 修改 WEBSOCKET_URL 为你的服务器IP

# Android模拟器
npx react-native run-android

# 真机：确保手机和电脑在同一WiFi，修改IP为电脑局域网IP
```

## 使用流程

1. **打开其他会议App**（钉钉、腾讯会议等）
2. **启动Meeting Assistant**
3. **点击"开始会议"**
4. **授予权限**（录音 + 屏幕录制）
5. **实时查看转录**
6. **点击"结束会议"**
7. **查看AI总结**

## 验证安装

### 测试后端

```bash
curl http://localhost:8080/api/meetings
# 应返回: []
```

### 测试WebSocket

```bash
# 使用websocat工具
brew install websocat
websocat ws://localhost:8080/ws/audio-stream
# 应显示连接成功
```

## 常见问题

### 后端无法启动

```bash
# 检查Java版本
java -version  # 应该是17+

# 检查PostgreSQL
psql -U postgres -c "\l"  # 列出所有数据库

# 查看详细错误
./mvnw spring-boot:run --debug
```

### 移动端无法连接

```bash
# 检查IP是否正确
# Android模拟器: 10.0.2.2
# 真机: 电脑的局域网IP (ipconfig/ifconfig查看)

# 检查防火墙
# macOS
sudo pfctl -d  # 临时关闭防火墙测试

# 查看React Native日志
npx react-native log-android
```

### 音频捕获失败

- ✅ 确保Android版本 >= 10
- ✅ 确保已授予所有权限
- ✅ 尝试重启应用和会议App
- ✅ 使用真机而非模拟器

## 下一步

- 📖 阅读 [docs/PLAN.md](docs/PLAN.md) 了解完整计划
- 🏗️ 阅读 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 了解架构
- 💾 阅读 [docs/DATABASE.md](docs/DATABASE.md) 了解数据库

## 技术支持

遇到问题？

1. 查看 [docs/STATUS.md](docs/STATUS.md) 确认当前进度
2. 查看日志文件排查错误
3. 提交Issue到GitHub

---

🚀 现在开始使用吧！
