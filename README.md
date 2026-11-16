# 阅读记录（YueDuJiLu）

本地优先的实体书阅读记录应用。包名 `com.scarlett.yuedujilu`，当前预发布 **1.0.1-preview**（versionName `1.0.1`，versionCode 2）。可覆盖安装 1.0.0，签名证书相同，用户数据保留。

仓库：https://github.com/scarlettfh1923-ui/YueDuJiLu

本仓库公开托管源码与预发布安装包，未附加开源许可证；公开可见不等于授权任意使用。

## 安装包

请从 GitHub Release 下载，APK 不纳入 git 跟踪。

- 当前预发布：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.1-preview
- 附件：`YueDuJiLu-1.0.1-preview.apk` 以及同名 `.sha256`
- SHA-256：`bf3b8e258a80c7f97c7a6ccf3fc65c4c6168ed54ab8906f3f7d642b657854487`
- 上一版：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.0-preview
- minSdk 26，targetSdk 35，compileSdk 36
- 独立签名，APK Signature Scheme v2。证书 DN：`CN=Scarlett YueDuJiLu, OU=Personal, O=Scarlett, L=Shanghai, ST=Shanghai, C=CN`

**安装与真机/模拟器运行截图：本环境未完成。** 没有可用的可交互模拟器截图可交付。请以下载的 APK 自行安装查看界面。

## 1.0.1 相对 1.0.0 已落地的界面

依据安装包实际问题改了源码，不是概念图：

- **底栏**：自定义三等分等宽等高，三项始终「图标在上、文字在下」，选中浅底铺满整格（不是图标背后的小胶囊，也不是系统 NavigationSuite 那种只给选中项加横排文字）。clip 与 ripple 同一边界，左右边距对称，并保留系统导航安全区。底栏为半透明合成色 + 细亮边 + 微影；高对比时回退实色。当前没有可用的应用内实时背景模糊公开 API，因此不会把预乘半透明说成实时折射。计时条仍为实色。
- **书架**：搜索与加号同为 48dp 高；状态改为轻量文字 + 下划线；来源缩成右侧轻量选择。书封按 2:3 做主角，书名 / 作者 / 进度分层，动作改为文字按钮，不再把大按钮和封面挤在同一排，也不再给第一本书套厚白卡片。
- **微信读书页**：去掉开发腔长说明，文案改为「尚未连接微信读书」或已保存状态；可填写官方 Skill 密钥、固定导入起点、「最近 7 天」只用于初始化起点。

启动器图标未改（图标候选另路进行）。

## 微信读书 API（1.0.1 已实现范围）

按官方公开 skill 1.0.5 文档实现，入口为 `POST https://i.weread.qq.com/api/agent/gateway`，`Authorization: Bearer` 用户 `wrk-` 密钥，业务参数与 `api_name`、`skill_version` 平铺，不传 `vid`，不把参数包进 `params`/`data`。

已实现：

- 测试连接：`/_list`
- 固定 `importStartDate` 同步到今天的**日阅读时长**（遍历年/月/必要时周，用日桶在 Asia/Shanghai 筛选；不用整月 `totalReadTime` 冒充截断区间；无日明细则标明不完整且不改写这些天）
- **划线与想法**按 `createTime` 分日、按 ID 去重；笔记本与想法均按文档分页到结束，不会只取第一页；书签无逐条日期，不计入每日新增
- 时长与笔记状态相互独立，失败或取消不覆盖已有数据；实体书记录不受影响
- 账户分区使用密钥的不可逆本地哈希，更换账号不会把数据混在一起
- 密钥只存 Android Keystore，备份 / 导出 / 日志 / 源码 / Release 不含密钥
- 回包若有 `upgrade_info`，会暂停并提示，不会盲执行远端指令

未声称用真实账号实测。没有用户密钥时不会发付费或真实账户请求。自定义统计范围下，微信「阅读本数」显示「暂无明细」，不会拿书架存量冒充。

「最近 7 天」= 今天往前共 7 个自然日（今天 − 6），只用于初始化起点；之后每次同步仍是原起点到今天，不会滚动或在成功后自动推进。

## 测试

31 项 JVM 单元测试已通过（0 失败），其中包括网关请求构造、回包解析、日界与起点保持、幂等更新、失败不写库（使用本地 fake fixture），以及底栏等宽/始终显示文字的静态约定。

仪器测试、模拟器安装和 adb 截屏：**未测**。扫码相机可打开但识别无反应的问题，本版未改扫描实现，另路诊断后再补。

本机构建（JDK 17 + Android SDK）：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew testDebugUnitTest assembleRelease
```

签名配置示例见 `keystore.properties.example`。不要提交 `signing/`、`local.properties` 或真实密码。
