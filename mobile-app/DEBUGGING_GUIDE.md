# React Native 调试指南

## 🔍 调试工具概览

React Native提供了多种调试方式，适用于不同的调试场景。

---

## 1. 查看日志输出

### 方式1: React Native CLI（推荐）

最简单的方式，自动过滤React Native相关日志：

```bash
npx react-native log-android
```

**优点**:
- 自动过滤，只显示应用日志
- 彩色输出，易于阅读
- 实时显示 `console.log()` 输出

### 方式2: ADB Logcat（完整日志）

查看所有系统日志，包括原生代码：

```bash
# 查看所有日志
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat

# 过滤React Native日志
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat | grep ReactNativeJS

# 过滤特定标签
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat -s "AudioCapture"

# 清空日志并重新开始
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat -c && /opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat
```

**常用标签**:
- `ReactNativeJS`: JavaScript日志
- `ReactNative`: 原生桥接日志
- `AudioCapture`: 我们的音频模块日志（如果你在Java中使用了这个标签）

---

## 2. 在代码中添加日志

### JavaScript/TypeScript

```typescript
// 基础日志
console.log('普通信息');
console.warn('警告信息');  // 黄色背景
console.error('错误信息'); // 红屏

// 查看对象
const user = { name: 'John', age: 30 };
console.log('User:', user);

// 更详细的对象查看
console.log('User详情:', JSON.stringify(user, null, 2));

// 在MeetingService.ts中添加调试日志
export class MeetingService {
  startMeeting() {
    console.log('[MeetingService] 开始会议');

    AudioCapture.requestPermission((granted) => {
      console.log('[MeetingService] 权限结果:', granted);

      if (granted) {
        AudioCapture.startCapture();
        console.log('[MeetingService] 音频捕获已启动');
      }
    });
  }
}
```

### Java/Kotlin原生代码

在 `AudioCaptureModule.java` 中添加日志：

```java
import android.util.Log;

public class AudioCaptureModule extends ReactContextBaseJavaModule {
    private static final String TAG = "AudioCapture";

    @ReactMethod
    public void startCapture() {
        Log.d(TAG, "开始捕获音频");

        try {
            // 你的代码
            Log.i(TAG, "AudioRecord已创建");
        } catch (Exception e) {
            Log.e(TAG, "捕获音频失败", e);
        }
    }
}
```

然后在终端查看：
```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat -s "AudioCapture"
```

---

## 3. 使用Chrome开发者工具调试

### 启用调试模式

1. **在应用中打开开发者菜单**:
   - 摇晃手机
   - 或运行: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell input keyevent 82`

2. **选择 "Debug"**

3. **在电脑浏览器中打开**:
   ```
   chrome://inspect
   ```

4. **点击 "inspect"**

### Chrome调试功能

- **Console**: 查看所有 `console.log` 输出
- **Sources**: 设置断点，单步调试
- **Network**: 查看网络请求（包括WebSocket）
- **Application**: 查看AsyncStorage等存储

### 在代码中设置断点

```typescript
export class MeetingService {
  startMeeting() {
    debugger; // Chrome会在这里暂停

    console.log('继续执行...');
  }
}
```

---

## 4. React DevTools

### 安装

```bash
npm install -g react-devtools
```

### 启动

```bash
react-devtools
```

然后在应用中打开开发者菜单 → 选择 "Show React DevTools"

### 功能

- **⚛️ Components**: 查看组件树，检查props和state
- **⚙️ Profiler**: 性能分析，找出渲染慢的组件

---

## 5. 网络调试

### 查看WebSocket连接

#### 方式1: Chrome DevTools

1. 开启Debug模式
2. 打开Chrome DevTools
3. 切换到 **Network** 标签
4. 筛选 **WS** (WebSocket)
5. 点击连接查看消息

#### 方式2: 在代码中添加日志

在 `WebSocketService.ts` 中：

```typescript
export class WebSocketService {
  connect(url: string) {
    console.log('[WebSocket] 连接中...', url);

    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      console.log('[WebSocket] ✅ 已连接');
    };

    this.ws.onmessage = (event) => {
      console.log('[WebSocket] 收到消息:', event.data);
    };

    this.ws.onerror = (error) => {
      console.error('[WebSocket] ❌ 错误:', error);
    };

