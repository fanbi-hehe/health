# 私人增重助手 (Health Assistant) — 技术文档

## 一、项目概述

一款 Android 本地优先的增重/健身助手 App。核心能力：AI 拍照识别食物热量、AI 对话教练、结构化训练记录、AI 生成训练计划、数据看板与趋势图。

**核心理念**：离线优先，AI 为副驾驶。除 AI 调用外，所有数据存储在本地 Room 数据库和 DataStore 中。

## 二、技术栈

| 层 | 技术 | 版本 |
|---|------|------|
| 语言 | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.01 |
| 导航 | Navigation Compose | 2.8.0 (单 Activity) |
| 数据库 | Room (SQLite) | 2.7.1 |
| 配置 | DataStore (Preferences) | 1.0.0 |
| 网络 | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| AI API | OpenAI 兼容格式 (智谱/DeepSeek 等) | — |
| 图表 | Vico (Compose 原生) | 2.0.1 |
| 图片 | Coil + coil-gif | 2.6.0 |
| JSON | Gson | 2.10.1 |
| 后台 | WorkManager | 2.9.0 |
| 构建 | Gradle + AGP + KSP | 9.5.0 / 9.3.0 |

## 三、项目结构

```
com.example.health/
├── HealthApp.kt                  # Application：初始化 DB、Preferences、Repository、WorkManager
├── MainActivity.kt               # 单 Activity 入口，enableEdgeToEdge()
├── domain/
│   ├── router/
│   │   ├── IntentQuery.kt        # 意图密封类：DietCalories / ExerciseProgress / OverallSummary / UserProfile / GeneralChat
│   │   └── IntentRouter.kt       # 关键词/正则意图路由引擎，4 类意图 + 优先级
│   └── context/
│       └── UserContextBuilder.kt  # 按意图查询 DAO → 格式化上下文（长短期记忆分层），供给 AI System Prompt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Room 数据库 (version=5, 开发期 fallbackToDestructiveMigration)
│   │   ├── converter/Converters.kt
│   │   ├── dao/                  # 7 个 DAO 接口（含意图路由所需的多日统计/动作名查询）
│   │   └── entity/               # 8 个实体类
│   ├── preference/
│   │   └── AppPreferences.kt     # DataStore：AI 配置(视觉/文本分离)、目标、档案、语录
│   ├── remote/
│   │   ├── api/ApiService.kt     # Retrofit 接口 (@Url 动态地址)
│   │   └── dto/ChatDto.kt        # ChatCompletion 请求/响应 DTO
│   └── repository/
│       ├── AiRepository.kt       # AI 调用：食物识别(视觉模型) + 聊天/计划(文本模型)
│       ├── FoodRepository.kt     # 食物库：内置 JSON 导入 + 自定义 CRUD
│       └── ExerciseRepository.kt # 动作库：exercises.json 导入 + 自定义 CRUD
├── ui/
│   ├── navigation/
│   │   ├── AppNavigation.kt      # 底部导航 4 Tab + settings/food_confirm/exercise_detail 路由
│   │   └── BottomNavItem.kt      # Tab 定义(饮食/训练/对话/看板)
│   ├── diet/
│   │   ├── DietScreen.kt         # 饮食主页：拍照 → AI 识别 → 确认 + 手动录入 + 记录列表
│   │   ├── DietViewModel.kt      # 相机 URI、AI 识别流程、保存/更新/删除
│   │   └── FoodConfirmScreen.kt  # AI 识别结果确认：图片预览 + 食物编辑 + 保存
│   ├── training/
│   │   ├── TrainingScreen.kt     # 训练主页：3 Tab(计划/记录/动作库) + 录入弹窗 + 编辑弹窗
│   │   ├── TrainingViewModel.kt  # 训练 CRUD + 计划生成(内置兜底) + 动作历史推荐
│   │   ├── TrainingPlanTab.kt    # 训练计划：HorizontalPager 日视图 + 周视图 + AI 生成
│   │   ├── ExerciseDetailScreen.kt # 动作详情：GIF 动图 + 分步教学 + 点击放大
│   │   └── RestTimer.kt          # Canvas 圆环倒计时：±5s 调节 + 结束震动
│   ├── chat/
│   │   ├── ChatScreen.kt         # AI 对话：类微信气泡 + 图片上传 + 拍照
│   │   └── ChatViewModel.kt      # 消息存储 + 意图路由 + 精准数据注入 System Prompt + AI 调用(滑动窗口 10 条) + 图片压缩
│   ├── dashboard/
│   │   ├── DashboardScreen.kt    # 看板：热量进度 + 体重趋势(Vico) + 训练概览 + 备份/恢复 + 档案编辑
│   │   └── DashboardViewModel.kt # 体重/训练/目标数据聚合 + JSON 导入导出
│   ├── settings/
│   │   ├── SettingsScreen.kt     # 设置：视觉/文本双 AI 配置 + 目标 + 食物库 + 备份恢复 + 语录管理
│   │   └── SettingsViewModel.kt  # 配置读写 + 导入导出 + 照片清理
│   ├── components/
│   │   └── ScrollableDropdown.kt # 可滚动下拉菜单(Popup+LazyColumn，替代 Material DropdownMenu)
│   └── theme/
├── worker/
│   ├── PhotoCleanupWorker.kt     # 每天清理 30 天前缓存照片
│   ├── CoachNotificationWorker.kt # OneTime 链式调度：提醒时间检查 + 摄入不足通知 + 随机语录
│   └── NotificationHelper.kt    # 通知渠道创建 + 发送
└── util/
    └── ImageCompressor.kt        # 图片压缩(1080px 长边, 70%质量) + Base64 转换
```

