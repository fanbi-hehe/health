# 内置训练动作库数据来源说明

## 数据来源

内置动作库共 **172 个常见训练动作**，数据来自：

- **Exercises Dataset**（MIT 许可，代码与数据均可自由使用）
- 仓库：<https://github.com/hasaneyldrm/exercises-dataset>
- 说明：1324 个动作的开放数据集，含身体部位、目标肌群、器械、多语言分步说明（含中文）与媒体文件。

本项目从中精选 172 个高频动作，翻译为中文动作名，并保留源数据集的字段结构。

## 生成方式

```bash
# 下载原始数据集（1324 个动作）
curl -o exercises_raw.json https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/data/exercises.json

# 精选并生成内置动作库 + 审计文件
python tools/build_exercises.py exercises_raw.json tools/exercises_final.json
```

最终文件：

| 文件 | 说明 |
| :--- | :--- |
| `app/src/main/assets/exercises.json` | App 内置动作库（172 条，源格式） |
| `tools/exercises_audit.json` | 每条动作的原始英文名/英文肌群等（可追溯） |
| `tools/build_exercises.py` | 动作筛选生成脚本 |

## 字段说明（与源数据集格式一致）

```json
{
  "id": "0025",
  "name": "杠铃卧推",
  "category": "胸",
  "body_part": "胸",
  "equipment": "杠铃",
  "instructions": { "zh": "..." },
  "instruction_steps": { "zh": ["..."] },
  "muscle_group": "胸肌",
  "secondary_muscles": ["肱三头肌", "三角肌"],
  "target": "胸肌",
  "media_id": "EIeI8Vf",
  "image": "images/0025-EIeI8Vf.jpg",
  "gif_url": "videos/0025-EIeI8Vf.gif",
  "attribution": "© Gym visual — https://gymvisual.com/",
  "created_at": "2026-03-18T12:31:32.88353+00:00"
}
```

- 字段结构与源仓库 `data/exercises.json` 完全一致
- 说明文字只保留中文（`instructions.zh`、`instruction_steps.zh`），其他语言已删除
- 标签值已中文化：`category` / `body_part` / `equipment` / `muscle_group` / `secondary_muscles` / `target`
- `category` / `body_part`：胸 / 背 / 腿 / 肩 / 手臂 / 核心 / 全身 / 有氧（有氧动作即 `body_part = "有氧"`）
- `image` / `gif_url`：相对路径，指向源仓库的 `images/` 和 `videos/` 目录，文件名规则为 `id-media_id.jpg|gif`
- 原始英文动作名、英文肌群、有氧/无氧标记保留在 `tools/exercises_audit.json` 供追溯

## 图片/动图落地（已执行）

源仓库每个动作都有 180×180 缩略图（jpg）和动画（gif），文件名与 JSON 中的 `image` / `gif_url` 一一对应。

已下载 **344 个文件**（172 张缩略图 + 172 个动图），实际占用 **17.5 MB**：

```text
app/src/main/assets/images/  172 个 jpg，约 1.1 MB
app/src/main/assets/videos/  172 个 gif，约 16.4 MB
```

与 `exercises.json` 中的路径一一对应，校验 0 缺失。

### 方案 A：Assets 目录 + 路径引用（推荐）

1. 按 JSON 中列出的 172 个 `image` / `gif_url`，从源仓库下载对应文件（共 344 个）：

   ```text
   https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/images/0025-EIeI8Vf.jpg
   https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0025-EIeI8Vf.gif
   ```

   （如果 raw 域名不通，用 `https://gh-proxy.com/` 前缀代理下载）

2. 放入 App 工程：

   ```text
   app/src/main/assets/images/0025-EIeI8Vf.jpg
   app/src/main/assets/videos/0025-EIeI8Vf.gif
   ```

3. Room 表里存 JSON 中的路径字符串（如 `images/0025-EIeI8Vf.jpg`），UI 用 Coil 直接加载：

   ```kotlin
   AsyncImage(
       model = "file:///android_asset/${record.imagePath}",
       contentDescription = record.exerciseName
   )
   ```

4. 体积估算：180×180 缩略图约 10~30KB/张，GIF 约 50~200KB/个，172 组文件预计 **10~40MB**（若下载全量 1324 组约为 80~300MB，不推荐全量入库）。

### 方案 B：首次启动复制到应用私有目录

Assets 方案在需要时把 `images/`、`videos/` 复制到 `filesDir/exercise_media/`，之后统一用文件路径加载，方便后续清理/替换，其余不变。

### 不建议方案

- 不要把图片字节直接存进 Room（BLOB）：172 组媒体会让数据库膨胀数十 MB，查询变慢。
- 不建议运行时从网络拉取：与“离线优先、数据私有”的定位冲突。

## 与训练记录模块的对接建议

- PRD 中 `TrainingRecord.bodyParts` 是多选部位（胸、背、腿、肩、手臂、核心），动作库的 `body_part` 可直接作为主部位，复合动作（如硬拉归“背”）已指定主部位。
- 有氧动作 `body_part` 为“有氧”，训练记录筛选时可单独显示。
- 用户手动输入自定义动作时，可以同时写入历史动作推荐，不受内置库限制。
