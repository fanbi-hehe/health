# -*- coding: utf-8 -*-
"""查看动作数据集（按部位分类列出动作名，调试用）。"""

import json
from collections import OrderedDict
from pathlib import Path


def main() -> None:
    data = json.loads(
        Path(r"C:\Users\haiza\AppData\Local\Temp\exercises_raw.json").read_text(
            encoding="utf-8"
        )
    )
    groups: "OrderedDict[str, list]" = OrderedDict()
    for ex in data:
        groups.setdefault(ex["category"], []).append(ex)

    lines = []
    for cat, exs in groups.items():
        lines.append(f"\n===== {cat} ({len(exs)}) =====")
        for ex in exs:
            lines.append(f"{ex['id']}\t{ex['name']}\t{ex['equipment']}\t{ex['target']}")
    Path(r"D:\app\tools\exercise_names.txt").write_text(
        "\n".join(lines), encoding="utf-8"
    )
    print("已生成 exercise_names.txt")


if __name__ == "__main__":
    main()