## 四、数据层

### Room 实体 (8 张表)

| 表名 | 关键字段 | 用途 |
|------|---------|------|
| `diet_record` | id, foodName, weightG, caloriesKcal, mealType, timestamp, imagePath | 饮食记录 |
| `training_record` | id, date, bodyParts, exerciseName, sets, reps, weightKg, notes | 训练记录 |
| `body_weight` | id, date, weightKg | 体重记录 |
| `chat_message` | id, role, content, imagePath, timestamp | 聊天消息 |
| `advice_log` | id, date, requestSnapshot, aiResponse | AI 评估日志(预留) |
| `food_library` | id, name, caloriesPer100g, isCustom | 食物库(内置+自定义) |
| `exercise_library` | id, name, bodyPart, equipment, muscleGroup, instructions, instructionSteps, image, gifUrl, isCustom | 动作库(内置+自定义) |
| `meal_template` | id, templateName, itemsJson | 餐食模板(预留) |

### DataStore 键值

- **视觉模型**（食物识别）：`vision_api_base_url`, `vision_api_key`, `vision_model`
- **文本模型**（AI 对话/计划）：`text_api_base_url`, `text_api_key`, `text_model`
- **目标**：`target_weight_kg`, `target_daily_calories`
- **用户档案**：`user_height_cm`, `user_current_weight`, `user_goal`, `user_experience`, `user_equipment`, `user_training_days`, `user_onboarded`
- **训练计划**：`training_plan_json`
- **通知**：`coach_notification_enabled`, `coach_reminder_hour/minute`, `coach_quotes`
- **初始化标记**：`foods_initialized`, `exercises_initialized`

### 首次启动流程
`HealthApp.onCreate()` → `FoodRepository.initializeBuiltinFoodsIfNeeded()` (读 `assets/builtin_foods.json` → Room) + `ExerciseRepository.initializeBuiltinExercisesIfNeeded()` (读 `assets/exercises.json` → Room)

## 五、AI 层

### API 调用方式
使用 Retrofit + `@Url` 动态地址，支持任意 OpenAI 兼容 API。默认配置指向智谱：
- 视觉：`https://open.bigmodel.cn/api/paas/v4/` + `glm-4v-flash`
- 文本：`https://open.bigmodel.cn/api/paas/v4/` + `glm-4-flash`

用户可在设置页独立配置两套 API（比如视觉用智谱、对话用 DeepSeek）。

### AiRepository 方法

| 方法 | 用途 | 使用的配置 |
|------|------|-----------|
| `recognizeFood(File)` | 拍照识别食物 → FoodRecognitionResult | 视觉模型 |
| `chatCompletion(text, imageFile?, history, maxTokens)` | 文本对话/计划生成 → String | 文本模型 |

### 三大 AI 功能调用链

1. **食物识别**：`DietViewModel.onPhotoTaken()` → `ImageCompressor.compress()` → `AiRepository.recognizeFood()` → `FoodConfirmScreen`
2. **AI 对话**：`ChatViewModel.sendMessage()/sendMessageWithImage()` → `AiRepository.chatCompletion()` (滑动窗口 10 条)
3. **训练计划**：`TrainingViewModel.generatePlan()` → 拼接用户档案+训练历史+动作库 prompt → `AiRepository.chatCompletion(maxTokens=4096)` → JSON → 解析或兜底内置计划

## 六、导航

### 底部 4 Tab
`饮食(diet) | 训练(training) | 对话(chat) | 看板(dashboard)`

### 子路由
- `food_confirm` — AI 识别确认页 (共享 DietViewModel)
- `settings` — 设置页 (SettingsViewModel)
- `exercise_detail/{exerciseName}` — 动作详情页 (共享 TrainingViewModel)

