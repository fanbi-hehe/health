# 内置食物库数据来源说明

## 数据来源

内置食物库共 **345 种常见食物**，热量数据来自：

- **台湾卫生福利部食品药物管理署《食品营养成分资料集》**
- 数据集页面：<https://data.gov.tw/dataset/8543>
- 镜像下载：<https://scidm.nchc.org.tw/dataset/best_wish8543>
- 授权：**政府资料开放授权条款-第 1 版**（允许自由使用、复制、修改，需注明出处）

## 生成方式

```bash
# 1. 从镜像下载 CSV（ZIP 内为 20_2.csv，约 52MB）
# 2. 解析全部食物热量数据
python tools/build_food_db.py <20_2.csv> tools/all_foods_raw.json

# 3. 筛选 346 种常见食物并输出内置库 + 审计文件
python tools/select_foods.py tools/all_foods_raw.json tools/builtin_foods_final.json
```

最终文件：

| 文件 | 说明 |
| :--- | :--- |
| `app/src/main/assets/builtin_foods.json` | App 内置食物库（345 条） |
| `tools/builtin_foods_audit.json` | 每条数据对应的原始记录（可追溯） |
| `tools/all_foods_raw.json` | 全量 2080 种带热量食物 |
| `tools/build_food_db.py` | 原始 CSV 解析脚本 |
| `tools/select_foods.py` | 常见食物筛选脚本 |

## 字段说明

```json
{
  "name": "米饭",
  "caloriesPer100g": 183,
  "isCustom": false
}
```

- `name`：大陆常用名称（已从繁体转换，并做别名映射，如 凤梨→菠萝、芭乐→番石榴、奇异果→猕猴桃、酪梨→牛油果、高丽菜→卷心菜、北蕉→香蕉）
- `caloriesPer100g`：每 100 克可食部的热量（kcal）
- `isCustom`：`false` 表示内置数据，用户自定义食物为 `true`

## 注意事项

- 数据为台湾食品成分数据库版本（2020 年发布），部分数值是平均值/代表值。
- 生熟状态已尽量统一：米饭为熟白饭（183 kcal），面条/米粉/冬粉/面线/荞麦面/通心面为干制品（名称已标注“干”），香菇/木耳/银耳为鲜品（39/38/22 kcal），燕麦为燕麦片（393 kcal），裙带菜为干品（名称已标注）。
- 若需更新，可从上述数据集页面下载最新版 CSV，重新运行两个脚本即可；App 侧只需替换 `assets/builtin_foods.json`。
