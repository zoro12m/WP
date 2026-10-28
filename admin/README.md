# Admin page for adding photos

A private web page where you sign in with a password, drag in photos, pick a category,
optionally type a caption, and press **نشر الصور** (Publish). The app shows the new photos the
next time it is opened (within a few minutes). You can also change a photo's category or
caption, mark it as popular, or delete it.

It runs as a Cloudflare Worker on the free plan, next to the R2 bucket that holds the photos.

## How it works

1. Your browser shrinks each photo to a WebP full image (1280 px wide at most) and a 500 px
   thumbnail, the same sizes as `tools/copy_images_to_r2.py`. Big phone photos upload fast.
2. The Worker ([`src/worker.js`](src/worker.js)) checks your password, stores both files in R2 at
   `images/<category>/<id>.webp` and `thumbs/<category>/<id>.webp`, then adds the photo to the top
   of its category in `catalog/catalog.json` **in R2**.
3. The app downloads that catalog from R2 (`CATALOG_URL` in `app/build.gradle.kts`).

A photo's id comes from its bytes, so publishing the same photo twice adds it once.
If two changes reach the catalog at the same moment, R2 refuses the second write and the Worker
retries it on the newest version, so nothing gets lost.

The repo keeps a copy of the catalog (bundled in the APK for the first offline launch).
**Actions → Save catalog copy** brings it up to date every night.

## Setting it up (once, about 10 minutes)

1. **Cloudflare API token.** In Cloudflare, go to **My Profile → API Tokens → Create Token**,
   pick the **Edit Cloudflare Workers** template, choose your account under *Account Resources*
   (and *All zones* under *Zone Resources*), then **Continue → Create Token**. Copy the token.
2. **workers.dev address.** Open **Workers & Pages** in the Cloudflare dashboard once. If it
   asks you to choose a `workers.dev` subdomain, pick one (for example your name).
3. **GitHub secrets.** In the repo: **Settings → Secrets and variables → Actions → New
   repository secret**. Add:
   - `CLOUDFLARE_API_TOKEN`: the token from step 1.
   - `ADMIN_PASSWORD`: the password for the page. Use a long one, for example four random words.
4. **Deploy.** **Actions → Deploy admin page → Run workflow.** The log of the *Deploy the
   Worker* step prints the page's address, like `https://sowarapp-admin.<you>.workers.dev`.
   Bookmark it on your phone and computer.

The workflow runs again by itself whenever `admin/` changes on `main`. To change the password,
update the `ADMIN_PASSWORD` secret and run the workflow; everyone signed in gets signed out.

## Trying it on your computer

```
cd admin
npm ci
echo ADMIN_PASSWORD=test > .dev.vars
npx wrangler r2 object put sowarapp-images/catalog/catalog.json --file ../catalog/catalog.json --local
npx wrangler dev
```

Then open http://localhost:8787. Everything stays on your computer (a pretend R2 bucket).

## Later

- **Own domain:** when the bucket gets a domain, change `PUBLIC_BASE_URL` in
  [`wrangler.toml`](wrangler.toml) and `CATALOG_URL` in `app/build.gradle.kts`.
- **Paging:** past roughly 1,000 photos, split the catalog into small pages per category, so the
  app downloads only what it shows.