    this.ws.onclose = () => {
      console.log('[WebSocket] 连接已关闭');
    };
  }

  send(data: any) {
    console.log('[WebSocket] 发送消息:', data);
    this.ws.send(data);
  }
}
```

---

## 6. 原生模块调试

### 检查原生模块是否注册

```typescript
import { NativeModules } from 'react-native';

console.log('AudioCapture模块:', NativeModules.AudioCapture);

// 应该输出模块的所有方法
// {
//   startCapture: [Function],
//   stopCapture: [Function],
//   requestPermission: [Function]
// }
```

### 测试原生方法调用

```typescript
import AudioCapture from './src/modules/AudioCapture';

// 测试权限请求
AudioCapture.requestPermission((granted) => {
  console.log('权限结果:', granted);

  if (granted) {
    console.log('✅ 已获得权限');
  } else {
    console.error('❌ 权限被拒绝');
  }
});
```

### 监听原生事件

```typescript
import AudioCapture from './src/modules/AudioCapture';

// 添加监听器
const subscription = AudioCapture.onAudioData((event) => {
  console.log('[原生事件] 收到音频数据:', {
    size: event.audioData.length,
    timestamp: new Date().toISOString()
  });
});

// 组件卸载时移除监听器
useEffect(() => {
  return () => {
    subscription?.remove();
  };
}, []);
```

---

## 7. 常见调试场景

### 场景1: 应用崩溃或红屏

**步骤**:

1. **查看错误信息**:
   - 红屏上有完整的错误堆栈
   - 记录错误信息

2. **查看完整日志**:
   ```bash
   npx react-native log-android
   ```

3. **重新加载应用**:
   - 摇晃手机 → Reload
   - 或按 `RR` 键

4. **清除缓存重试**:
   ```bash
   # 停止Metro
   # 清除缓存
   npx react-native start --reset-cache
   ```

### 场景2: WebSocket连接失败

**检查清单**:

```typescript
// 1. 检查URL配置
const WEBSOCKET_URL = 'ws://10.0.2.2:8080/ws/audio-stream'; // 模拟器
// const WEBSOCKET_URL = 'ws://192.168.1.100:8080/ws/audio-stream'; // 真机

console.log('WebSocket URL:', WEBSOCKET_URL);

// 2. 添加详细日志
this.ws = new WebSocket(WEBSOCKET_URL);

this.ws.onerror = (error) => {
  console.error('WebSocket错误详情:', {
    message: error.message,
    type: error.type,
    url: WEBSOCKET_URL
  });
};

// 3. 检查后端是否运行
// 在电脑上访问: http://localhost:8080
```

**网络测试**:

```bash
# 测试后端连通性（真机情况）
# 在电脑上查看IP
ifconfig | grep inet

# 在手机浏览器访问
# http://你的电脑IP:8080
```

### 场景3: 音频捕获不工作

**调试步骤**:

```typescript
// 1. 检查权限
AudioCapture.requestPermission((granted) => {
  console.log('录音权限:', granted);

  if (!granted) {
    console.error('❌ 用户拒绝了录音权限');
    Alert.alert('需要权限', '请在设置中开启录音权限');
    return;
  }

  // 2. 启动捕获
  console.log('尝试启动音频捕获...');
  AudioCapture.startCapture();
});

// 3. 监听音频数据
const subscription = AudioCapture.onAudioData((event) => {
  console.log('✅ 收到音频数据:', event.audioData.length, 'bytes');
});

// 4. 在原生代码中添加日志
// 编辑 AudioCaptureModule.java:

@ReactMethod
public void startCapture() {
    Log.d("AudioCapture", "startCapture被调用");

    if (audioRecord == null) {
        Log.e("AudioCapture", "AudioRecord未初始化");
        return;
    }

    Log.d("AudioCapture", "开始录制...");
    audioRecord.startRecording();

    // ... 读取数据的循环
    Log.d("AudioCapture", "音频数据大小: " + read + " bytes");
}
```

然后查看原生日志：
```bash
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat -s "AudioCapture"
```

### 场景4: 性能问题

**使用React DevTools Profiler**:

```bash
react-devtools
```

在应用中开启Profiler，进行以下操作：

1. 点击 **⚛️ Profiler** 标签
2. 点击 **Record** 按钮
3. 在应用中进行操作
4. 点击 **Stop** 停止录制
5. 查看哪些组件渲染慢

**查看帧率**:

在应用开发者菜单中 → 选择 "Show Perf Monitor"

---

## 8. 调试最佳实践

### 添加调试开关

```typescript
// config.ts
export const DEBUG = __DEV__; // 开发环境自动启用

