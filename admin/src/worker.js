// The admin page for adding photos to SowarApp.
//
// The browser does the heavy work: it shrinks each photo to a WebP full image and thumbnail,
// uploads both with PUT /api/files/..., then sends the new catalog entries in one POST /api/items.
// This Worker checks the password, stores the files in R2 and keeps catalog/catalog.json up to date.
// The app downloads that catalog from R2, so new photos show up without an app update.

import PAGE from "./page.html";
// Turns iPhone/Samsung HEIC photos into JPEG in the browser (MIT license, npm package heic2any).
import HEIC2ANY from "heic2any/dist/heic2any.min.js";

const CATALOG_KEY = "catalog/catalog.json";
// The app checks for a new catalog each time it starts; a short cache keeps that fresh.
const CATALOG_CACHE = "public, max-age=60";
// The file for an id never changes (a new photo gets a new id), so phones may keep it for a year.
const PHOTO_CACHE = "public, max-age=31536000, immutable";
const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
const SESSION_COOKIE = "sowar_admin";
const SESSION_DAYS = 30;
// Photos live at images/<category>/<id>.<ext> and thumbs/<category>/<id>.<ext>.
const PHOTO_PATH = /^(images|thumbs)\/([a-z0-9_]+)\/([a-z0-9_]+)\.(webp|jpg)$/;
const ID_PATTERN = /^[a-z0-9_]+$/;
// Looks at a photo and picks its category (Cloudflare Workers AI, free daily allowance).
const VISION_MODEL = "@cf/meta/llama-3.2-11b-vision-instruct";
const MAX_SUGGEST_BYTES = 1024 * 1024;

export default {
  async fetch(request, env) {
    try {
      return await route(request, env);
    } catch (error) {
      if (error instanceof HttpError) return json({ error: error.message }, error.status);
      console.error(error);
      return json({ error: "Something went wrong on the server" }, 500);
    }
  },
};

async function route(request, env) {
  if (!env.ADMIN_PASSWORD) throw new HttpError(500, "ADMIN_PASSWORD is not set");
  const url = new URL(request.url);
  const path = url.pathname;
  const method = request.method;

  if (path === "/" && method === "GET") {
    return new Response(PAGE, {
      headers: {
        "Content-Type": "text/html; charset=utf-8",
        "Cache-Control": "no-store",
        "X-Frame-Options": "DENY",
        "Referrer-Policy": "no-referrer",
      },
    });
  }
  if (path === "/vendor/heic2any.js" && method === "GET") {
    return new Response(HEIC2ANY, {
      headers: { "Content-Type": "text/javascript; charset=utf-8", "Cache-Control": "public, max-age=604800" },
    });
  }
  if (path === "/api/login" && method === "POST") return login(request, env);
  if (path === "/api/logout" && method === "POST") return logout();

  if (!path.startsWith("/api/")) throw new HttpError(404, "Not found");
  // Everything below needs a signed-in session.
  if (!(await isSignedIn(request, env))) throw new HttpError(401, "Please sign in");
  // Browsers always send Origin on these requests; refusing other origins blocks cross-site tricks.
  const origin = request.headers.get("Origin");
  if (method !== "GET" && origin && origin !== url.origin) throw new HttpError(403, "Wrong origin");

  if (path === "/api/catalog" && method === "GET") {
    const { catalog } = await readCatalog(env);
    return json({ catalog, publicBaseUrl: publicBase(env) });
  }
  if (path.startsWith("/api/files/") && method === "PUT") return putPhoto(request, env, path.slice("/api/files/".length));
  if (path === "/api/items" && method === "POST") return addItems(request, env);

  if (path === "/api/suggest" && method === "POST") return suggestCategory(request, env);
  if (path === "/api/categories" && method === "POST") return addCategory(request, env);
  const categoryMatch = path.match(/^\/api\/categories\/([a-z0-9_]+)(\/move)?$/);
  if (categoryMatch && categoryMatch[2] && method === "POST") return moveCategory(request, env, categoryMatch[1]);
  if (categoryMatch && !categoryMatch[2] && method === "PATCH") return renameCategory(request, env, categoryMatch[1]);
  if (categoryMatch && !categoryMatch[2] && method === "DELETE") return deleteCategory(env, categoryMatch[1]);

  const itemMatch = path.match(/^\/api\/items\/([a-z0-9_]+)$/);
  if (itemMatch && method === "PATCH") return editItem(request, env, itemMatch[1]);
  if (itemMatch && method === "DELETE") return deleteItem(env, itemMatch[1]);

  throw new HttpError(404, "Not found");
}

// ---------- Signing in ----------

