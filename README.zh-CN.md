# Heimdall

[English](README.md) | 简体中文

Heimdall 是面向 AYN Thor 下屏的开源游戏助手。它将 Profile、宏、触摸板与右摇杆控制、地图、攻略、静态图片 Canvas、放大镜和快捷操作保留在下屏，让游戏继续显示在上屏。

> Alpha 软件：Heimdall 以 AYN Thor 为目标设备，不承诺兼容所有固件、游戏、模拟器或其他双屏设备。

## 下载

请只安装本仓库 Releases 页面中带固定版本号、不可覆盖的预发布版本。每个版本都会提供：

- `heimdall-vX.Y.Z-alpha.N.apk`
- `SHA256SUMS.txt`
- 对应源码提交和签名证书 SHA-256

可变的私有开发 `debug-latest` 通道不是公开发行版。

## 设置

- 基础触控需要启用 Heimdall 无障碍服务。
- 控制器增强、实体手柄录制/回放、虚拟右摇杆、精准瞄准和映射兼容的 Shizuku 触控需要安装、运行并授权 Shizuku。
- 上屏实时放大镜和录屏需要 Android MediaProjection 授权。
- 游戏声音录制使用 Android Audio Playback Capture，而不是麦克风输入；上屏应用仍可禁止自身声音被捕获。

迁移步骤和已知限制请阅读对应版本的发行说明。发布维护者另请阅读 [docs/RELEASING.md](docs/RELEASING.md)（英语）。

## 从源码构建

要求：

- JDK 17
- Android SDK 35
- CMake 3.22.1
- Android NDK `30.0.14904198`

构建 Debug APK：

```text
./gradlew :assistant:assembleDebug --no-daemon
```

运行 Alpha 使用的同类静态检查：

```text
./gradlew :assistant:lintDebug --no-daemon
```

公开仓库不包含任何签名密钥。本地 Debug 构建使用 Android 开发签名；公开 Alpha 只由受 Tag 约束的发布工作流配合外部私有签名密钥生成。

## 数据与隐私

除非玩家主动导入或导出，Profile 数据只保存在本机。Alpha 默认关闭 Android 平台备份。Heimdall 的联网权限用于玩家自行配置的 Interactive Map 页面；应用不会向这些页面暴露 JavaScript 接口。

能力和数据边界请阅读 [docs/PRIVACY.zh-CN.md](docs/PRIVACY.zh-CN.md)。

## 贡献

提交更改前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)（英语）。原生输入、跨屏路由、MediaProjection、性能和实体操作体验都需要范围明确的 AYN Thor 实机证据。

## 许可证

Heimdall 源码采用 Apache License 2.0，详见 [LICENSE](LICENSE) 和 [NOTICE](NOTICE)。第三方组件保留其各自许可证，详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

AYN 与 Thor 是其各自权利人的商标。Heimdall 是社区项目，不代表 AYN 官方应用。
