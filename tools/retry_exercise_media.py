# -*- coding: utf-8 -*-
"""重试下载缺失的动作媒体文件（最多 3 轮，换镜像）。"""

from __future__ import annotations

import json
import time
import urllib.request
from pathlib import Path


ASSETS = Path(r"D:\app\app\src\main\assets")
PROXIES = [
    "https://gh-proxy.com/https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/",
    "https://ghfast.top/https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/",
]


def main() -> None:
    data = json.loads((ASSETS / "exercises.json").read_text(encoding="utf-8"))
    media = []
    for ex in data:
        for key in ("image", "gif_url"):
            rel = ex[key]
            local = ASSETS / rel
            if not local.exists() or local.stat().st_size == 0:
                media.append((rel, local))

    print(f"待重试: {len(media)}")
    for round_no in range(1, 4):
        if not media:
            break
        pending = []
        for rel, local in media:
            ok = False
            for proxy in PROXIES:
                try:
                    local.parent.mkdir(parents=True, exist_ok=True)
                    urllib.request.urlretrieve(proxy + rel, local)
                    if local.stat().st_size > 0:
                        ok = True
                        break
                except Exception:
                    continue
            if not ok:
                pending.append((rel, local))
        print(f"第 {round_no} 轮完成，剩余 {len(pending)}")
        media = pending
        if media:
            time.sleep(2)

    if media:
        print("仍失败:")
        for rel, _ in media:
            print("  ", rel)
    else:
        print("全部补齐")


if __name__ == "__main__":
    main()
