/* Pattukal for iOS (PWA). No build step; plain JS. */
(() => {
"use strict";
const $ = (s, r = document) => r.querySelector(s);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const LS = {
  get(k, d) { try { const v = localStorage.getItem("pk_" + k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem("pk_" + k, JSON.stringify(v)); } catch {} },
};
const VERSION = "1.0 (iOS web app)";
const INV = ["https://inv.nadeko.net", "https://yewtu.be", "https://invidious.nerdvpn.de", "https://iv.ggtyler.dev", "https://invidious.privacyredirect.com"];
const PIPED = ["https://pipedapi.kavin.rocks", "https://pipedapi.adminforge.de"];
const LANGS = {
  malayalam: { name: "Malayalam", q: ["Malayalam new hit songs", "Malayalam romantic songs", "Malayalam 90s evergreen songs"] },
  hindi: { name: "Hindi / Bollywood", q: ["Bollywood latest songs", "Hindi romantic songs", "Arijit Singh hits"] },
  tamil: { name: "Tamil / Kollywood", q: ["Tamil new hit songs", "Anirudh hits", "Tamil melody songs"] },
  telugu: { name: "Telugu / Tollywood", q: ["Telugu new hit songs", "Telugu melody songs", "Telugu dance hits"] },
  kannada: { name: "Kannada", q: ["Kannada new hit songs", "Kannada melody songs"] },
  punjabi: { name: "Punjabi", q: ["Punjabi latest songs", "Punjabi hits"] },
  english: { name: "English", q: ["English top hits", "English chill songs", "Pop hits 2026"] },
};
const BGS = { waves: "Liquid waves", snow: "Snowland", tokyo: "Tokyo", fireworks: "Fireworks", drift: "Car drift", plain: "Plain black" };
const set = {
  lang: LS.get("lang", null), slider: LS.get("slider", "bar"), bg: LS.get("bg", "waves"), crop: LS.get("crop", true),
  sponsor: LS.get("sponsor", true), cube: LS.get("cube", true), cubeIds: LS.get("cubeIds", []), hideThumb: LS.get("hideThumb", false),
  buddy: LS.get("buddy", "luna"),
};
const save = (k, v) => { set[k] = v; LS.set(k, v); };
let liked = LS.get("liked", []), history = LS.get("history", []), playlists = LS.get("playlists", {});
let queue = [], qi = -1, playing = false, pos = 0, dur = 0, tab = "home", homeSongs = [], lyr = [], sbSegs = [], sleepTimer = null;
let yt = null, ytReady = false, pendingId = null, playerOpen = false;

const thumb = (id) => `https://i.ytimg.com/vi/${id}/hqdefault.jpg`;
function toast(m) { const t = $("#toast"); t.textContent = m; t.classList.add("show"); clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove("show"), 2600); }
const fmt = (s) => { s = Math.max(0, Math.floor(s || 0)); return Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0"); };

/* ---------- networking with automatic source switching (first answer wins) ---------- */
function timed(url, opt = {}, ms = 7000) {
  const ac = new AbortController(); const id = setTimeout(() => ac.abort(), ms);
  return fetch(url, { ...opt, signal: ac.signal }).then((r) => { clearTimeout(id); if (!r.ok) throw new Error(r.status); return r; });
}
const anyOf = (ps) => new Promise((res, rej) => { let n = ps.length, done = false; ps.forEach((p) => p.then((v) => { if (!done) { done = true; res(v); } }, () => { if (--n === 0 && !done) rej(new Error("all sources failed")); })); });
const cache = new Map();
async function searchSongs(q) {
  if (cache.has(q)) return cache.get(q);
  const jobs = [
    ...INV.map((b) => timed(`${b}/api/v1/search?q=${encodeURIComponent(q)}&type=video`).then((r) => r.json()).then((a) => { if (!Array.isArray(a) || !a.length) throw 0; return a.filter((x) => x.videoId && x.lengthSeconds > 60 && x.lengthSeconds < 900).map((x) => ({ id: x.videoId, title: x.title, artist: (x.author || "").replace(/ - Topic$/, ""), dur: x.lengthSeconds })); })),
    ...PIPED.map((b) => timed(`${b}/search?q=${encodeURIComponent(q)}&filter=music_songs`).then((r) => r.json()).then((j) => { const a = (j.items || []).filter((x) => x.url); if (!a.length) throw 0; return a.map((x) => ({ id: x.url.split("v=")[1], title: x.title, artist: (x.uploaderName || "").replace(/ - Topic$/, ""), dur: x.duration })); })),
  ];
  const out = await anyOf(jobs); cache.set(q, out); return out;
}
async function lyricsFor(s) {
  const t = s.title.replace(/\(.*?\)|\[.*?\]|official.*$/gi, "").trim();
  const urls = [`https://lrclib.net/api/get?track_name=${encodeURIComponent(t)}&artist_name=${encodeURIComponent(s.artist)}`, `https://lrclib.net/api/search?q=${encodeURIComponent(t + " " + s.artist)}`];
  for (const u of urls) {
    try { let j = await (await timed(u, {}, 8000)).json(); if (Array.isArray(j)) j = j.find((x) => x.syncedLyrics) || j[0]; if (j && (j.syncedLyrics || j.plainLyrics)) return parseLrc(j.syncedLyrics, j.plainLyrics); } catch {}
  }
  return [];
}
function parseLrc(synced, plain) {
  if (synced) {
    const out = []; for (const l of synced.split("\n")) { const m = l.match(/^\[(\d+):(\d+)(?:\.(\d+))?\]\s*(.*)$/); if (m && m[4]) out.push({ t: +m[1] * 60 + +m[2] + (m[3] ? +("0." + m[3]) : 0), x: m[4] }); }
    if (out.length) return out;
  }
  return (plain || "").split("\n").filter(Boolean).map((x) => ({ t: -1, x }));
}
async function sponsorSegs(id) {
  try { const j = await (await timed(`https://sponsor.ajay.app/api/skipSegments?videoID=${id}&categories=${encodeURIComponent('["sponsor","selfpromo","intro","outro","interaction"]')}`, {}, 6000)).json(); return j.map((s) => s.segment); } catch { return []; }
}

/* ---------- YouTube player ---------- */
function loadYT() {
  if (window.YT && YT.Player) return onYT();
  const s = document.createElement("script"); s.src = "https://www.youtube.com/iframe_api"; document.head.appendChild(s);
  window.onYouTubeIframeAPIReady = onYT;
}
function onYT() {
  const d = document.createElement("div"); d.id = "ytp"; $("#yt").appendChild(d);
  yt = new YT.Player("ytp", { width: "200", height: "200", playerVars: { playsinline: 1, controls: 0, disablekb: 1, rel: 0, origin: location.origin },
    events: { onReady: () => { ytReady = true; if (pendingId) { yt.loadVideoById(pendingId); pendingId = null; } }, onStateChange: (e) => { playing = e.data === 1; if (e.data === 0) next(true); render(); mediaState(); }, onError: () => { toast("This song can't be played, skipping"); next(true); } } });
}
function playIndex(i, list) {
  if (list) { queue = list.slice(); }
  if (i < 0 || i >= queue.length) return;
  qi = i; const s = queue[qi]; pos = 0; dur = s.dur || 0; lyr = []; sbSegs = [];
  if (ytReady) yt.loadVideoById(s.id); else pendingId = s.id;
  history = [s, ...history.filter((h) => h.id !== s.id)].slice(0, 60); LS.set("history", history);
  lyricsFor(s).then((l) => { if (queue[qi] && queue[qi].id === s.id) { lyr = l; drawLyr(); } });
  if (set.sponsor) sponsorSegs(s.id).then((g) => { if (queue[qi] && queue[qi].id === s.id) sbSegs = g; });
  mediaMeta(s); render(); if (playerOpen) drawPlayer();
}
function toggle() { if (!ytReady) return; playing ? yt.pauseVideo() : yt.playVideo(); }
function next(auto) { if (qi < queue.length - 1) playIndex(qi + 1); else if (auto && queue.length) playIndex(0); }
function prev() { if (pos > 4) { yt && yt.seekTo(0, true); } else if (qi > 0) playIndex(qi - 1); }
setInterval(() => {
  if (!ytReady || !yt || !yt.getCurrentTime || qi < 0) return;
  pos = yt.getCurrentTime() || 0; const d = yt.getDuration && yt.getDuration(); if (d) dur = d;
  if (set.sponsor) for (const [a, b] of sbSegs) if (pos >= a && pos < b - .3) { yt.seekTo(b, true); toast("Skipped a sponsor segment"); break; }
  if (playerOpen) tickPlayer();
}, 250);
function mediaMeta(s) {
  if (!("mediaSession" in navigator)) return;
  navigator.mediaSession.metadata = new MediaMetadata({ title: s.title, artist: s.artist, artwork: [{ src: thumb(s.id), sizes: "480x360", type: "image/jpeg" }] });
  const h = { play: toggle, pause: toggle, nexttrack: () => next(), previoustrack: prev };
  for (const k in h) try { navigator.mediaSession.setActionHandler(k, h[k]); } catch {}
}
function mediaState() { if ("mediaSession" in navigator) navigator.mediaSession.playbackState = playing ? "playing" : "paused"; }

/* ---------- rendering ---------- */
function cur() { return queue[qi]; }
function songRow(s, i, list) {
  return `<div class="li ${cur() && cur().id === s.id ? "now" : ""}" data-play="${i}" data-list="${list}"><img loading="lazy" src="${thumb(s.id)}" alt=""><div class="t"><b>${esc(s.title)}</b><small>${esc(s.artist)}</small></div><button class="ib" data-menu="${esc(s.id)}" aria-label="More">⋯</button></div>`;
}
const lists = {};
function reg(name, arr) { lists[name] = arr; return name; }
function skeleton() { return `<div class="row">${'<div class="card"><div class="sk" style="width:138px;height:138px"></div></div>'.repeat(4)}</div>`; }

async function view() {
  const v = $("#view");
  if (tab === "home") {
    const langs = set.lang || [];
    v.innerHTML = `<h1>Pattukal</h1>` + (set.cube ? `<div id="cubeBox"></div>` : "") + langs.map((l, i) => `<h2>${esc(LANGS[l].name)}</h2><div class="row" id="row${i}">${skeleton()}</div>`).join("") + (langs.length ? "" : `<div class="empty">Pick your music language to get started.<br><button class="pill" data-act="lang">Choose language</button></div>`);
    drawCube();
    langs.forEach(async (l, i) => {
      const box = $("#row" + i); if (!box) return;
      try {
        const q = LANGS[l].q[Math.floor(Math.random() * LANGS[l].q.length)];
        const songs = (await searchSongs(q)).slice(0, 12); const name = reg("home" + i, songs);
        homeSongs = [...homeSongs.filter((h) => !songs.some((s) => s.id === h.id)), ...songs];
        if ($("#row" + i)) box.innerHTML = songs.map((s, k) => `<div class="card" data-play="${k}" data-list="${name}"><img loading="lazy" src="${thumb(s.id)}" alt=""><b>${esc(s.title)}</b><small>${esc(s.artist)}</small></div>`).join("");
        drawCube();
      } catch { box.innerHTML = `<div class="empty">Couldn't load right now. <button class="pill" data-act="reload">Retry</button></div>`; }
    });
  } else if (tab === "search") {
    v.innerHTML = `<h1>Search</h1><form class="search" id="sf"><input id="sq" type="search" placeholder="Songs, artists, movies" autocomplete="off"><button class="ib">⌕</button></form><div id="sr"></div>`;
    $("#sf").onsubmit = async (e) => { e.preventDefault(); const q = $("#sq").value.trim(); if (!q) return; $("#sr").innerHTML = skeleton(); try { const r = await searchSongs(q); const n = reg("search", r); $("#sr").innerHTML = r.map((s, i) => songRow(s, i, n)).join("") || `<div class="empty">No songs found.</div>`; } catch { $("#sr").innerHTML = `<div class="empty">All sources are busy. Try again in a moment.</div>`; } };
  } else if (tab === "library") {
    const pl = Object.keys(playlists);
    v.innerHTML = `<h1>Library</h1><h2>Liked songs (${liked.length})</h2>${liked.map((s, i) => songRow(s, i, reg("liked", liked))).join("") || '<div class="empty">Tap ♡ in the player to like songs.</div>'}<h2>Playlists</h2>${pl.map((n) => `<div class="li" data-pl="${esc(n)}"><div class="t"><b>${esc(n)}</b><small>${playlists[n].length} songs</small></div><button class="ib" data-plplay="${esc(n)}">▶</button></div>`).join("")}<button class="pill" data-act="newpl">+ New playlist</button><h2>Recently played</h2>${history.map((s, i) => songRow(s, i, reg("history", history))).join("") || '<div class="empty">Nothing yet.</div>'}`;
  } else if (tab === "buddy") buddyView();
  else if (tab === "settings") settingsView();
}

function drawCube() {
  const b = $("#cubeBox"); if (!b) return;
  let ids = (set.cubeIds || []).map((id) => [...homeSongs, ...liked, ...history].find((s) => s.id === id)).filter(Boolean);
  if (ids.length < 6) ids = [...ids, ...homeSongs.filter((s) => !ids.some((x) => x.id === s.id))].slice(0, 6);
  if (!ids.length) { b.innerHTML = ""; return; }
  const name = reg("cube", ids);
  b.innerHTML = `<div class="cube-wrap" data-play="0" data-list="${name}"><div class="cube">${Array.from({ length: 6 }, (_, i) => `<div style="background-image:url(${thumb(ids[i % ids.length].id)})"></div>`).join("")}</div></div><div style="text-align:center;color:var(--dim);font-size:13px">Music Cube · tap to play <button class="pill" data-act="cubepick">Customize 6 songs</button></div>`;
}

function renderMini() {
  const m = $("#mini"), s = cur();
  if (!s || playerOpen) { m.classList.add("hidden"); return; }
  m.classList.remove("hidden");
  m.innerHTML = `<img src="${thumb(s.id)}" alt=""><div class="t" data-act="openp"><b>${esc(s.title)}</b><small>${esc(s.artist)}</small></div><button class="ib" data-act="toggle">${playing ? "❚❚" : "▶"}</button><button class="ib" data-act="next">⏭</button>`;
}
function render() { renderMini(); const p = $("#pp"); if (p) p.textContent = playing ? "❚❚" : "▶"; }

/* ---------- full player ---------- */
function drawPlayer() {
  const s = cur(), p = $("#player"); if (!s) return;
  p.classList.remove("hidden"); $("#app").classList.add("pl"); playerOpen = true; renderMini();
  const isLiked = liked.some((l) => l.id === s.id);
  const artCls = set.hideThumb ? "art icon" : set.crop ? "art" : "art contain";
  p.innerHTML = `<div class="top"><button class="ib" data-act="closep">⌄</button><b>Now Playing</b><button class="ib" data-menu="${esc(s.id)}">⋯</button></div>
  <img class="${artCls}" src="${set.hideThumb ? "assets/icon-512.png" : thumb(s.id)}" alt="">
  <h3>${esc(s.title)}</h3><div class="ar">${esc(s.artist)}</div>
  <div class="seek"><div class="bar" id="bar"><canvas id="barc"></canvas></div><div class="times"><span id="tc">0:00</span><span id="td">0:00</span></div></div>
  <div class="ctrls"><button class="ib ${isLiked ? "on" : ""}" id="likeb" data-act="like">${isLiked ? "♥" : "♡"}</button><button class="ib" data-act="prev">⏮</button><button class="ib big" id="pp" data-act="toggle">${playing ? "❚❚" : "▶"}</button><button class="ib" data-act="next">⏭</button><button class="ib" data-act="queue">☰</button></div>
  <div class="row2"><button class="pill" data-act="lyrics">Lyrics</button><button class="pill" data-act="bgpick">Player background</button><button class="pill" data-act="sleep">Sleep timer</button><button class="pill" data-act="share">Share</button></div>
  <div id="lyr"></div>`;
  bindBar(); drawLyr(); tickPlayer();
}
function tickPlayer() {
  const tc = $("#tc"); if (!tc) return; tc.textContent = fmt(pos); $("#td").textContent = fmt(dur);
  drawBar(); const cp = $("#lyr .cur");
  if (lyr.length && lyr[0].t >= 0) { let k = -1; for (let i = 0; i < lyr.length; i++) if (lyr[i].t <= pos + .2) k = i; const ps = $$("#lyr p"); ps.forEach((e, i) => e.classList.toggle("cur", i === k)); const c = ps[k]; if (c && c !== cp && showLyr) c.scrollIntoView({ block: "center", behavior: "smooth" }); }
}
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
let showLyr = false;
function drawLyr() { const l = $("#lyr"); if (!l) return; l.innerHTML = showLyr ? (lyr.length ? lyr.map((x) => `<p>${esc(x.x)}</p>`).join("") : `<div class="empty">Looking for lyrics… or none available.</div>`) : ""; }
function drawBar() {
  const c = $("#barc"); if (!c) return; const w = c.clientWidth, h = c.clientHeight, d = devicePixelRatio || 1;
  if (c.width !== w * d) { c.width = w * d; c.height = h * d; }
  const x = c.getContext("2d"); x.setTransform(d, 0, 0, d, 0, 0); x.clearRect(0, 0, w, h);
  const f = dur ? Math.min(1, pos / dur) : 0, fx = w * f, cy = h / 2, t = performance.now() / 600, st = set.slider;
  x.lineCap = "round";
  const acc = getComputedStyle(document.documentElement).getPropertyValue("--acc") || "#a46bff";
  if (st === "bar") {
    x.fillStyle = "rgba(255,255,255,.14)"; x.beginPath(); x.roundRect(0, cy - 5, w, 10, 5); x.fill();
    const g = x.createLinearGradient(0, 0, fx || 1, 0); g.addColorStop(0, acc); g.addColorStop(1, "#fff"); x.fillStyle = g; x.beginPath(); x.roundRect(0, cy - 4, Math.max(fx, 8), 8, 4); x.fill();
    if (playing) { x.fillStyle = "rgba(255,255,255,.5)"; for (let i = 0; i < fx; i += 9) x.fillRect(i, cy - 6 + Math.sin(i / 14 + t * 2) * 1.5, 2, 2); }
    x.fillStyle = "#fff"; x.beginPath(); x.arc(Math.min(Math.max(fx, 12), w - 12), cy, 12, 0, 6.3); x.fill();
  } else if (st === "wavy") {
    x.strokeStyle = "rgba(255,255,255,.2)"; x.lineWidth = 4; x.beginPath(); x.moveTo(0, cy); x.lineTo(w, cy); x.stroke();
    x.strokeStyle = acc; x.lineWidth = 4; x.beginPath(); const amp = playing ? 6 : 2; for (let i = 0; i <= fx; i += 3) { const y = cy + Math.sin(i / 11 - t * 3) * amp; i ? x.lineTo(i, y) : x.moveTo(i, y); } x.stroke();
    x.fillStyle = "#fff"; x.beginPath(); x.arc(fx, cy + Math.sin(fx / 11 - t * 3) * amp, 9, 0, 6.3); x.fill();
  } else {
    x.strokeStyle = "rgba(255,255,255,.2)"; x.lineWidth = 3; x.beginPath(); x.moveTo(0, cy); x.lineTo(w, cy); x.stroke();
    x.strokeStyle = acc; x.beginPath(); x.moveTo(0, cy); x.lineTo(fx, cy); x.stroke(); x.fillStyle = "#fff"; x.beginPath(); x.arc(fx, cy, 6, 0, 6.3); x.fill();
  }
}
function bindBar() {
  const b = $("#bar"); if (!b) return; let drag = false;
  const at = (e) => { const r = b.getBoundingClientRect(); const px = (e.touches ? e.touches[0].clientX : e.clientX) - r.left; pos = Math.max(0, Math.min(1, px / r.width)) * dur; drawBar(); $("#tc").textContent = fmt(pos); };
  const end = () => { if (drag) { drag = false; yt && yt.seekTo(pos, true); } };
  b.addEventListener("pointerdown", (e) => { drag = true; b.setPointerCapture(e.pointerId); at(e); });
  b.addEventListener("pointermove", (e) => { if (drag) at(e); });
  b.addEventListener("pointerup", end); b.addEventListener("pointercancel", end);
}

/* ---------- sheets ---------- */
function sheet(html) { const s = $("#sheet"); s.innerHTML = `<div class="in">${html}</div>`; s.classList.remove("hidden"); s.onclick = (e) => { if (e.target === s) closeSheet(); }; }
function closeSheet() { $("#sheet").classList.add("hidden"); }
function findSong(id) { return [...queue, ...homeSongs, ...liked, ...history, ...Object.values(lists).flat()].find((s) => s.id === id); }
function songMenu(id) {
  const s = findSong(id); if (!s) return;
  sheet(`<h2 style="margin-top:0">${esc(s.title)}</h2><button class="pill" data-sm="next" data-id="${esc(id)}">Play next</button> <button class="pill" data-sm="queue" data-id="${esc(id)}">Add to queue</button> <button class="pill" data-sm="like" data-id="${esc(id)}">${liked.some((l) => l.id === id) ? "Unlike" : "Like"}</button> <button class="pill" data-sm="pl" data-id="${esc(id)}">Add to playlist</button> <button class="pill" data-sm="share" data-id="${esc(id)}">Share</button>`);
}
function queueSheet() { sheet(`<h2 style="margin-top:0">Up next</h2>` + (queue.map((s, i) => `<div class="li ${i === qi ? "now" : ""}" data-qplay="${i}"><img src="${thumb(s.id)}" alt=""><div class="t"><b>${esc(s.title)}</b><small>${esc(s.artist)}</small></div></div>`).join("") || '<div class="empty">Queue is empty.</div>')); }
function langSheet(first) {
  const sel = new Set(set.lang || []);
  sheet(`<h2 style="margin-top:0">Choose your music language</h2><p style="color:var(--dim)">Home songs follow your choice. The app always stays in English.</p><div>${Object.keys(LANGS).map((k) => `<button class="chip ${sel.has(k) ? "on" : ""}" data-lg="${k}">${esc(LANGS[k].name)}</button>`).join("")}</div><button class="pill on" data-act="langdone">Done</button>`);
  langSheet.sel = sel;
}
function cubePick() {
  const sel = new Set(set.cubeIds || []); cubePick.sel = sel;
  const pool = [...homeSongs, ...liked, ...history].filter((s, i, a) => a.findIndex((x) => x.id === s.id) === i);
  sheet(`<h2 style="margin-top:0">Pick 6 songs for your cube</h2><div id="cp">${pool.map((s) => `<div class="li" data-cp="${esc(s.id)}"><img src="${thumb(s.id)}" alt=""><div class="t"><b>${esc(s.title)}</b><small>${esc(s.artist)}</small></div><span>${sel.has(s.id) ? "✓" : ""}</span></div>`).join("")}</div><button class="pill on" data-act="cubedone">Save</button>`);
}
function sleepSheet() { sheet(`<h2 style="margin-top:0">Sleep timer</h2>${[15, 30, 45, 60].map((m) => `<button class="pill" data-sleep="${m}">${m} min</button> `).join("")}<button class="pill" data-sleep="0">Off</button>`); }
function bgSheet() { sheet(`<h2 style="margin-top:0">Player background</h2>${Object.entries(BGS).map(([k, n]) => `<button class="chip ${set.bg === k ? "on" : ""}" data-bg="${k}">${esc(n)}</button>`).join("")}`); }
function plSheet(id) { sheet(`<h2 style="margin-top:0">Add to playlist</h2>${Object.keys(playlists).map((n) => `<button class="pill" data-addpl="${esc(n)}" data-id="${esc(id)}">${esc(n)}</button> `).join("") || '<div class="empty">No playlists yet. Create one in Library.</div>'}`); }

/* ---------- Buddy AI companion ---------- */
let chat = LS.get("chat", []);
function buddyView() {
  const b = set.buddy;
  $("#view").innerHTML = `<div class="buddy-head"><img src="assets/buddy_${b}_happy.jpg" alt=""><div><h1 style="margin:0">${b === "luna" ? "Luna" : "Kai"}</h1><small style="color:var(--dim)">Your music. Your vibes.</small></div><button class="pill" data-act="swapbuddy" style="margin-left:auto">Switch</button></div><div id="chat" style="margin-top:12px">${chat.map((m) => `<div class="bubble ${m.r === "u" ? "me" : ""}">${esc(m.t)}</div>`).join("") || `<div class="empty">Ask anything, or say “play Ninnodu Paryan”.</div>`}</div><form class="search" id="cf" style="position:sticky;bottom:0"><input id="ci" placeholder="Message ${b === "luna" ? "Luna" : "Kai"}" autocomplete="off"><button class="ib">➤</button></form>`;
  $("#cf").onsubmit = async (e) => { e.preventDefault(); const t = $("#ci").value.trim(); if (!t) return; $("#ci").value = ""; chat.push({ r: "u", t }); chat = chat.slice(-30); buddyView(); await buddyReply(t); };
}
const AI_MODELS = ["openai-fast", "mistral", "openai"];
async function buddyReply(t) {
  const m = t.match(/^\s*play\s+(.+)/i);
  if (m) { try { const r = await searchSongs(m[1]); playIndex(0, r); chat.push({ r: "b", t: `Playing ${r[0].title} 🎵` }); } catch { chat.push({ r: "b", t: "I couldn't reach the music sources right now." }); } LS.set("chat", chat); return buddyView(); }
  const sys = `You are ${set.buddy === "luna" ? "Luna, a warm cute" : "Kai, a cool playful"} music companion inside the Pattukal app. Be concise and friendly. You can answer any question.`;
  const msgs = [{ role: "system", content: sys }, ...chat.slice(-8).map((x) => ({ role: x.r === "u" ? "user" : "assistant", content: x.t }))];
  const jobs = AI_MODELS.map((model) => timed("https://text.pollinations.ai/openai", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ model, messages: msgs, max_tokens: 350 }) }, 14000).then((r) => r.json()).then((j) => { const x = j.choices && j.choices[0].message.content; if (!x) throw 0; return x; }));
  try { chat.push({ r: "b", t: await anyOf(jobs) }); } catch { chat.push({ r: "b", t: "All AI sources are busy right now. Please try again in a moment." }); }
  LS.set("chat", chat); if (tab === "buddy") buddyView();
}

