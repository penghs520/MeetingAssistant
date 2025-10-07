# 项目设置总结 - Meeting Assistant Mobile

本文档记录了从零开始搭建Meeting Assistant Android应用的完整过程。

## 📅 时间线

**日期**: 2025-10-07
**完成状态**: ✅ 已成功在真机上运行

---

## 🎯 项目目标

构建一个Android应用，能够：
1. 捕获系统音频（不只是麦克风）
2. 将音频实时发送到后端
3. 接收并显示AI转录结果
4. 支持会议总结功能

---

## 🛠️ 技术栈

- **框架**: React Native 0.81.4
- **包管理器**: pnpm（使用 `--shamefully-hoist`）
- **原生语言**: Kotlin + Java
- **构建工具**: Gradle 8.14.3
- **Android API**: AudioPlaybackCapture (Android 10+)
- **通信协议**: WebSocket
- **开发服务器**: Metro Bundler

---

## 🔧 环境配置

### 1. Android SDK

**安装方式**: Homebrew

```bash
brew install android-commandlinetools
```

**安装位置**: `/opt/homebrew/share/android-commandlinetools`

**已安装组件**:
- platform-tools (adb, fastboot等)
- platforms;android-34
- build-tools;34.0.0, 35.0.0, 36.0.0
- NDK 27.1.12297006
- CMake 3.22.1

**配置文件**: `android/local.properties`
```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

### 2. Node.js依赖

**包管理器**: pnpm 10.14.0

**关键命令**:
```bash
pnpm install --shamefully-hoist
```

**为什么使用 `--shamefully-hoist`**:
- React Native的Gradle插件需要从`node_modules/@react-native/`直接访问某些包
- pnpm默认使用符号链接存储在`.pnpm`目录
- `--shamefully-hoist`将所有依赖提升到`node_modules`根目录，解决符号链接问题

**主要依赖**:
- react-native 0.81.4
- @react-native/codegen 0.81.4
- @react-native/gradle-plugin 0.81.4
- react-native-safe-area-context 5.6.1

---

## 🏗️ 项目结构

```
mobile-app/
├── android/                      # Android原生代码
│   ├── app/
│   │   ├── src/main/java/com/meetingassistantapp/
│   │   │   ├── AudioCaptureModule.java      # 音频捕获原生模块
│   │   │   ├── AudioCapturePackage.java     # 模块注册
│   │   │   └── MainApplication.kt           # 应用入口
│   │   ├── build.gradle                     # App构建配置
│   │   └── src/main/AndroidManifest.xml     # 权限和组件配置
│   └── local.properties                      # SDK路径配置
├── src/
│   ├── modules/
│   │   └── AudioCapture.ts                  # 原生模块桥接
│   ├── services/
│   │   ├── MeetingService.ts                # 会议业务逻辑
│   │   └── WebSocketService.ts              # WebSocket通信
│   └── screens/
│       └── MeetingScreen.tsx                # 主界面
├── App.tsx                                  # 应用根组件
├── package.json                             # 依赖配置
├── README.md                                # 主文档
├── QUICKSTART_DEVICE.md                     # 快速启动指南
├── RUN_ON_DEVICE.md                         # 真机调试详细指南
├── ANDROID_SETUP.md                         # Android环境配置
└── SETUP_SUMMARY.md                         # 本文档
```

---

## 🐛 遇到的问题及解决方案

### 问题1: React Native CLI已弃用

**错误**:
```
npx react-native init 已弃用
```

**解决方案**:
```bash
npx @react-native-community/cli init MeetingAssistantApp
```

### 问题2: pnpm符号链接问题

**错误**:
```
Could not find @react-native/gradle-plugin
Cannot find module '@react-native/codegen/lib/cli/combine/combine-js-to-schema-cli.js'
```

**原因**: pnpm将包存储在`.pnpm`目录，React Native的Gradle任务无法找到

**解决方案**:
```bash
pnpm install --shamefully-hoist
```

这会将所有依赖提升到`node_modules`根目录，自动创建必要的符号链接。

### 问题3: Android SDK location not found

**错误**:
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME
environment variable or by setting the sdk.dir path in your project's
local properties file
```

**解决方案**:

创建 `android/local.properties`:
```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

### 问题4: NDK不完整

**错误**:
```
NDK at path did not have a source.properties file
```

**解决方案**:
```bash
# 删除不完整的NDK，让Gradle自动下载
rm -rf /opt/homebrew/share/android-commandlinetools/ndk
./gradlew assembleDebug
```

Gradle会自动下载完整的NDK。

### 问题5: Unable to load script

**错误**: 应用启动时显示红屏，提示无法加载script

**原因**: 应用无法连接到Metro bundler

**解决方案**:
```bash
# 1. 启动Metro
npx react-native start

# 2. 设置端口转发
/opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081

