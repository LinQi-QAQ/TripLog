# 出行记 TripLog

一个记录出行信息的安卓应用。拍下车票，AI 自动识别车次、时间、座位、票价，行程自动归档统计。

数据全部存在本机，不依赖任何账号和云服务。唯一的联网行为是识别车票时调用你自己配置的 AI 接口。

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

## 许可

MIT License，详见 [LICENSE](LICENSE)。
