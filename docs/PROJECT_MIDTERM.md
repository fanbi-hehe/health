# 私人增重助手 —— 项目中期文档

| 项目 | 内容 |
| :--- | :--- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-08-06 |
| 项目状态 | 开发期（可运行的 Demo） |
| 配套文档 | [PRD.md](../PRD.md) V4.1、[后期开发需求文档](FUTURE_REQUIREMENTS.md) |

---

## 一、项目现状概览

本项目是一款 Android 本地优先的增重/健身助手，核心能力包括：AI 拍照识别食物热量、结构化训练记录、AI 对话、AI 生成训练计划、数据看板、暴躁教练通知。

**当前规模与状态（2026-08-06 实测）：**

| 指标 | 数值 |
| :--- | :--- |
| Kotlin 源码 | 约 6263 行 / 60 个文件 |
| 数据库 | Room 8 张表 / 8 个 DAO |
| 后台任务 | WorkManager 2 个 Worker（照片清理、教练通知） |
| 内置资产 | 200+ 食物 JSON、170+ 动作 JSON、动作图片 1.1MB、动作 GIF 16.4MB（合计约 17.7MB） |
| 编译状态 | `:app:compileDebugKotlin` 通过（2026-08-06，Android Studio JBR） |
| Git 提交 | 有完整提交历史，近期为训练计划/饮食功能迭代 |

**结论：** 项目处于"能跑的 Demo"阶段，架构骨架正确、功能主链路闭环，但存在若干数据安全级别的缺陷和一批未完成的 PRD 需求，距离可发布的产品还有一段距离。

---

## 二、技术栈与架构现状

| 层次 | 技术方案 | 现状 |
| :--- | :--- | :--- |
| 语言 | Kotlin 2.2.10 | ✅ |
| UI | Jetpack Compose + Material 3（BOM 2026.02.01） | ✅ |
| 导航 | Navigation Compose，单 Activity + 底部 4 Tab | ✅ |
| 状态管理 | ViewModel + StateFlow + collectAsState() | ✅ |
| 数据库 | Room 2.7.1（version = 4） | ⚠️ 无 Migration（见缺陷 D4） |
| 配置 | DataStore Preferences | ✅ |
| 网络 | Retrofit + OkHttp + Gson（动态 Base URL） | ✅ |
| 图表 | Vico 2.0.1 | ⚠️ 仅体重折线图 |
| 图片 | Coil + coil-gif | ✅ |
| 后台 | WorkManager 2.9.0 | ✅ |
| 桌面小组件 | Glance 依赖已引入 | ❌ 未实现（仅空包） |

包结构：`data`（local/remote/preference/repository）、`ui`（按功能分模块）、`worker`、`util`、`domain`（仅空壳）、`widget`（仅空壳）。

---

## 三、功能实现状态对照（对照 PRD V4.1 七大模块）

### 模块一：饮食记录

| PRD 要求 | 现状 | 状态 |
| :--- | :--- | :--- |
| 拍照入口 | DietScreen FAB + 相机权限 | ✅ |
| 图像压缩（1080px / 70%） | ImageCompressor | ✅ |
| AI 识别 + JSON 容错 | AiRepository.recognizeFood + 首尾花括号提取 | ✅ |
| 确认页：编辑名称/重量/热量 | FoodConfirmScreen | ✅ |
| 重量快捷调整 ±10/±50 | ✅ | ✅ |
| 热量按重量联动重算 | ✅ | ✅ |
| 总热量 + 餐别选择 | ✅ | ✅ |
| **"存为模板"按钮** | MealTemplate 表已建，无 UI 无逻辑 | ❌ 未实现 |
| 手动录入 + 自动补全 | ManualInputDialog + ScrollableDropdown | ✅ |
| **快捷模板一键填充** | 无 | ❌ 未实现 |

### 模块二：训练记录

| PRD 要求 | 现状 | 状态 |
| :--- | :--- | :--- |
| 结构化记录（日期/部位/动作/组数/次数/重量/备注） | TrainingRecord + 录入/编辑弹窗 | ✅ |
| 历史动作推荐 | 选择部位后查询历史动作 | ✅ |
| 圆环倒计时（60s 默认、±5s 调节、震动） | RestTimer（Canvas 圆环） | ✅ |
| **结束提示音** | 仅震动 | ❌ 未实现 |
| **后台继续计时（通知栏常驻）** | 纯 UI 计时器，退出页面即失效 | ❌ 未实现 |
| **小米 HyperOS 焦点通知/灵动岛** | 无 | ❌ 未实现 |

