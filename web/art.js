/* art.js — প্রজাতির ছবি প্রোগ্রামে আঁকা (অ্যান্ড্রয়েড অ্যাপের SpeciesArt-এর ওয়েব রূপ)।
 *
 * কোনো ছবি-ফাইল নেই: প্রতিটি প্রজাতির id/শ্রেণি/বাসস্থান থেকে একটি বীজ (seed)
 * বানিয়ে ক্যানভাসে পটভূমি, সিলুয়েট ও নকশা আঁকা হয়। ফলে ২.৫ লক্ষ প্রজাতির জন্য
 * মেগাবাইটের পর মেগাবাইট ছবি লাগে না — নেটওয়ার্ক খরচ শূন্য।
 */
(function (global) {
  'use strict';

  // ---------- বীজ ও র‍্যান্ডম ----------
  function hash32(str, salt) {
    let h = 2166136261 >>> 0;
    for (let i = 0; i < str.length; i++) {
      h ^= str.charCodeAt(i);
      h = Math.imul(h, 16777619) >>> 0;
    }
    h = (h ^ Math.imul(salt >>> 0, 2654435761 >>> 0)) >>> 0;
    h = Math.imul(h, 16777619) >>> 0;
    return h >>> 0;
  }

  function mulberry(seed) {
    let a = seed >>> 0;
    return function () {
      a = (a + 0x6D2B79F5) >>> 0;
      let t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  // ---------- রঙ ----------
  function hx(n) {
    return '#' + (n >>> 0).toString(16).padStart(6, '0');
  }
  function shade(c, amt) {
    const r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
    return ((r * (1 - amt)) | 0) << 16 | ((g * (1 - amt)) | 0) << 8 | ((b * (1 - amt)) | 0);
  }
  function tint(c, amt) {
    const r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
    return ((r + (255 - r) * amt) | 0) << 16 | ((g + (255 - g) * amt) | 0) << 8
      | ((b + (255 - b) * amt) | 0);
  }
  function mix(a, b, t) {
    const ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
    const br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
    return ((ar + (br - ar) * t) | 0) << 16 | ((ag + (bg - ag) * t) | 0) << 8
      | ((ab + (bb - ab) * t) | 0);
  }
  function alpha(c, a) {
    const r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
    return 'rgba(' + r + ',' + g + ',' + b + ',' + a + ')';
  }

  // ---------- বাসস্থান অনুযায়ী প্যালেট ----------
  const HABITAT_SKY = {
    'সুন্দরবন': [0x1b4332, 0x2d6a4f], 'ম্যানগ্রোভ': [0x1b4332, 0x2d6a4f],
    'বন': [0x2d6a4f, 0x74c69d], 'পাহাড়': [0x4a5b6b, 0x9fb3c8],
    'সমুদ্র': [0x023e8a, 0x0096c7], 'নদী': [0x04668c, 0x48cae4],
    'মরুভূমি': [0xc9a227, 0xe9c46a], 'ঘাস': [0x6a994e, 0xa7c957],
    'তৃণ': [0x6a994e, 0xa7c957], 'হিম': [0x8ecae6, 0xcdeffd],
    'শহর': [0x5c677d, 0x9ba4b4],
  };

  function palette(habitat, group, variant, rnd) {
    let top = 0x2d6a4f, bottom = 0x95d5b2;
    for (const key of Object.keys(HABITAT_SKY)) {
      if (habitat && habitat.indexOf(key) >= 0) {
        top = HABITAT_SKY[key][0];
        bottom = HABITAT_SKY[key][1];
        break;
      }
    }
    if (variant === 1) { top = shade(top, 0.55); bottom = shade(bottom, 0.45); }   // রাত
    if (variant === 2) { top = mix(top, 0xf4a261, 0.45); bottom = mix(bottom, 0xffd166, 0.5); }
    if (variant === 3) { top = mix(top, 0x90e0ef, 0.4); bottom = mix(bottom, 0xffffff, 0.35); }
    return { top: top, bottom: bottom, night: variant === 1 };
  }

  // ---------- দেহের রঙ ----------
  const GROUP_BASE = {
    mammals: [0xb5651d, 0x8d99ae, 0x6b705c, 0xa98467, 0x414833],
    birds: [0xb4472e, 0x2e6da4, 0x3d8b37, 0xc9a227, 0x6a4c93, 0x1f7a6c],
    fishes: [0x3c8da8, 0x7fa650, 0xc48a2e, 0x5a6e9c, 0x2f8f7e],
    marine: [0x9b5de5, 0xf15bb5, 0x00bbf9, 0x00f5d4, 0xfee440],
    reptiles: [0x6b7a3a, 0x8a6a3b, 0x4a6b4e, 0x7d6b4f],
    amphibians: [0x5d9b3a, 0xb08a2e, 0x3e7d6e, 0x8b5e3c],
    inverts: [0x8b5e3c, 0xa63d2f, 0x3d6b8b, 0x6b8b3d, 0x4b3d8b],
    fungi: [0xc2703d, 0xd9cba0, 0x9e5a5a, 0xb9a15e],
    plants: [0x4f7a2e, 0x6e9b3c, 0x3e6b4a, 0x8b9b3c],
    dinosaurs: [0x7a6a4f, 0x5a6b4a, 0x8b6a4a, 0x4a5b6b],
    microbes: [0x7fb069, 0x5fa8d3, 0xd3a15f, 0xa17fb0],
    viruses: [0x9b5de5, 0x00bbf9, 0xf15bb5, 0x8ac926],
  };

  function bodyColor(seed, group, venom, extinct, rnd) {
    const list = GROUP_BASE[group] || GROUP_BASE.mammals;
    let base = list[Math.floor(rnd() * list.length) % list.length];
    if (venom >= 2) base = mix(base, venom >= 4 ? 0x6a040f : 0xe9c46a, 0.45);
    if (extinct) base = mix(base, 0x8d99ae, 0.55);
    return base;
  }

  // ---------- সিলুয়েট ----------
  function blob(ctx, cx, cy, rx, ry, rnd, lumps) {
    ctx.beginPath();
    const steps = 26;
    for (let i = 0; i <= steps; i++) {
      const a = (i / steps) * Math.PI * 2;
      const wob = 1 + (rnd() - 0.5) * (lumps || 0.16);
      const x = cx + Math.cos(a) * rx * wob;
      const y = cy + Math.sin(a) * ry * wob;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.closePath();
  }

  function drawMammal(ctx, w, h, u, rnd, body, dark, light) {
    const cx = w * 0.5, cy = h * 0.62;
    // লেজ
    ctx.strokeStyle = hx(dark); ctx.lineWidth = u * 0.045; ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(cx - u * 0.3, cy);
    ctx.quadraticCurveTo(cx - u * 0.55, cy + u * 0.05, cx - u * 0.5, cy - u * 0.22);
    ctx.stroke();
    // দেহ
    ctx.fillStyle = hx(body);
    blob(ctx, cx, cy, u * 0.34, u * 0.22, rnd, 0.1);
    ctx.fill();
    // পা
    ctx.strokeStyle = hx(shade(body, 0.3)); ctx.lineWidth = u * 0.055;
    for (let i = 0; i < 4; i++) {
      const x = cx - u * 0.2 + i * u * 0.135;
      ctx.beginPath(); ctx.moveTo(x, cy + u * 0.12); ctx.lineTo(x + u * 0.01, cy + u * 0.34); ctx.stroke();
    }
    // মাথা
    ctx.fillStyle = hx(tint(body, 0.06));
    blob(ctx, cx + u * 0.3, cy - u * 0.14, u * 0.15, u * 0.13, rnd, 0.08);
    ctx.fill();
    // কান
    ctx.beginPath(); ctx.arc(cx + u * 0.26, cy - u * 0.27, u * 0.05, 0, 7); ctx.fill();
    // চোখ ও নাক
    ctx.fillStyle = hx(0x22201c);
    ctx.beginPath(); ctx.arc(cx + u * 0.34, cy - u * 0.16, u * 0.018, 0, 7); ctx.fill();
    ctx.fillStyle = hx(shade(body, 0.45));
    ctx.beginPath(); ctx.arc(cx + u * 0.43, cy - u * 0.11, u * 0.025, 0, 7); ctx.fill();
  }

  function drawBird(ctx, w, h, u, rnd, body, dark, light) {
    const cx = w * 0.5, cy = h * 0.6;
    ctx.fillStyle = hx(body);
    blob(ctx, cx, cy, u * 0.22, u * 0.26, rnd, 0.1);
    ctx.fill();
    // ডানা
    ctx.fillStyle = hx(shade(body, 0.22));
    ctx.beginPath();
    ctx.moveTo(cx - u * 0.05, cy - u * 0.1);
    ctx.quadraticCurveTo(cx - u * 0.45, cy - u * 0.05, cx - u * 0.34, cy + u * 0.2);
    ctx.quadraticCurveTo(cx - u * 0.1, cy + u * 0.12, cx - u * 0.05, cy - u * 0.1);
    ctx.fill();
    // মাথা
    ctx.fillStyle = hx(tint(body, 0.1));
    ctx.beginPath(); ctx.arc(cx + u * 0.12, cy - u * 0.28, u * 0.12, 0, 7); ctx.fill();
    // ঠোঁট
    ctx.fillStyle = hx(0xe9c46a);
    ctx.beginPath();
    ctx.moveTo(cx + u * 0.22, cy - u * 0.3);
    ctx.lineTo(cx + u * 0.42, cy - u * 0.25);
    ctx.lineTo(cx + u * 0.22, cy - u * 0.21);
    ctx.fill();
    // চোখ, পা, লেজ
    ctx.fillStyle = hx(0x22201c);
    ctx.beginPath(); ctx.arc(cx + u * 0.16, cy - u * 0.31, u * 0.02, 0, 7); ctx.fill();
    ctx.strokeStyle = hx(0x8a6a3b); ctx.lineWidth = u * 0.025;
    ctx.beginPath(); ctx.moveTo(cx - u * 0.03, cy + u * 0.24); ctx.lineTo(cx - u * 0.05, cy + u * 0.38);
    ctx.moveTo(cx + u * 0.07, cy + u * 0.24); ctx.lineTo(cx + u * 0.09, cy + u * 0.38); ctx.stroke();
    ctx.fillStyle = hx(dark);
    ctx.beginPath();
    ctx.moveTo(cx - u * 0.16, cy + u * 0.1);
    ctx.lineTo(cx - u * 0.46, cy + u * 0.28);
    ctx.lineTo(cx - u * 0.14, cy + u * 0.24);
    ctx.fill();
  }

  function drawFish(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.5, cy = h * 0.58;
    ctx.fillStyle = hx(body);
    ctx.beginPath(); ctx.ellipse(cx, cy, u * 0.34, u * 0.17, 0, 0, 7); ctx.fill();
    // লেজ
    ctx.fillStyle = hx(shade(body, 0.25));
    ctx.beginPath();
    ctx.moveTo(cx - u * 0.32, cy);
    ctx.lineTo(cx - u * 0.52, cy - u * 0.16);
    ctx.lineTo(cx - u * 0.46, cy);
    ctx.lineTo(cx - u * 0.52, cy + u * 0.16);
    ctx.fill();
    // পাখা
    ctx.beginPath();
    ctx.moveTo(cx, cy - u * 0.14); ctx.lineTo(cx + u * 0.06, cy - u * 0.3);
    ctx.lineTo(cx + u * 0.14, cy - u * 0.12); ctx.fill();
    // ডোরা
    ctx.strokeStyle = alpha(shade(body, 0.4), 0.5); ctx.lineWidth = u * 0.014;
    for (let i = 0; i < 5; i++) {
      const x = cx - u * 0.2 + i * u * 0.1;
      ctx.beginPath(); ctx.moveTo(x, cy - u * 0.12); ctx.lineTo(x - u * 0.03, cy + u * 0.12); ctx.stroke();
    }
    ctx.fillStyle = hx(0xffffff);
    ctx.beginPath(); ctx.arc(cx + u * 0.24, cy - u * 0.03, u * 0.032, 0, 7); ctx.fill();
    ctx.fillStyle = hx(0x14213d);
    ctx.beginPath(); ctx.arc(cx + u * 0.245, cy - u * 0.03, u * 0.016, 0, 7); ctx.fill();
  }

  function drawReptile(ctx, w, h, u, rnd, body, dark) {
    const cy = h * 0.62;
    ctx.strokeStyle = hx(body); ctx.lineWidth = u * 0.13; ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(w * 0.12, cy);
    for (let i = 0; i <= 8; i++) {
      const t = i / 8;
      ctx.lineTo(w * 0.12 + t * w * 0.76, cy + Math.sin(t * Math.PI * 2.4) * u * 0.11);
    }
    ctx.stroke();
    // মাথা
    ctx.fillStyle = hx(tint(body, 0.1));
    ctx.beginPath(); ctx.ellipse(w * 0.88, cy + u * 0.02, u * 0.1, u * 0.07, 0, 0, 7); ctx.fill();
    ctx.fillStyle = hx(0xe9c46a);
    ctx.beginPath(); ctx.arc(w * 0.9, cy - u * 0.01, u * 0.018, 0, 7); ctx.fill();
    // আঁশ
    ctx.fillStyle = alpha(shade(body, 0.35), 0.45);
    for (let i = 0; i < 22; i++) {
      ctx.beginPath();
      ctx.arc(w * 0.15 + rnd() * w * 0.7, cy + (rnd() - 0.5) * u * 0.1, u * 0.012, 0, 7);
      ctx.fill();
    }
  }

  function drawInsect(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.5, cy = h * 0.6;
    // ডানা
    ctx.fillStyle = alpha(tint(body, 0.55), 0.55);
    ctx.beginPath(); ctx.ellipse(cx - u * 0.18, cy - u * 0.12, u * 0.24, u * 0.1, -0.5, 0, 7); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + u * 0.18, cy - u * 0.12, u * 0.24, u * 0.1, 0.5, 0, 7); ctx.fill();
    // দেহ
    ctx.fillStyle = hx(body);
    ctx.beginPath(); ctx.ellipse(cx, cy + u * 0.05, u * 0.1, u * 0.22, 0, 0, 7); ctx.fill();
    ctx.fillStyle = hx(shade(body, 0.3));
    ctx.beginPath(); ctx.arc(cx, cy - u * 0.2, u * 0.075, 0, 7); ctx.fill();
    // পা ও অ্যান্টেনা
    ctx.strokeStyle = hx(shade(body, 0.5)); ctx.lineWidth = u * 0.018;
    for (let i = 0; i < 3; i++) {
      const y = cy - u * 0.02 + i * u * 0.1;
      ctx.beginPath();
      ctx.moveTo(cx - u * 0.08, y); ctx.lineTo(cx - u * 0.3, y + u * 0.1);
      ctx.moveTo(cx + u * 0.08, y); ctx.lineTo(cx + u * 0.3, y + u * 0.1);
      ctx.stroke();
    }
    ctx.beginPath();
    ctx.moveTo(cx - u * 0.03, cy - u * 0.26); ctx.lineTo(cx - u * 0.14, cy - u * 0.42);
    ctx.moveTo(cx + u * 0.03, cy - u * 0.26); ctx.lineTo(cx + u * 0.14, cy - u * 0.42);
    ctx.stroke();
  }

  function drawPlant(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.5;
    ctx.strokeStyle = hx(shade(body, 0.3)); ctx.lineWidth = u * 0.035; ctx.lineCap = 'round';
    ctx.beginPath(); ctx.moveTo(cx, h * 0.92); ctx.quadraticCurveTo(cx + u * 0.04, h * 0.6, cx, h * 0.34); ctx.stroke();
    ctx.fillStyle = hx(body);
    for (let i = 0; i < 9; i++) {
      const t = i / 8;
      const y = h * 0.9 - t * h * 0.55;
      const dir = i % 2 === 0 ? -1 : 1;
      ctx.beginPath();
      ctx.ellipse(cx + dir * u * 0.16, y, u * 0.17, u * 0.07, dir * 0.4, 0, 7);
      ctx.fill();
    }
    // ফুল
    ctx.fillStyle = hx(0xf4a261);
    for (let i = 0; i < 6; i++) {
      const a = i / 6 * Math.PI * 2;
      ctx.beginPath();
      ctx.arc(cx + Math.cos(a) * u * 0.08, h * 0.3 + Math.sin(a) * u * 0.08, u * 0.05, 0, 7);
      ctx.fill();
    }
    ctx.fillStyle = hx(0xffd166);
    ctx.beginPath(); ctx.arc(cx, h * 0.3, u * 0.05, 0, 7); ctx.fill();
  }

  function drawFungus(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.5, cy = h * 0.72;
    ctx.fillStyle = hx(0xe8dcc4);
    ctx.beginPath(); ctx.moveTo(cx - u * 0.07, cy); ctx.lineTo(cx - u * 0.05, cy - u * 0.22);
    ctx.lineTo(cx + u * 0.05, cy - u * 0.22); ctx.lineTo(cx + u * 0.07, cy); ctx.fill();
    ctx.fillStyle = hx(body);
    ctx.beginPath(); ctx.ellipse(cx, cy - u * 0.24, u * 0.28, u * 0.17, 0, Math.PI, 0); ctx.fill();
    ctx.fillStyle = alpha(0xffffff, 0.6);
    for (let i = 0; i < 7; i++) {
      ctx.beginPath();
      ctx.arc(cx - u * 0.22 + rnd() * u * 0.44, cy - u * (0.24 + rnd() * 0.13), u * 0.025, 0, 7);
      ctx.fill();
    }
  }

  function drawMarine(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.5, cy = h * 0.55;
    for (let k = 0; k < 6; k++) {
      ctx.strokeStyle = alpha(mix(body, 0xffffff, k * 0.1), 0.85);
      ctx.lineWidth = u * 0.035;
      ctx.beginPath();
      ctx.moveTo(cx - u * 0.2 + k * u * 0.08, cy);
      ctx.quadraticCurveTo(cx - u * 0.2 + k * u * 0.08 + u * 0.06, cy + u * 0.25,
        cx - u * 0.2 + k * u * 0.08, cy + u * 0.45);
      ctx.stroke();
    }
    ctx.fillStyle = hx(body);
    ctx.beginPath(); ctx.ellipse(cx, cy - u * 0.05, u * 0.3, u * 0.18, 0, Math.PI, 0); ctx.fill();
    ctx.fillStyle = alpha(0xffffff, 0.35);
    ctx.beginPath(); ctx.ellipse(cx, cy - u * 0.1, u * 0.2, u * 0.08, 0, Math.PI, 0); ctx.fill();
  }

  function drawMicrobe(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.5, cy = h * 0.55;
    ctx.fillStyle = alpha(body, 0.9);
    ctx.beginPath(); ctx.ellipse(cx, cy, u * 0.26, u * 0.17, rnd() * 3, 0, 7); ctx.fill();
    ctx.strokeStyle = alpha(shade(body, 0.3), 0.9); ctx.lineWidth = u * 0.02;
    for (let i = 0; i < 10; i++) {
      const a = rnd() * Math.PI * 2;
      ctx.beginPath();
      ctx.moveTo(cx + Math.cos(a) * u * 0.24, cy + Math.sin(a) * u * 0.15);
      ctx.lineTo(cx + Math.cos(a) * u * 0.4, cy + Math.sin(a) * u * 0.28);
      ctx.stroke();
    }
    ctx.fillStyle = alpha(tint(body, 0.5), 0.7);
    ctx.beginPath(); ctx.arc(cx - u * 0.06, cy - u * 0.03, u * 0.06, 0, 7); ctx.fill();
  }

  function drawVirus(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.5, cy = h * 0.55, rr = u * 0.2;
    ctx.strokeStyle = hx(shade(body, 0.2)); ctx.lineWidth = u * 0.03;
    for (let i = 0; i < 12; i++) {
      const a = i / 12 * Math.PI * 2;
      ctx.beginPath();
      ctx.moveTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
      ctx.lineTo(cx + Math.cos(a) * rr * 1.6, cy + Math.sin(a) * rr * 1.6);
      ctx.stroke();
      ctx.fillStyle = hx(tint(body, 0.3));
      ctx.beginPath();
      ctx.arc(cx + Math.cos(a) * rr * 1.65, cy + Math.sin(a) * rr * 1.65, u * 0.03, 0, 7);
      ctx.fill();
    }
    ctx.fillStyle = hx(body);
    ctx.beginPath(); ctx.arc(cx, cy, rr, 0, 7); ctx.fill();
    ctx.fillStyle = alpha(shade(body, 0.45), 0.6);
    ctx.beginPath(); ctx.arc(cx, cy, rr * 0.55, 0, 7); ctx.fill();
  }

  function drawDino(ctx, w, h, u, rnd, body, dark) {
    const cx = w * 0.46, cy = h * 0.6;
    ctx.fillStyle = hx(body);
    blob(ctx, cx, cy, u * 0.3, u * 0.2, rnd, 0.08); ctx.fill();
    // লেজ ও গলা
    ctx.strokeStyle = hx(body); ctx.lineWidth = u * 0.1; ctx.lineCap = 'round';
    ctx.beginPath(); ctx.moveTo(cx - u * 0.24, cy); ctx.lineTo(cx - u * 0.55, cy - u * 0.12); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx + u * 0.22, cy - u * 0.06);
    ctx.quadraticCurveTo(cx + u * 0.4, cy - u * 0.2, cx + u * 0.36, cy - u * 0.36); ctx.stroke();
    ctx.fillStyle = hx(tint(body, 0.08));
    ctx.beginPath(); ctx.ellipse(cx + u * 0.37, cy - u * 0.4, u * 0.11, u * 0.08, -0.3, 0, 7); ctx.fill();
    // পা
    ctx.strokeStyle = hx(shade(body, 0.3)); ctx.lineWidth = u * 0.07;
    ctx.beginPath(); ctx.moveTo(cx - u * 0.1, cy + u * 0.14); ctx.lineTo(cx - u * 0.12, cy + u * 0.36);
    ctx.moveTo(cx + u * 0.12, cy + u * 0.14); ctx.lineTo(cx + u * 0.14, cy + u * 0.36); ctx.stroke();
    // প্লেট
    ctx.fillStyle = hx(0xe9c46a);
    for (let i = 0; i < 5; i++) {
      const x = cx - u * 0.2 + i * u * 0.1;
      ctx.beginPath();
      ctx.moveTo(x - u * 0.04, cy - u * 0.16);
      ctx.lineTo(x, cy - u * 0.3);
      ctx.lineTo(x + u * 0.04, cy - u * 0.16);
      ctx.fill();
    }
  }

  const PAINTERS = {
    mammals: drawMammal, birds: drawBird, fishes: drawFish, reptiles: drawReptile,
    amphibians: drawReptile, inverts: drawInsect, plants: drawPlant, fungi: drawFungus,
    marine: drawMarine, microbes: drawMicrobe, viruses: drawVirus, dinosaurs: drawDino,
  };

  // ---------- পটভূমি ----------
  function backdrop(ctx, w, h, u, rnd, pal, group) {
    const night = pal.night;
    if (night) {
      ctx.fillStyle = alpha(0xffffff, 0.8);
      for (let i = 0; i < 26; i++) {
        ctx.beginPath(); ctx.arc(rnd() * w, rnd() * h * 0.6, u * 0.006 + rnd() * u * 0.008, 0, 7); ctx.fill();
      }
      ctx.fillStyle = alpha(0xfff3b0, 0.9);
      ctx.beginPath(); ctx.arc(w * 0.78, h * 0.2, u * 0.09, 0, 7); ctx.fill();
    } else {
      ctx.fillStyle = alpha(0xfff3b0, 0.55);
      ctx.beginPath(); ctx.arc(w * 0.8, h * 0.18, u * 0.1, 0, 7); ctx.fill();
    }
    // দূরের পাহাড়/তরঙ্গ
    ctx.fillStyle = alpha(shade(pal.bottom, 0.35), 0.5);
    ctx.beginPath();
    ctx.moveTo(0, h * 0.72);
    for (let i = 0; i <= 6; i++) {
      ctx.lineTo(i * w / 6, h * (0.62 + rnd() * 0.14));
    }
    ctx.lineTo(w, h); ctx.lineTo(0, h); ctx.fill();
    // মাটি
    ctx.fillStyle = alpha(shade(pal.bottom, 0.55), 0.85);
    ctx.beginPath();
    ctx.moveTo(0, h * 0.86);
    ctx.quadraticCurveTo(w * 0.5, h * 0.8, w, h * 0.88);
    ctx.lineTo(w, h); ctx.lineTo(0, h); ctx.fill();
    // সামনের ঘাস/বুদবুদ
    ctx.strokeStyle = alpha(shade(pal.bottom, 0.7), 0.55); ctx.lineWidth = u * 0.012;
    for (let i = 0; i < 22; i++) {
      const x = rnd() * w, y = h * (0.88 + rnd() * 0.1);
      ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + (rnd() - 0.5) * u * 0.05, y - u * 0.07); ctx.stroke();
    }
  }

  /** প্রধান আঁকার ফাংশন। */
  function draw(canvas, s, variant, detail) {
    const w = canvas.width, h = canvas.height;
    const ctx = canvas.getContext('2d');
    const seed = hash32(String(s.id) + '|' + (s.class_id || '') + '|' + (s.group_id || ''),
      (variant || 0) + 1);
    const rnd = mulberry(seed);
    const u = Math.min(w, h);
    const pal = palette(s.habitat_bn || '', s.group_id, variant || 0, rnd);

    const g = ctx.createLinearGradient(0, 0, 0, h);
    g.addColorStop(0, hx(pal.top));
    g.addColorStop(1, hx(pal.bottom));
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, w, h);

    backdrop(ctx, w, h, u, rnd, pal, s.group_id);

    const body = bodyColor(seed, s.group_id, s.venom_level || 0, !!s.extinct, rnd);
    const dark = shade(body, 0.35);
    const light = tint(body, 0.3);

    // হালকা ছায়া
    ctx.fillStyle = alpha(0x000000, 0.18);
    ctx.beginPath(); ctx.ellipse(w * 0.5, h * 0.9, u * 0.3, u * 0.05, 0, 0, 7); ctx.fill();

    const painter = PAINTERS[s.group_id] || drawMammal;
    painter(ctx, w, h, u, rnd, body, dark, light);

    // নকশা (ডোরা/ছোপ)
    if (detail !== false) {
      const pattern = seed % 3;
      ctx.save();
      ctx.globalAlpha = 0.28;
      ctx.fillStyle = hx(shade(body, 0.55));
      if (pattern === 0) {
        for (let i = 0; i < 7; i++) {
          ctx.fillRect(w * 0.3 + i * u * 0.06, h * 0.5, u * 0.018, u * 0.22);
        }
      } else if (pattern === 1) {
        for (let i = 0; i < 16; i++) {
          ctx.beginPath();
          ctx.arc(w * 0.32 + rnd() * w * 0.36, h * 0.48 + rnd() * h * 0.24, u * 0.018, 0, 7);
          ctx.fill();
        }
      }
      ctx.restore();
    }

    // বিষাক্ত / বিলুপ্ত সংকেত
    if (s.venom_level >= 3) {
      ctx.fillStyle = alpha(0x6a040f, 0.22);
      ctx.fillRect(0, 0, w, h);
    }
    if (s.extinct) {
      ctx.fillStyle = alpha(0x2b2d42, 0.28);
      ctx.fillRect(0, 0, w, h);
    }

    // ভিনেট
    const vg = ctx.createRadialGradient(w / 2, h / 2, u * 0.2, w / 2, h / 2, u * 0.85);
    vg.addColorStop(0, 'rgba(0,0,0,0)');
    vg.addColorStop(1, 'rgba(0,0,0,0.35)');
    ctx.fillStyle = vg;
    ctx.fillRect(0, 0, w, h);
  }

  /** থাম্বনেইল — কম বিস্তারিত। */
  function drawThumb(canvas, s) { draw(canvas, s, 0, false); }

  /** ছবির URL (ক্যাশসহ)। */
  const urlCache = new Map();
  function url(s, variant, w, h) {
    const key = s.id + ':' + variant + ':' + w + 'x' + h;
    if (urlCache.has(key)) return urlCache.get(key);
    const c = document.createElement('canvas');
    c.width = w; c.height = h;
    draw(c, s, variant || 0, true);
    const u = c.toDataURL('image/webp', 0.82);
    urlCache.set(key, u);
    return u;
  }

  global.PrakritiArt = { draw: draw, drawThumb: drawThumb, url: url, hash32: hash32 };
})(window);
