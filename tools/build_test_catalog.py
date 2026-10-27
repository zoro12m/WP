#!/usr/bin/env python3
"""Builds catalog/catalog.json: the list of test images the app loads from the internet.

The photos come from Wikimedia Commons "Quality images". Only licenses that allow use in
an app (with credit) are kept: CC0, public domain and CC BY, then CC BY-SA if a category
still needs more photos. Each photo keeps its author and license so the app can show the credit.

This is for testing. The real catalog (your own images) will replace this file later.

Run:  python3 tools/build_test_catalog.py
(GitHub Actions runs it for you, see .github/workflows/test-catalog.yml.)
"""

import html
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "SowarApp-catalog-builder/1.0 (https://github.com/zoro12m/WP)"
OUTPUT = Path(__file__).resolve().parent.parent / "catalog" / "catalog.json"

IMAGES_PER_CATEGORY = 12
# Wikimedia serves these widths from its cache; other widths can be refused for apps.
FULL_WIDTH = 1280
THUMB_WIDTH = 500

# Most permissive first. CC BY-SA is only used when a category can't be filled otherwise.
PREFERRED_LICENSE = re.compile(r"^(cc0|public domain|pd\b|cc[ -]by[ -]\d|cc[ -]by$)", re.IGNORECASE)
FALLBACK_LICENSE = re.compile(r"^cc[ -]by[ -]sa", re.IGNORECASE)
rejections = {}

# Each category: Arabic title, Commons searches, and captions drawn on top of the photos.
CATEGORIES = [
    {
        "id": "good_morning",
        "title": "صباح الخير",
        "queries": ["sunrise", "morning coffee cup", "morning dew"],
        "captions": ["صباح الخير", "صباحكم سعادة وتفاؤل", "صباح الورد والياسمين",
                     "يوم جديد وأمل جديد", "صباحك أجمل", "صباح النور"],
    },
    {
        "id": "good_evening",
        "title": "مساء الخير",
        "queries": ["sunset", "night sky stars", "evening lake"],
        "captions": ["مساء الخير", "مساء الهدوء والسكينة", "مساؤكم سعيد",
                     "ليلة هادئة وأحلام سعيدة", "مساء الورد", "تصبحون على خير"],
    },
    {
        "id": "flowers",
        "title": "ورود",
        "queries": ["rose flower", "tulip", "flower bouquet"],
        "captions": ["باقة ورد لك", "ورود الربيع", "جمال الطبيعة",
                     "زهرة لأجمل قلب", "عطر الورد", "وردة لك"],
    },
    {
        "id": "wisdom",
        "title": "حكم وعبر",
        "queries": ["mountain landscape", "calm sea", "forest path"],
        "captions": ["الصبر مفتاح الفرج", "من جدّ وجد", "العلم نور",
                     "خير الكلام ما قلّ ودلّ", "اصنع من كل تحدٍّ فرصة", "القناعة كنز لا يفنى"],
    },
    {
        "id": "jumuah",
        "title": "جمعة مباركة",
        "queries": ["mosque", "mosque minaret", "islamic architecture"],
        "captions": ["جمعة مباركة", "جمعة طيبة عليكم", "اللهم اجعلها جمعة خير وبركة",
                     "جمعة مباركة على الجميع", "يوم الجمعة نور وسرور", "جمعة مباركة وطيبة"],
    },
    {
        "id": "occasions",
        "title": "مناسبات",
        "queries": ["fireworks", "birthday cake", "lantern"],
        "captions": ["عيد مبارك", "كل عام وأنتم بخير", "عيد ميلاد سعيد",
                     "ألف مبروك", "رمضان كريم", "مبارك عليكم"],
    },
]


def api_get(params):
    params = {**params, "format": "json", "formatversion": "2"}
    url = API + "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except Exception as error:  # network hiccup: wait and try again
            print(f"  retry {attempt + 1} after error: {error}", file=sys.stderr)
            time.sleep(2 ** attempt)
    raise RuntimeError(f"Commons API failed for {params}")


