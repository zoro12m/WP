#!/usr/bin/env python3
"""Copies every photo in catalog/catalog.json to Cloudflare R2 and points the catalog at it.

For each image whose link is not on our image domain yet, the script:
  1. downloads the photo from wherever it is now (for the test images: Wikimedia Commons),
  2. makes two WebP files: a full one (at most 1280 px wide) and a thumbnail (500 px wide),
  3. uploads both to the R2 bucket as images/<category>/<id>.webp and thumbs/<category>/<id>.webp,
  4. rewrites "url" and "thumbnailUrl" in the catalog to https://<domain>/... .
Images already on the domain are skipped, so running it again only copies new ones.
Author, license and source link ("credit") stay in the catalog unchanged.

R2 settings come from environment variables (GitHub Actions passes them from the repo secrets):
  R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY

Run:  python3 tools/copy_images_to_r2.py --domain img.example.com --bucket sowarapp-images
Try without R2:  add --dry-run out/ to write the files to a folder instead of uploading.
(GitHub Actions runs it for you, see .github/workflows/copy-images-to-r2.yml.)
"""

import argparse
import io
import json
import os
import sys
import time
import urllib.request
from pathlib import Path

from PIL import Image, ImageOps

CATALOG = Path(__file__).resolve().parent.parent / "catalog" / "catalog.json"
USER_AGENT = "SowarApp-image-copier/1.0 (https://github.com/zoro12m/WP)"

FULL_WIDTH = 1280
THUMB_WIDTH = 500
WEBP_QUALITY = 82
# The file for an id never changes (a new photo gets a new id), so phones and the CDN may keep it for a year.
CACHE_CONTROL = "public, max-age=31536000, immutable"


def download(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read()
        except Exception as error:  # network hiccup: wait and try again
            print(f"  retry {attempt + 1} after error: {error}", file=sys.stderr)
            time.sleep(2 ** attempt)
    raise RuntimeError(f"Could not download {url}")


def to_webp(photo, max_width):
    """Returns (webp bytes, width, height), never making the photo bigger than it is."""
    image = ImageOps.exif_transpose(photo).convert("RGB")
    if image.width > max_width:
        height = round(image.height * max_width / image.width)
        image = image.resize((max_width, height), Image.LANCZOS)
    output = io.BytesIO()
    image.save(output, "WEBP", quality=WEBP_QUALITY, method=6)
    return output.getvalue(), image.width, image.height


class R2Uploader:
    def __init__(self, bucket):
        import boto3  # only needed for real uploads

        self.bucket = bucket
        self.client = boto3.client(
            "s3",
            endpoint_url=f"https://{os.environ['R2_ACCOUNT_ID']}.r2.cloudflarestorage.com",
            aws_access_key_id=os.environ["R2_ACCESS_KEY_ID"],
            aws_secret_access_key=os.environ["R2_SECRET_ACCESS_KEY"],
            region_name="auto",
        )

    def upload(self, key, data):
        self.client.put_object(
            Bucket=self.bucket, Key=key, Body=data,
            ContentType="image/webp", CacheControl=CACHE_CONTROL,
        )


class FolderUploader:
    """--dry-run: writes the files to a local folder so you can look at them."""

    def __init__(self, folder):
        self.folder = Path(folder)

    def upload(self, key, data):
        path = self.folder / key
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def copy_catalog(catalog, domain, uploader):
    base = f"https://{domain}/"
    copied = 0
    for item in catalog["items"]:
        if item.get("type", "image") != "image" or item["url"].startswith(base):
            continue
        print(f"{item['id']}: {item['url']}")
        photo = Image.open(io.BytesIO(download(item["url"])))
        full, width, height = to_webp(photo, FULL_WIDTH)
        thumb, _, _ = to_webp(photo, THUMB_WIDTH)

        name = f"{item['categoryId']}/{item['id']}.webp"
        uploader.upload(f"images/{name}", full)
        uploader.upload(f"thumbs/{name}", thumb)
        item.update(url=base + f"images/{name}", thumbnailUrl=base + f"thumbs/{name}",
                    width=width, height=height)
        copied += 1

    # A category cover is one of its photos' thumbnails; point it at the copied one.
    for category in catalog["categories"]:
        if category.get("coverUrl", "").startswith(base):
            continue
        first = next((item for item in catalog["items"]
                      if item["categoryId"] == category["id"] and item["url"].startswith(base)), None)
        if first:
            category["coverUrl"] = first["thumbnailUrl"]
    return copied


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--domain", required=True, help="the bucket's custom domain, e.g. img.example.com")
    parser.add_argument("--bucket", default="sowarapp-images")
    parser.add_argument("--catalog", type=Path, default=CATALOG)
    parser.add_argument("--dry-run", metavar="FOLDER", help="write files to FOLDER instead of R2")
    args = parser.parse_args()

    domain = args.domain.strip().removeprefix("https://").removeprefix("http://").strip("/")
    uploader = FolderUploader(args.dry_run) if args.dry_run else R2Uploader(args.bucket)
    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    copied = copy_catalog(catalog, domain, uploader)
    args.catalog.write_text(json.dumps(catalog, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Copied {copied} images to {domain}")


if __name__ == "__main__":
    main()