### 模块三：目标设定

| PRD 要求 | 现状 | 状态 |
| :--- | :--- | :--- |
| 目标体重/每日热量设置 | 设置页 DataStore | ✅ |
| 主页顶部进度条 | 饮食页热量汇总 + 看板进度条 | ✅ |

### 模块四：AI 每日综合评估

| PRD 要求 | 现状 | 状态 |
| :--- | :--- | :--- |
| "今日综合评估"入口 | 无 | ❌ 整体未实现 |
| 组装饮食/训练/体重上下文 | 无 | ❌ |
| 结果存入 AdviceLog + 历史查看 | 表/DAO 已建，无业务调用 | ❌ |

### 模块五：AI 对话 Agent

| PRD 要求 | 现状 | 状态 |
| :--- | :--- | :--- |
| 聊天界面（类微信气泡、多轮） | ChatScreen + LazyColumn | ✅ |
| 滑动窗口（最近 10 条） | ChatViewModel + DAO | ✅ |
| 图片上传/拍照 | 额外实现 | ✅（超出 PRD） |
| **本地意图路由（关键词 → 精准查询）** | domain/router 仅空壳 | ❌ 未实现 |
| **精准数据注入 + 长短期记忆分层** | 无，直接发历史+问题 | ❌ 未实现 |

### 模块六：数据看板

| PRD 要求 | 现状 | 状态 |
| :--- | :--- | :--- |
| 体重录入 | 看板弹窗 | ✅ |
| 体重折线图（7/30/90 天） | Vico 折线图 | ✅ |
| **每日热量柱状图** | 无 | ❌ 未实现 |
| 智能周报（未来扩展） | 无 | 暂不纳入 |

### 模块七：设置与自定义中心

| PRD 要求 | 现状 | 状态 |
| :--- | :--- | :--- |
| 视觉/文本两套 API 配置（URL/Key/模型） | 设置页 | ✅ |
| **自定义食物库 CRUD 页面** | DAO/Repository 已备好，"食物管理"为死按钮 | ❌ 未实现 |
| 数据导出（JSON） | 有实现，但存在缺陷（见 D2） | ⚠️ 不可靠 |
| 数据导入（JSON） | 有实现，但只删不导（见 D1） | ❌ 实质未实现 |
| 清理旧照片 | 设置页 + PhotoCleanupWorker | ✅ |
| 暴躁教练通知（开关/时间/语录） | CoachNotificationWorker + 语录管理弹窗 | ✅ |
| **桌面小组件（Glance）** | 仅空包，manifest 无注册 | ❌ 未实现 |

### PRD 未规划但已实现的功能（额外加分项）

- AI 生成周训练计划（含用户档案、兜底 3/4/5 分化计划）
- 训练计划日视图/周视图（HorizontalPager）
- 动作库（170+ 动作 + GIF 动图 + 分步教学详情页）
- 训练档案（身高/体重/目标/经验/器材/每周天数）
- 聊天带图片

---

## 四、代码质量评估

### 4.1 优点（应继续保持）

1. **架构分层清晰**：data / ui / worker / util 职责明确，Repository 收敛数据访问，ViewModel 管理状态。
2. **技术栈现代且选型合理**：Compose M3、Room、DataStore、Retrofit、WorkManager、Navigation Compose 均为当前 Android 标准方案。
3. **命名规范、注释友好**：类名/函数名/字段名语义清楚，中文注释便于快速理解业务。
4. **编译通过、可维护性底子好**：6263 行代码一次编译通过，没有语法垃圾。
5. **错误处理方向正确**：AI 调用使用 `Result` 封装，识别状态用 sealed class，数据流用 Flow/StateFlow。
6. **文档完整**：PRD、CLAUDE.md 技术文档齐全，Git 提交信息描述清晰。

### 4.2 问题分级清单

**P0 —— 数据安全 / 现有功能错误（必须尽早修复）**