def strip_html(text):
    return html.unescape(re.sub(r"<[^>]+>", "", text or "")).strip()


def search_files(query):
    """Returns Commons files matching the query, with their size, URLs and license."""
    data = api_get({
        "action": "query",
        "generator": "search",
        "gsrnamespace": "6",
        "gsrsearch": f"{query} incategory:Quality_images filemime:image/jpeg",
        "gsrlimit": "50",
        "prop": "imageinfo",
        "iiprop": "url|size|extmetadata",
        "iiurlwidth": str(FULL_WIDTH),
        "iiextmetadatafilter": "LicenseShortName|Artist|Restrictions",
    })
    pages = data.get("query", {}).get("pages", [])
    print(f"    search '{query}' returned {len(pages)} files")
    # The search returns pages in an arbitrary order; "index" keeps the relevance order.
    return sorted(pages, key=lambda page: page.get("index", 0))


def reject(reason):
    rejections[reason] = rejections.get(reason, 0) + 1
    return None


def to_item(page, license_pattern):
    """Turns one Commons file into a catalog entry, or None if it isn't suitable."""
    info = (page.get("imageinfo") or [None])[0]
    if not info:
        return reject("no imageinfo")
    meta = info.get("extmetadata", {})
    license_name = strip_html(meta.get("LicenseShortName", {}).get("value"))
    restrictions = strip_html(meta.get("Restrictions", {}).get("value"))
    width, height = info.get("width", 0), info.get("height", 0)

    if not license_pattern.match(license_name):
        return reject(f"license {license_name}")
    if restrictions:  # e.g. "personality" (identifiable people) or "trademarked"
        return reject("restrictions")
    if width < FULL_WIDTH or height < 900:
        return reject("too small")

    full_url = info.get("thumburl") or info["url"]
    thumb_url = full_url.replace(f"/{FULL_WIDTH}px-", f"/{THUMB_WIDTH}px-")
    scaled_height = round(height * FULL_WIDTH / width)
    return {
        "type": "image",
        "url": full_url,
        "thumbnailUrl": thumb_url,
        "width": FULL_WIDTH,
        "height": scaled_height,
        "credit": {
            "author": strip_html(meta.get("Artist", {}).get("value"))[:80] or "Wikimedia Commons",
            "license": license_name,
            "sourceUrl": info.get("descriptionurl", ""),
        },
    }


def build():
    categories, items, used_urls = [], [], set()
    for category in CATEGORIES:
        print(f"{category['id']}:")
        results = [page for query in category["queries"] for page in search_files(query)]
        picked = []
        for license_pattern in (PREFERRED_LICENSE, FALLBACK_LICENSE):
            for page in results:
                if len(picked) >= IMAGES_PER_CATEGORY:
                    break
                item = to_item(page, license_pattern)
                if item and item["url"] not in used_urls:
                    used_urls.add(item["url"])
                    picked.append(item)
        print(f"  picked {len(picked)}")
        if not picked:
            print("  rejected:", sorted(rejections.items(), key=lambda kv: -kv[1])[:15])
            raise RuntimeError(f"No usable images for {category['id']}")

        captions = category["captions"]
        for index, item in enumerate(picked):
            caption = captions[index % len(captions)]
            items.append({
                "id": f"{category['id']}_{index + 1}",
                "categoryId": category["id"],
                **item,
                "caption": caption,
                # Every 4th photo is shown without text, to test images that need no caption.
                "showCaption": index % 4 != 3,
                "tags": [category["title"]],
                "popular": index == 0,
            })
        categories.append({
            "id": category["id"],
            "title": category["title"],
            "coverUrl": picked[0]["thumbnailUrl"],
        })

    catalog = {"version": 1, "categories": categories, "items": items}
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(catalog, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Wrote {len(items)} images in {len(categories)} categories to {OUTPUT}")


if __name__ == "__main__":
    build()
