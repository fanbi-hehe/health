# 私人增重助手 —— 开发维护文档

| 项目 | 内容 |
| :--- | :--- |
| 文档版本 | V1.0 |
| 编写日期 | 2026-08-08 |
| 当前应用版本 | 0.26（versionCode 26） |
| 数据库版本 | 8（完整 Migration 链 1→8） |

---

## 一、项目概览

Android 本地优先的增重/健身助手：AI 拍照识别食物、饮食记录与宏量管理、训练记录与 AI 训练计划、AI 对话教练（function calling）、GPS 运动与系统步数、热量评估看板、暴躁教练通知、桌面小组件。

**核心哲学**：离线优先、数据本地（Room + DataStore）、AI 仅做识别/建议/工具调用，所有写库操作由本地白名单校验后执行。

**当前功能清单（v0.26）**

- 饮食：拍照识别（v0.1 Prompt）、手动录入（含宏量）、食物自动补全、餐食模板、日期回看 + 月历跳转、宏量显示
- 训练：结构化记录、历史动作推荐、组间休息前台服务（提示音/震动/常驻通知）、动作库（170+ 动作 + GIF 教学）、AI 生成 7 天训练计划、记录日期回看
- 运动：GPS 跑步/骑行/步行（前台服务、距离/配速/轨迹入库）、系统步数（传感器按天记账）、运动统计独立 Tab、手动补录
- AI 对话：意图路由 + 精准数据注入、function calling 工具（记录训练/添加修改食物/生成计划，只写不删）、图片消息
- 看板：热量评估（摄入/运动/步数/净摄入/缺口/BMR/宏量）、体重折线、近 7 天热量柱状图（自绘）、AI 每日复盘（含语言模型宏量估算）、备份恢复、训练档案
- 其他：自定义食物库管理、暴躁教练通知（时间/语录可配）、Glance 桌面小组件、日历组件

---

## 二、技术栈

| 层 | 技术 | 版本 |
| :--- | :--- | :--- |
| 语言 | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.01 |
| 导航 | Navigation Compose（单 Activity） | 2.8.0 |
| 数据库 | Room（10 表） | 2.7.1 |
| 配置 | DataStore Preferences | 1.0.0 |
| 网络 | Retrofit + OkHttp + Gson | 2.9.0 / 4.12.0 |
| AI API | OpenAI 兼容（智谱 GLM / DeepSeek 可配） | — |
| 图片 | Coil + coil-gif | 2.6.0 |
| 后台 | WorkManager + 前台服务 | 2.9.0 |
| 小组件 | Glance | 1.0.0 |
| 构建 | Gradle + AGP + KSP | 9.5.0 / 9.3.0 / 2.2.10-2.0.2 |

> 图表已改为 Compose Canvas 自绘（不再依赖 Vico），依赖仍保留在构建文件中但无代码引用，可清理。

---

## 三、项目结构

```
com.example.health/
├── HealthApp.kt            # Application：初始化 DB/Repository/WorkManager/内置数据导入
├── MainActivity.kt         # 单 Activity，通知权限请求
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt  # Room v8，注册 Migrations.ALL
│   │   ├── Migrations.kt   # 1→8 完整迁移链（改表必看）
│   │   ├── dao/            # 10 个 DAO
│   │   └── entity/         # 10 个实体
│   ├── preference/AppPreferences.kt   # DataStore（AI 配置/档案/目标/步数基线/语录）
│   ├── remote/api/ApiService.kt       # Retrofit（@Url 动态地址 + 自定义 Header）
│   ├── remote/dto/ChatDto.kt          # ChatCompletion DTO + Tool/FunctionSpec/ToolCall
│   └── repository/
│       ├── AiRepository.kt            # 识别/对话/工具调用/宏量估算
│       ├── BackupRepository.kt        # MediaStore 导出 + 事务导入（10 表）
│       ├── FoodRepository.kt          # 食物库 CRUD + 内置导入
│       └── ExerciseRepository.kt      # 动作库 CRUD + 内置导入
├── domain/
│   ├── action/             # AI 工具层：ToolDefinitions / ToolExecutor / UserActionParser / UserActionExecutor
│   ├── calorie/CalorieCalculator.kt   # BMR(Mifflin-St Jeor)/Keytel/MET/步数估算/心率区间
│   ├── context/UserContextBuilder.kt  # 意图→精准数据注入（长短期记忆分层）
│   ├── plan/               # 训练计划模型 + TrainingPlanGenerator（训练页与 AI 对话共用）
│   └── router/             # IntentQuery / IntentRouter（关键词意图路由）
├── ui/
│   ├── navigation/         # AppNavigation（5 Tab）+ BottomNavItem
│   ├── diet/               # 饮食主页/确认页/ViewModel（含模板、日期回看）
│   ├── training/           # 训练页/计划/动作库/休息计时（Service + Controller）
│   ├── activity/           # GPS 运动记录页（GpsTrackController）
│   ├── stats/              # 运动统计 Tab（步数 + 运动汇总）
│   ├── chat/               # AI 对话页（工具调用 + 上下文注入）
│   ├── dashboard/          # 看板（热量评估/图表/AI 复盘/备份）
│   ├── settings/           # 设置 + 食物库管理
│   ├── components/         # CalendarPickerDialog（月历）/ ScrollableDropdown
│   └── theme/
├── widget/CalorieWidget.kt # Glance 桌面小组件（热量进度）
├── worker/                 # 后台任务与前台服务
└── util/                   # ImageCompressor / StepCounterManager
```

