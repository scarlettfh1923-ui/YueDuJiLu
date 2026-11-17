# 书蠹（YueDuJiLu）

本地优先的实体书阅读记录应用。包名 `com.scarlett.yuedujilu`，当前版本为 **1.0.3 青瓷预览版**（versionName `1.0.3`，versionCode 4）。上版为1.0.2。包名、签名与数据文件兼容条件保持不变，不清除用户数据。

仓库：https://github.com/scarlettfh1923-ui/YueDuJiLu

本仓库公开托管源码与预发布安装包，未附加开源许可证；公开可见不等于授权任意使用。

## 安装包

请从 GitHub Release 下载，APK 不纳入 git 跟踪。

- 当前预发布：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.3-preview
- 附件：`ShuDu-1.0.3.apk` 以及同名 `.sha256`
- SHA-256：`4eb5f0c464bad5f61bc13c704a10baf133a218ec9e3c1f634bce10225cfc1408`
- 上一版：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.2-preview
- 更早：https://github.com/scarlettfh1923-ui/YueDuJiLu/releases/tag/v1.0.0-preview
- minSdk 26，targetSdk 35，compileSdk 36
- 独立签名，APK Signature Scheme v2。证书 DN：`CN=Scarlett YueDuJiLu, OU=Personal, O=Scarlett, L=Shanghai, ST=Shanghai, C=CN`

**真机（含小米15）、相机实扫、真实账号同步与实机覆盖升级尚未完成。** 1.0.3候选已采用生产Compose组件的API35原生HWUI离屏渲染验证，不是SVG设计稿；这不等同设备GPU或系统窗口实测。

## 1.0.3 书蠹 · 青瓷预览版

- 应用显示名改为「书蠹」；包名仍为`com.scarlett.yuedujilu`，仓库/目录、签名钥匙与`library.json`等数据标识不变。versionCode从已发布1.0.2的3升到4；未清数据。原有“阅读记录”作为记录类型/页面功能名称保留。

- 全套页面重做：书架、统计、四态日历、我的、详情、计时与结束、书籍表单、ISBN与扫码外层、补录、四主题、万维/微信/DeepSeek配置、备份恢复和导出。
- 普通形状统一22dp，胶囊使用实际高度50%曲率；共享形状用于背景、clip、点击反馈和阴影。统计等高槽、跨卡固定数字基线，单位按文本基线对齐。四个低饱和指标色组随主题协调。
- 底栏采用Android原生Backdrop1.0.4（Apache2.0）真实背景层采样、模糊与API33+AGSL边缘透镜、高光/内阴影；图标文字后绘制保持清晰。API31/32仅背景模糊，低版本/高对比/减弱透明/低内存实色降级。它是接近Liquid Glass的Android实现，不宣称等同苹果私有光学效果。
- 主页面直接切换、不交叉叠绘文字；三项等热区，移动选中胶囊可中断重定向；抬起提交、滑出取消；系统关闭动画时直达状态；页面滚动与非密钥草稿可恢复。玻璃只用于导航和计时浮条，不铺满统计卡片。
- 日历读取真实记录：有正阅读记录 / 所选来源完整明确为零 / 不全未知 / 未来。实体未读须主动确认，可撤销；微信缺失永不补零。日期并集、点日明细与统计一致。微信区间本数无明细时，实体本数显式标明口径，不冒充合计。
- 正式启动器采用已选定的「墨蓝书脊」，含adaptive、legacy、round、monochrome。旧B图标弃用。青瓷为新安装默认；旧安装明确保存的主题不强改。
- 密钥字段全部遮盖并关闭输入联想，不放入可保存页面状态；恢复数据增加覆盖前确认。
- 技术/验证范围见[UI实现说明](docs/UI-1.0.3.md)。本地原生离屏渲染已覆盖全套页面；仍不宣称完成真机相机、真实账号同步、GPU性能或实机升级测试。

## 1.0.2 相对 1.0.1（历史记录）

