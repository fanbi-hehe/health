# 私人增重助手 Health Assistant

一款 Android 本地优先的增重 / 健身助手：AI 拍照识别食物、AI 对话教练、结构化训练记录、AI 生成训练计划、热量评估看板与趋势图。

**核心理念**：离线优先，AI 为副驾驶。除 AI 调用外，所有健康数据（饮食、训练、体重、聊天记录）只保存在手机本地 Room 数据库和 DataStore 中，不上传任何第三方服务器。

## 功能一览

- **饮食记录**：AI 拍照识别食物并估算热量 / 蛋白质 / 碳水 / 脂肪，支持手动录入、智能补全、餐食模板、日期回看与月历跳转
- **训练记录**：结构化记录组数 / 次数 / 重量 / 部位，170+ 动作库（含 GIF 教学），组间休息前台倒计时，AI 生成 7 天训练计划
- **AI 对话教练**：说话即可记录训练、管理食物库、生成计划；按需查询本地数据精准回答；function calling 只写不删
- **运动与热量**：GPS 跑步 / 骑行 / 步行（前台服务）、系统步数统计、热量缺口评估、AI 每日复盘
- **看板与小组件**：热量进度、体重折线、近 7 天热量柱状图、一键备份 / 恢复、Glance 桌面小组件
- **附带模块**：独立计算器 App（calculator/，极简风格，含历史记录与多套配色）

## 模块结构

```
app/        健康助手主应用（Compose 单 Activity）
calculator/ 独立计算器模块
docs/       需求、开发、维护、产品文档
tools/      内置食物库 / 动作库的构建与数据来源脚本
```

主应用代码按 `data / domain / ui / worker / util` 分层，详见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) 与 [CLAUDE.md](CLAUDE.md)。

## 技术栈

| 层 | 技术 |
|---|------|
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3（单 Activity） |
| 数据库 | Room（SQLite，10 张表，完整 Migration 链 1→8） |
| 配置 | DataStore Preferences |
| 网络 | Retrofit + OkHttp + Gson |
| AI API | OpenAI 兼容格式（智谱 GLM / DeepSeek 等均可配置） |
| 后台 | WorkManager + 前台服务 |
| 小组件 | Glance |
| 构建 | Gradle 9.5 + AGP 9.3 + KSP |

## 环境要求与构建

- JDK 17+（Android Studio 自带 JBR 即可）
- Android SDK：compileSdk 37、minSdk 30、targetSdk 37
- Gradle 版本由 `gradlew` wrapper 自动下载（Gradle 9.5.0）

```bash
# 构建 Debug APK
./gradlew :app:assembleDebug

# 构建计算器模块
./gradlew :calculator:assembleDebug

# 运行单元测试
./gradlew test
```

## AI 配置说明

App 不内置任何 API Key。安装后在「设置」页分别配置两套模型（均可指向 OpenAI 兼容接口）：

- **视觉模型**：`api_base_url`、`api_key`、`model`（用于拍照食物识别）
- **文本模型**：`api_base_url`、`api_key`、`model`（用于 AI 对话、训练计划、每日复盘）

密钥只写入本机 DataStore，不会进入代码仓库，也不会上传到任何服务器；请求仅发送到你配置的 `api_base_url`。

## 隐私与安全约定

- 健康数据（饮食、训练、体重、聊天）全部本地存储；备份文件由用户手动导出到手机媒体库
- AI 只能写入 / 修改记录，没有删除权限
- 本仓库**严禁提交**：`local.properties`、`.idea/`、`.claude/`、`.claude-tasks/`、构建产物、签名密钥（`*.jks` / `*.keystore` / `*.p12` 等）、任何真实 API Key 或口令（已通过 `.gitignore` 排除）
- 推送前请运行一次敏感信息扫描，例如：

```bash
rg -n -i "api[_-]?key|secret|password|sk-[A-Za-z0-9]{16,}|AKIA[0-9A-Z]{16}" --hidden -g '!.git/**'
```

## 内置数据来源

- 食物库：台湾政府开放资料「食品营养成分资料集」（[data.gov.tw/dataset/8543](https://data.gov.tw/dataset/8543)），构建脚本见 `tools/build_food_db.py`
- 动作库与 GIF / 图片：[hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset)（Gym visual 资源），构建脚本见 `tools/build_exercises.py`、`tools/download_exercise_media.py`
- 数据出处详见 [tools/DATA_SOURCE.md](tools/DATA_SOURCE.md) 与 [tools/EXERCISES_SOURCE.md](tools/EXERCISES_SOURCE.md)

## 相关文档

- [PRD.md](PRD.md) — 产品需求文档
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) — 开发维护指南
- [docs/PRODUCT_INTRO.md](docs/PRODUCT_INTRO.md) — 产品介绍
- [docs/ISSUES_LOG.md](docs/ISSUES_LOG.md) — 用户反馈与问题记录
- [CLAUDE.md](CLAUDE.md) — 技术约定
