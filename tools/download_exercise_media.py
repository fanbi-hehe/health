# -*- coding: utf-8 -*-
"""
下载 exercises.json 中 172 个动作对应的缩略图与动图到
app/src/main/assets/images 与 app/src/main/assets/videos。

先并发 HEAD 估算总大小，超过 2GB 则中止；否则并发下载并校验。

用法：
  python tools/download_exercise_media.py
"""

from __future__ import annotations

import json
import re
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path


ASSETS_ROOT = Path(r"D:\app\app\src\main\assets")
EXERCISES_JSON = ASSETS_ROOT / "exercises.json"
PROXY_PREFIX = "https://gh-proxy.com/https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/"
MAX_TOTAL = 2 * 1024 * 1024 * 1024  # 2GB

SAFE_NAME = re.compile(r"^[0-9A-Za-z]+-[A-Za-z0-9]+\.(jpg|gif)$")
SAFE_DIRS = {"images", "videos"}


def build_media_list() -> list[tuple[str, str, str]]:
    """返回 [(相对路径, 本地绝对路径, URL)]"""
    data = json.loads(EXERCISES_JSON.read_text(encoding="utf-8"))
    media = []
    for ex in data:
        for key in ("image", "gif_url"):
            rel = ex[key]
            parts = rel.split("/")
            if len(parts) != 2 or parts[0] not in SAFE_DIRS or not SAFE_NAME.match(parts[1]):
                raise ValueError(f"非法媒体路径: {rel}")
            local = ASSETS_ROOT / rel
            url = PROXY_PREFIX + rel
            media.append((rel, str(local), url))
    return media


def head_size(url: str) -> int | None:
    req = urllib.request.Request(url, method="HEAD")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return int(r.headers.get("Content-Length", 0))
    except Exception:
        return None


def download(url: str, local: str) -> bool:
    Path(local).parent.mkdir(parents=True, exist_ok=True)
    try:
        urllib.request.urlretrieve(url, local)
        return True
    except Exception:
        return False


def main() -> None:
    media = build_media_list()
    print(f"待下载文件数: {len(media)}")

    # ── 估算 ──
    sizes: dict[str, int] = {}
    with ThreadPoolExecutor(max_workers=24) as pool:
        futures = {pool.submit(head_size, url): rel for rel, _, url in media}
        done = 0
        for fut in as_completed(futures):
            rel = futures[fut]
            try:
                size = fut.result()
            except Exception:
                size = None
            if size is not None:
                sizes[rel] = size
            done += 1
            if done % 50 == 0 or done == len(media):
                print(f"估算进度 {done}/{len(media)}", flush=True)

    img_total = sum(v for k, v in sizes.items() if k.startswith("images/"))
    vid_total = sum(v for k, v in sizes.items() if k.startswith("videos/"))
    total = img_total + vid_total
    missing = [rel for rel, _, _ in media if rel not in sizes]
    print(f"\n可获取文件数: {len(sizes)} / {len(media)}")
    print(f"图片合计: {img_total / 1024 / 1024:.1f} MB")
    print(f"动图合计: {vid_total / 1024 / 1024:.1f} MB")
    print(f"总计: {total / 1024 / 1024:.1f} MB")
    if missing:
        print("HEAD 失败的文件:")
        for rel in missing:
            print("  ", rel)

    if total > MAX_TOTAL:
        print(f"\n超过 2GB（{total / 1024 / 1024 / 1024:.2f} GB），按约定中止下载。")
        sys.exit(2)

    print(f"\n未超过 2GB，开始下载到 {ASSETS_ROOT} ...")
    ok, fail = [], []
    with ThreadPoolExecutor(max_workers=16) as pool:
        futures = {
            pool.submit(download, url, local): (rel, local)
            for rel, local, url in media
        }
        done = 0
        for fut in as_completed(futures):
            rel, local = futures[fut]
            if fut.result():
                ok.append(rel)
            else:
                fail.append(rel)
            done += 1
            if done % 50 == 0 or done == len(media):
                print(f"下载进度 {done}/{len(media)}", flush=True)

    print(f"\n下载完成: 成功 {len(ok)} / {len(media)}")
    if fail:
        print("失败文件:")
        for rel in fail:
            print("  ", rel)
        sys.exit(1)

    # 校验实际大小
    real = sum(Path(ASSETS_ROOT / rel).stat().st_size for rel in ok)
    print(f"实际占用: {real / 1024 / 1024:.1f} MB")


if __name__ == "__main__":
    main()
