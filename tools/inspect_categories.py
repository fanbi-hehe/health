# -*- coding: utf-8 -*-
"""查看台湾食物数据集中指定分类/关键词的名称（调试用）。"""

import json
import sys
from pathlib import Path

import opencc


def main() -> None:
    rows = json.loads(Path(r"D:\app\tools\all_foods_raw.json").read_text(encoding="utf-8"))
    cc = opencc.OpenCC("t2s")
    # 要检查的关键词/分类（写死在脚本里，避免命令行编码问题）
    args = sys.argv[1:] or [
        "菇類",
        "堅果及種子類",
        "糖類",
        "芋头糕",
        "碗粿",
        "粽子",
        "卤肉饭",
        "白吐司",
        "全麦吐司",
        "牛油果",
        "蓝莓",
        "香蕉",
        "火龙果",
        "榴莲",
        "桑椹",
        "柿子",
        "牛腩",
        "牛腱",
        "鸡爪",
        "鲈鱼",
        "鳗鱼",
        "大白菜",
        "韭黄",
        "生菜",
        "大头菜",
        "青椒",
        "黄瓜",
        "节瓜",
        "佛手瓜",
        "脱脂牛奶",
        "鲜奶油",
        "黄油",
        "奇亚籽",
        "葡萄汁",
        "高粱酒",
        "威士忌",
        "冰糖",
        "芝麻酱",
        "番茄酱",
        "奶黄包",
        "盐酥鸡",
        "卤味",
        "关东煮",
        "咖喱饭",
        "蛋包饭",
        "牛丼",
        "味噌汤",
        "酸辣汤",
        "蛋花汤",
        "四神汤",
        "红豆汤",
        "绿豆汤",
        "鹰嘴豆",
        "豆包",
        "豆腐乳",
        "臭豆腐",
        "冻豆腐",
        "烤麸",
        "猪里脊",
        "猪五花",
        "牛里脊",
        "羊里脊",
        "羊腿",
        "鸭肉",
        "兔肉",
        "沙丁鱼",
        "鲫鱼",
        "鲤鱼",
        "桃子",
        "柚子",
        "橘子",
        "马芬",
        "沙琪玛",
        "爆米花",
        "贝果",
    ]
    for arg in args:
        if arg in {r["category"] for r in rows}:
            print(f"\n===== 分类: {arg} =====")
            for r in rows:
                if r["category"] == arg:
                    print(f"{r['name']}\t{r['kcal']}\t{cc.convert(r['name'])}")
        else:
            print(f"\n===== 关键词: {arg} =====")
            for r in rows:
                s = cc.convert(r["name"])
                if arg in s:
                    print(f"{r['category']} | {r['name']}\t{r['kcal']}\t{s}")


if __name__ == "__main__":
    main()
