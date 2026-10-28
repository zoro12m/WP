#!/usr/bin/env python3
"""Adds the photos you uploaded to the R2 "inbox" folder to the app's catalog.

How to add photos:
  1. In Cloudflare: R2 -> sowarapp-images -> Upload. Put each photo in
     inbox/<category id>/, for example inbox/flowers/my-rose.jpg.
  2. On GitHub: Actions -> Add new images -> Run workflow.

For each photo in inbox/, the script makes a WebP full image and a thumbnail (same as
copy_images_to_r2.py), uploads them next to the other photos, adds an entry at the top of
its category in catalog/catalog.json, and deletes the original from inbox/.
New photos are shown as they are, without a caption drawn on top (most already have text).

Category ids: good_morning, good_evening, flowers, wisdom, jumuah, occasions
(the "id" of each entry in "categories" in catalog/catalog.json).

R2 settings come from R2_ACCOUNT_ID, R2_ACCESS_KEY_ID and R2_SECRET_ACCESS_KEY.
Run:  python3 tools/add_new_images.py --domain pub-xxxx.r2.dev --bucket sowarapp-images
"""

import argparse
import hashlib
import io
import json
import sys
from pathlib import Path

from PIL import Image, UnidentifiedImageError

from copy_images_to_r2 import CATALOG, FULL_WIDTH, THUMB_WIDTH, R2Uploader, to_webp

INBOX = "inbox/"
PHOTO_TYPES = (".jpg", ".jpeg", ".png", ".webp")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--domain", required=True, help="the bucket's public address, e.g. pub-xxxx.r2.dev")
    parser.add_argument("--bucket", default="sowarapp-images")
    parser.add_argument("--catalog", type=Path, default=CATALOG)
    args = parser.parse_args()

    base = "https://" + args.domain.strip().removeprefix("https://").removeprefix("http://").strip("/") + "/"
    r2 = R2Uploader(args.bucket)
    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    categories = {category["id"]: category for category in catalog["categories"]}
    known_ids = {item["id"] for item in catalog["items"]}

    keys = []
    for page in r2.client.get_paginator("list_objects_v2").paginate(Bucket=args.bucket, Prefix=INBOX):
        keys += [obj["Key"] for obj in page.get("Contents", []) if not obj["Key"].endswith("/")]
    print(f"Found {len(keys)} files in {INBOX}")

    added, problems = [], []
    for key in sorted(keys):
        parts = key[len(INBOX):].split("/")
        category_id = parts[0] if len(parts) == 2 else None
        if category_id not in categories:
            problems.append(f"{key}: put it in inbox/<category>/ with one of: {', '.join(categories)}")
            continue
        if not key.lower().endswith(PHOTO_TYPES):
            problems.append(f"{key}: not a photo (use .jpg, .png or .webp)")
            continue

        data = r2.client.get_object(Bucket=args.bucket, Key=key)["Body"].read()
        try:
            photo = Image.open(io.BytesIO(data))
            photo.load()
        except (UnidentifiedImageError, OSError):
            problems.append(f"{key}: could not open the photo")
            continue

        # The id comes from the photo's bytes, so uploading the same photo twice adds it once.
        image_id = f"{category_id}_{hashlib.sha256(data).hexdigest()[:10]}"
        if image_id not in known_ids:
            full, width, height = to_webp(photo, FULL_WIDTH)
            thumb, _, _ = to_webp(photo, THUMB_WIDTH)
            name = f"{category_id}/{image_id}.webp"
            r2.upload(f"images/{name}", full)
            r2.upload(f"thumbs/{name}", thumb)
            added.append({
                "id": image_id,
                "categoryId": category_id,
                "type": "image",
                "url": base + f"images/{name}",
                "thumbnailUrl": base + f"thumbs/{name}",
                "width": width,
                "height": height,
                "caption": "",
                "showCaption": False,
                "tags": [categories[category_id]["title"]],
                "popular": False,
                "credit": {"author": "SowarApp", "license": "Own image"},
            })
            known_ids.add(image_id)
            print(f"added {key} as {image_id}")
        else:
            print(f"{key} is already in the catalog")
        r2.client.delete_object(Bucket=args.bucket, Key=key)

    # Newest first: new photos go to the top of the list, so they show first in their category.
    catalog["items"] = added + catalog["items"]
    args.catalog.write_text(json.dumps(catalog, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Added {len(added)} new photos")
    for problem in problems:
        print(f"::warning::{problem}")
    if problems and not added:
        sys.exit(1)


if __name__ == "__main__":
    main()
