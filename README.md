# 阅读记录（YueDuJiLu）

本地优先的实体书阅读记录应用。包名 `com.scarlett.yuedujilu`，版本 **1.0.0-preview**（versionName `1.0.0`，versionCode 1）。

这是从零建立的独立 Android 项目（Kotlin + Jetpack Compose）。本仓库公开托管源码与预发布安装包，未附加开源许可证；公开可见不等于授权任意使用。

仓库：https://github.com/scarlettfh1923-ui/YueDuJiLu

## 安装包

请从 GitHub Release 下载，不要从 git 源码树里找 APK（APK 不纳入版本跟踪）。

- 预发布：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.0-preview
- 附件：`YueDuJiLu-1.0.0-preview.apk` 以及同名 `.sha256` 校验文件
- SHA-256：`390e55f678da6f8f33a8ce6e908a35342c5635f1ade88f9b9e51d5bc1191cae7`
- minSdk 26，targetSdk 35，compileSdk 36
- 相机权限仅在「扫描 ISBN」时申请；网络权限用于（自行填写 AppKey 之后的）万维易源查询

本仓库源码对应上述已交付的稳定 preview 包。签名为项目独立密钥，APK Signature Scheme v2。用 `apksigner verify` 验证即可。此包未使用 v1（JAR）签名，所以 `jarsigner -verify` 可能显示 unsigned，这不代表 APK 未签名。

证书 DN：`CN=Scarlett YueDuJiLu, OU=Personal, O=Scarlett, L=Shanghai, ST=Shanghai, C=CN`

**安装与真机/模拟器运行验证：未通过。** 构建时曾创建专用 AVD `YueDuJiLu` 并启动无头模拟器，但 `adb` 一直将其列为 `offline`，因此没有完成安装、冒烟或截屏。请以下载的 APK 自行安装查看。

## 已实现范围

### 图标

统一矢量图标系统：24dp 视口、2dp 描边、圆线帽、圆转角，光学尺寸一致。底栏「书架 / 统计 / 我的」有描边与填充两种，选中为填充 + 主题色胶囊。不使用 emoji，也不混用其他图标库。

### 主题（可替换 token）

四套可立即切换并本地持久化的主题，重启后保持。切换会同时作用到背景、卡片、文字、图标、按钮和系统栏。

1. **雾紫**（默认，雾紫底 + 深梅强调）
2. **青绿**
3. **灰蓝**
4. **暖白**

「我的 → 外观主题」提供四色预览、名称和勾选。更换主题不会丢掉其他配置。

颜色、圆角、间距来自可替换的 `theme-tokens.json`：

- 默认：`app/src/main/assets/theme-tokens.json`
- 运行时覆盖：应用私有目录 `files/theme-tokens.json`
- 解析失败或缺字段时回退到内置默认值

### 交互原则（Android 约定优先）

- 按下立刻给出 ripple + 轻微缩放（约 0.97），不等到抬手才反馈
- 系统关闭动画，或字体放大到 1.3 及以上时，减弱动效：取消缩放与展开动画，仅保留短淡入淡出和 ripple
- 未把 Web 弹簧或玻璃拟态直接搬到 Android

### 三栏与实体书闭环

- 书架顶部是全宽搜索和加号，不再重复「书架 / 统计 / 我的」标题。
- 加号菜单：扫描 ISBN、输入 ISBN、手动填写。扫描入口只在加号里。
- 状态筛选：全部 / 在读 / 想读 / 已读 / 弃读；来源筛选：全部来源 / 实体书 / 微信读书。
- 可添加、编辑、查看书籍详情。相同 ISBN 不会重复入库。
- 阅读计时可持久化，支持暂停 / 继续；结束时保存起止页。进行中时按钮为「查看计时」，底部有计时条（与悬浮底栏留有间距）。
- 可补录、修改已结束的记录。
- 可主动记为读完、修改完读日期、撤销读完。500 页读到 475 页可以记为读完，进度仍保留 475/500。
- 没有预置示例书，也没有伪造的同步数据。空态按真实数据提示。
- 全局统计不展示页数。展示真实本地时长、阅读天数、阅读本数、读完本数；支持周 / 月 / 年 / 全部，以及按上海时区自然日切开的日历（跨天会话会按天拆分有效时长）。