async function login(request, env) {
  const { password } = await readJson(request);
  if (typeof password !== "string" || !(await sameText(password, env.ADMIN_PASSWORD))) {
    // Slows down anyone guessing passwords.
    await new Promise((resolve) => setTimeout(resolve, 1000));
    throw new HttpError(401, "Wrong password");
  }
  const expires = Date.now() + SESSION_DAYS * 24 * 60 * 60 * 1000;
  const token = `${expires}.${await sign(String(expires), env)}`;
  return json({ ok: true }, 200, {
    "Set-Cookie": `${SESSION_COOKIE}=${token}; Path=/; Max-Age=${SESSION_DAYS * 86400}; HttpOnly; Secure; SameSite=Strict`,
  });
}

function logout() {
  return json({ ok: true }, 200, {
    "Set-Cookie": `${SESSION_COOKIE}=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict`,
  });
}

/** The cookie is "<expiry time>.<signature>"; the signature uses the password, so changing it signs everyone out. */
async function isSignedIn(request, env) {
  const cookies = request.headers.get("Cookie") || "";
  const match = cookies.match(new RegExp(`(?:^|;\\s*)${SESSION_COOKIE}=(\\d+)\\.([A-Za-z0-9_-]+)`));
  if (!match) return false;
  const [, expires, signature] = match;
  if (Number(expires) < Date.now()) return false;
  return sameText(signature, await sign(expires, env));
}

async function sign(text, env) {
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(`session:${env.ADMIN_PASSWORD}`),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"],
  );
  const bytes = new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(text)));
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Compares without leaking how many characters matched (hashing first makes the lengths equal). */
async function sameText(a, b) {
  const [ha, hb] = await Promise.all([a, b].map((text) => crypto.subtle.digest("SHA-256", new TextEncoder().encode(text))));
  return crypto.subtle.timingSafeEqual(ha, hb);
}

// ---------- Photos ----------

async function putPhoto(request, env, key) {
  const match = key.match(PHOTO_PATH);
  if (!match) throw new HttpError(400, "Bad file name");
  const { catalog } = await readCatalog(env);
  if (!catalog.categories.some((category) => category.id === match[2])) throw new HttpError(400, "Unknown category");
  const contentType = match[4] === "webp" ? "image/webp" : "image/jpeg";
  if (request.headers.get("Content-Type") !== contentType) throw new HttpError(400, `Send it as ${contentType}`);

  const body = await request.arrayBuffer();
  if (body.byteLength === 0 || body.byteLength > MAX_PHOTO_BYTES) throw new HttpError(413, "The photo is empty or bigger than 5 MB");
  if (!looksLikePhoto(new Uint8Array(body), match[4])) throw new HttpError(400, "That is not a photo");

  await env.BUCKET.put(key, body, { httpMetadata: { contentType, cacheControl: PHOTO_CACHE } });
  return json({ ok: true, url: publicBase(env) + key });
}

/** Checks the first bytes, so only real WebP or JPEG files get stored. */
function looksLikePhoto(bytes, ext) {
  if (ext === "jpg") return bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;
  const text = String.fromCharCode(...bytes.slice(0, 12));
  return text.startsWith("RIFF") && text.slice(8) === "WEBP";
}

// ---------- Catalog entries ----------

async function addItems(request, env) {
  const { items } = await readJson(request);
  if (!Array.isArray(items) || items.length === 0 || items.length > 100) throw new HttpError(400, "Send 1 to 100 items");

  return updateCatalog(env, async (catalog) => {
    const base = publicBase(env);
    const categories = new Map(catalog.categories.map((category) => [category.id, category]));
    const known = new Set(catalog.items.map((item) => item.id));
    const added = [];
    const skipped = [];

    for (const input of items) {
      const category = categories.get(input?.categoryId);
      if (!category) throw new HttpError(400, "Unknown category");
      if (typeof input.id !== "string" || !ID_PATTERN.test(input.id)) throw new HttpError(400, "Bad id");
      if (known.has(input.id)) {
        skipped.push(input.id);
        continue;
      }
      const files = [input.file, input.thumbFile];
      for (const file of files) {
        const match = typeof file === "string" && file.match(PHOTO_PATH);
        if (!match || match[2] !== category.id || match[3] !== input.id) throw new HttpError(400, "Bad file name");
        // The photo must already be uploaded, so the app never gets a broken link.
        if (!(await env.BUCKET.head(file))) throw new HttpError(400, `Upload ${file} first`);
      }
      const caption = cleanCaption(input.caption);
      added.push({
        id: input.id,
        categoryId: category.id,
        type: "image",
        url: base + input.file,
        thumbnailUrl: base + input.thumbFile,
        width: positiveInt(input.width),
        height: positiveInt(input.height),
        caption,
        showCaption: caption !== "" && input.showCaption === true,
        tags: [category.title],
        popular: false,
        credit: { author: "SowarApp", license: "Own image" },
      });
      known.add(input.id);
    }
    // Newest first, so new photos show at the top of their category.
    catalog.items = [...added, ...catalog.items];
    // A new section gets its first photo as its cover.
    fixCovers(catalog);
    return { added: added.map((item) => item.id), skipped };
  });
}