# 3. 重新打开应用
```

---

## ✅ 构建过程

### 首次完整构建

```bash
cd mobile-app/android
./gradlew assembleDebug
```

**构建结果**:
- ✅ 耗时: 53秒
- ✅ 任务: 91个（73个执行，18个复用）
- ✅ APK大小: 97MB
- ✅ 输出位置: `android/app/build/outputs/apk/debug/app-debug.apk`

**包含的架构**:
- arm64-v8a (64位ARM)
- armeabi-v7a (32位ARM)
- x86 (32位模拟器)
- x86_64 (64位模拟器)

---

## 📱 在真机上运行

### 方式1: 一键启动（推荐）

```bash
npx react-native run-android
```

### 方式2: 分步操作

#### 步骤1: 设备准备

1. 开启开发者选项（连续点击"版本号"7次）
2. 开启"USB调试"
3. USB连接手机到电脑
4. 授权USB调试

#### 步骤2: 验证连接

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb devices
```

应显示:
```
List of devices attached
d39256f8	device
```

#### 步骤3: 启动Metro

```bash
cd mobile-app
npx react-native start
```

#### 步骤4: 设置端口转发

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081
```

#### 步骤5: 安装并启动

```bash
# 手动安装APK
/opt/homebrew/share/android-commandlinetools/platform-tools/adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# 然后在手机上打开应用
```

或者使用：
```bash
npx react-native run-android
```

---

## 🎉 成功标志

当看到以下输出时，说明应用运行成功：

**Metro输出**:
```
 BUNDLE  ./index.js
 LOG  Running "MeetingAssistantApp" with {"rootTag":1}
```

**手机上**:
- 应用正常显示界面
- 没有红屏错误
- 可以点击按钮交互

---

## 📝 关键原生代码

### AudioCaptureModule.java

核心功能: 捕获系统音频

**关键API**:
- `MediaProjection` - 屏幕录制权限（用于捕获系统音频）
- `AudioPlaybackCaptureConfiguration` - 配置要捕获的音频流
- `AudioRecord` - 录制音频数据

**使用的音频源**:
- `USAGE_MEDIA` - 媒体播放音频
- `USAGE_VOICE_COMMUNICATION` - 语音通话音频

**数据流**:
```
系统音频 → AudioPlaybackCapture → AudioRecord → Buffer → React Native Event
```

### MainApplication.kt

**作用**: 注册原生模块

```kotlin
override fun getPackages(): List<ReactPackage> =
    PackageList(this).packages.apply {
        add(AudioCapturePackage())
    }
```

---

## 🔄 开发工作流

### 日常开发

1. **启动Metro**（只需一次）:
   ```bash
   npx react-native start
   ```

2. **连接设备时**（每次插拔USB后）:
   ```bash
   /opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081
   ```

3. **修改代码**: 自动热重载（Fast Refresh）

4. **手动重载**: 摇晃手机 → Reload

### 重新构建原生代码

当修改了原生代码（Java/Kotlin）或配置文件时：

```bash
cd android
./gradlew clean
./gradlew assembleDebug
npx react-native run-android
```

---

## 🚀 下一步

### 已完成 ✅

- [x] React Native项目初始化
- [x] Android SDK配置
- [x] 原生音频捕获模块
- [x] WebSocket通信服务
- [x] 基本UI界面
- [x] 首次构建成功
- [x] 真机运行成功

### 待完成 ⏳

1. **后端集成**
   - 启动Spring Boot后端
   - 配置WebSocket连接地址
   - 测试端到端通信

2. **音频功能测试**
   - 授予录音和屏幕录制权限
   - 播放测试音频
   - 验证音频捕获
   - 验证WebSocket传输

3. **AI转录测试**
   - 验证后端收到音频数据
   - 测试OpenAI转录
   - 验证转录结果返回

4. **UI优化**
   - 实时转录文本滚动显示
   - 说话人标记UI
   - 会议总结展示

5. **错误处理**
   - 权限被拒绝的处理
   - 网络断线重连
   - 音频捕获失败提示

---

## 📚 相关文档

- [QUICKSTART_DEVICE.md](./QUICKSTART_DEVICE.md) - 快速启动指南
- [RUN_ON_DEVICE.md](./RUN_ON_DEVICE.md) - 真机调试完整教程
- [ANDROID_SETUP.md](./ANDROID_SETUP.md) - Android环境配置详解
- [README.md](./README.md) - 项目主文档

---

## 🙏 经验总结

### 关键决策

1. **删除iOS支持**: 专注Android MVP，简化开发
2. **使用pnpm**: 更快的安装速度和更好的依赖管理
3. **Homebrew安装SDK**: 简化macOS上的SDK管理
4. **混合架构**: React Native + 最小原生模块

### 最佳实践

1. **始终使用 `--shamefully-hoist`**: 避免pnpm符号链接问题
2. **保持Metro运行**: 开发时不要关闭Metro
3. **使用adb完整路径**: 避免PATH配置问题
4. **检查设备连接**: 出问题先跑 `adb devices`

### 避免的陷阱

1. ❌ 不要使用 `npm install`（切换到pnpm后）
2. ❌ 不要手动创建符号链接（使用 `--shamefully-hoist`）
3. ❌ 不要修改SDK位置后忘记更新 `local.properties`
4. ❌ 不要在手机上拒绝USB调试授权

---

**最后更新**: 2025-10-07 08:50
**测试设备**: PJD110 (Android 14)
**项目状态**: ✅ 可运行，等待后端集成