/* ---------- settings ---------- */
function sw(k, title, sub) { return `<div class="set"><div>${title}<small>${sub || ""}</small></div><button class="sw ${set[k] ? "on" : ""}" data-sw="${k}" aria-label="${title}"></button></div>`; }
function settingsView() {
  $("#view").innerHTML = `<h1>Settings</h1>
  <div class="set" data-act="lang"><div>Music language<small>${(set.lang || []).map((l) => LANGS[l].name).join(", ") || "Not chosen"} · the app stays in English</small></div><span>›</span></div>
  <div class="set" data-act="bgpick"><div>Player background<small>${BGS[set.bg]}</small></div><span>›</span></div>
  <div class="set"><div>Slider style</div><div>${["bar", "wavy", "slim"].map((k) => `<button class="chip ${set.slider === k ? "on" : ""}" data-sl="${k}">${k[0].toUpperCase() + k.slice(1)}</button>`).join("")}</div></div>
  ${sw("crop", "Crop thumbnail", "Fill the artwork frame")}${sw("hideThumb", "Hide thumbnail", "Shows the Pattukal icon instead")}${sw("sponsor", "SponsorBlock auto-skip", "Skips sponsor, intro and outro segments")}${sw("cube", "Music Cube on Home", "Six songs on a rotating glass cube")}
  <div class="set" data-act="cubepick"><div>Customize cube songs<small>Choose your own six</small></div><span>›</span></div>
  <div class="set" data-act="bug"><div>Report a bug<small>Opens a pre-filled email to the developer</small></div><span>›</span></div>
  <div class="set" data-act="whatsnew"><div>What's new</div><span>›</span></div>
  <h2>About</h2><div class="set"><div>Pattukal ${VERSION}<small>Contact: akshaybiju602@gmail.com<br>pattukaloffical@gmail.com</small></div></div>
  <div class="set"><div>iPhone version notes<small>Karaoke vocal removal, notification icons and the floating companion are Android-only; iOS web apps can't do them.</small></div></div>`;
}
function whatsNew() {
  sheet(`<h2 style="margin-top:0">What's new</h2><ul style="padding-left:18px;line-height:1.6"><li>Pattukal now runs on iPhone and iPad as an installable web app</li><li>Search, Home rows in your language, queue, synced lyrics and Sleep timer</li><li>Liquid-glass player with Liquid waves, Snowland, Tokyo, Fireworks and Car drift backgrounds</li><li>Slider styles: Bar, Wavy, Slim</li><li>Music Cube with six songs you choose</li><li>SponsorBlock auto-skip on by default; crop thumbnail on by default</li><li>Luna and Kai AI companion with automatic source switching</li></ul><p style="color:var(--dim)">Version ${VERSION}</p>`);
}

