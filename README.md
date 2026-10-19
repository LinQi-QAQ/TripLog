# 出行记 TripLog

一个记录出行信息的安卓应用。拍下车票，AI 自动识别车次、时间、座位、票价，行程自动归档统计。

数据全部存在本机，不依赖任何账号和云服务。唯一的联网行为是识别车票时调用你自己配置的 AI 接口。

[**点这里下载 APK**](https://github.com/LinQi-QAQ/TripLog/releases/latest) · 安卓 8.0 以上 · 13 MB · 免费无广告

<p align="center">
  <img src="docs/screenshots/02-截图2.png" width="260" alt="行程列表">
  <img src="docs/screenshots/01-截图1.png" width="260" alt="添加行程">
</p>

## 怎么装到手机上

1. 手机浏览器打开上面那个下载链接，或者电脑下载后传手机也行。
2. 点那个 `app-release.apk`，手机会弹窗说「不允许安装未知来源应用」「此来源无法安装」之类的话。这是安卓的保护机制，不是包有问题。
3. 弹窗里点「设置」或「允许」，把「允许来自此来源的应用」开关打开，返回再点一次安装。
4. 装好之后打开，会先弹一个隐私说明，看完点「同意并继续」。
5. 想用 AI 识别车票的话，去「我的 → AI 设置」填自己的接口地址、模型名和 API Key。不填也能用，手动录入行程不受影响。

第一次装会在弹窗里绕一下，装完之后更新就是直接覆盖，不会再问了。

几个常见疑问：

手机提示「应用未安装」，多半是之前装过别的签名版本，卸载旧的再装。
提示「解析包时出现问题」，是下载不完整，删掉重下一次。
装完找不到图标，去应用列表里搜「出行记」。

## 功能

- **车票识别**：选图或拍照，AI 提取车次号、出发到达站、时间、座位、票价、乘车人，自动填入表单供你核对修改
- **行程管理**：按时间分区展示，即将出发在前，历史在后，支持搜索、按交通工具筛选、批量删除
- **出发提醒**：按发车时间提前通知，开机后自动恢复
- **出行统计**：年度总览、交通工具占比、常去城市、月度明细、总花费
- **数据备份**：导出导入 zip，换机不丢数据
- **多交通工具**：火车、飞机、汽车统一管理

## 隐私

- 行程、图片、设置全部保存在应用私有目录，不上传任何服务器
- 卸载应用即彻底删除
- 不含广告、统计、崩溃上报等第三方 SDK
- **仅「识别车票」功能**会把选中图片发送到你自己在设置里填写的 AI 服务商（DeepSeek、OpenAI、通义、智谱等任何 OpenAI 兼容接口），这是识别所必需的。请勿使用含敏感信息的图片

## 技术栈

| 层 | 方案 |
|---|---|
| 语言 | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| 架构 | 单 Activity + Navigation Compose + ViewModel |
| 数据库 | Room |
| 依赖注入 | 手写 AppContainer（未引入 Hilt） |
| 网络 | OkHttp + kotlinx.serialization |
| 图片 | Coil |
| 密钥存储 | EncryptedSharedPreferences（AES256_GCM） |
| 最低版本 | Android 8.0（API 26） |

## 自己构建

需要 Android Studio 与 Android SDK。

```bash
git clone https://github.com/LinQi-QAQ/TripLog.git
cd TripLog

# debug 包
./gradlew assembleDebug

# release 包（需要签名配置）
./gradlew assembleRelease
```

产物在 `app/build/outputs/apk/`。

### 发布签名

release 包需要签名才能安装。在项目根目录建 `keystore.properties`：

```properties
storeFile=your-release.jks
storePassword=你的密钥库密码
keyAlias=你的别名
keyPassword=你的密钥密码
```

生成密钥库：

```bash
keytool -genkeypair -v -keystore your-release.jks -alias your \
  -keyalg RSA -keysize 4096 -validity 10950
```

`keystore.properties` 与 `*.jks` 已被 `.gitignore` 排除，不会提交。
没有签名配置时 release 构建退化为未签名，debug 构建不受影响。

### AI 接口配置

应用内「我的 → AI 设置」填三项：接口地址、模型名、API Key。
任何 OpenAI 兼容的 `/chat/completions` 端点都能用，系统提示词可自行修改。

接口地址填到 `/v1` 这一层，模型名必须支持图片输入。

## 测试

```bash
# 单元测试（Robolectric，无需设备）
./gradlew testDebugUnitTest

# 插桩测试（需要真机）
./gradlew connectedDebugAndroidTest
```

## 目录结构

```
app/src/main/java/com/linqi/triplog/
├── ai/              AI 识别客户端与结果解析
├── data/
│   ├── backup/      备份导入导出
│   ├── local/       Room 数据库与设置存储
│   ├── repo/        仓库层
│   └── stats/       统计计算
├── remind/          出发提醒与开机恢复
├── ui/              各页面 Compose 界面
└── di/              手写依赖容器
```

更详细的开发笔记见 [docs/开发笔记.md](docs/开发笔记.md)。

## 发新版

打一个 `v` 开头的标签推上去，GitHub 会自动编译、签名、挂到 Releases，不用在自己电脑上打包。

```bash
# 1. 先把 versionName 改成新版本号，比如 1.1.0
# 2. 提交推送
git add -A && git commit -m "chore: 版本号 1.1.0"
git push origin main

# 3. 打标签并推送，这一步会触发自动构建
git tag v1.1.0
git push origin v1.1.0
```

然后去仓库的 Actions 页面看构建进度，绿了之后 Releases 页面就多出一条，APK 挂在里面。

自动构建需要仓库 Settings → Secrets and variables → Actions 里配好四个密钥，
缺了会构建失败。密钥库转 base64 的方法：

```bash
# Linux / macOS
base64 -i release.jks | tr -d '\n' | pbcopy

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Clipboard
```

把复制到剪贴板的那串填进 `KEYSTORE_BASE64`，另外三个
`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 按密钥库实际信息填。

## 许可

MIT License，详见 [LICENSE](LICENSE)。
