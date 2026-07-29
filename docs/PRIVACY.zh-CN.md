# 隐私与能力边界

[English](PRIVACY.md) | 简体中文

Heimdall 不提供云端账户或分析服务。

## 本地数据

Profile、宏、布局数据、地图、攻略、Canvas 图片、设置、导入图标和自动 Profile 恢复快照均保存在应用本机。只有玩家主动导出或分享时，这些数据才会离开应用。

本 Alpha 默认关闭 Android 平台备份。卸载、更换签名通道或迁移到其他设备前，玩家应使用明确的 Profile 导出功能。

Alpha 的 Profile 导出是配置 JSON，不是包含全部资源的归档。卸载或迁移设备后，自定义 Profile 图标、导入的 Macro 图标和 Canvas 图片文件需要重新选择或导入。

## 无障碍服务

基础触控使用无障碍服务发送兼容的上屏触控操作，并为可选的 Profile 匹配取得保守的上屏应用与窗口上下文。Heimdall 不应把任意无障碍事件文本作为游戏或 ROM 身份。

## Shizuku

控制器增强通过已授权的 Shizuku UserService 提供原生手柄录制/回放、虚拟右摇杆、精准瞄准和选定的 Thor 触控通道。Shizuku 未运行或未授权时，这些能力不可用。Heimdall 不捆绑 Shizuku。

## 屏幕捕获与音频

在系统支持时，截图使用 Android 可识别显示屏的无障碍 API。录屏和实时放大镜需要明确的 Android MediaProjection 授权。游戏声音录制使用 Android Audio Playback Capture，不选择麦克风输入；上屏应用可以拒绝播放音频捕获。

## 网络

联网和网络状态权限用于玩家自行配置的 Interactive Map 页面及连接状态。为保证页面兼容性，Interactive Map 页面可以运行 JavaScript，但 Heimdall 不向页面暴露 JavaScript 接口。

## 问题报告

分享日志、截图、录屏或 Profile 导出前，请移除不应公开的姓名、路径、URL、令牌、账户信息和游戏数据。
