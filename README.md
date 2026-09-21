# BemfaControl

自用的巴法云 TCP 智能插座 Android 控制器。按房间分组展示设备，完整显示主题昵称和 topic，并通过巴法云 HTTP API 下发开关指令。

## 当前功能

- 首次配置巴法云用户私钥 UID
- 拉取 `type=3` 的 TCP 设备主题
- 识别插座设备并按巴法云房间字段分组
- 支持移动设备、新建房间、房间改名和排序
- 完整显示设备昵称，不截断 topic
- 单个插座开关控制和状态展示
- GitHub Actions 构建调试 APK

## 房间管理说明

- 首页“房间”行末的设置按钮用于管理已有房间，目前支持房间改名和排序，不提供单独创建空房间的入口。
- 新建房间需要先打开某个设备的详情页，点击“移动房间”，在“选择房间”窗口中选择“新建房间”，输入房间名并确认。
- 新房间会在设备移动成功后创建，因此暂不支持创建不包含设备的空房间。

## 技术架构

- Kotlin + Jetpack Compose + Material 3
- MVVM + StateFlow
- Retrofit + OkHttp + Kotlin Serialization
- DataStore 保存 UID
- Android Gradle Plugin 8.7.3 / Kotlin 2.0.21

## 构建与测试

CI 使用 GitHub Actions 构建：

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## 发布

推送 `v*` tag 后，Release 工作流会：

1. 构建 release APK
2. 计算 SHA-256
3. 创建 GitHub Release
4. 生成 `gh-proxy.com` 加速下载链接
5. 保留 GitHub 原始下载链接

签名密钥需要通过 GitHub Secrets 配置。