export const DEBUG_FLAGS = {
  WEBSOCKET: DEBUG && true,
  AUDIO: DEBUG && true,
  UI: DEBUG && false,
};

// 在代码中使用
if (DEBUG_FLAGS.WEBSOCKET) {
  console.log('[WebSocket]', message);
}
```

### 使用调试工具函数

```typescript
// utils/debug.ts
export const debugLog = (tag: string, ...args: any[]) => {
  if (__DEV__) {
    console.log(`[${tag}]`, ...args);
  }
};

export const debugError = (tag: string, error: any) => {
  if (__DEV__) {
    console.error(`[${tag}] 错误:`, error);
  }
};

// 使用
import { debugLog, debugError } from './utils/debug';

debugLog('MeetingService', '会议已开始');
debugError('WebSocket', new Error('连接失败'));
```

### 状态日志

```typescript
// 在useEffect中监控状态变化
useEffect(() => {
  console.log('会议状态变化:', {
    isRecording,
    transcripts: transcripts.length,
    wsConnected: wsService.isConnected()
  });
}, [isRecording, transcripts]);
```

---

## 9. 快捷命令参考

```bash
# 查看应用日志
npx react-native log-android

# 查看原生日志
/opt/homebrew/share/android-commandlinetools/platform-tools/adb logcat

# 重启应用
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell am force-stop com.meetingassistantapp
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell am start -n com.meetingassistantapp/.MainActivity

# 清除应用数据
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell pm clear com.meetingassistantapp

# 打开开发者菜单
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell input keyevent 82

# 重新加载JS
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell input text "RR"

# 安装APK
/opt/homebrew/share/android-commandlinetools/platform-tools/adb install -r android/app/build/outputs/apk/debug/app-debug.apk

# 截图调试UI问题
/opt/homebrew/share/android-commandlinetools/platform-tools/adb shell screencap -p /sdcard/screenshot.png
/opt/homebrew/share/android-commandlinetools/platform-tools/adb pull /sdcard/screenshot.png
```

---

## 10. 调试清单

遇到问题时，按此清单逐项检查：

### ✅ 基础检查

- [ ] Metro bundler正在运行
- [ ] 手机已连接并被adb识别
- [ ] 端口转发已设置 (8081)
- [ ] 应用版本是最新的

### ✅ 日志检查

- [ ] 查看了React Native日志
- [ ] 查看了原生logcat日志
- [ ] 在关键位置添加了console.log

### ✅ 网络检查（如果涉及后端）

- [ ] 后端服务正在运行
- [ ] WebSocket URL配置正确
- [ ] 网络连通性正常
- [ ] 防火墙未阻止连接

### ✅ 权限检查

- [ ] 应用已请求必要权限
- [ ] 用户已授予权限
- [ ] AndroidManifest.xml中声明了权限

### ✅ 代码检查

- [ ] 没有JavaScript语法错误
- [ ] 导入路径正确
- [ ] 原生模块已正确注册

---

## 🆘 仍然无法解决？

### 获取更多帮助

1. **查看完整错误堆栈**:
   ```bash
   npx react-native log-android > debug.log
   ```

2. **重新构建**:
   ```bash
   cd android
   ./gradlew clean
   ./gradlew assembleDebug
   ```

3. **完全重置**:
   ```bash
   # 清除所有缓存
   rm -rf node_modules
   pnpm install --shamefully-hoist

   # 清除Gradle缓存
   cd android
   ./gradlew clean
   cd ..

   # 重新启动Metro
   npx react-native start --reset-cache
   ```

4. **搜索错误信息**:
   - 复制完整错误信息
   - Google搜索 "react native [错误信息]"
   - 查看Stack Overflow

---

**最后更新**: 2025-10-07
**适用版本**: React Native 0.81.4