### 「我的」

- **微信读书**：入口清楚，状态为待接入，不会假装同步成功。保留接口位置，未实现拉取。
- **图书查询配置**（万维易源 · AppKey）：按公开文档实现字段（`https://route.showapi.com/1626-1?appKey=`，表单字段 `isbn`，成功条件 `showapi_res_body.ret_code == 0`）。可自行填写 AppKey，并在已保存后使用「测试连接」。没有密钥时不会发请求。
- **AI 入口**：仅配置入口，用途待定，不提供对话或推荐。
- **DeepSeek 配置**：可保存密钥、接口地址、模型名；深度思考默认关闭；温度 0–2，默认 0.7；开启深度思考后温度禁用。当前不会调用接口。
- 备份与恢复、导出阅读记录（JSON，通过系统文件选择器）。密钥不会进入备份或导出。
- 外观主题。

密钥使用 Android Keystore（EncryptedSharedPreferences）加密保存在本机，不写入日志，`allowBackup=false`。应用不会向使用者索要真实凭证。

## 已知限制与未完成项

- **微信读书**：同步未实现。统计里的同步按钮只会说明尚未接入；「新增笔记」显示「暂无数据」，没有占位数字。
- **AI / DeepSeek**：只做配置存储，不发起网络调用，也没有聊天界面。
- **万维易源**：客户端与解析已按公开文档写好，并有解析单测。没有用户 AppKey 时不会发出付费或测试请求，也不声称「连接已通过」。
- **ISBN 扫描**：使用应用内 CameraX + ML Kit，不必再装其他扫码应用。**真机相机与识别未实测。**
- **安装运行**：模拟器曾出现 `offline`，**安装、冒烟和截屏均未验证**。构建成功不等于已经在设备上跑通。
- **最新版 apple-design 未合入**本仓库与本 APK。设计交接中带 `meta/themes/contrast` 的 token 方案，以及据此调整的底栏、圆角、字号等视觉改动，发生在该稳定包构建之后，因此没有打进 `v1.0.0-preview`，以免源码与已构建包不一致。本包只包含构建当时已有的可替换 token 结构与默认四套主题。
- 未做书摘编辑、推荐、云同步、社交。
- 实体书与电子书的「关联为同一部」未做。
- 计时以本地时间戳为准；没有保证 HyperOS 后台保活的前台服务承诺。
- 界面由 Compose 绘制。概念图只作层次参考，不承诺像素一致。

## 测试

在项目根目录，使用 **JDK 17** 和已安装的 Android SDK：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew testDebugUnitTest
```

若本机要签名构建 release 包，还需按下一节自行准备密钥，然后执行 `./gradlew assembleRelease`。

**21 项 JVM 单元测试已通过**，覆盖：计时暂停/继续、跨上海自然日拆分、475 页读完保留页数、补录不回退真实进度、主题持久化且不丢其他设置、ISBN 去重、万维易源响应解析、网络错误脱敏、主题 token JSON 覆盖与解析。

仪器测试、模拟器安装、adb 截屏：**未测 / 模拟器 offline**。

## 本机构建与签名（示例，不含真实密码）

独立签名材料只应放在本机 `signing/` 目录，不要提交到仓库。可将下面的示例复制为 `signing/keystore.properties`（该路径已被 `.gitignore` 排除）：

```
storeFile=signing/your-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

仓库根目录也提供了 `keystore.properties.example`，复制后填入本机值即可。构建脚本仅在该文件存在时读取签名配置；缺少它时仍可编译和跑单测，但不能产出与本预发布包相同签名的 APK。

需要 JDK 17。如需指定本机 JDK 路径，可在本地 `gradle.properties` 中设置 `org.gradle.java.home`，不要把机器绝对路径提交进仓库。

`local.properties`、密钥库、密码文件、构建目录和 APK 均已被忽略，请不要手动添加。

## 界面与边到边

- `enableEdgeToEdge()`，状态栏 / 导航栏随主题浅色图标适配
- 窗口 `windowSoftInputMode=adjustResize`
- 表单页使用 `imePadding()`，避免输入法遮挡；主页面为状态栏与导航栏留出安全区
- 针对小米 15 / HyperOS / Android 15+ 按边到边惯例处理，但未在该真机上验证
