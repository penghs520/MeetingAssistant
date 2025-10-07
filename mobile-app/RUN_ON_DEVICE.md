# 在真实设备上运行应用 - 完整指南

## 前置条件

✅ Android SDK已安装（位置：`/opt/homebrew/share/android-commandlinetools`）
✅ 依赖已安装（使用 `pnpm install --shamefully-hoist`）
✅ APK已构建成功

## 方法1：快速启动（推荐）

如果你已经熟悉流程，使用这个一键命令：

```bash
# 在项目根目录 mobile-app 下执行
npx react-native run-android
```

这个命令会：
1. 自动检测连接的设备
2. 构建并安装APK
3. 启动Metro bundler
4. 启动应用

**注意**：首次运行前需要确保手机已开启USB调试并连接到电脑。

## 方法2：分步操作（完整流程）

### 步骤1：在手机上开启USB调试

#### 1.1 开启开发者选项

1. 打开手机 **"设置"**
2. 进入 **"关于手机"**
3. 找到 **"版本号"** 或 **"MIUI版本"**（不同品牌位置略有不同）
4. **连续快速点击7次**
5. 会提示 **"您已处于开发者模式"**

#### 1.2 开启USB调试

1. 返回 **"设置"** → **"系统"** 或 **"更多设置"**
2. 找到 **"开发者选项"**
3. 开启 **"USB调试"**
4. 建议同时开启 **"USB安装"**（允许通过USB安装应用）

#### 1.3 连接手机

1. 用USB线连接手机到电脑
2. 手机会弹出 **"允许USB调试"** 提示
3. 勾选 **"始终允许使用这台计算机进行调试"**
4. 点击 **"允许"**

### 步骤2：验证设备连接

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb devices
```

**预期输出**：
```
List of devices attached
d39256f8	device
```

如果显示 `List of devices attached` 但下面是空的：
- 检查手机是否弹出USB调试授权提示
- 尝试拔掉USB线重新插一次
- 在手机通知栏点击USB连接，选择 **"传输文件(MTP)"** 模式

### 步骤3：启动Metro Bundler

在一个终端窗口中：

```bash
cd /Users/penghongsi/workspaces/sources/my/MeetingAssistant/mobile-app
npx react-native start
```

**预期输出**：
```
Welcome to Metro v0.83.3
Fast - Scalable - Integrated

INFO  Dev server ready. Press Ctrl+C to exit.
```

**保持这个终端窗口运行**，不要关闭。

### 步骤4：设置端口转发

在另一个终端窗口中：

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081

#这一步要确保USB连接正常，如果不正常的话请重新连接
```

**预期输出**：
```
8081
```

这个命令将手机的8081端口转发到电脑的8081端口（Metro bundler的端口）。

### 步骤5：安装并启动应用

#### 方式A：使用已构建的APK手动安装

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb install -r /Users/penghongsi/workspaces/sources/my/MeetingAssistant/mobile-app/android/app/build/outputs/apk/debug/app-debug.apk
```

然后在手机上手动打开 **Meeting Assistant** 应用。

#### 方式B：使用react-native命令（推荐，但我这不太耗时，先用方式A吧）

```bash
cd /Users/penghongsi/workspaces/sources/my/MeetingAssistant/mobile-app
npx react-native run-android
```

这会自动构建、安装并启动应用。

### 步骤6：验证成功

在Metro bundler的终端窗口中，应该能看到：

```
 BUNDLE  ./index.js
 LOG  Running "MeetingAssistantApp" with {"rootTag":1}
```

手机上应用应该正常显示界面。

## 常见问题

### Q1: `adb: command not found`

**原因**：adb命令不在系统PATH中。

**解决方案**：使用完整路径调用adb：
```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb
```

或者添加到PATH（在 `~/.zshrc` 中）：
```bash
export PATH=$PATH:/opt/homebrew/share/android-commandlinetools/platform-tools
```

然后执行 `source ~/.zshrc`。

### Q2: `adb: no devices/emulators found`

**可能原因**：
1. 手机未连接或USB调试未开启
2. 未授权USB调试
3. USB连接模式不正确

**解决步骤**：
1. 检查USB线是否连接
2. 在手机通知栏点击USB连接，选择"传输文件"模式
3. 重新插拔USB线
4. 在手机上重新授权USB调试

### Q3: 应用显示红屏错误 "Unable to load script"

**原因**：应用无法连接到Metro bundler。

**解决方案**：
1. 确保Metro bundler正在运行（步骤3）
2. 确保端口转发已设置（步骤4）
3. 重新执行端口转发命令：
   ```bash
   /opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081
   ```
4. 重启应用

### Q4: Metro bundler启动失败 "Port 8081 already in use"

**原因**：8081端口被占用。

**解决方案1**（推荐）：
```bash
npx react-native start --port 8082
```

然后更新端口转发：
```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8082
```

**解决方案2**：杀掉占用端口的进程：
```bash
lsof -ti:8081 | xargs kill -9
```

### Q5: 修改代码后应用没有更新

**解决方案**：
1. 在手机上摇晃设备或按菜单键
2. 选择 **"Reload"**
3. 或者在Metro终端中按 `r` 键重新加载

## 开发工作流

### 日常开发流程

1. **启动Metro**（只需启动一次，保持运行）：
   ```bash
   cd mobile-app
   npx react-native start
   ```

2. **连接设备并设置端口转发**（每次连接设备时）：
   ```bash
   /opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081
   ```

3. **在手机上打开应用**

4. **修改代码后自动热重载**
   - Metro支持Fast Refresh，修改代码后自动更新
   - 如果没有自动更新，在手机上双击R键或摇晃设备选择Reload

### 完全重新构建

如果需要重新构建原生代码（修改了Java/Kotlin代码或配置）：

```bash
cd mobile-app/android
./gradlew clean
./gradlew assembleDebug
npx react-native run-android
```

## Metro Bundler 快捷键

在Metro终端中，可以使用以下快捷键：

- `r` - 重新加载应用
- `d` - 打开开发者菜单
- `i` - 在iOS模拟器中运行（如果有）
- `a` - 在Android设备/模拟器中运行

## 设备开发者菜单

在应用运行时，可以通过以下方式打开开发者菜单：

- **真机**：摇晃手机
- **或**：运行命令 `adb shell input keyevent 82`

开发者菜单选项：
- **Reload** - 重新加载JavaScript
- **Debug** - 启用Chrome调试
- **Enable Fast Refresh** - 启用快速刷新
- **Show Inspector** - 显示元素检查器
- **Show Perf Monitor** - 显示性能监控

## 调试技巧

### 查看应用日志

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat | grep ReactNativeJS
```

或者使用React Native CLI：
```bash
npx react-native log-android
```

### 清除应用数据

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell pm clear com.meetingassistantapp
```

### 卸载应用

```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb uninstall com.meetingassistantapp
```

## 下一步

应用成功运行后，可以开始测试功能：

1. ✅ 点击"开始会议"按钮
2. ✅ 授予录音权限和屏幕录制权限
3. ✅ 确保后端服务已启动（见 `/backend/README.md`）
4. ✅ 测试系统音频捕获功能

---

**最后更新**: 2025-10-07
**测试设备**: PJD110 (Android 14)