async function editItem(request, env, id) {
  const changes = await readJson(request);
  return updateCatalog(env, async (catalog) => {
    const item = catalog.items.find((entry) => entry.id === id);
    if (!item) throw new HttpError(404, "No photo with that id");
    if ("caption" in changes) item.caption = cleanCaption(changes.caption);
    if ("showCaption" in changes) item.showCaption = changes.showCaption === true;
    if (item.caption === "") item.showCaption = false;
    if ("popular" in changes) item.popular = changes.popular === true;
    if ("categoryId" in changes && changes.categoryId !== item.categoryId) {
      const category = catalog.categories.find((entry) => entry.id === changes.categoryId);
      if (!category) throw new HttpError(400, "Unknown category");
      // The file stays where it is; only the catalog says which category it belongs to.
      const oldTitle = catalog.categories.find((entry) => entry.id === item.categoryId)?.title;
      item.categoryId = category.id;
      item.tags = [category.title, ...(item.tags || []).filter((tag) => tag !== oldTitle && tag !== category.title)];
      fixCovers(catalog);
    }
    return { item };
  });
}

async function deleteItem(env, id) {
  let removed;
  const result = await updateCatalog(env, async (catalog) => {
    removed = catalog.items.find((entry) => entry.id === id);
    if (!removed) throw new HttpError(404, "No photo with that id");
    catalog.items = catalog.items.filter((entry) => entry !== removed);
    fixCovers(catalog);
    return { deleted: id };
  });
  // Delete the files only after the catalog no longer points at them.
  const base = publicBase(env);
  const keys = [removed.url, removed.thumbnailUrl]
    .filter((url) => typeof url === "string" && url.startsWith(base))
    .map((url) => url.slice(base.length));
  if (keys.length) await env.BUCKET.delete(keys);
  return result;
}

/**
 * A category whose cover photo is gone (or no longer in it) gets its newest photo as the new cover.
 * An empty category gets no cover; the app hides categories without photos.
 */
function fixCovers(catalog) {
  for (const category of catalog.categories) {
    const inCategory = catalog.items.filter((item) => item.categoryId === category.id);
    if (!inCategory.length) {
      category.coverUrl = "";
    } else if (!inCategory.some((item) => item.thumbnailUrl === category.coverUrl)) {
      category.coverUrl = inCategory[0].thumbnailUrl;
    }
  }
}

// ---------- Categories (the sections of the app) ----------

async function addCategory(request, env) {
  const title = cleanTitle((await readJson(request)).title);
  return updateCatalog(env, async (catalog) => {
    if (catalog.categories.some((category) => category.title === title)) throw new HttpError(400, "A section with that name already exists");
    // Ids appear in photo paths, so they stay plain letters and digits; the Arabic title is what people see.
    let id;
    do id = `c_${crypto.randomUUID().replace(/-/g, "").slice(0, 8)}`;
    while (catalog.categories.some((category) => category.id === id));
    const category = { id, title, coverUrl: "" };
    catalog.categories.push(category);
    return { category };
  });
}

async function renameCategory(request, env, id) {
  const title = cleanTitle((await readJson(request)).title);
  return updateCatalog(env, async (catalog) => {
    const category = findCategory(catalog, id);
    if (catalog.categories.some((other) => other !== category && other.title === title)) throw new HttpError(400, "A section with that name already exists");
    // Photos carry their category's name as a search word; keep that in step.
    for (const item of catalog.items) {
      if (item.categoryId === id) item.tags = (item.tags || []).map((tag) => (tag === category.title ? title : tag));
    }
    category.title = title;
    return { category };
  });
}

async function moveCategory(request, env, id) {
  const { direction } = await readJson(request);
  if (direction !== -1 && direction !== 1) throw new HttpError(400, "direction must be -1 or 1");
  return updateCatalog(env, async (catalog) => {
    const from = catalog.categories.indexOf(findCategory(catalog, id));
    const to = from + direction;
    if (to >= 0 && to < catalog.categories.length) {
      [catalog.categories[from], catalog.categories[to]] = [catalog.categories[to], catalog.categories[from]];
    }
    return { order: catalog.categories.map((category) => category.id) };
  });
}

