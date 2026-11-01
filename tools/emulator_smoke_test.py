#!/usr/bin/env python3
"""Drives the debug APK on an emulator like a person would, and takes screenshots.

Used by .github/workflows/emulator-test.yml. Steps:
home -> a category -> first image -> download -> share -> favorite -> favorites tab,
then a fresh start on a slow (EDGE, 2G-like) connection.
It fails if the catalog doesn't show, no photo loads, the app crashes,
or Download doesn't put a file in the gallery. Screenshots and logs land in emulator-results/.
"""

import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "com.sowarapp.android"
APK = "app/build/outputs/apk/debug/app-debug.apk"
OUT = Path("emulator-results")
OUT.mkdir(exist_ok=True)
problems = []


def adb(*args, check=True):
    result = subprocess.run(["adb", *args], capture_output=True, text=True, errors="replace")
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} failed: {result.stderr}")
    return result.stdout


def screenshot(name):
    with open(OUT / f"{name}.png", "wb") as file:
        file.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)
    print(f"screenshot {name}")


def ui_nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", check=False)
    xml = adb("shell", "cat", "/sdcard/ui.xml", check=False)
    if "<" not in xml:
        return []
    return list(ET.fromstring(xml[xml.index("<"):]).iter("node"))


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def find(label, timeout=20):
    """Waits for an element whose text or content description contains the label."""
    end = time.time() + timeout
    while time.time() < end:
        for node in ui_nodes():
            if label in (node.get("text", "") + " " + node.get("content-desc", "")):
                return node
        time.sleep(1)
    return None


def tap(label, step):
    node = find(label)
    if node is None:
        problems.append(f"{step}: could not find '{label}' on screen")
        screenshot(f"missing_{step}")
        return False
    adb("shell", "input", "tap", *map(str, center(node)))
    return True


def gallery_count():
    out = adb("shell", "content", "query", "--uri", "content://media/external/images/media",
              "--projection", "_display_name", check=False)
    return out.count("sowar_")


def slow_network_check():
    """A fresh install on a slow connection must still download the catalog and show photos."""
    # The emulator console makes the connection slow; if that's unavailable, say so and skip.
    result = subprocess.run(["adb", "emu", "network", "speed", "edge"], capture_output=True, text=True)
    output = result.stdout + result.stderr
    if result.returncode != 0 or "KO" in output or "error" in output.lower():
        print(f"slow network: could not slow the emulator down ({output.strip()!r}); skipped")
        return
    adb("emu", "network", "delay", "edge", check=False)
    adb("shell", "pm", "clear", PACKAGE)  # no saved catalog, like a new user
    adb("logcat", "-c")
    started = time.time()
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    if find("الأقسام", timeout=60) is None:
        problems.append("slow network: the home screen never appeared")
    time.sleep(40)  # catalog + first thumbnails over EDGE
    screenshot("08_slow_network_home")
    log = adb("logcat", "-d", "-v", "brief", check=False)
    (OUT / "logcat_slow_network.txt").write_text(log)
    if "Could not download the catalog" in log:
        problems.append("slow network: the catalog download failed (see logcat_slow_network.txt)")
    if "FATAL EXCEPTION" in log:
        problems.append("slow network: the app crashed (see logcat_slow_network.txt)")
    loaded = sum(1 for line in log.splitlines() if "RealImageLoader" in line and "Successful" in line)
    print(f"slow network: {loaded} images loaded within {time.time() - started:.0f}s")
    adb("emu", "network", "speed", "full", check=False)
    adb("emu", "network", "delay", "none", check=False)


def main():
    adb("install", "-r", "-g", APK)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(25)  # catalog download + first photos
    screenshot("01_home")

    if find("الأقسام") is None:
        problems.append("home: the categories section never appeared")

    adb("shell", "input", "swipe", "500", "1600", "500", "600", "400")
    time.sleep(8)
    screenshot("02_home_scrolled")
    adb("shell", "input", "swipe", "500", "600", "500", "1600", "400")
    time.sleep(2)

    if tap("ورود", "category"):
        time.sleep(12)
        screenshot("03_category_flowers")

        # The first image card: the first clickable element below the top bar.
        cards = [n for n in ui_nodes() if n.get("clickable") == "true" and center(n)[1] > 400]
        if cards:
            adb("shell", "input", "tap", *map(str, center(cards[0])))
            time.sleep(12)
            screenshot("04_details")

            before = gallery_count()
            if tap("تحميل", "download"):
                time.sleep(10)
                screenshot("05_after_download")
                if gallery_count() <= before:
                    problems.append("download: no new sowar_ file appeared in the gallery")
                else:
                    print("download: file saved to gallery")

            if tap("مشاركة", "share"):
                time.sleep(6)
                screenshot("06_share_sheet")
                adb("shell", "input", "keyevent", "KEYCODE_BACK")
                time.sleep(2)

            tap("إضافة إلى المفضلة", "favorite")
            time.sleep(2)
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
            time.sleep(1)
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
            time.sleep(2)
            if tap("المفضلة", "favorites_tab"):
                time.sleep(6)
                screenshot("07_favorites")
        else:
            problems.append("category: no image cards found")

    log = adb("logcat", "-d", "-v", "brief", check=False)
    (OUT / "logcat.txt").write_text(log)
    slow_network_check()
    lines = log.splitlines()
    # Coil's DebugLogger (debug builds only) logs each load under the RealImageLoader tag.
    coil_lines = [line for line in lines if "RealImageLoader" in line]
    coil_errors = [line for line in coil_lines if "Failed" in line]
    successes = sum(1 for line in coil_lines if "Successful" in line)
    print(f"Coil: {successes} images loaded, {len(coil_errors)} failed")
    for line in coil_errors[:10]:
        print("  ", line)
    if successes == 0:
        problems.append("no image loaded successfully (see logcat.txt)")
    if "FATAL EXCEPTION" in log:
        problems.append("the app crashed (see logcat.txt)")

    (OUT / "summary.txt").write_text(
        f"images loaded: {successes}\nimage failures: {len(coil_errors)}\n"
        + "\n".join(coil_errors[:20]) + "\nproblems:\n" + "\n".join(problems) + "\n"
    )
    if problems:
        print("PROBLEMS:\n" + "\n".join(problems))
        sys.exit(1)
    print("All steps passed")


if __name__ == "__main__":
    main()