| 编号 | 问题 | 位置 | 影响 |
| :--- | :--- | :--- | :--- |
| D1 | 导入功能只清空数据、不写入任何记录 | DashboardViewModel.kt:124、SettingsViewModel.kt:116 | "导入恢复"会删光用户数据 |
| D2 | 导出直接写 `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)` | SettingsViewModel.kt:104、DashboardViewModel.kt:110 | targetSdk 30+ 该路径不可写，导出大概率失败 |
| D3 | 编辑记录时重置时间戳/日期并清空图片路径 | DietViewModel.kt:162、TrainingViewModel.kt:322 | 编辑昨天的记录会跑到今天，图片丢失 |
| D4 | 数据库 version=4 但无 Migration，且 `fallbackToDestructiveMigration(false)` | AppDatabase.kt:61 | 旧版本升级时 schema 变化直接闪退 |
| D5 | AI 识别失败时同时显示错误卡片并跳转空确认页 | DietViewModel.onPhotoTaken | 用户流程混乱，错误状态残留 |

**P1 —— 架构 / 写法问题（产品化前处理）**

| 编号 | 问题 | 位置 |
| :--- | :--- | :--- |
| P1-1 | suspend 函数中临时 `stateIn(...).value` 取数据（10+ 处），应改用 `first()` | TrainingViewModel.kt:94/96、DashboardViewModel.kt:97-107、SettingsViewModel.kt:95-100 |
| P1-2 | 死代码与占位：Converters 未注册、`toBase64` 无人调用、AiRepository 空 if、MealTemplate/AdviceLog 表建而不用、设置页"食物管理"死按钮 | Converters.kt、ImageCompressor.kt:60、AiRepository.kt:112、SettingsScreen.kt:133 |
| P1-3 | 巨型文件 + 重复代码：TrainingScreen 755 行装 6 个组件；Onboarding/Profile 弹窗、Add/Edit 训练弹窗高度重复；两套导入导出逻辑不一致 | TrainingScreen.kt、DashboardScreen.kt、SettingsScreen.kt |
| P1-4 | Compose 组合期直接写 state；数字输入框 `toIntOrNull()?.let` 导致无法清空 | FoodConfirmScreen.kt:58 |
| P1-5 | 零业务测试：仅模板测试（2+2、包名）；无 lint/ktlint/detekt 配置 | app/src/test、app/src/androidTest |

**P2 —— 整洁度 / 工程化（可延后）**

| 编号 | 问题 | 位置 |
| :--- | :--- | :--- |
| P2-1 | ChatScreen 缩进混乱、import 顺序不统一 | ChatScreen.kt |
| P2-2 | 主题仍为模板默认紫色，未定制品牌色 | ui/theme |
| P2-3 | API Key 明文存储于 DataStore | AppPreferences.kt |
| P2-4 | MainActivity 用旧式 `requestPermissions` 请求通知权限 | MainActivity.kt |
| P2-5 | `windowSoftInputMode="adjustNothing"`，键盘可能遮挡输入 | AndroidManifest.xml |
| P2-6 | release 未启用 R8/minify（`optimization.enable=false`） | app/build.gradle.kts |
| P2-7 | 16.4MB GIF 全部打包进 APK，包体偏大 | assets/videos |
| P2-8 | TrainingViewModel 的 `todayDate` 在创建时固定，跨天不刷新 | TrainingViewModel.kt:34 |

---

## 五、风险与注意事项

1. **数据丢失风险**：当前最高风险点是 D1（导入=清库）和 D4（升级闪退），开发期数据量小，修复成本低，建议本周内处理。
2. **AI 成本与稳定性**：识别/对话/计划全部依赖用户自配 API Key；目前无超时重试策略、无请求频率限制，网络异常时只提示不崩溃（可接受）。
3. **包体膨胀**：GIF 资源 16.4MB，长期建议压缩或按需下载。
4. **数据库 schema 已迭代到 v4**：后续每次改表都应同步 Migration，否则发布后升级必崩。

---

## 六、后续路线建议

| 阶段 | 内容 | 预计工作量 |
| :--- | :--- | :--- |
| 近期（P0） | 修复 D1-D5 五个数据/流程缺陷 | 1-2 天 |
| 中期（P1） | 补齐核心缺失功能：每日评估、意图路由、食物库管理、餐食模板、热量柱状图、小组件、倒计时增强 | 6-9 天 |
| 产品化（P2） | 测试补齐、lint/ktlint、代码整理、主题定制、R8、密钥加密、包体优化 | 3-5 天 |

详细未完成项与验收标准见《[后期开发需求文档](FUTURE_REQUIREMENTS.md)》。