async function deleteCategory(env, id) {
  return updateCatalog(env, async (catalog) => {
    findCategory(catalog, id);
    // Never delete photos by accident: the section must be emptied (moved or deleted) first.
    if (catalog.items.some((item) => item.categoryId === id)) throw new HttpError(400, "Move or delete this section's photos first");
    catalog.categories = catalog.categories.filter((category) => category.id !== id);
    return { deleted: id };
  });
}

function findCategory(catalog, id) {
  const category = catalog.categories.find((entry) => entry.id === id);
  if (!category) throw new HttpError(404, "No section with that id");
  return category;
}

function cleanTitle(value) {
  const title = typeof value === "string" ? value.trim().replace(/\s+/g, " ") : "";
  if (!title || title.length > 40) throw new HttpError(400, "The name must be 1 to 40 characters");
  return title;
}

// ---------- Suggesting a category with AI ----------

/**
 * The page sends a small JPEG of the photo; a vision model looks at it (and any text on it) and
 * picks one of the sections. It is only a suggestion: the page shows it and you can change it.
 */
async function suggestCategory(request, env) {
  if (!env.AI) throw new HttpError(501, "AI is not set up for this Worker");
  if (request.headers.get("Content-Type") !== "image/jpeg") throw new HttpError(400, "Send a JPEG");
  const bytes = new Uint8Array(await request.arrayBuffer());
  if (!bytes.length || bytes.length > MAX_SUGGEST_BYTES) throw new HttpError(413, "Send a small JPEG (under 1 MB)");
  if (!looksLikePhoto(bytes, "jpg")) throw new HttpError(400, "That is not a JPEG");

  const { catalog } = await readCatalog(env);
  const list = catalog.categories.map((category) => `${category.id}: ${category.title}`).join("\n");
  const prompt =
    "You sort pictures for an Arabic greeting-image app. The sections are (id: Arabic name):\n" +
    `${list}\n\n` +
    "Look at the picture and read any Arabic or English text written on it. " +
    "Reply with only the id of the one section it fits best, nothing else.";
  const input = { image: Array.from(bytes), prompt, max_tokens: 20, temperature: 0 };

  let result;
  try {
    result = await env.AI.run(VISION_MODEL, input);
  } catch (error) {
    // Meta's license must be accepted once per account by sending "agree".
    if (!/agree/i.test(String(error?.message))) throw error;
    await env.AI.run(VISION_MODEL, { prompt: "agree" });
    result = await env.AI.run(VISION_MODEL, input);
  }
  const answer = String(result?.response ?? "");
  return json({ categoryId: pickCategory(answer, catalog.categories)?.id ?? null, answer: answer.slice(0, 100) });
}

/** The category whose id (or Arabic name) appears in the model's answer; the longest match wins. */
function pickCategory(answer, categories) {
  const text = answer.toLowerCase();
  const found = categories.filter((category) => text.includes(category.id) || answer.includes(category.title));
  return found.sort((a, b) => b.id.length - a.id.length)[0];
}

function cleanCaption(value) {
  return typeof value === "string" ? value.trim().slice(0, 300) : "";
}

function positiveInt(value) {
  const number = Math.round(Number(value));
  return Number.isFinite(number) && number > 0 && number < 100000 ? number : 0;
}

// ---------- Reading and writing catalog.json ----------

async function readCatalog(env) {
  const object = await env.BUCKET.get(CATALOG_KEY);
  if (!object) throw new HttpError(500, `${CATALOG_KEY} is missing from the bucket`);
  return { catalog: await object.json(), etag: object.etag };
}

/**
 * Reads the catalog, lets [change] edit it, then saves it only if nobody else saved in between
 * (R2 checks the etag). If someone did, it starts again from their version.
 */
async function updateCatalog(env, change) {
  for (let attempt = 0; attempt < 5; attempt++) {
    const { catalog, etag } = await readCatalog(env);
    const result = await change(catalog);
    const saved = await env.BUCKET.put(CATALOG_KEY, JSON.stringify(catalog, null, 1) + "\n", {
      onlyIf: { etagMatches: etag },
      httpMetadata: { contentType: "application/json; charset=utf-8", cacheControl: CATALOG_CACHE },
    });
    if (saved) return json({ ok: true, ...result, total: catalog.items.length });
  }
  throw new HttpError(409, "The catalog kept changing; please try again");
}

// ---------- Small helpers ----------

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function publicBase(env) {
  return env.PUBLIC_BASE_URL.endsWith("/") ? env.PUBLIC_BASE_URL : env.PUBLIC_BASE_URL + "/";
}

async function readJson(request) {
  try {
    return await request.json();
  } catch {
    throw new HttpError(400, "Expected JSON");
  }
}

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store", ...headers },
  });
}