- **启动器图标**：采用用户选定的 B「青绿书页」。自适应图标前景是源 SVG 的双页路径（青绿 `#2F5D4E`），背景层暖白 `#F3EFE4` 铺满，不含预烘焙圆形遮罩或白边；旧版方形与圆形 webp、以及单色层一并更新。构图按源图 1:1 映射，没有缩放重排。
- **扫描 ISBN**：修好 CameraX 1.5.2 下手绑 Preview + ImageAnalysis 却使用 `CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED` 导致每帧直接关闭、检测不执行的问题，改为 `ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL`。相机绑定改到稳定生命周期，`dispose` 会释放 scanner / executor / 用例，不再在 `AndroidView.update` 里反复解绑重绑。支持 QR，但只接受严格校验的 ISBN（ISBN-13 必须 978/979 + 校验位，ISBN-10 必须校验位正确；允许带「ISBN」字样或横线）。普通 URL 二维码和商品码会给出可见提示，不会打开链接。约 15 秒无结果会给出手填入口。文案写明扫描书背或封底上的 ISBN 条码。识别模型随包内置（APK 中含 `assets/mlkit_barcode_models`），不必等 Play 下载。本环境没有真机实扫，不能声称扫码已成功。
- **统计**：概览布局改为双列轻量圆角卡（约 24dp），含阅读天数、阅读时长、阅读本数、读完本数、连续阅读、日均阅读时长，以及「新增笔记」轻条（口径为微信划线 + 想法，书签无逐条日期不计入）。默认来源为全部（实体 + 微信），可只看实体或只看微信。时长按来源相加，阅读天数按自然日去重合并，不能把两天相加。日均分母为区间内已过的自然日（含未阅读日）。连续阅读定义为：从区间末日（未结束则今天）向前，连续有阅读记录的自然日；遇到微信日数据缺失即停止计算，不把缺失当作未读。微信区间本数无明细时显示「未提供 / 仅实体书」，不会用月度书数求和或书架存量冒充，也不会把缺失填成 0。期对比只在两期口径都完整时显示。页数仍只出现在书和记录里。底栏仍是三等分，没有做成参考图的四栏。
- **底栏玻璃**：在 API 31+ 且未开启高对比 / 减弱透明、也不是低内存设备时，使用 Haze 1.7.2（Apache 2.0）对主界面内容做采样模糊，再叠加浅色染和细亮边。这是背景采样，不是 `Modifier.blur`（不会把底栏文字图标糊掉），细亮边是画上去的高光，不是物理折射。Haze 1.7.3 会强制更新到需要 compileSdk 37 / AGP 9.1 的 Compose 1.12，因此固定在 1.7.2，以便与本项目 compileSdk 36、AGP 9.0.1 兼容。API 30 及以下退回预乘半透明合成色；无障碍或低内存时退回近实色。计时条和统计数据卡保持实色。独立窗口中的弹层无法采样主界面，也使用合成色。未在真机上验证观感。三等分选中区域和 48dp 热区没有改掉。

1.0.1 的书架、微信读书同步、四套主题和全部既有功能都保留。

## 微信读书 API（1.0.1 已实现，1.0.2 保持）

按官方公开 skill 1.0.5 文档实现，入口为 `POST https://i.weread.qq.com/api/agent/gateway`，`Authorization: Bearer` 用户 `wrk-` 密钥，业务参数与 `api_name`、`skill_version` 平铺，不传 `vid`，不把参数包进 `params`/`data`。

已实现：测试连接 `/_list`；固定 `importStartDate` 同步到今天的日阅读时长；划线与想法按 `createTime` 分日、按 ID 去重并分页到结束；书签无逐条日期，不计入每日新增；失败或取消不覆盖已有数据；密钥只存 Android Keystore。未声称用真实账号实测。

## 测试

70项单元、交互和原生渲染测试通过（0失败，0跳过）；Release Lint无错误（35条主要为版本/风格提醒）。36张逐页/边界原生PNG与64帧导航动画留作本地质量审阅。测试使用Robolectric4.16，API35、NATIVE图形模式和PixelCopy硬件渲染路径。包括全部页面、360/432dp宽度、1.0/1.3字体、四主题、空态/长书名/长指标、导航取消/快速重定向/滚动恢复与日历真实性。测试夹具仅位于test源集，不进入生产APK；未使用真实账号或真实密钥。

构建使用JDK17工具链；Backdrop发布字节码要求宿主测试JVM为JDK21。AGP、Compose和compileSdk未升级。示例（按本机路径替换）：

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew -Dorg.gradle.java.home="$JAVA_HOME" \
  -Porg.gradle.java.installations.paths=/path/to/jdk-17,/path/to/jdk-21 \
  testDebugUnitTest lintRelease assembleRelease
```

签名配置示例见 `keystore.properties.example`。不要提交 `signing/`、`local.properties` 或真实密码。
