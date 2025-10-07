# Android开发环境配置

## 当前状态

✅ **已完成**:
- 删除iOS文件夹
- 安装npm依赖（使用pnpm --shamefully-hoist）
- 修复pnpm符号链接（codegen, gradle-plugin）
- 创建原生Android音频捕获模块
- 安装Android SDK（Homebrew方式）
- 配置local.properties指向SDK路径
- 首次构建成功 ✨

✅ **Android SDK已配置**: `/opt/homebrew/share/android-commandlinetools`

## ✅ 构建成功

首次构建已成功完成！APK文件位置：
```
mobile-app/android/app/build/outputs/apk/debug/app-debug.apk (97MB)
```

构建时间：53秒
任务完成：91个任务（73个执行，18个up-to-date）

## 已解决的问题

### ✅ SDK location not found
- **解决方案**：在`local.properties`中配置`sdk.dir=/opt/homebrew/share/android-commandlinetools`

### ✅ pnpm符号链接问题
- **解决方案**：使用`pnpm install --shamefully-hoist`将所有依赖提升到node_modules根目录

### ✅ @react-native/codegen模块找不到
- **解决方案**：pnpm --shamefully-hoist自动创建所有必需的符号链接

## 安装说明（已完成的步骤）

### 方法1: 安装Android Studio（推荐）

1. **下载Android Studio**
   ```bash
   # 访问官网下载
   open https://developer.android.com/studio
   ```

2. **安装Android Studio**
   - 运行安装程序
   - 选择"Standard"安装
   - 等待SDK和工具下载完成

3. **设置环境变量**

   编辑 `~/.zshrc` 或 `~/.bash_profile`:
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/emulator
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

   应用配置:
   ```bash
   source ~/.zshrc
   ```

4. **验证安装**
   ```bash
   echo $ANDROID_HOME
   # 应输出: /Users/你的用户名/Library/Android/sdk

   adb version
   # 应显示adb版本信息
   ```

### 方法2: 仅安装SDK命令行工具

如果不想安装完整的Android Studio:

1. **下载Command Line Tools**
   ```bash
   cd ~/Downloads
   curl -O https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip
   ```

2. **解压并配置**
   ```bash
   mkdir -p ~/Library/Android/sdk/cmdline-tools
   unzip commandlinetools-mac-*_latest.zip -d ~/Library/Android/sdk/cmdline-tools
   mv ~/Library/Android/sdk/cmdline-tools/cmdline-tools ~/Library/Android/sdk/cmdline-tools/latest
   ```

3. **设置环境变量**（同方法1）

4. **安装必需的SDK包**
   ```bash
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```

## 配置完成后

### 1. 重新构建项目

```bash
cd mobile-app/android
./gradlew clean
./gradlew assembleDebug
```

### 2. 运行应用

**使用模拟器**:
```bash
cd mobile-app
npx react-native run-android
```

**使用真机**:
1. 手机开启USB调试
2. 连接手机到电脑
3. 运行: `npx react-native run-android`

## 验证编译成功

编译成功后会生成APK：
```
mobile-app/android/app/build/outputs/apk/debug/app-debug.apk
```

可以手动安装：
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

## 常见问题

### Q: Gradle构建很慢？

A: 首次构建需要下载依赖，耐心等待。后续构建会快很多。

### Q: 找不到adb命令？

A: 确保已添加platform-tools到PATH：
```bash
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

### Q: 模拟器启动失败？

A:
1. 打开Android Studio
2. Tools → Device Manager
3. 创建一个虚拟设备（推荐Pixel 6, API 34）

## 下一步

配置好Android SDK后，继续以下步骤：

1. ✅ 启动后端服务（见backend/README.md）
2. ✅ 配置WebSocket地址（src/services/MeetingService.ts）
3. ✅ 构建并运行App
4. ✅ 测试音频捕获功能

---

**当前构建状态**: ✅ 编译成功，APK已生成
**最后更新**: 2025-10-07 08:18
**APK大小**: 97MB
