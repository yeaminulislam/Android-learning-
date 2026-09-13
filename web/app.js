/* app.js — প্রকৃতি কোষের ওয়েব প্রোটোটাইপ (অ্যান্ড্রয়েড অ্যাপের স্ক্রিনগুলো হুবহু নকল)।
 *
 * এখানে ডেটা ছোট নমুনা (৬০০ প্রজাতি); অ্যাপে একই কোড-পথ SQLite (২.৫ লক্ষ সারি)
 * থেকে চলে। স্ক্রিন: স্প্ল্যাশ → পরিচয় → প্রধান → শ্রেণিবিন্যাস → তালিকা → বিস্তারিত
 * → অনুসন্ধান → সেটিংস।
 */
(function () {
  'use strict';

  const PAGE_SIZE = 24;          // অ্যাপের মতোই এক পৃষ্ঠায় ২৪টি
  const DEBOUNCE_MS = 220;       // অনুসন্ধানের ডিবাউন্স

  const BN_DIGITS = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'];
  const bn = (n) => String(n).replace(/\d/g, (d) => BN_DIGITS[+d]);
  const human = (n) => {
    if (n >= 100000) return bn((n / 100000).toFixed(2).replace(/\.?0+$/, '')) + ' লক্ষ';
    if (n >= 1000) return bn((n / 1000).toFixed(1).replace(/\.0$/, '')) + ' হাজার';
    return bn(n);
  };
  const IUCN = {
    LC: ['ন্যূনতম ঝুঁকি', 'var(--iucn-lc)'], NT: ['প্রায়-বিপন্ন', 'var(--iucn-nt)'],
    VU: ['বিপন্ন', 'var(--iucn-vu)'], EN: ['সংকটাপন্ন', 'var(--iucn-en)'],
    CR: ['মারাত্মক সংকটাপন্ন', 'var(--iucn-cr)'], EW: ['বন্যে বিলুপ্ত', 'var(--iucn-ew)'],
    EX: ['বিলুপ্ত', 'var(--iucn-ex)'], DD: ['তথ্য অপ্রতুল', 'var(--text-muted)'],
  };
  const VENOM = ['নিরীহ', 'সামান্য বিষ', 'মাঝারি বিষ', 'তীব্র বিষ', 'মারাত্মক বিষ'];

  // ---------- ডেটা ----------
  const state = {
    species: [], taxonomy: null, byId: new Map(),
    groups: [], classes: [], orders: [], families: [],
    favorites: new Set(JSON.parse(localStorage.getItem('pk.fav') || '[]')),
    notes: JSON.parse(localStorage.getItem('pk.notes') || '{}'),
    introSeen: localStorage.getItem('pk.intro') === '1',
    screen: 'splash', route: {},
    list: { items: [], cursor: 0, hasMore: true, filter: null, sort: 'seq', title: '', color: 'var(--green-primary)' },
  };

  function savePrefs() {
    localStorage.setItem('pk.fav', JSON.stringify([...state.favorites]));
    localStorage.setItem('pk.notes', JSON.stringify(state.notes));
  }

  // ---------- DOM ----------
  const $ = (sel) => document.querySelector(sel);
  const el = (tag, cls, html) => {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (html !== undefined) n.innerHTML = html;
    return n;
  };

  function artCanvas(s, w, h, variant) {
    const c = document.createElement('canvas');
    c.width = w; c.height = h;
    window.PrakritiArt.draw(c, s, variant || 0, true);
    return c;
  }

  // ---------- স্ক্রিন কাঠামো ----------
  const phone = $('#phone');
  const toolbar = $('#toolbar');
  const chipbar = $('#chipbar');
  const screenEl = $('#screen');

  function setToolbar(title, subtitle, color, opts) {
    opts = opts || {};
    toolbar.style.background = color || 'var(--green-primary)';
    toolbar.innerHTML = '';
    if (opts.back !== false) {
      const b = el('div', 'back', '‹');
      b.onclick = opts.onBack || (() => history.back());
      toolbar.appendChild(b);
    }
    const t = el('div', 'titles');
    t.appendChild(el('h1', null, title || ''));
    if (subtitle) t.appendChild(el('div', 'subtitle', subtitle));
    toolbar.appendChild(t);
    (opts.actions || []).forEach((a) => {
      const n = el('div', a.text ? 'action' : 'icon', a.text || a.icon);
      n.onclick = a.onClick;
      toolbar.appendChild(n);
    });
    if (opts.search !== false) {
      const s = el('div', 'icon', '🔎');
      s.onclick = () => go('search');
      toolbar.appendChild(s);
    }
  }

  function setChips(chips) {
    chipbar.innerHTML = '';
    if (!chips || !chips.length) { chipbar.style.display = 'none'; return; }
    chipbar.style.display = 'flex';
    chips.forEach((c) => {
      const n = el('div', 'chip' + (c.on ? ' on' : '') + (c.ghost ? ' ghost' : ''), c.label);
      n.onclick = c.onClick;
      chipbar.appendChild(n);
    });
  }

  function go(screen, route) {
    state.screen = screen;
    state.route = route || {};
    render();
  }

  // ---------- প্রধান পর্দা ----------
  function renderMain() {
    setToolbar('প্রকৃতি কোষ', '২.৫ লক্ষ প্রজাতির অফলাইন বিশ্বকোষ', 'var(--green-primary)',
      { back: false, actions: [{ icon: '⚙', onClick: () => go('settings') }] });
    setChips(null);
    screenEl.className = 'screen pad';
    screenEl.innerHTML = '';

    const popular = [...state.species].sort((a, b) => b.popularity - a.popularity).slice(0, 12);
    const threatened = state.species.filter((s) => ['VU', 'EN', 'CR'].includes(s.iucn)).slice(0, 12);
    const total = state.taxonomy.groups.reduce((a, g) => a + g.species_count, 0);
    const extinct = state.species.filter((s) => s.extinct).length;

    // হিরো
    const hero = el('div', 'hero');
    const row = el('div', 'hero-row');
    const txt = el('div');
    txt.style.flex = '1';
    txt.appendChild(el('div', 'kicker', 'পৃথিবীর জীবজগৎ'));
    txt.appendChild(el('div', 'big', human(total) + ' প্রজাতি'));
    txt.appendChild(el('p', null,
      'বাংলা নাম, ইংরেজি নাম, বৈজ্ঞানিক নাম, বাসস্থান, খাদ্য, প্রজনন, আকার ও বিষাক্ততা — '
      + 'সব কিছু এক জায়গায়, সম্পূর্ণ অফলাইনে।'));
    row.appendChild(txt);
    const ha = el('div', 'hero-art');
    ha.appendChild(artCanvas(popular[0], 184, 184, 1));
    row.appendChild(ha);
    hero.appendChild(row);
    const fs = el('div', 'fake-search', '🔎  প্রাণী, উদ্ভিদ বা বৈজ্ঞানিক নাম খুঁজুন…');
    fs.onclick = () => go('search');
    hero.appendChild(fs);
    screenEl.appendChild(hero);

    // দ্রুত প্রবেশ
    screenEl.appendChild(sectionTitle('দ্রুত প্রবেশ', null));
    const quick = el('div', 'quick');
    [
      ['🌟', 'জনপ্রিয়', human(popular.length), () => openList('popular', 'সবচেয়ে পরিচিত প্রজাতি', 'var(--green-primary)')],
      ['⚠️', 'বিপন্ন', human(threatened.length), () => openList('threatened', 'বিপন্ন প্রজাতি', 'var(--iucn-en)')],
      ['🕯', 'বিলুপ্ত', bn(extinct), () => openList('extinct', 'বিলুপ্ত প্রজাতি', 'var(--iucn-ex)')],
      ['☠', 'বিষাক্ত', bn(state.species.filter((s) => s.venom_level >= 2).length),
        () => openList('venom', 'বিষাক্ত প্রজাতি', 'var(--venom-4)')],
      ['❤', 'পছন্দ', bn(state.favorites.size), () => openList('fav', 'পছন্দের প্রজাতি', 'var(--green-accent)')],
      ['🗂', 'সব প্রজাতি', human(state.species.length), () => openList('all', 'সব প্রজাতি', 'var(--green-dark)')],
    ].forEach(([emoji, label, count, onClick]) => {
      const q = el('div', 'q');
      q.appendChild(el('div', 'emoji', emoji));
      q.appendChild(el('div', 'label', label));
      q.appendChild(el('div', 'count', count));
      q.onclick = onClick;
      quick.appendChild(q);
    });
    screenEl.appendChild(quick);

    // ১২টি বিভাগ
    screenEl.appendChild(sectionTitle('১২টি প্রধান বিভাগ', human(total) + ' প্রজাতি'));
    const grid = el('div', 'grid2');
    state.taxonomy.groups.forEach((g) => {
      const c = el('div', 'group-card');
      c.style.background = `linear-gradient(180deg, ${g.color}1a, ${g.color}33)`;
      c.style.borderColor = g.color + '55';
      c.appendChild(el('div', 'emoji', g.emoji));
      c.appendChild(el('h3', null, g.bn_name));
      c.appendChild(el('div', 'en', g.en_name));
      c.appendChild(el('div', 'badges',
        `<span class="badge" style="background:${g.color}">${human(g.species_count)} প্রজাতি</span>`));
      c.appendChild(el('div', 'meta',
        bn(g.family_count) + ' পরিবার · ' + bn(g.order_count) + ' বর্গ'));
      c.onclick = () => go('taxonomy', { group: g });
      grid.appendChild(c);
    });
    screenEl.appendChild(grid);

    // পরিচিত ও বিপন্ন
    screenEl.appendChild(sectionTitle('সবচেয়ে পরিচিত', 'জনপ্রিয়তার ক্রমে'));
    screenEl.appendChild(strip(popular));
    screenEl.appendChild(sectionTitle('বিপন্ন প্রজাতি', 'সংরক্ষণ প্রয়োজন'));
    screenEl.appendChild(strip(threatened));

    screenEl.appendChild(el('div', 'footer-note',
      'সম্পূর্ণ অফলাইন — কোনো ইন্টারনেট অনুমতি নেই। ডেটা: '
      + human(total) + ' প্রজাতি · ' + bn(state.taxonomy.groups.length) + ' বিভাগ · '
      + bn(state.taxonomy.families.length) + ' পরিবার (নমুনায়)।<br>'
      + 'প্রোটোটাইপে ' + bn(state.species.length) + ' প্রজাতি দেখানো হচ্ছে; '
      + 'অ্যাপে ২,৫০,০০০ প্রজাতি SQLite + FTS5 ডেটাবেসে থাকে।'));
  }

  function sectionTitle(title, sub) {
    const n = el('div', 'section-title');
    n.appendChild(el('h2', null, title));
    if (sub) n.appendChild(el('span', null, sub));
    return n;
  }

  function strip(list) {
    const st = el('div', 'strip');
    list.forEach((s) => {
      const c = el('div', 's-card');
      const a = el('div', 'art');
      a.appendChild(artCanvas(s, 296, 184, 0));
      c.appendChild(a);
      const t = el('div', 'txt');
      t.appendChild(el('div', 'bn', s.bn_name));
      t.appendChild(el('div', 'en', s.en_name || '—'));
      const [label, color] = IUCN[s.iucn] || ['—', '#888'];
      t.appendChild(el('div', 'badges', `<span class="badge" style="background:${color}">${s.iucn} · ${label}</span>`));
      c.appendChild(t);
      c.onclick = () => go('detail', { id: s.id });
      st.appendChild(c);
    });
    return st;
  }

  function rowCard(s) {
    const r = el('div', 'row-card');
    const a = el('div', 'row-art');
    a.appendChild(artCanvas(s, 176, 176, 0));
    r.appendChild(a);
    const t = el('div', 'row-text');
    t.appendChild(el('div', 'bn', s.bn_name));
    t.appendChild(el('div', 'en', s.en_name || '—'));
    t.appendChild(el('div', 'sci', s.sci_name));
    const [label, color] = IUCN[s.iucn] || ['—', '#888'];
    let badges = `<span class="badge" style="background:${color}">${s.iucn}</span>`;
    if (s.venom_level >= 2) {
      badges += `<span class="badge" style="background:var(--venom-${s.venom_level})">বিষ ${bn(s.venom_level)}</span>`;
    }
    if (s.extinct) badges += '<span class="badge" style="background:var(--iucn-ex)">বিলুপ্ত</span>';
    badges += `<span class="fam">${s.family_bn || ''}</span>`;
    t.appendChild(el('div', 'badges', badges));
    r.appendChild(t);
    r.appendChild(el('div', 'row-arrow', '›'));
    r.onclick = () => go('detail', { id: s.id });
    return r;
  }

  // ---------- তালিকা (পেজিনেশন) ----------
  function openList(mode, title, color, extra) {
    state.list = {
      items: [], cursor: 0, hasMore: true, filter: null, sort: 'seq',
      title: title, color: color || 'var(--green-primary)', mode: mode, extra: extra || {},
    };
    go('list', { mode: mode, title: title, color: color });
  }

  function listSource() {
    const L = state.list;
    let src = state.species;
    if (L.mode === 'group') src = src.filter((s) => s.group_id === L.extra.group);
    else if (L.mode === 'family') {
      src = src.filter((s) => s.family_id === L.extra.family && s.group_id === L.extra.group);
    } else if (L.mode === 'threatened') src = src.filter((s) => ['VU', 'EN', 'CR'].includes(s.iucn));
    else if (L.mode === 'extinct') src = src.filter((s) => s.extinct);
    else if (L.mode === 'venom') src = src.filter((s) => s.venom_level >= 2 || s.dangerous);
    else if (L.mode === 'fav') src = src.filter((s) => state.favorites.has(s.id));
    else if (L.mode === 'related') {
      src = src.filter((s) => s.family_id === L.extra.family && s.id !== L.extra.id);
    }
    if (L.filter) src = src.filter((s) => s.iucn === L.filter);
    src = [...src];
    if (L.mode === 'popular' || L.sort === 'pop') src.sort((a, b) => b.popularity - a.popularity);
    else src.sort((a, b) => a.row_seq - b.row_seq);
    return src;
  }

  function renderList() {
    const L = state.list;
    setToolbar(L.title, '', L.color, {
      actions: [{ icon: '⚙', onClick: () => go('settings') }],
    });
    const chips = [
      { label: L.sort === 'pop' ? '🔥 জনপ্রিয়' : '↕ তালিকা-ক্রম', on: true, ghost: true,
        onClick: () => { L.sort = L.sort === 'pop' ? 'seq' : 'pop'; L.cursor = 0; L.items = []; renderList(); loadPage(); } },
      { label: 'সব', on: !L.filter, onClick: () => setFilter(null) },
      { label: 'ন্যূনতম ঝুঁকি', on: L.filter === 'LC', onClick: () => setFilter('LC') },
      { label: 'বিপন্ন', on: L.filter === 'VU', onClick: () => setFilter('VU') },
      { label: 'সংকটাপন্ন', on: L.filter === 'EN', onClick: () => setFilter('EN') },
      { label: 'মারাত্মক', on: L.filter === 'CR', onClick: () => setFilter('CR') },
      { label: 'বিলুপ্ত', on: L.filter === 'EX', onClick: () => setFilter('EX') },
    ];
    setChips(chips);

    screenEl.className = 'screen pad';
    screenEl.innerHTML = '';
    screenEl.appendChild(el('div', 'hintline',
      'এক পৃষ্ঠায় ' + bn(PAGE_SIZE) + 'টি — শেষে পৌঁছালে পরের পৃষ্ঠা নিজে থেকেই যোগ হয় (অসীম স্ক্রোল)।'));
    const listBox = el('div', null, '');
    listBox.id = 'listbox';
    screenEl.appendChild(listBox);
    const foot = el('div', 'loading', 'আরও দেখানো হচ্ছে…');
    foot.id = 'listfoot';
    screenEl.appendChild(foot);
    L.cursor = 0; L.items = [];
    loadPage();
    screenEl.onscroll = () => {
      if (screenEl.scrollTop + screenEl.clientHeight >= screenEl.scrollHeight - 500) loadPage();
    };
  }

  function setFilter(f) {
    state.list.filter = f;
    state.list.cursor = 0;
    state.list.items = [];
    renderList();
  }

  function loadPage() {
    const L = state.list;
    if (!L.hasMore) return;
    const src = listSource();
    const slice = src.slice(L.cursor, L.cursor + PAGE_SIZE);
    L.cursor += slice.length;
    L.hasMore = L.cursor < src.length;
    const box = $('#listbox');
    if (!box) return;
    slice.forEach((s) => box.appendChild(rowCard(s)));
    const foot = $('#listfoot');
    if (foot) {
      foot.textContent = L.hasMore ? 'আরও দেখানো হচ্ছে…'
        : (L.items.length || L.cursor ? '— তালিকার শেষ — ' + bn(L.cursor) + '/' + bn(src.length) + ' প্রজাতি —'
          : (L.mode === 'fav' ? 'এখনো কোনো প্রজাতি পছন্দ করা হয়নি' : 'কিছু পাওয়া যায়নি'));
    }
    setToolbar(L.title, bn(L.cursor) + ' / ' + bn(src.length) + ' প্রজাতি', L.color,
      { actions: [{ icon: '⚙', onClick: () => go('settings') }] });
  }

  // ---------- শ্রেণিবিন্যাস ----------
  function renderTaxonomy() {
    const g = state.route.group;
    setToolbar(g.bn_name, '', g.color, { actions: [{ icon: '⚙', onClick: () => go('settings') }] });
    setChips(null);
    screenEl.className = 'screen pad';
    screenEl.innerHTML = '';

    const head = el('div', 'card');
    const row = el('div', null);
    row.style.display = 'flex';
    row.style.gap = '12px';
    row.appendChild(el('div', 'emoji', `<div style="font-size:30px;color:${g.color}">${g.emoji}</div>`));
    const t = el('div');
    t.style.flex = '1';
    t.appendChild(el('div', null, `<div style="font-size:18px;font-weight:700">${g.bn_name}</div>`));
    t.appendChild(el('div', 'en', `<div style="font-size:12px;color:var(--text-muted)">${g.en_name}</div>`));
    row.appendChild(t);
    head.appendChild(row);
    head.appendChild(el('p', null,
      `<p style="font-size:13px;line-height:1.7;color:var(--text-secondary);margin:10px 0 0">${g.blurb_bn}</p>`));
    const stats = el('div', null);
    stats.style.cssText = 'display:flex;margin-top:12px;text-align:center';
    [[human(g.species_count), 'প্রজাতি'], [bn(g.class_count), 'শ্রেণি'],
    [bn(g.order_count), 'বর্গ'], [bn(g.family_count), 'পরিবার']].forEach(([v, l]) => {
      const c = el('div');
      c.style.flex = '1';
      c.appendChild(el('div', null, `<div style="font-size:15px;font-weight:700">${v}</div>`));
      c.appendChild(el('div', null, `<div style="font-size:11px;color:var(--text-muted)">${l}</div>`));
      stats.appendChild(c);
    });
    head.appendChild(stats);
    const all = el('div', null,
      `<div style="margin-top:14px;text-align:center;padding:11px;border-radius:12px;cursor:pointer;
        background:${g.color}1f;border:1px solid ${g.color}66;color:${g.color};font-weight:700;font-size:13.5px">
        এই বিভাগের সব প্রজাতি দেখুন ›</div>`);
    all.firstChild.onclick = () => openList('group', g.bn_name, g.color, { group: g.group_id });
    head.appendChild(all);
    screenEl.appendChild(head);

    // শ্রেণি → বর্গ → পরিবার
    const classes = state.taxonomy.classes.filter((c) => c.group_id === g.group_id);
    screenEl.appendChild(sectionTitle(bn(classes.length) + 'টি শ্রেণি', 'ধাপে ধাপে নামুন'));
    classes.slice(0, 40).forEach((c) => {
      const orders = state.taxonomy.orders.filter((o) => o.group_id === g.group_id && o.class_id === c.class_id);
      const card = el('div', 'tax-card');
      card.appendChild(el('div', 't',
        `<div class="bn">${c.bn_name}</div><div class="sci">${c.sci_name}</div>`));
      card.appendChild(el('div', 'badges',
        `<span class="badge" style="background:${g.color}">${human(c.species_count)} প্রজাতি</span>
         <span class="badge soft" style="background:var(--surface-alt)">${bn(orders.length)} বর্গ</span>`));
      card.appendChild(el('div', 'row-arrow', '›'));
      card.onclick = () => go('orders', { group: g, cls: c });
      screenEl.appendChild(card);
    });
  }

  function renderOrders() {
    const { group: g, cls: c } = state.route;
    setToolbar(g.bn_name, c.bn_name + ' · বর্গ', g.color);
    setChips(null);
    screenEl.className = 'screen pad';
    screenEl.innerHTML = '';
    const orders = state.taxonomy.orders.filter((o) => o.group_id === g.group_id && o.class_id === c.class_id);
    orders.forEach((o) => {
      const fams = state.taxonomy.families.filter((f) => f.order_id === o.order_id && f.group_id === g.group_id);
      const card = el('div', 'tax-card');
      card.appendChild(el('div', 't', `<div class="bn">${o.bn_name}</div><div class="sci">${o.sci_name}</div>`));
      card.appendChild(el('div', 'badges',
        `<span class="badge" style="background:${g.color}">${human(o.species_count)} প্রজাতি</span>
         <span class="badge soft" style="background:var(--surface-alt)">${bn(fams.length)} পরিবার</span>`));
      card.appendChild(el('div', 'row-arrow', '›'));
      card.onclick = () => go('families', { group: g, cls: c, order: o });
      screenEl.appendChild(card);
    });
  }

  function renderFamilies() {
    const { group: g, cls: c, order: o } = state.route;
    setToolbar(g.bn_name, o.bn_name + ' · পরিবার', g.color);
    setChips(null);
    screenEl.className = 'screen pad';
    screenEl.innerHTML = '';
    const fams = state.taxonomy.families.filter((f) => f.order_id === o.order_id && f.group_id === g.group_id);
    fams.forEach((f) => {
      const card = el('div', 'tax-card');
      card.appendChild(el('div', 't', `<div class="bn">${f.bn_name}</div><div class="sci">${f.sci_name.replace('_fam', '')}</div>`));
      card.appendChild(el('div', 'badges',
        `<span class="badge" style="background:${g.color}">${human(f.species_count)} প্রজাতি</span>`));
      card.appendChild(el('div', 'row-arrow', '›'));
      card.onclick = () => openList('family', f.bn_name, g.color,
        { group: g.group_id, family: f.family_id });
      screenEl.appendChild(card);
    });
    if (!fams.length) screenEl.appendChild(el('div', 'empty', '<div class="emoji">🍃</div>নমুনায় এই বর্গের পরিবার নেই'));
  }

  // ---------- বিস্তারিত ----------
  function renderDetail() {
    const s = state.byId.get(state.route.id) || state.species[0];
    const fav = state.favorites.has(s.id);
    setToolbar(s.bn_name, s.group_bn + ' · ' + (s.family_bn || ''), s.color || 'var(--green-primary)', {
      actions: [
        { text: fav ? '❤ পছন্দের' : '♡ পছন্দ', onClick: () => {
          if (fav) state.favorites.delete(s.id); else state.favorites.add(s.id);
          savePrefs(); renderDetail();
        } },
        { text: '⤴ শেয়ার', onClick: () => copyText(shareText(s)) },
      ],
    });
    setChips(null);
    screenEl.className = 'screen';
    screenEl.innerHTML = '';

    const [iucnLabel, iucnColor] = IUCN[s.iucn] || ['—', '#888'];
    const gal = el('div', 'gallery');
    let variant = 0;
    const art = el('div', 'art');
    const captions = ['স্বাভাবিক পরিবেশ', 'রাতের বেলায়', 'সূর্যাস্তে', 'মেঘলা দিনে'];
    const cap = el('div', 'caption', captions[0]);
    const dots = el('div', 'dots');
    function paint() {
      art.innerHTML = '';
      const c = artCanvas(s, 824, 512, variant);
      c.style.width = '100%'; c.style.height = '100%';
      art.appendChild(c);
      cap.textContent = captions[variant];
      dots.innerHTML = '';
      for (let i = 0; i < 4; i++) dots.appendChild(el('i', i === variant ? 'on' : ''));
    }
    gal.appendChild(art);
    gal.appendChild(cap);
    gal.appendChild(dots);
    const b1 = el('span', 'badge', `${s.iucn} · ${iucnLabel}`);
    b1.style.background = iucnColor;
    gal.appendChild(b1);
    if (s.extinct) {
      const b2 = el('span', 'badge right', 'বিলুপ্ত');
      b2.style.background = 'var(--iucn-ex)';
      gal.appendChild(b2);
    }
    gal.onclick = () => { variant = (variant + 1) % 4; paint(); };
    paint();
    screenEl.appendChild(gal);

    // নাম
    const nameCard = el('div', 'card detail-card');
    nameCard.appendChild(el('h2', null, s.bn_name));
    if (s.en_name) nameCard.appendChild(el('div', 'en', s.en_name));
    nameCard.appendChild(el('div', 'sci', s.sci_name));
    if (s.authority) nameCard.appendChild(el('div', 'auth', 'প্রণেতা: ' + s.authority));
    const chips = el('div', 'badges');
    chips.style.marginTop = '12px';
    const gchip = el('span', 'badge', s.group_bn);
    gchip.style.background = s.color || 'var(--green-primary)';
    chips.appendChild(gchip);
    [s.class_bn, s.order_bn, s.family_bn].forEach((t) => {
      if (!t) return;
      const c = el('span', 'badge soft', t);
      c.style.cssText = 'background:var(--surface-alt);border:1px solid var(--outline)';
      chips.appendChild(c);
    });
    nameCard.appendChild(chips);
    const copy = el('span', 'linkish', 'নাম ও তথ্য কপি করুন');
    copy.onclick = () => copyText(shareText(s));
    nameCard.appendChild(copy);
    screenEl.appendChild(nameCard);

    // শ্রেণিবিন্যাস
    const tax = el('div', 'card detail-card');
    const taxHead = el('div', 'label', 'শ্রেণিবিন্যাস');
    taxHead.style.cssText = 'font-size:11.5px;font-weight:700;color:var(--text-muted);letter-spacing:.04em';
    tax.appendChild(taxHead);
    [['বিভাগ', s.group_bn + (s.group_en ? ' (' + s.group_en + ')' : '')],
    ['শ্রেণি', s.class_bn + ' · ' + s.class_id],
    ['বর্গ', s.order_bn + ' · ' + s.order_id],
    ['পরিবার', (s.family_bn || '') + ' · ' + s.family_id]].forEach(([k, v]) => {
      const r = el('div', 'taxrow');
      r.appendChild(el('div', 'k', k));
      r.appendChild(el('div', 'v', v));
      tax.appendChild(r);
    });
    screenEl.appendChild(tax);

    // তথ্যসমূহ
    const facts = [
      ['সংরক্ষণ অবস্থা', iucnLabel + ' (' + s.iucn + ')', iucnColor],
      ['বাসস্থান ও অবস্থান', (s.habitat_bn || '') + (s.region_bn ? ' · ' + s.region_bn : ''), 'var(--moss)'],
      ['খাদ্য', s.diet_bn, 'var(--earth-primary)'],
      ['প্রজনন', s.repro_bn, 'var(--green-accent)'],
      ['আকার', s.size_bn, 'var(--sky)'],
      ['বিষাক্ততা', VENOM[s.venom_level] + ' · ' + s.venom_bn, `var(--venom-${Math.max(1, s.venom_level)})`],
    ];
    if (s.extinct_bn) facts.push(['বিলুপ্তি', s.extinct_bn, 'var(--iucn-ex)']);
    if (s.fact_bn) facts.push(['আকর্ষণীয় তথ্য', s.fact_bn, 'var(--leaf)']);
    if (s.notes_bn && s.notes_bn !== s.fact_bn) facts.push(['অতিরিক্ত নোট', s.notes_bn, 'var(--clay)']);
    facts.forEach(([label, value, color]) => {
      if (!value) return;
      const f = el('div', 'fact');
      f.style.borderLeftColor = color;
      f.appendChild(el('div', 'label', label));
      f.appendChild(el('div', 'value', value));
      screenEl.appendChild(f);
    });

    // নোট
    const note = el('div', 'card detail-card');
    const noteHead = el('div', null, 'আমার নোট');
    noteHead.style.cssText = 'font-size:11.5px;font-weight:700;color:var(--text-muted);letter-spacing:.04em;margin-bottom:8px';
    note.appendChild(noteHead);
    const ta = el('textarea', 'note');
    ta.placeholder = 'এই প্রজাতি সম্পর্কে নিজের নোট লিখুন…';
    ta.value = state.notes[s.id] || '';
    note.appendChild(ta);
    const btns = el('div');
    btns.style.cssText = 'display:flex;gap:8px;margin-top:10px';
    const save = el('button', 'btn', 'সংরক্ষণ');
    save.onclick = () => { state.notes[s.id] = ta.value; savePrefs(); toast('নোট সংরক্ষিত হয়েছে'); };
    const clear = el('button', 'btn ghost', 'মুছুন');
    clear.onclick = () => { ta.value = ''; delete state.notes[s.id]; savePrefs(); };
    btns.appendChild(save); btns.appendChild(clear);
    note.appendChild(btns);
    screenEl.appendChild(note);

    // সম্পর্কিত
    const rel = state.species.filter((x) => x.family_id === s.family_id && x.id !== s.id).slice(0, 12);
    if (rel.length) {
      screenEl.appendChild(el('div', null,
        `<div style="padding:22px 14px 6px">${sectionTitle('একই পরিবারের আরও', '').outerHTML}</div>`));
      const st = strip(rel);
      st.style.padding = '0 12px 12px';
      screenEl.appendChild(st);
      const more = el('div', null,
        `<div style="padding:6px 14px 28px;color:${s.color || 'var(--green-primary)'};
         font-weight:700;font-size:13px;cursor:pointer">${s.family_bn || ''}-এর সব প্রজাতি দেখুন ›</div>`);
      more.firstChild.onclick = () => openList('family', s.family_bn || '', s.color,
        { group: s.group_id, family: s.family_id });
      screenEl.appendChild(more);
    } else {
      screenEl.appendChild(el('div', null, '<div style="height:28px"></div>'));
    }
  }

  function shareText(s) {
    return `${s.bn_name} (${s.en_name || '—'})\n${s.sci_name}\n`
      + `শ্রেণিবিন্যাস: ${s.group_bn} → ${s.class_bn} → ${s.order_bn} → ${s.family_bn}\n`
      + `বাসস্থান: ${s.habitat_bn || '—'}\nখাদ্য: ${s.diet_bn || '—'}\n`
      + `প্রজনন: ${s.repro_bn || '—'}\nআকার: ${s.size_bn || '—'}\n`
      + `সংরক্ষণ: ${s.iucn}\nবিষাক্ততা: ${VENOM[s.venom_level]} — ${s.venom_bn || ''}\n`
      + (s.fact_bn ? `তথ্য: ${s.fact_bn}\n` : '') + '— প্রকৃতি কোষ (অফলাইন)';
  }

  function copyText(t) {
    navigator.clipboard.writeText(t).then(() => toast('কপি হয়েছে'), () => toast('কপি করা যায়নি'));
  }

  let toastTimer;
  function toast(msg) {
    let t = $('#toast');
    if (!t) {
      t = el('div', null, '');
      t.id = 'toast';
      t.style.cssText = 'position:absolute;left:50%;bottom:26px;transform:translateX(-50%);'
        + 'background:#16241a;color:#fff;padding:10px 18px;border-radius:999px;font-size:13px;'
        + 'opacity:0;transition:opacity .2s;z-index:40;pointer-events:none;max-width:80%;text-align:center';
      phone.appendChild(t);
    }
    t.textContent = msg;
    t.style.opacity = '1';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.style.opacity = '0'; }, 1800);
  }

  // ---------- অনুসন্ধান ----------
  let searchTimer;
  function renderSearch() {
    setToolbar('অনুসন্ধান', 'বাংলা, ইংরেজি বা বৈজ্ঞানিক নামে খুঁজুন', 'var(--green-dark)');
    setChips(null);
    screenEl.className = 'screen pad';
    screenEl.innerHTML = '';
    const box = el('div', 'searchbox');
    box.appendChild(el('div', null, '<div style="color:var(--green-accent)">🔎</div>'));
    const input = el('input');
    input.placeholder = 'প্রাণী, উদ্ভিদ বা বৈজ্ঞানিক নাম খুঁজুন…';
    input.value = state.route.q || '';
    box.appendChild(input);
    const clr = el('div', null, '<div style="color:var(--text-muted);cursor:pointer;padding:0 4px">✕</div>');
    clr.firstChild.onclick = () => { input.value = ''; run(''); };
    box.appendChild(clr);
    screenEl.appendChild(box);

    const hint = el('div', 'hintline',
      'চেষ্টা করুন: বাঘ · ইলিশ · মাছরাঙা · সুন্দরবন · Panthera · তিমি · সাপ');
    hint.onclick = () => {
      const opts = ['বাঘ', 'ইলিশ', 'মাছরাঙা', 'সুন্দরবন', 'তিমি', 'হাতি', 'সাপ', 'Panthera', 'ময়ূর'];
      input.value = opts[Math.floor(Math.random() * opts.length)];
      run(input.value);
    };
    screenEl.appendChild(hint);

    const head = el('div', 'hintline', '');
    head.id = 'searchhead';
    screenEl.appendChild(head);
    const out = el('div');
    out.id = 'searchout';
    screenEl.appendChild(out);

    function run(qRaw) {
      clearTimeout(searchTimer);
      const q = (qRaw || '').trim();
      searchTimer = setTimeout(() => {
        const t0 = performance.now();
        const res = q ? searchSpecies(q, 60) : [];
        const ms = (performance.now() - t0).toFixed(1);
        const h = $('#searchhead');
        if (h) {
          h.textContent = q ? `“${q}” — ${bn(res.length)}টি ফল (${bn(ms)} মি.সে., FTS5-এর অনুকরণ)`
            : 'লিখতে শুরু করুন — ২২০ মি.সে. পর খোঁজা হয় (ডিবাউন্স)';
        }
        const o = $('#searchout');
        if (!o) return;
        o.innerHTML = '';
        if (!q) return;
        if (!res.length) {
          o.appendChild(el('div', 'empty', '<div class="emoji">🍃</div>কোনো ফলাফল নেই — অন্য শব্দ চেষ্টা করুন'));
          return;
        }
        res.forEach((s) => o.appendChild(rowCard(s)));
      }, DEBOUNCE_MS);
    }
    input.oninput = () => run(input.value);
    if (input.value) run(input.value);
    setTimeout(() => input.focus(), 60);
  }

  /** অ্যাপের Repository.search()-এর অনুকরণ: হুবহু → উপসর্গ → পূর্ণ-পাঠ। */
  function searchSpecies(q, limit) {
    const lower = q.toLowerCase();
    const seen = new Set();
    const out = [];
    const push = (s) => { if (!seen.has(s.id)) { seen.add(s.id); out.push(s); } };
    // ১) হুবহু
    state.species.forEach((s) => {
      if (s.bn_name === q || (s.en_name || '').toLowerCase() === lower
        || (s.sci_name || '').toLowerCase() === lower) push(s);
    });
    // ২) উপসর্গ
    state.species.forEach((s) => {
      if (s.bn_name.startsWith(q) || (s.en_name || '').toLowerCase().startsWith(lower)
        || (s.sci_name || '').toLowerCase().startsWith(lower)) push(s);
    });
    // ৩) পূর্ণ-পাঠ (নাম, বাসস্থান, খাদ্য, তথ্য)
    const words = lower.split(/\s+/).filter(Boolean);
    state.species.forEach((s) => {
      const hay = [s.bn_name, s.en_name, s.sci_name, s.habitat_bn, s.diet_bn, s.repro_bn,
        s.fact_bn, s.family_bn, s.order_bn, s.class_bn, s.region_bn, s.size_bn, s.venom_bn]
        .filter(Boolean).join(' ').toLowerCase();
      if (words.every((w) => hay.indexOf(w) >= 0)) push(s);
    });
    out.sort((a, b) => b.popularity - a.popularity);
    return out.slice(0, limit);
  }

  // ---------- সেটিংস ----------
  function renderSettings() {
    setToolbar('সেটিংস', null, 'var(--green-dark)');
    setChips(null);
    screenEl.className = 'screen pad';
    screenEl.innerHTML = '';
    const total = state.taxonomy.groups.reduce((a, g) => a + g.species_count, 0);

    screenEl.appendChild(el('div', 'set-head', 'সংগ্রহশালা (ডেটাবেস)'));
    [
      ['📚', 'মোট প্রজাতি', human(total) + ' (অ্যাপে ২,৫০,০০০)', null],
      ['🗂', 'প্রধান বিভাগ', bn(state.taxonomy.groups.length), null],
      ['💾', 'ডেটাবেসের আকার', '৪৩১ মেগাবাইট → সংকুচিত ৭৫.৯ মেগাবাইট', null],
      ['🔎', 'পূর্ণ-পাঠ সূচক (FTS5)', 'বাংলা, ইংরেজি ও বৈজ্ঞানিক নামে মিলিয়ে খোঁজে', () => go('search')],
      ['🔁', 'ডেটাবেস আবার বানান', 'সংরক্ষণাগার থেকে নতুন করে খোলা হবে (কয়েক সেকেন্ড)', () => toast('অ্যাপে এটি assets থেকে আবার খোলে')],
    ].forEach(([e, t, s2, onClick]) => screenEl.appendChild(setRow(e, t, s2, onClick)));

    screenEl.appendChild(el('div', 'set-head', 'আমার ডেটা'));
    [
      ['❤', 'পছন্দের প্রজাতি', bn(state.favorites.size) + 'টি প্রজাতি', () => openList('fav', 'পছন্দের প্রজাতি', 'var(--green-accent)')],
      ['📝', 'আমার নোট', bn(Object.keys(state.notes).filter((k) => state.notes[k]).length) + 'টি নোট', () => showNotes()],
      ['🧹', 'পছন্দের তালিকা মুছুন', null, () => { state.favorites.clear(); savePrefs(); renderSettings(); toast('পছন্দের তালিকা খালি করা হয়েছে'); }],
      ['🗑', 'সব নোট মুছুন', null, () => { state.notes = {}; savePrefs(); renderSettings(); toast('সব নোট মুছে ফেলা হয়েছে'); }],
    ].forEach(([e, t, s2, onClick]) => screenEl.appendChild(setRow(e, t, s2, onClick)));

    screenEl.appendChild(el('div', 'set-head', 'অ্যাপ সম্পর্কে'));
    screenEl.appendChild(setRow('🌿', 'প্রকৃতি কোষ', 'সংস্করণ ১.০ · com.prakriti.kosh', null));
    screenEl.appendChild(el('div', 'footer-note',
      'সম্পূর্ণ অফলাইন — কোনো ইন্টারনেট অনুমতি নেই।<br>'
      + 'প্রযুক্তি: SQLite (FTS5 পূর্ণ-পাঠ সূচক + এক্সপ্রেশন ইনডেক্স) · প্রোগ্রামে আঁকা '
      + 'ভেক্টর ছবি · কীসেট পেজিনেশন (২৪টি/পৃষ্ঠা) · ব্যাকগ্রাউন্ড থ্রেড · RGB_565 বিটম্যাপ ক্যাশ।'));
  }

  function setRow(emoji, title, sub, onClick) {
    const r = el('div', 'set-row');
    r.appendChild(el('div', 'emoji', emoji));
    const t = el('div', 't');
    t.appendChild(el('div', 'title', title));
    if (sub) t.appendChild(el('div', 'sub', sub));
    r.appendChild(t);
    if (onClick) { r.appendChild(el('div', 'row-arrow', '›')); r.onclick = onClick; }
    return r;
  }

  function showNotes() {
    const entries = Object.keys(state.notes).filter((k) => state.notes[k]);
    const text = entries.length
      ? entries.map((k) => {
        const s = state.byId.get(+k);
        return (s ? s.bn_name : 'প্রজাতি #' + k) + ': ' + state.notes[k];
      }).join('\n\n')
      : 'কোনো নোট নেই।';
    alert(text);
  }

  // ---------- স্প্ল্যাশ ও পরিচয় ----------
  function renderSplash() {
    toolbar.style.display = 'none';
    chipbar.style.display = 'none';
    screenEl.className = 'screen';
    screenEl.innerHTML = '';
    const sp = el('div', 'splash');
    sp.appendChild(el('div', 'logo', '🌿'));
    sp.appendChild(el('h2', null, 'প্রকৃতি কোষ'));
    sp.appendChild(el('p', null, '২.৫ লক্ষ প্রজাতির অফলাইন বিশ্বকোষ'));
    sp.appendChild(el('div', 'stage', 'প্রথমবারের মতো ডেটাবেস প্রস্তুত হচ্ছে…'));
    const pr = el('div', 'progress');
    const bar = el('i');
    pr.appendChild(bar);
    sp.appendChild(pr);
    sp.appendChild(el('p', null, '<p style="font-size:11.5px;color:rgba(255,255,255,.6);margin-top:26px">'
      + 'সম্পূর্ণ অফলাইন · ' + human(250000) + ' প্রজাতি</p>'));
    screenEl.appendChild(sp);
    toolbar.style.display = '';
    let pct = 0;
    const stages = ['সংরক্ষণাগার খোলা হচ্ছে…', 'ডেটাবেস লেখা হচ্ছে…', 'যাচাই করা হচ্ছে…', 'প্রস্তুত!'];
    const timer = setInterval(() => {
      pct += 12 + Math.random() * 20;
      if (pct > 100) pct = 100;
      bar.style.width = pct + '%';
      sp.querySelector('.stage').textContent = stages[Math.min(3, Math.floor(pct / 28))]
        + ' (' + bn(Math.round(pct)) + '%)';
      if (pct >= 100) {
        clearInterval(timer);
        setTimeout(() => {
          if (state.introSeen) go('main');
          else go('intro');
        }, 450);
      }
    }, 240);
  }

  const INTRO = [
    ['🌿', 'ইন্টারনেট ছাড়াই পুরো পৃথিবী',
      'প্রায় দুই লক্ষ পঞ্চাশ হাজার প্রজাতির তথ্য অ্যাপের ভেতরেই সংরক্ষিত। একবার প্রস্তুত হওয়ার পর কখনো নেট বা মোবাইল ডেটা লাগবে না।'],
    ['🗂', '১২টি বিভাগ, ধাপে ধাপে',
      'প্রধান বিভাগ থেকে শ্রেণি, বর্গ, পরিবার — তারপর প্রজাতির তালিকা। প্রতিটি প্রজাতির বাসস্থান, খাদ্য, প্রজনন, আকার ও বিষাক্ততা আলাদা করে লেখা।'],
    ['🔎', 'নাম লিখলেই খুঁজে পাবেন',
      'বাংলা, ইংরেজি বা বৈজ্ঞানিক — যে নামেই লিখুন, পূর্ণ-পাঠ সূচক (FTS5) মিলিয়ে দেবে। বিপন্ন ও বিলুপ্ত প্রজাতি আলাদা করেও ঘোরা যাবে।'],
  ];

  function renderIntro() {
    toolbar.style.display = 'none';
    chipbar.style.display = 'none';
    screenEl.className = 'screen';
    screenEl.innerHTML = '';
    const wrap = el('div', 'intro');
    const skip = el('div', 'skip', 'এড়িয়ে যান ›');
    skip.onclick = finish;
    wrap.appendChild(skip);
    const pages = el('div', 'pages');
    INTRO.forEach(([emoji, title, body]) => {
      const p = el('div', 'page');
      p.appendChild(el('div', 'emoji', emoji));
      p.appendChild(el('h3', null, title));
      p.appendChild(el('p', null, body));
      pages.appendChild(p);
    });
    wrap.appendChild(pages);
    const dots = el('div', 'dots');
    wrap.appendChild(dots);
    const next = el('div', null, '<button class="btn next" style="width:calc(100% - 56px)">পরবর্তী</button>');
    const btn = next.firstChild;
    wrap.appendChild(next);
    phone.appendChild(wrap);
    let idx = 0;
    function sync() {
      dots.innerHTML = '';
      INTRO.forEach((_, i) => dots.appendChild(el('i', i === idx ? 'on' : '')));
      btn.textContent = idx === INTRO.length - 1 ? 'শুরু করুন' : 'পরবর্তী';
    }
    btn.onclick = () => {
      if (idx >= INTRO.length - 1) finish();
      else { idx++; pages.scrollTo({ left: idx * pages.clientWidth, behavior: 'smooth' }); sync(); }
    };
    pages.onscroll = () => {
      const i = Math.round(pages.scrollLeft / pages.clientWidth);
      if (i !== idx) { idx = i; sync(); }
    };
    function finish() {
      state.introSeen = true;
      localStorage.setItem('pk.intro', '1');
      wrap.remove();
      go('main');
    }
    sync();
  }

  // ---------- রাউটার ----------
  function render() {
    screenEl.onscroll = null;
    toolbar.style.display = '';
    switch (state.screen) {
      case 'splash': renderSplash(); break;
      case 'intro': renderIntro(); break;
      case 'main': renderMain(); break;
      case 'taxonomy': renderTaxonomy(); break;
      case 'orders': renderOrders(); break;
      case 'families': renderFamilies(); break;
      case 'list': renderList(); break;
      case 'detail': renderDetail(); break;
      case 'search': renderSearch(); break;
      case 'settings': renderSettings(); break;
      default: renderMain();
    }
    syncAside();
  }

  const SCREENS = [
    ['splash', 'স্প্ল্যাশ'], ['intro', 'পরিচয়'], ['main', 'প্রধান'],
    ['taxonomy', 'শ্রেণিবিন্যাস'], ['list', 'তালিকা'], ['detail', 'বিস্তারিত'],
    ['search', 'অনুসন্ধান'], ['settings', 'সেটিংস'],
  ];

  function syncAside() {
    const nav = $('#screenNav');
    if (!nav) return;
    nav.innerHTML = '';
    SCREENS.forEach(([key, label]) => {
      const b = el('button', state.screen === key ? 'on' : '', label);
      b.onclick = () => {
        if (key === 'list') openList('all', 'সব প্রজাতি', 'var(--green-dark)');
        else if (key === 'taxonomy') go('taxonomy', { group: state.taxonomy.groups[0] });
        else if (key === 'detail') go('detail', { id: state.species[0].id });
        else go(key);
      };
      nav.appendChild(b);
    });
    const info = $('#screenInfo');
    if (info) info.innerHTML = SCREEN_INFO[state.screen] || '';
  }

  const SCREEN_INFO = {
    splash: '<b>SplashActivity</b> — অ্যাপ খোলার সঙ্গে সঙ্গে ব্যাকগ্রাউন্ড থ্রেডে ৭৫.৯ মেগাবাইট সংরক্ষণাগার খোলে, SHA-256 যাচাই করে, অগ্রগতি দেখায়।',
    intro: '<b>SetupActivity</b> — প্রথমবারের তিন পাতার ওয়াকথ্রু; <code>intro_seen</code> সংরক্ষিত থাকলে আর দেখানো হয় না।',
    main: '<b>MainActivity</b> — হিরো কার্ড, ৬টি দ্রুত-প্রবেশ, ১২ বিভাগের গ্রিড, জনপ্রিয় ও বিপন্ন প্রজাতির আনুভূমিক স্ট্রিপ।',
    taxonomy: '<b>TaxonomyActivity</b> — বিভাগ → শ্রেণি → বর্গ → পরিবার। প্রতিটি ধাপে কত প্রজাতি তা আগেই দেখানো হয়।',
    list: '<b>SpeciesListActivity</b> — কীসেট পেজিনেশন (<code>row_seq &gt; ?</code>), ২৪টি/পৃষ্ঠা, শেষে পৌঁছালে পরের পৃষ্ঠা, IUCN ছাঁকনি ও সাজানো।',
    detail: '<b>SpeciesDetailActivity</b> — সোয়াইপ-গ্যালারি (৪ পরিবেশ), নাম, শ্রেণিবিন্যাস, বাসস্থান, খাদ্য, প্রজনন, আকার, বিষাক্ততা, নোট, পছন্দ ও শেয়ার।',
    search: '<b>SearchActivity</b> — ২২০ মি.সে. ডিবাউন্স; হুবহু → উপসর্গ (ইনডেক্স) → FTS5 পূর্ণ-পাঠ।',
    settings: '<b>SettingsActivity</b> — ডেটাবেসের তথ্য, পছন্দ/নোট, আবার প্রস্তুত করা, অ্যাপ-পরিচয়।',
  };

  // ---------- শুরু ----------
  Promise.all([
    fetch('data/species.json').then((r) => r.json()),
    fetch('data/taxonomy.json').then((r) => r.json()),
  ]).then(([sp, tax]) => {
    state.species = sp;
    state.taxonomy = tax;
    sp.forEach((s) => state.byId.set(s.id, s));
    render();
  }).catch((e) => {
    document.body.innerHTML = '<p style="padding:40px">ডেটা লোড করা যায়নি: ' + e + '</p>';
  });
})();
