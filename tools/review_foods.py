# -*- coding: utf-8 -*-
"""审查内置食物库：按分类列出全部条目，并标记可疑数值。"""

import json
from collections import OrderedDict
from pathlib import Path


def main() -> None:
    audit = json.loads(
        Path(r"D:\app\tools\builtin_foods_audit.json").read_text(encoding="utf-8")
    )
    groups: "OrderedDict[str, list]" = OrderedDict()
    for r in audit:
        groups.setdefault(r["sourceCategory"], []).append(r)

    suspicious_high = []
    suspicious_low = []
    high_ok_cats = {"油脂類", "堅果及種子類", "糕餅點心類", "糖類", "調味料及香辛料類"}
    low_ok_cats = {"嗜好性飲料類", "調味料及香辛料類", "蔬菜類", "藻類"}

    lines = []
    for cat, rows in groups.items():
        lines.append(f"\n===== {cat} ({len(rows)}) =====")
        for r in rows:
            lines.append(f"{r['name']}\t{r['caloriesPer100g']}\t<- {r['sourceName']}")
            if r["caloriesPer100g"] >= 500 and cat not in high_ok_cats:
                suspicious_high.append(r)
            if r["caloriesPer100g"] <= 10 and cat not in low_ok_cats:
                suspicious_low.append(r)

    Path(r"D:\app\tools\food_review.txt").write_text(
        "\n".join(lines), encoding="utf-8"
    )
    print("已生成 food_review.txt")
    print("\n== 可疑高热量（非油脂/坚果/糕饼/糖类 >=500）==")
    for r in suspicious_high:
        print(f"  {r['name']} {r['caloriesPer100g']} <- {r['sourceName']} ({r['sourceCategory']})")
    print("\n== 可疑低热量（非饮料/调味/蔬菜/藻类 <=10）==")
    for r in suspicious_low:
        print(f"  {r['name']} {r['caloriesPer100g']} <- {r['sourceName']} ({r['sourceCategory']})")


if __name__ == "__main__":
    main()
