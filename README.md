# 阅读记录（YueDuJiLu）

本地优先的实体书阅读记录应用。包名 `com.scarlett.yuedujilu`，当前预发布 **1.0.2-preview**（versionName `1.0.2`，versionCode 3）。可覆盖安装 1.0.0 / 1.0.1，签名证书相同，用户数据保留。

仓库：https://github.com/scarlettfh1923-ui/YueDuJiLu

本仓库公开托管源码与预发布安装包，未附加开源许可证；公开可见不等于授权任意使用。

## 安装包

请从 GitHub Release 下载，APK 不纳入 git 跟踪。

- 当前预发布：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.2-preview
- 附件：`YueDuJiLu-1.0.2-preview.apk` 以及同名 `.sha256`
- SHA-256：`49ca5ff1391e16d81e717a2cbffb4b0480c89610574cfd75c4cea02d66cb41a1`
- 上一版：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.1-preview
- 更早：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.0-preview
- minSdk 26，targetSdk 35，compileSdk 36
- 独立签名，APK Signature Scheme v2。证书 DN：`CN=Scarlett YueDuJiLu, OU=Personal, O=Scarlett, L=Shanghai, ST=Shanghai, C=CN`

**安装、真机（含小米 15）与模拟器运行：本环境未完成，不得据此声称扫码或玻璃效果已在真机验证。** 请以下载的 APK 自行安装查看。

## 1.0.2 相对 1.0.1

- **启动器图标**：采用用户选定的 B「青绿书页」。自适应图标前景是源 SVG 的双页路径（青绿 `#2F5D4E`），背景层暖白 `#F3EFE4` 铺满，不含预烘焙圆形遮罩或白边；旧版方形与圆形 webp、以及单色层一并更新。构图按源图 1:1 映射，没有缩放重排。
- **扫描 ISBN**：修好 CameraX 1.5.2 下手绑 Preview + ImageAnalysis 却使用 `CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED` 导致每帧直接关闭、检测不执行的问题，改为 `ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL`。相机绑定改到稳定生命周期，`dispose` 会释放 scanner / executor / 用例，不再在 `AndroidView.update` 里反复解绑重绑。支持 QR，但只接受严格校验的 ISBN（ISBN-13 必须 978/979 + 校验位，ISBN-10 必须校验位正确；允许带「ISBN」字样或横线）。普通 URL 二维码和商品码会给出可见提示，不会打开链接。约 15 秒无结果会给出手填入口。文案写明扫描书背或封底上的 ISBN 条码。识别模型随包内置（APK 中含 `assets/mlkit_barcode_models`），不必等 Play 下载。本环境没有真机实扫，不能声称扫码已成功。
- **统计**：概览布局改为双列轻量圆角卡（约 24dp），含阅读天数、阅读时长、阅读本数、读完本数、连续阅读、日均阅读时长，以及「新增笔记」轻条（口径为微信划线 + 想法，书签无逐条日期不计入）。默认来源为全部（实体 + 微信），可只看实体或只看微信。时长按来源相加，阅读天数按自然日去重合并，不能把两天相加。日均分母为区间内已过的自然日（含未阅读日）。连续阅读定义为：从区间末日（未结束则今天）向前，连续有阅读记录的自然日；遇到微信日数据缺失即停止计算，不把缺失当作未读。微信区间本数无明细时显示「未提供 / 仅实体书」，不会用月度书数求和或书架存量冒充，也不会把缺失填成 0。期对比只在两期口径都完整时显示。页数仍只出现在书和记录里。底栏仍是三等分，没有做成参考图的四栏。
- **底栏玻璃**：在 API 31+ 且未开启高对比 / 减弱透明、也不是低内存设备时，使用 Haze 1.7.2（Apache 2.0）对主界面内容做采样模糊，再叠加浅色染和细亮边。这是背景采样，不是 `Modifier.blur`（不会把底栏文字图标糊掉），细亮边是画上去的高光，不是物理折射。Haze 1.7.3 会强制更新到需要 compileSdk 37 / AGP 9.1 的 Compose 1.12，因此固定在 1.7.2，以便与本项目 compileSdk 36、AGP 9.0.1 兼容。API 30 及以下退回预乘半透明合成色；无障碍或低内存时退回近实色。计时条和统计数据卡保持实色。独立窗口中的弹层无法采样主界面，也使用合成色。未在真机上验证观感。三等分选中区域和 48dp 热区没有改掉。

1.0.1 的书架、微信读书同步、四套主题和全部既有功能都保留。

## 微信读书 API（1.0.1 已实现，1.0.2 保持）

按官方公开 skill 1.0.5 文档实现，入口为 `POST https://i.weread.qq.com/api/agent/gateway`，`Authorization: Bearer` 用户 `wrk-` 密钥，业务参数与 `api_name`、`skill_version` 平铺，不传 `vid`，不把参数包进 `params`/`data`。

已实现：测试连接 `/_list`；固定 `importStartDate` 同步到今天的日阅读时长；划线与想法按 `createTime` 分日、按 ID 去重并分页到结束；书签无逐条日期，不计入每日新增；失败或取消不覆盖已有数据；密钥只存 Android Keystore。未声称用真实账号实测。

## 测试

49 项 JVM 单元测试已通过（0 失败），包括 ISBN 合法 / 非法 / 带横线文本 / URL 拒绝 / 商品码拒绝，以及统计的重叠日期去重、跨月切分、来源过滤和缺失日不填 0。没有做静态图像 ML Kit 检测（JVM 上不具备可行的仪器环境）。仪器测试、模拟器安装和 adb 截屏：**未测**（本环境 AVD 离线）。

本机构建（JDK 17 + Android SDK）：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew testDebugUnitTest assembleRelease
```

签名配置示例见 `keystore.properties.example`。不要提交 `signing/`、`local.properties` 或真实密码。
