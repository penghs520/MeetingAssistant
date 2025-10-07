# 调试快速参考

## 🚨 最常用的命令

### Metro Bundler

```bash
# 启动Metro
npx react-native start

# Metro端口被占用时
lsof -ti:8081 | xargs kill -9 && npx react-native start

# 清除缓存启动
npx react-native start --reset-cache
```

### 查看日志
```bash
# React Native日志（推荐）
npx react-native log-android

# 完整系统日志
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat

# 过滤特定标签
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat -s "ReactNativeJS"
```

### 重启应用
```bash
# 重新加载JS（快）
摇晃手机 → Reload

# 或在Metro中按 r 键

# 完全重启应用（慢）
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell am force-stop com.meetingassistantapp
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell am start -n com.meetingassistantapp/.MainActivity
```

### 清除缓存
```bash
# 清除应用数据
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell pm clear com.meetingassistantapp

# 重启Metro并清除缓存
npx react-native start --reset-cache
```

## 🔧 常见问题快速修复

### "Unable to load script"
```bash
# 1. 检查Metro是否运行
ps aux | grep metro

# 2. 重新设置端口转发
/opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081

# 3. 重新加载
摇晃手机 → Reload
```

### WebSocket连接失败
```typescript
// 检查URL配置 (src/services/MeetingService.ts)
// 真机使用电脑IP，不是localhost
const WEBSOCKET_URL = 'ws://192.168.1.100:8080/ws/audio-stream';
```

### 权限问题
```bash
# 手动授予所有权限
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell pm grant com.meetingassistantapp android.permission.RECORD_AUDIO
```

## 📱 在代码中添加日志

```typescript
// JavaScript
console.log('调试信息:', data);
console.warn('警告');  // 黄色
console.error('错误'); // 红屏
```

```java
// Java (AudioCaptureModule.java)
import android.util.Log;
Log.d("AudioCapture", "调试信息");
```

## 🌐 Chrome调试

1. 摇晃手机 → Debug
2. Chrome打开: `chrome://inspect`
3. 点击 inspect

## 📚 详细文档

- [DEBUGGING_GUIDE.md](./DEBUGGING_GUIDE.md) - 完整调试指南
- [METRO_DEBUG_GUIDE.md](./METRO_DEBUG_GUIDE.md) - Metro专项调试指南