/* ---------- events ---------- */
document.addEventListener("click", (e) => {
  const t = e.target.closest("[data-play],[data-act],[data-menu],[data-tab],[data-sm],[data-qplay],[data-lg],[data-cp],[data-sleep],[data-bg],[data-addpl],[data-sw],[data-sl],[data-plplay],[data-pl]"); if (!t) return;
  const d = t.dataset;
  if (d.tab) { tab = d.tab; $$("#nav button").forEach((b) => b.classList.toggle("on", b === t)); $("#view").scrollTop = 0; view(); return; }
  if (d.menu) { e.stopPropagation(); return songMenu(d.menu); }
  if (d.play !== undefined) { const arr = lists[d.list] || []; return playIndex(+d.play, arr); }
  if (d.qplay !== undefined) { closeSheet(); return playIndex(+d.qplay); }
  if (d.lg) { const s = langSheet.sel; s.has(d.lg) ? s.delete(d.lg) : s.add(d.lg); t.classList.toggle("on"); return; }
  if (d.cp) { const s = cubePick.sel; if (s.has(d.cp)) s.delete(d.cp); else if (s.size < 6) s.add(d.cp); else toast("Only six songs fit on the cube"); t.lastElementChild.textContent = s.has(d.cp) ? "✓" : ""; return; }
  if (d.sleep !== undefined) { clearTimeout(sleepTimer); const m = +d.sleep; if (m) sleepTimer = setTimeout(() => { yt && yt.pauseVideo(); toast("Sleep timer ended"); }, m * 60000); toast(m ? `Sleep timer: ${m} min` : "Sleep timer off"); return closeSheet(); }
  if (d.bg) { save("bg", d.bg); Scenes.set(d.bg); closeSheet(); if (tab === "settings") settingsView(); return; }
  if (d.addpl) { const s = findSong(d.id); if (s && !playlists[d.addpl].some((x) => x.id === s.id)) playlists[d.addpl].push(s); LS.set("playlists", playlists); toast("Added"); return closeSheet(); }
  if (d.sw) { save(d.sw, !set[d.sw]); settingsView(); return; }
  if (d.sl) { save("slider", d.sl); settingsView(); return; }
  if (d.plplay !== undefined) { e.stopPropagation(); return playIndex(0, playlists[d.plplay]); }
  if (d.sm) {
    const s = findSong(d.id); if (!s) return;
    if (d.sm === "next") { queue.splice(qi + 1, 0, s); toast("Playing next"); closeSheet(); }
    else if (d.sm === "queue") { queue.push(s); toast("Added to queue"); closeSheet(); }
    else if (d.sm === "like") { toggleLike(s); closeSheet(); }
    else if (d.sm === "pl") plSheet(d.id);
    else if (d.sm === "share") { shareSong(s); closeSheet(); }
    return;
  }
  const a = d.act;
  if (a === "toggle") toggle(); else if (a === "next") next(); else if (a === "prev") prev();
  else if (a === "openp") drawPlayer(); else if (a === "closep") { $("#player").classList.add("hidden"); $("#app").classList.remove("pl"); playerOpen = false; showLyr = false; renderMini(); }
  else if (a === "like") { toggleLike(cur()); drawPlayer(); }
  else if (a === "queue") queueSheet(); else if (a === "lyrics") { showLyr = !showLyr; drawLyr(); tickPlayer(); }
  else if (a === "bgpick") bgSheet(); else if (a === "sleep") sleepSheet(); else if (a === "share") shareSong(cur());
  else if (a === "lang") langSheet(); else if (a === "langdone") { save("lang", [...langSheet.sel]); closeSheet(); homeSongs = []; view(); }
  else if (a === "cubepick") cubePick(); else if (a === "cubedone") { save("cubeIds", [...cubePick.sel]); closeSheet(); view(); }
  else if (a === "reload") view(); else if (a === "whatsnew") whatsNew();
  else if (a === "swapbuddy") { save("buddy", set.buddy === "luna" ? "kai" : "luna"); buddyView(); }
  else if (a === "newpl") { const n = prompt("Playlist name"); if (n && !playlists[n]) { playlists[n] = []; LS.set("playlists", playlists); view(); } }
  else if (a === "bug") { const m = prompt("Describe the bug"); if (m) location.href = "mailto:akshaybiju602@gmail.com?subject=" + encodeURIComponent("Pattukal iOS bug report") + "&body=" + encodeURIComponent(m + "\n\n" + navigator.userAgent); }
});
function toggleLike(s) { if (!s) return; liked = liked.some((l) => l.id === s.id) ? liked.filter((l) => l.id !== s.id) : [s, ...liked]; LS.set("liked", liked); }
function shareSong(s) { const u = `https://music.youtube.com/watch?v=${s.id}`; if (navigator.share) navigator.share({ title: s.title, url: u }).catch(() => {}); else { navigator.clipboard && navigator.clipboard.writeText(u); toast("Link copied"); } }

/* ---------- boot ---------- */
Scenes.start($("#bg")); Scenes.set(set.bg);
loadYT();
view();
if (!set.lang) setTimeout(() => langSheet(true), 400);
if ("serviceWorker" in navigator) navigator.serviceWorker.register("sw.js").catch(() => {});
})();