---

## 四、数据层

### 4.1 Room 表（v8，10 张）

| 表 | 用途 | 关键字段 |
| :--- | :--- | :--- |
| diet_record | 饮食记录 | foodName, weightG, caloriesKcal, proteinG, carbsG, fatG, mealType, timestamp, imagePath |
| training_record | 训练记录 | date, timestamp, bodyParts, exerciseName, sets, reps, weightKg, notes |
| body_weight | 体重 | date, weightKg |
| chat_message | 聊天 | role, content, imagePath, timestamp |
| advice_log | AI 每日评估 | date, requestSnapshot, aiResponse |
| food_library | 食物库 | name, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, isCustom |
| meal_template | 餐食模板 | templateName, itemsJson |
| exercise_library | 动作库 | name, bodyPart, equipment, muscleGroup, target, instructions, image, gifUrl, isCustom 等 |
| activity_record | 运动记录 | type, startTime, durationMinutes, caloriesKcal, distanceMeters, avgPace, routeJson, source |
| daily_step_count | 每日步数 | date(PK), steps, caloriesKcal |

### 4.2 迁移约定（重要）

- `AppDatabase.version` 与 `app/schemas/.../*.json` 由 KSP 自动导出；
- **改表结构必须**：① 改 entity；② version+1；③ 在 `Migrations.kt` 追加 `MIGRATION_x_y` 并注册到 `ALL`；④ 编译生成新 schema JSON 并提交。
- 已移除 destructive fallback：缺失 Migration 会抛异常而不是清库，**禁止恢复清库策略**。
- 可用 `.claude-tasks/migration_check.py` 本地模拟迁移并对照 schema 校验（Python + sqlite3）。

### 4.3 DataStore 键位

`app_settings.preferences_pb`（已从云备份排除，含 API Key）：

- AI：vision/text 各一套 `api_base_url` / `api_key` / `model`
- 档案：身高/体重/年龄/性别/目标/经验/器材/每周天数/onboarded
- 目标：target_weight_kg / target_daily_calories
- 训练计划：training_plan_json
- 步数基线：step_base_total / step_base_date（轻量档记账）
- 通知：开关/提醒时间/暴躁语录 JSON
- 内置数据初始化标记：foods_initialized / exercises_initialized

---

## 五、AI 集成架构

### 5.1 三大能力

1. **食物识别（视觉模型）**：`AiRepository.recognizeFood(File)`，Prompt 与 v0.1 完全一致（只要求名称/重量/热量，保证多食物识别稳定）；识别后不等待宏量。
2. **宏量估算（语言模型）**：`AiRepository.estimateMacros(List<RecognizedFood>)`——在**每日 AI 复盘**时对宏量为 0 的记录批量估算，结果只进总结上下文、不写库；失败静默降级。
3. **对话教练（function calling）**：`AiRepository.chatCompletionWithTools(...)`，工具定义见 `ToolDefinitions.coachTools`：
   - `record_training`：记录训练（今日已有同动作不重复）
   - `add_food`：添加食物（可带每 100g 宏量；`amount_g` 带分量时同时记饮食）
   - `update_food`：修改食物热量
   - `generate_training_plan`：生成并保存 7 天计划
   - **安全**：无删除工具；`ToolExecutor` 白名单 + 参数范围校验；模型不支持 tools 时自动降级普通对话 + 正则兜底（`UserActionParser`）。

### 5.2 意图路由

`IntentRouter` 把用户问题映射为 5 类：动作进度 > 运动/步数 > 热量 > 整体趋势 > 档案 > 闲聊；`UserContextBuilder` 按意图查询 DAO 并注入"用户近期数据"（近 3 天详细、7 天以上仅统计）。

### 5.3 训练计划生成

`TrainingPlanGenerator`（训练页按钮与 AI 对话工具共用）：读档案/历史/动作库 → 调文本模型（maxTokens 4096）→ JSON 校验 → 存 DataStore；失败用内置 3/4/5 分化兜底。

