# 书蠹 · 青瓷UI与液态玻璃：实现范围

## 页面覆盖

|获批设计页|生产实现|
|---|---|
|01书架 /19空态 /20添加弹层|ShelfScreen；列表信息层级、来源、进度、实心阅读行动、可读空态、三添加入口|
|02统计概览|StatsScreen；双列三行，低饱和四色，同步标签/数字/注释槽，固定34sp数字基线；长数字自动适配，长说明可点查看|
|03日历|ReadingCalendarPanel与ReadingCalendar；四态、点日真实实体记录与微信日时长、可撤销实体未读确认|
|04我的|MineScreen；阅读连接、外观、AI、数据分组，非底栏AI|
|05详情|BookDetailScreen；封面/元信息横排、主行动、等宽次行动、记录与完读日期|
|06计时 /07结束|EndSessionScreen两阶段；暂停恢复、有效时长、结束页、可选读完，475/500不篡改成500|
|08加书/编辑|EditBookScreen；统一字段与滚动、草稿恢复|
|09 ISBN /10扫码|IsbnEntryScreen、ScanIsbnScreen；扫码仅外观改动，CameraX ORIGINAL分析器与严格ISBN/QR解析保持原实现|
|11补录/修改|BackfillScreen；起止时间、起止页与跨日口径，上海时区|
|12四主题|AppearanceScreen；青瓷青绿/雾紫/灰蓝/暖白持久化，保留旧主题选择|
|13万维|ShowApiConfigScreen；AppKey本机保存、遮盖输入、保存/测试/清除|
|14微信|WereadScreen；官方skill1.0.5，固定起点至今天，首次7天初始化，幂等upsert，失败不覆盖|
|15 AI /16 DeepSeek|AiEntryScreen、DeepSeekScreen；只配置，不调用模型，默认关深思，温度0–2/.7，深思禁调温度|
|17备份 /18导出|BackupScreen、ExportScreen；无密钥，恢复覆盖前确认|

## 集中曲率

`UiShape.Regular`=22dp：普通卡片、字段、按钮、封面、取景与通知。
`UiShape.Capsule`=50%：主导航、内选中项、搜索、周期/状态控件、计时附条。
底栏外高H，内缩6dp，外半径H/2，内半径(H−12)/2，共同中心；字体变大时同步增高。
`UiShape.Sheet`仅顶部22dp，与普通形态同弧度。圆色样、日历点、启动器系统遮罩不使用随意卡片半径。
Material所有常规shape和色阶也统一，消除默认紫色Switch/Slider混入青瓷。

## 玻璃选择

原生Android公开库AndroidLiquidGlass / Backdrop1.0.4，Apache2.0。依赖与来源见THIRD_PARTY_NOTICES。
它与现有Compose1.10/Kotlin2.3匹配；未为最新2.x库冒险升级AGP/Compose。
采样层包含不透明页面背景；先模糊/AGSL边缘透镜，再绘制乳白材质与柔亮边/内暗边，最后独立绘制文字图标。
API33+启用lens；API31/32只模糊；旧API/无障碍/低内存降为实色。
这不是Modifier.blur自身内容，也不冒充苹果私有折射和动态形变。

## 证据与限制

所有质量图来自生产Composable的Robolectric API35、NATIVE/HWUI与PixelCopy，非静态概念稿。
测试数据、配置状态均为本机隔离夹具，不代表微信已实连；不进入生产源集。
离屏窗口截图没有验证手机实际系统栏、IME、设备GPU/功耗、触觉、相机与真实网络。
专用模拟器在一次保守重启后仍offline，已停止其进程节约资源；没有操作其他AVD或应用。
原生动画以确定性测试时钟逐帧捕获；视频不是设备录屏或帧率基准，需人工观看后判断手感。
签名与包名/版本升级条件可校验，但未执行实机覆盖安装，不保证设备OEM行为。

## 口径审计

阅读天数按日期并集，不汇总页数；页码仅属于书籍/记录。
正的部分微信记录可证明该日读过，但不能冒充完整时长；缺失、null与未覆盖区间不补零、不编造环比。
实体无记录仅代表未知，除非用户主动确认未读。确认与正阅读冲突时正记录优先。
微信完整明确零与实体确认均满足，合并视图才显示确认未读；未来独立处理。

## 应用改名与升级身份

Launcher/Recents/权限请求等系统应用名由Manifest的`@string/app_name`统一解析为「书蠹」。应用内记录类页面仍称“阅读记录”，不是旧品牌残留。包名、项目/仓库名、数据文件`library.json`、加密偏好与签名配置保持原标识；versionCode3→4，同证书覆盖条件已核验。未执行真机覆盖安装，不能把静态身份条件核验冒充设备升级实测。

## 交付前转场修正

生产MainTabs原先使用140ms Crossfade，会同时绘制两页文字。现改为直接提交单页，保持SaveableStateHolder、独立底栏胶囊弹簧与按下/取消反馈。回归逐帧检查16/32/64/140ms各时点均仅存在单页，提交完成后仅为目标页；滑块仍为浅色透明胶囊，未根据视频描述改成深色。
