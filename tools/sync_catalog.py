#!/usr/bin/env python3
"""Copies catalog.json between the repo and the R2 bucket.

Since the admin page, the real catalog lives in R2 at catalog/catalog.json: the app downloads it
from there, and the admin page edits it there. The repo keeps a copy, which is bundled in the APK
so the app works on first launch without internet.

  --seed   copy the repo's catalog to R2, only if R2 has none yet (the deploy workflow runs this)
  --pull   copy R2's catalog into the repo (the "Save catalog copy" workflow commits it)

R2 settings come from R2_ACCOUNT_ID, R2_ACCESS_KEY_ID and R2_SECRET_ACCESS_KEY.
"""

import argparse
import json
import sys
from pathlib import Path

from copy_images_to_r2 import CATALOG, R2Uploader

CATALOG_KEY = "catalog/catalog.json"
# The app checks for a new catalog each time it starts; a short cache keeps that fresh.
CATALOG_CACHE = "public, max-age=60"


def read_r2_catalog(r2):
    """The catalog text in R2, or None if there isn't one yet."""
    try:
        return r2.client.get_object(Bucket=r2.bucket, Key=CATALOG_KEY)["Body"].read().decode("utf-8")
    except r2.client.exceptions.NoSuchKey:
        return None


def write_r2_catalog(r2, text):
    json.loads(text)  # never upload a broken file
    r2.client.put_object(
        Bucket=r2.bucket, Key=CATALOG_KEY, Body=text.encode("utf-8"),
        ContentType="application/json; charset=utf-8", CacheControl=CATALOG_CACHE,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument("--seed", action="store_true")
    action.add_argument("--pull", action="store_true")
    parser.add_argument("--bucket", default="sowarapp-images")
    parser.add_argument("--catalog", type=Path, default=CATALOG)
    args = parser.parse_args()

    r2 = R2Uploader(args.bucket)
    current = read_r2_catalog(r2)
    if args.seed:
        if current is not None:
            print(f"R2 already has {CATALOG_KEY}; leaving it as it is")
            return
        write_r2_catalog(r2, args.catalog.read_text(encoding="utf-8"))
        print(f"Copied {args.catalog.name} to R2")
    else:
        if current is None:
            sys.exit(f"R2 has no {CATALOG_KEY} yet")
        # Same formatting as the other tools, so the git diff only shows real changes.
        catalog = json.loads(current)
        args.catalog.write_text(json.dumps(catalog, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        print(f"Saved R2's catalog ({len(catalog['items'])} photos) to {args.catalog}")


if __name__ == "__main__":
    main()