### 导航模式
- 单 Activity + Navigation Compose
- 同一流程的屏幕共享 ViewModel（通过 `viewModel()` Activity 级别作用域）

## 七、训练计划模块

### 生成流程
1. 用户填写档案（看板 → 训练档案卡片）
2. 可选：在计划页输入框写自定义需求
3. 点击"AI 生成计划" → 调用文本模型 → 解析 JSON → 保存到 DataStore
4. AI 失败时自动使用内置 3/4/5 分化标准计划兜底

### 计划数据结构 (JSON)
```json
[{"day":"周一","date":"08-11","focus":"胸+三头","exercises":[
  {"name":"杠铃卧推","sets":4,"reps":"8-12","notes":null}
]}]
```

### UI 特性
- HorizontalPager 日视图：左右滑动切换天，中间大卡片展示动作
- 周视图：右上角日历图标切换为紧凑周概览
- 点击动作名 → 跳转 ExerciseDetailScreen (GIF 动图+分步教学)
- "完成"按钮 → 输入实际组数/次数/重量 → 存入 TrainingRecord
- 已完成动作显示 ✅，可手动添加/删除动作

## 八、后台任务

| Worker | 调度方式 | 作用 |
|--------|---------|------|
| PhotoCleanupWorker | PeriodicWorkRequest (每天) | 删除 cache 中 30 天前照片 |
| CoachNotificationWorker | OneTimeWorkRequest 链式 | 提醒时间检查 → 摄入<80% 发通知 → 排下一次 |

## 九、关键设计决策

1. **AI 配置分离**：视觉/文本两套独立的 API Key/URL/Model，可在设置页独立配置
2. **内置数据兜底**：AI 生成计划失败 → 标准 3/4/5 分化方案；食物库 200+ 内置食物；动作库 170+ 内置动作带 GIF
3. **滑动窗口对话**：AI 对话只发最近 10 条历史，节省 Token
4. **路径处理**：`/mnt/d/app` 是 WSL 路径，实际 Windows 路径为 `D:\app`
5. **DB 版本**：version=5，开发期使用 `fallbackToDestructiveMigration(true)` — 改 schema 会清库重建；发布前需补真实 Migration（schema 已导出至 app/schemas）
6. **edge-to-edge**：`enableEdgeToEdge()` 已启用，键盘处理用 `windowSoftInputMode="adjustNothing"` + `imePadding()`
7. **对话意图路由**：ChatViewModel 每次对话经 IntentRouter 识别 4 类意图（热量/动作/趋势/闲聊），按需查询 DAO 注入精准上下文到 System Prompt，实现长短期记忆分层（近 3 天详细 + 7 天以上仅统计）

## 十、快速上手

### 环境要求
- Android Studio (自带 JDK 在 `C:\Program Files\Android\Android Studio\jbr`)
- Android SDK 35, minSdk 30

### 构建命令
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew compileDebugKotlin     # 仅编译
./gradlew assembleDebug          # 打包 APK
```

### 首次运行配置
1. 安装 APK → 打开 App
2. 底部导航 → "饮食" → 右上角齿轮 → 设置页
3. 配置视觉模型 API Key (食物识别) 和文本模型 API Key (对话/计划)
4. 底部导航 → "看板" → 训练档案 → 编辑 → 填写身高体重目标等
5. 训练页 → "计划" Tab → "AI 生成训练计划"

### 内置数据文件
- `assets/builtin_foods.json` — 200+ 常见食物 (名称 + 每100g热量)
- `assets/exercises.json` — 170+ 动作 (含中文分步教学、器械、部位)
- `assets/images/*.jpg` — 动作演示图
- `assets/videos/*.gif` — 动作演示动图

## 十一、常见问题

1. **编译慢** → `settings.gradle.kts` 已配置阿里云镜像；`gradle.properties` 已开启并行+缓存
2. **AI 调用失败** → 检查设置页的 API Key 和模型名是否正确；确认网络能访问 API 地址
3. **GIF 不动** → ExerciseDetailScreen 使用自定义 `ImageLoader+GifDecoder`，确保 `coil-gif` 依赖存在
4. **键盘遮挡** → ChatScreen 使用 `adjustNothing` + `imePadding()` 仅在输入栏，简单 Column 布局
5. **通知不工作** → 首次启动会请求 `POST_NOTIFICATIONS` 权限；Worker 在提醒时间才触发；摄入需 < 目标 80%
6. **数据库 schema 变更** → 改 entity 后递增 AppDatabase.version，`fallbackToDestructiveMigration` 会清库重建
