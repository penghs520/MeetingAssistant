# Meeting Assistant - Mobile App

AI会议助手的移动端应用（React Native for Android），支持系统音频捕获和实时转录。

## 📚 快速链接

- **🚀 [快速启动](./QUICKSTART_DEVICE.md)** - 一键运行应用
- **📱 [设备调试完整指南](./RUN_ON_DEVICE.md)** - 详细的真机调试步骤
- **🔧 [Android环境配置](./ANDROID_SETUP.md)** - SDK安装和构建过程
- **🐛 [调试指南](./DEBUGGING_GUIDE.md)** - 如何调试应用
- **⚡ [Metro调试指南](./METRO_DEBUG_GUIDE.md)** - Metro Bundler使用和故障排查
- **📝 [设置总结](./SETUP_SUMMARY.md)** - 完整的项目搭建记录

## ⚡ 快速开始

### 使用真机运行（推荐）

```bash
# 方式1：一键启动（最简单）
npx react-native run-android

# 方式2：分步操作（更可控）
# 终端1：启动Metro
npx react-native start

# 终端2：设置端口转发
/opt/homebrew/share/android-commandlinetools/platform-tools/adb reverse tcp:8081 tcp:8081

# 然后在手机上打开应用
```

**前提条件**：
- ✅ 手机已开启USB调试
- ✅ 手机通过USB连接到电脑
- ✅ 依赖已安装（`pnpm install --shamefully-hoist`）

---

## 原始React Native文档

This project was bootstrapped using [`@react-native-community/cli`](https://github.com/react-native-community/cli).

> **Note**: Make sure you have completed the [Set Up Your Environment](https://reactnative.dev/docs/set-up-your-environment) guide before proceeding.

## Step 1: Start Metro

First, you will need to run **Metro**, the JavaScript build tool for React Native.

To start the Metro dev server, run the following command from the root of your React Native project:

```sh
# Using npm
npm start

# OR using Yarn
yarn start
```

## Step 2: Build and run your app

With Metro running, open a new terminal window/pane from the root of your React Native project, and use one of the following commands to build and run your Android or iOS app:

### Android

```sh
# Using pnpm (recommended)
pnpm run android

# OR using npm
npm run android
```

**Note**: iOS support has been removed. This app is Android-only for the MVP.

If everything is set up correctly, you should see your new app running on your Android device.

This is one way to run your app — you can also build it directly from Android Studio.

## Step 3: Modify your app

Now that you have successfully run the app, let's make changes!

Open `App.tsx` in your text editor of choice and make some changes. When you save, your app will automatically update and reflect these changes — this is powered by [Fast Refresh](https://reactnative.dev/docs/fast-refresh).

When you want to forcefully reload, for example to reset the state of your app, you can perform a full reload:

- **Android**: Press the <kbd>R</kbd> key twice or select **"Reload"** from the **Dev Menu** (shake device to open).

## Congratulations! :tada:

You've successfully run and modified your React Native App. :partying_face:

### Now what?

- If you want to add this new React Native code to an existing application, check out the [Integration guide](https://reactnative.dev/docs/integration-with-existing-apps).
- If you're curious to learn more about React Native, check out the [docs](https://reactnative.dev/docs/getting-started).

# Troubleshooting

If you're having issues getting the above steps to work, see the [Troubleshooting](https://reactnative.dev/docs/troubleshooting) page.

# Learn More

To learn more about React Native, take a look at the following resources:

- [React Native Website](https://reactnative.dev) - learn more about React Native.
- [Getting Started](https://reactnative.dev/docs/environment-setup) - an **overview** of React Native and how setup your environment.
- [Learn the Basics](https://reactnative.dev/docs/getting-started) - a **guided tour** of the React Native **basics**.
- [Blog](https://reactnative.dev/blog) - read the latest official React Native **Blog** posts.
- [`@facebook/react-native`](https://github.com/facebook/react-native) - the Open Source; GitHub **repository** for React Native.
