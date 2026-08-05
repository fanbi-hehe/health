# -*- coding: utf-8 -*-
"""检查生成的内置食物库 JSON（调试用）。"""

import json
from pathlib import Path


def main() -> None:
    audit = json.loads(
        Path(r"D:\app\tools\builtin_foods_audit.json").read_text(encoding="utf-8")
    )
    foods = [
        {"name": r["name"], "caloriesPer100g": r["caloriesPer100g"]}
        for r in audit
    ]
    print("总数:", len(foods), "去重:", len({f["name"] for f in foods}))
    print("\n热量 <= 5:")
    for f in foods:
        if f["caloriesPer100g"] <= 5:
            print(" ", f)
    print("\n热量 >= 800:")
    for f in foods:
        if f["caloriesPer100g"] >= 800:
            print(" ", f)
    keys = [
        "米饭", "馒头", "面条", "油条", "玉米", "红薯", "马铃薯",
        "鸡胸肉", "鸡腿", "猪肉", "猪里脊", "牛肉", "牛腱", "羊肉",
        "鸡蛋", "鲑鱼", "虾", "豆腐", "豆浆", "黄豆",
        "苹果", "香蕉", "西瓜", "牛油果",
        "全脂牛奶", "酸奶", "奶酪", "黄油",
        "花生", "核桃", "腰果",
        "可乐", "奶茶", "啤酒",
        "酱油", "盐", "白糖", "蜂蜜",
        "白吐司", "全麦吐司", "方便面", "水饺",
    ]
    by_name = {f["name"]: f["caloriesPer100g"] for f in foods}
    print("\n关键项抽查:")
    for k in keys:
        print(f"  {k}: {by_name.get(k, 'MISSING')}")


if __name__ == "__main__":
    main()
