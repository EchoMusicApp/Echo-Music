/* Animated player backgrounds: waves, snowland, tokyo, fireworks, drift, plain */
(function () {
  const S = { raf: 0, name: "waves", cv: null, ctx: null, w: 0, h: 0, t: 0, tint: "#a46bff", paused: false };
  const rnd = (a, b) => a + Math.random() * (b - a);
  let flakes = [], sparks = [], rockets = [], trails = [], rain = [];

  function resize() {
    const d = Math.min(window.devicePixelRatio || 1, 2);
    S.w = S.cv.clientWidth; S.h = S.cv.clientHeight;
    S.cv.width = S.w * d; S.cv.height = S.h * d;
    S.ctx.setTransform(d, 0, 0, d, 0, 0);
    flakes = Array.from({ length: 110 }, () => ({ x: rnd(0, S.w), y: rnd(0, S.h), r: rnd(1, 3.6), v: rnd(.3, 1.2), s: rnd(0, 6) }));
    rain = Array.from({ length: 70 }, () => ({ x: rnd(0, S.w), y: rnd(0, S.h), v: rnd(5, 11) }));
  }

  function waves(c, w, h, t) {
    const g = c.createLinearGradient(0, 0, 0, h); g.addColorStop(0, "#120a26"); g.addColorStop(1, "#05030c");
    c.fillStyle = g; c.fillRect(0, 0, w, h);
    for (let i = 0; i < 11; i++) {
      const f = i / 10, base = h * (.28 + .6 * f), amp = h * (.016 + .02 * Math.sin(f * Math.PI));
      c.beginPath();
      for (let x = 0; x <= w; x += 8) {
        const u = x / w;
        const y = base + amp * Math.sin(u * (1.4 + f * 1.4) * 6.283 + t * (i % 2 ? -.9 : .9) + i) + amp * .5 * Math.sin(u * (3 + f) * 6.283 - t * 1.4);
        x ? c.lineTo(x, y) : c.moveTo(x, y);
      }
      const a = .1 + .22 * (1 - Math.abs(f - .5) * 1.7);
      const lg = c.createLinearGradient(0, 0, w, 0);
      lg.addColorStop(0, "rgba(255,255,255,0)"); lg.addColorStop(.35, S.tint); lg.addColorStop(.5, "#fff"); lg.addColorStop(.65, S.tint); lg.addColorStop(1, "rgba(255,255,255,0)");
      c.globalAlpha = a * .5; c.lineWidth = 7; c.strokeStyle = lg; c.stroke();
      c.globalAlpha = a * 1.6; c.lineWidth = 1.6; c.stroke(); c.globalAlpha = 1;
    }
  }

  function snow(c, w, h, t) {
    const g = c.createLinearGradient(0, 0, 0, h); g.addColorStop(0, "#0a1830"); g.addColorStop(.7, "#1a3558"); g.addColorStop(1, "#cfe4ff");
    c.fillStyle = g; c.fillRect(0, 0, w, h);
    c.fillStyle = "rgba(255,255,255,.9)";
    c.beginPath(); c.moveTo(0, h);
    for (let x = 0; x <= w; x += 10) c.lineTo(x, h * .88 + Math.sin(x / 70) * 10 + Math.sin(x / 23) * 4);
    c.lineTo(w, h); c.fill();
    for (const f of flakes) {
      f.y += f.v; f.x += Math.sin(t + f.s) * .5;
      if (f.y > h) { f.y = -5; f.x = rnd(0, w); }
      c.globalAlpha = .4 + f.r / 6; c.beginPath(); c.arc(f.x, f.y, f.r, 0, 6.283); c.fillStyle = "#fff"; c.fill();
    }
    c.globalAlpha = 1;
  }

  function tokyo(c, w, h, t) {
    const g = c.createLinearGradient(0, 0, 0, h); g.addColorStop(0, "#05061a"); g.addColorStop(.6, "#1a0f3d"); g.addColorStop(1, "#04050c");
    c.fillStyle = g; c.fillRect(0, 0, w, h);
    const hz = h * .66, cols = ["#ff3fa4", "#2de2ff", "#ffd23f", "#9b6bff"];
    for (let layer = 0; layer < 3; layer++) {
      let x = -((t * (6 + layer * 8)) % 120);
      let seed = layer * 17;
      while (x < w + 120) {
        seed = (seed * 9301 + 49297) % 233280;
        const bw = 36 + (seed % 60), bh = h * (.12 + layer * .05) + (seed % 140);
        c.fillStyle = layer == 0 ? "#0b0d24" : layer == 1 ? "#0e1130" : "#14173d";
        c.fillRect(x, hz - bh, bw, bh);
        for (let wy = hz - bh + 8; wy < hz - 8; wy += 11) for (let wx = x + 5; wx < x + bw - 6; wx += 9) {
          const on = ((wx * 7 + wy * 13 + seed) % 5) > 1;
          if (on) { c.fillStyle = cols[(wx + wy + seed | 0) % 4]; c.globalAlpha = .35 + .35 * Math.sin(t * 2 + wx + wy); c.fillRect(wx, wy, 3, 5); }
        }
        c.globalAlpha = 1;
        if (seed % 4 == 0) { const col = cols[seed % 4 + layer]; c.fillStyle = col; c.shadowColor = col; c.shadowBlur = 14; c.fillRect(x + 6, hz - bh + 14, bw - 12, 10); c.shadowBlur = 0; }
        x += bw + 6;
      }
    }
    // tokyo tower
    const tx = w * .78; c.strokeStyle = "#ff5a3c"; c.shadowColor = "#ff5a3c"; c.shadowBlur = 12; c.lineWidth = 2;
    c.beginPath(); c.moveTo(tx - 34, hz); c.lineTo(tx, hz - h * .34); c.lineTo(tx + 34, hz); c.moveTo(tx - 20, hz - h * .12); c.lineTo(tx + 20, hz - h * .12); c.stroke(); c.shadowBlur = 0;
    // wet street with light trails
    const sg = c.createLinearGradient(0, hz, 0, h); sg.addColorStop(0, "#14102e"); sg.addColorStop(1, "#05050c"); c.fillStyle = sg; c.fillRect(0, hz, w, h - hz);
    for (let i = 0; i < 14; i++) { const y = hz + 10 + (i * 31 + t * (30 + i * 6)) % (h - hz - 10); const x = (i * 97 + t * (60 + i * 8) * (i % 2 ? 1 : -1)) % (w + 200); const xx = x < 0 ? x + w + 200 : x; c.fillStyle = i % 2 ? "rgba(255,70,120,.7)" : "rgba(60,220,255,.7)"; c.fillRect(xx - 100, y, 90, 2.2); }
    c.strokeStyle = "rgba(190,210,255,.25)"; c.lineWidth = 1;
    for (const r of rain) { r.y += r.v; r.x -= 1; if (r.y > h) { r.y = -10; r.x = rnd(0, w); } c.beginPath(); c.moveTo(r.x, r.y); c.lineTo(r.x + 2, r.y + 9); c.stroke(); }
  }

  function fireworks(c, w, h, t) {
    c.fillStyle = "rgba(3,5,16,.22)"; c.fillRect(0, 0, w, h);
    if (Math.random() < .035) rockets.push({ x: rnd(w * .15, w * .85), y: h, ty: rnd(h * .15, h * .5), v: rnd(6, 9), hue: rnd(0, 360) });
    for (let i = rockets.length - 1; i >= 0; i--) {
      const r = rockets[i]; r.y -= r.v; c.fillStyle = "#fff"; c.fillRect(r.x, r.y, 2, 8);
      if (r.y <= r.ty) { for (let k = 0; k < 70; k++) { const a = Math.random() * 6.283, s = rnd(1, 5); sparks.push({ x: r.x, y: r.y, vx: Math.cos(a) * s, vy: Math.sin(a) * s, life: 1, hue: r.hue + rnd(-20, 20) }); } rockets.splice(i, 1); }
    }
    for (let i = sparks.length - 1; i >= 0; i--) {
      const p = sparks[i]; p.x += p.vx; p.y += p.vy; p.vy += .045; p.vx *= .985; p.life -= .014;
      if (p.life <= 0) { sparks.splice(i, 1); continue; }
      c.fillStyle = `hsla(${p.hue},95%,62%,${p.life})`; c.beginPath(); c.arc(p.x, p.y, 1.8, 0, 6.283); c.fill();
    }
  }

  function drift(c, w, h, t) {
    const g = c.createLinearGradient(0, 0, 0, h); g.addColorStop(0, "#0d0f1c"); g.addColorStop(1, "#1b1b26"); c.fillStyle = g; c.fillRect(0, 0, w, h);
    const cx = w / 2, cy = h * .62, R = Math.min(w, h) * .26, a = t * 1.5;
    c.strokeStyle = "rgba(255,255,255,.07)"; c.lineWidth = 26; c.beginPath(); c.ellipse(cx, cy, R * 1.5, R * .62, 0, 0, 6.283); c.stroke();
    // skid marks
    for (let i = 0; i < 90; i++) { const aa = a - i * .028; c.fillStyle = `rgba(0,0,0,${.5 - i * .0055})`; c.beginPath(); c.arc(cx + Math.cos(aa) * R * 1.5, cy + Math.sin(aa) * R * .62, 4.5, 0, 6.283); c.fill(); c.fillStyle = `rgba(220,220,230,${.14 - i * .0015})`; c.beginPath(); c.arc(cx + Math.cos(aa) * R * 1.5 - 8, cy + Math.sin(aa) * R * .62 - 8, 8 + i * .15, 0, 6.283); c.fill(); }
    const px = cx + Math.cos(a) * R * 1.5, py = cy + Math.sin(a) * R * .62;
    c.save(); c.translate(px, py); c.rotate(a + Math.PI / 2 + .55);
    c.fillStyle = "#e8e6df"; c.beginPath(); c.roundRect ? c.roundRect(-17, -38, 34, 76, 8) : c.rect(-17, -38, 34, 76); c.fill();
    c.fillStyle = "#1a2a4a"; c.fillRect(-12, -16, 24, 12); c.fillRect(-12, 10, 24, 9);
    c.fillStyle = "#2b6be0"; c.fillRect(-4, -38, 3, 20); c.fillStyle = "#6b3bd6"; c.fillRect(-1, -38, 3, 20); c.fillStyle = "#e0283a"; c.fillRect(2, -38, 3, 20);
    c.fillStyle = "#fff6b0"; c.shadowColor = "#fff6b0"; c.shadowBlur = 16; c.fillRect(-14, -40, 7, 3); c.fillRect(7, -40, 7, 3); c.shadowBlur = 0;
    c.restore();
  }

  function frame(ts) {
    S.raf = requestAnimationFrame(frame);
    if (S.paused) return;
    S.t = ts / 1000;
    const c = S.ctx, w = S.w, h = S.h, t = S.t;
    if (S.name === "snow") snow(c, w, h, t);
    else if (S.name === "tokyo") tokyo(c, w, h, t);
    else if (S.name === "fireworks") fireworks(c, w, h, t);
    else if (S.name === "drift") drift(c, w, h, t);
    else if (S.name === "plain") { c.fillStyle = "#0b0b0b"; c.fillRect(0, 0, w, h); }
    else waves(c, w, h, t);
  }

  window.Scenes = {
    start(cv) { S.cv = cv; S.ctx = cv.getContext("2d"); resize(); window.addEventListener("resize", resize); cancelAnimationFrame(S.raf); S.raf = requestAnimationFrame(frame); },
    set(name) { S.name = name; if (S.ctx) { S.ctx.fillStyle = "#05030c"; S.ctx.fillRect(0, 0, S.w, S.h); } },
    tint(c) { S.tint = c; },
    pause(p) { S.paused = p; },
  };
})();