---

## 六、关键流程

| 流程 | 链路 |
| :--- | :--- |
| 拍照识别 | DietScreen 相机 → FileProvider → ImageCompressor（v0.1 解码）→ recognizeFood → FoodConfirmScreen |
| 手动录入 | 补全（食物库）→ 宏量可填 → DietRecord（按选中日期） |
| AI 记录训练 | Chat → ToolExecutor.record_training → 查今日 → 写入 |
| GPS 运动 | ActivityScreen → GpsTrackService（前台 location）→ Haversine 距离/配速 → ActivityRecord |
| 步数 | StepCounterManager（传感器累计值 + 基线差值）→ daily_step_count；看板可见每 30s 自动同步 |
| 每日复盘 | 看板 → 今日饮食/训练/运动/体重 + 宏量估算 → AdviceLog 存档 |
| 备份 | BackupRepository：MediaStore 导出 JSON（10 表）→ 事务校验导入 |
| 提醒 | CoachNotificationWorker 链式调度；改时间会立即重排 |

### 前台服务

| 服务 | 类型 | 说明 |
| :--- | :--- | :--- |
| RestTimerService | specialUse | 组间休息倒计时，通知常驻，结束提示音+震动 |
| GpsTrackService | location | GPS 采集（5s/10m，精度 30m 过滤），结束入库 |

> 权限：`FOREGROUND_SERVICE_LOCATION` 等已声明；新增 location 类前台服务需同步补权限。

---

## 七、构建 / 测试 / 打包

```bash
# JDK 使用 Android Studio JBR
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'

.\gradlew.bat :app:compileDebugKotlin    # 仅编译
.\gradlew.bat :app:testDebugUnitTest     # JVM 单测（当前 70+ 用例）
.\gradlew.bat :app:assembleDebug         # 打包 APK
```

- APK 输出：`app/build/outputs/apk/debug/app-debug.apk`；发布副本放 `dist/health-vX.Y.apk`（`dist/` 已 gitignore）
- 测试覆盖：热量计算、意图路由、工具执行器、动作解析、迁移（脚本校验）
- **发布前待办**：release 签名 + R8、数据库再验证一轮真机升级、lint/ktlint 接入

---

## 八、开发约定

1. 每个功能段完成后：编译 + 单测 + `git commit`（消息带功能名，如 `Feat:` / `Fix:` / `Perf:` / `Release:`）
2. 改表必写 Migration（见 4.2），schema JSON 必须提交
3. AI 工具只加"写/改"白名单，禁止提供删除工具；参数一律范围校验
4. 中文注释；包职责：`domain` 纯逻辑、`data` 数据、`ui` 界面
5. 大段自然语言解析优先走 function calling，不要堆正则（`UserActionParser` 仅作降级兜底）
6. 图表类轻量 UI 优先 Compose Canvas 自绘，避免引入重型图表库
7. 发布版本号：`versionCode` +1、`versionName` 语义化（当前 0.26），同步更新 `dist/` 与本文档

---

## 九、已知限制与后续路线

| 项 | 状态 |
| :--- | :--- |
| 米环 7 心率接入（Keytel 运动消耗） | 延期：公式与工具已就绪，缺 BLE 心率广播数据源 |
| 相册/多图识别 | 已回退（v0.13 引入问题）；如恢复用老式文件选择器实现 |
| 训练计划两套入口 | 训练页按钮与 AI 工具逻辑已共享 `TrainingPlanGenerator`，代码可再收敛 |
| release 构建 | 未配置签名/R8，仅 debug 分发 |
| 真机回归清单 | 建议整理：GPS 精度、步数跨天、小米后台策略、通知 |
| 测试覆盖 | 核心逻辑已覆盖；DAO/UI/备份导入导出建议补 instrumentation 测试 |
| 数据库 | v8 + 1→8 迁移已实测通过；**后续每次改表严格执行 4.2 约定** |

---

## 十、常见问题排查

1. **拍照识别不工作**：先确认 `ImageCompressor` 未被人为改回采样/EXIF 版（v0.16 后保持 v0.1 解码）；再检查视觉 API Key/模型
2. **AI 工具没执行**：看对话页 Toast——"已写入/已存在/参数错误/未知操作"；工具集变更后模型可能需要几轮才稳定
3. **步数不准**：轻量档限制（仅打开 App/看板轮询时同步）；传感器重启会重置基线；不常驻后台
4. **数据库升级闪退**：检查 `Migrations.ALL` 是否覆盖该版本路径；用 migration_check.py 验证
5. **看板卡顿**：图表已自绘，若复现检查是否重新引入 Vico/大列表；VM 已提升到 Activity 级
