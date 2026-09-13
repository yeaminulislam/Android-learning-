# -*- coding: utf-8 -*-
"""প্রকৃতি কোষ — ডেটাবেস নির্মাণ।

curated (২২৭ বাস্তব প্রজাতি) + generator (নির্ধারিত বীজ থেকে ~২.৫ লক্ষ)
মিলিয়ে একটি SQLite + FTS5 ডেটাবেস তৈরি করে, তারপর gzip করে
`prakriti_kosh.db.z` (PKDB1 হেডারসহ) বানায় — অ্যাপের assets-এ যায়।

ব্যবহার:
    python3 tools/build_dataset.py --total 250000
    python3 tools/build_dataset.py --quick          # ছোট পরীক্ষামূলক বিল্ড
"""

import argparse
import gzip
import hashlib
import json
import os
import sqlite3
import struct
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "data"))

import curated_species as CS          # noqa: E402
import generator as GEN               # noqa: E402
import taxonomy as TAX                # noqa: E402
from groups import GROUPS, GLOBAL_COUNT, CLASS_BN, ORDER_BN  # noqa: E402

# গ্রুপ → (রাজ্য, পর্ব) — বিস্তারিত পর্বে দেখানোর জন্য
KINGDOM_PHYLUM = {
    "insects": ("প্রাণীজগৎ (Animalia)", "সন্ধিপদী (Arthropoda)"),
    "arachnids": ("প্রাণীজগৎ (Animalia)", "সন্ধিপদী (Arthropoda)"),
    "ants": ("প্রাণীজগৎ (Animalia)", "সন্ধিপদী (Arthropoda)"),
    "mollusks": ("প্রাণীজগৎ (Animalia)", "মলুসকা (Mollusca)"),
    "crustaceans": ("প্রাণীজগৎ (Animalia)", "সন্ধিপদী (Arthropoda)"),
    "other_inverts": ("প্রাণীজগৎ (Animalia)", "বিবিধ পর্ব"),
    "snakes": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata)"),
    "lizards_turtles": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata)"),
    "micro_life": ("ছত্রাক, প্রোক্যারিওট ও ভাইরাস", "বিবিধ"),
    "mammals": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata)"),
    "birds": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata)"),
    "reptiles": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata)"),
    "amphibians": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata)"),
    "fishes": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata)"),
    "marine": ("প্রাণীজগৎ ও শৈবাল", "বিবিধ পর্ব"),
    "inverts": ("প্রাণীজগৎ (Animalia)", "বিবিধ পর্ব"),
    "dinosaurs": ("প্রাণীজগৎ (Animalia)", "কর্ডাটা (Chordata) — বিলুপ্ত"),
    "fungi": ("ছত্রাকজগৎ (Fungi)", "—"),
    "plants": ("উদ্ভিদজগৎ (Plantae)", "বিবিধ পর্ব"),
    "microbes": ("প্রোক্যারিওট ও প্রোটিস্টা", "বিবিধ পর্ব"),
    "viruses": ("ভাইরাস (কোষহীন)", "—"),
}
GROUP_INDEX = {g[0]: g for g in GROUPS}

DB_NAME = "prakriti_kosh.db"
DBZ_NAME = "prakriti_kosh.db.z"
MAGIC = b"PKDB1"
CHUNK = 4000

SPECIES_COLUMNS = [
    "id", "bn_name", "en_name", "sci_name", "authority",
    "class_id", "order_id", "family_id", "group_id",
    "region_bn", "region_key", "habitat_bn", "habitat_key",
    "diet_bn", "iucn", "size_bn",
    "venom_level", "venom_bn", "repro_bn", "fact_bn", "extinct_bn",
    "notes_bn", "is_demo", "popularity", "row_seq", "extinct", "dangerous",
]

# FTS-এ যে তালিকা থাকবে (species + ট্যাক্সোনমি join থেকে ভরা হয়)
FTS_COLUMNS = [
    "bn_name", "en_name", "sci_name", "authority",
    "class_bn", "order_bn", "family_bn", "group_bn",
    "region_bn", "habitat_bn", "diet_bn", "size_bn",
    "venom_bn", "repro_bn", "fact_bn", "extinct_bn", "notes_bn",
]

SCHEMA = """
PRAGMA journal_mode = OFF;
PRAGMA synchronous = OFF;
PRAGMA temp_store = MEMORY;
PRAGMA cache_size = -200000;
PRAGMA page_size = 4096;
PRAGMA mmap_size = 268435456;

CREATE TABLE species (
    id           INTEGER PRIMARY KEY,
    bn_name      TEXT NOT NULL,
    en_name      TEXT NOT NULL DEFAULT '',
    sci_name     TEXT NOT NULL DEFAULT '',
    authority    TEXT NOT NULL DEFAULT '',
    class_id     TEXT NOT NULL DEFAULT '',
    order_id     TEXT NOT NULL DEFAULT '',
    family_id    TEXT NOT NULL DEFAULT '',
    group_id     TEXT NOT NULL,
    region_bn    TEXT NOT NULL DEFAULT '',
    region_key   TEXT NOT NULL DEFAULT '',
    habitat_bn   TEXT NOT NULL DEFAULT '',
    habitat_key  TEXT NOT NULL DEFAULT '',
    diet_bn      TEXT NOT NULL DEFAULT '',
    iucn         TEXT NOT NULL DEFAULT 'LC',
    size_bn      TEXT NOT NULL DEFAULT '',
    venom_level  INTEGER NOT NULL DEFAULT 0,
    venom_bn     TEXT NOT NULL DEFAULT '',
    repro_bn     TEXT NOT NULL DEFAULT '',
    fact_bn      TEXT NOT NULL DEFAULT '',
    extinct_bn   TEXT NOT NULL DEFAULT '',
    notes_bn     TEXT NOT NULL DEFAULT '',
    is_demo      INTEGER NOT NULL DEFAULT 0,
    popularity   INTEGER NOT NULL DEFAULT 0,
    row_seq      INTEGER NOT NULL DEFAULT 0,
    extinct      INTEGER NOT NULL DEFAULT 0,
    dangerous    INTEGER NOT NULL DEFAULT 0
) WITHOUT ROWID;

CREATE VIRTUAL TABLE species_fts USING fts5(
    bn_name, en_name, sci_name, authority,
    class_bn, order_bn, family_bn, group_bn,
    region_bn, habitat_bn, diet_bn, size_bn,
    venom_bn, repro_bn, fact_bn, extinct_bn, notes_bn,
    content='', content_rowid='id', tokenize='unicode61'
);

CREATE TABLE category_group (
    group_id    TEXT PRIMARY KEY,
    bn_name     TEXT NOT NULL,
    en_name     TEXT NOT NULL,
    emoji       TEXT NOT NULL DEFAULT '',
    color       TEXT NOT NULL DEFAULT '#2E7D32',
    blurb_bn    TEXT NOT NULL DEFAULT '',
    species_count INTEGER NOT NULL DEFAULT 0,
    class_count   INTEGER NOT NULL DEFAULT 0,
    order_count   INTEGER NOT NULL DEFAULT 0,
    family_count  INTEGER NOT NULL DEFAULT 0,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    global_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE region (
    id        TEXT PRIMARY KEY,
    bn_name   TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
) WITHOUT ROWID;

CREATE TABLE habitat (
    id        TEXT PRIMARY KEY,
    bn_name   TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
) WITHOUT ROWID;

CREATE TABLE taxon_class (
    class_id    TEXT NOT NULL,
    group_id    TEXT NOT NULL,
    sci_name    TEXT NOT NULL,
    bn_name     TEXT NOT NULL,
    species_count INTEGER NOT NULL DEFAULT 0,
    order_count   INTEGER NOT NULL DEFAULT 0,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (group_id, class_id)
) WITHOUT ROWID;

CREATE TABLE taxon_order (
    order_id    TEXT NOT NULL,
    group_id    TEXT NOT NULL,
    class_id    TEXT NOT NULL,
    sci_name    TEXT NOT NULL,
    bn_name     TEXT NOT NULL,
    species_count INTEGER NOT NULL DEFAULT 0,
    family_count  INTEGER NOT NULL DEFAULT 0,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (group_id, class_id, order_id)
) WITHOUT ROWID;

CREATE TABLE taxon_family (
    family_id   TEXT NOT NULL,
    group_id    TEXT NOT NULL,
    class_id    TEXT NOT NULL,
    order_id    TEXT NOT NULL,
    sci_name    TEXT NOT NULL,
    bn_name     TEXT NOT NULL,
    species_count INTEGER NOT NULL DEFAULT 0,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (group_id, class_id, order_id, family_id)
) WITHOUT ROWID;

CREATE TABLE favorite (
    species_id INTEGER PRIMARY KEY,
    added_at   INTEGER NOT NULL DEFAULT 0
) WITHOUT ROWID;

CREATE TABLE note (
    species_id INTEGER PRIMARY KEY,
    body       TEXT NOT NULL DEFAULT '',
    updated_at INTEGER NOT NULL DEFAULT 0
) WITHOUT ROWID;

CREATE TABLE meta (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
) WITHOUT ROWID;
"""

INDEXES = """
CREATE INDEX idx_species_group     ON species(group_id, row_seq);
CREATE INDEX idx_species_class     ON species(group_id, class_id, row_seq);
CREATE INDEX idx_species_order     ON species(group_id, class_id, order_id, row_seq);
CREATE INDEX idx_species_family    ON species(group_id, class_id, order_id, family_id, row_seq);
CREATE INDEX idx_species_popular   ON species(popularity DESC, id);
CREATE INDEX idx_species_group_pop ON species(group_id, popularity DESC, id);
CREATE INDEX idx_species_iucn      ON species(iucn, row_seq);
CREATE INDEX idx_species_venom     ON species(venom_level, row_seq);
CREATE INDEX idx_species_danger    ON species(dangerous, row_seq);
CREATE INDEX idx_species_extinct   ON species(extinct, row_seq);
CREATE INDEX idx_species_demo      ON species(is_demo, row_seq);
CREATE INDEX idx_species_region    ON species(group_id, region_key, row_seq);
CREATE INDEX idx_species_habitat   ON species(group_id, habitat_key, row_seq);
CREATE INDEX idx_species_reg_hab   ON species(group_id, region_key, habitat_key, row_seq);
CREATE INDEX idx_species_bn        ON species(bn_name);
CREATE INDEX idx_species_en_lower  ON species(lower(en_name));
CREATE INDEX idx_species_sci_lower ON species(lower(sci_name));
CREATE INDEX idx_species_sci       ON species(sci_name);
CREATE INDEX idx_species_en        ON species(en_name);
CREATE INDEX idx_class_group       ON taxon_class(group_id, sort_order);
CREATE INDEX idx_order_class       ON taxon_order(group_id, class_id, sort_order);
CREATE INDEX idx_family_order      ON taxon_family(group_id, class_id, order_id, sort_order);
CREATE INDEX idx_family_count      ON taxon_family(species_count DESC);
"""

GROUP_BLURB = {
    "insects": "কীটপতঙ্গ — পৃথিবীর সবচেয়ে বড় প্রাণীগ্রুপ; প্রজাপতি, মাছি, বিটল ও পঙ্গপাল।",
    "arachnids": "মাকড়সা ও অ্যারাকনিড — আট পা, দুই অংশের দেহ; তারান্তুলা, বিচ্ছু ও মাইট।",
    "ants": "পিঁপড়া (Formicidae) — সমাজবদ্ধ কীট; লিফ-কাটার, ফায়ার এন্ট ও ডেজার্ট এন্ট।",
    "mollusks": "মলুস্ক — নরম দেহ, প্রায়ই খোলসে ঢাকা; অক্টোপাস, স্কুইড, শামুক ও ঝিনুক।",
    "crustaceans": "ক্রাস্টেশিয়ান — খোলসযুক্ত জলজ সন্ধিপদী; চিংড়ি, কাঁকড়া, লবস্টার ও বার্নাকল।",
    "snakes": "সাপ (Serpentes) — পাবিহীন সরীসৃপ; বিষাক্ত, বিষহীন, সামুদ্রিক ও বৃক্ষবাসী।",
    "lizards_turtles": "টিকটিকি, গুইসাপ, ক্যামেলিয়ন, কচ্ছপ ও কুমির — খোলস বা আঁশযুক্ত সরীসৃপ।",
    "micro_life": "অণুজীব, ছত্রাক ও ভাইরাস — ব্যাকটেরিয়া, মাশরুম, ইস্ট, প্রোটোজোয়া ও ভাইরাস।",
    "other_inverts": "অন্যান্য অমেরুদণ্ডী — জেলিফিশ, প্রবাল, কেঁচো, তারামাছ ও স্পঞ্জ।",
    "mammals": "স্তন্যপায়ী — বাচ্চাকে দুধ খাওয়ায়, শরীরে লোম বা পশম।",
    "birds": "পাখি — পালক, ডানা ও ডিম পাড়া উষ্ণরক্তী প্রাণী।",
    "reptiles": "সরীসৃপ — আঁশে ঢাকা ঠান্ডা রক্তের ডিমপাড়ী প্রাণী।",
    "amphibians": "উভচর — জল ও ডাঙা দুই জায়গাতেই বাস করে।",
    "fishes": "মাছ — ফুলকায় শ্বাস নেয়, জলের ভেতরেই সারা জীবন।",
    "inverts": "অমেরুদণ্ডী — মেরুদণ্ড ছাড়া পোকা, মাকড়সা, শামুক ও কৃমি।",
    "marine": "সামুদ্রিক জীব — প্রবাল, জেলি, স্পঞ্জ ও সমুদ্রের অন্যান্য বাসিন্দা।",
    "fungi": "ছত্রাক — মাশরুম, ইস্ট ও ছাঁচ; সালোকসংশ্লেষ করে না।",
    "plants": "উদ্ভিদ — সালোকসংশ্লেষী গাছ, ঘাস, ফার্ন ও শ্যাওলা।",
    "dinosaurs": "ডাইনোসর ও প্রাগৈতিহাসিক জীব — কোটি বছর আগে বিলুপ্ত।",
    "microbes": "অণুজীব — ব্যাকটেরিয়া, আর্কিয়া ও নীল-সবুজ শৈবাল।",
    "viruses": "ভাইরাস — কোষের বাইরে নিষ্ক্রিয়, ভেতরে প্রবেশ করে বংশবৃদ্ধি।",
}


# ---------------------------------------------------------------------------
# সারি প্রস্তুতি
# ---------------------------------------------------------------------------

def _curated_rows(start_id, demo_upto):
    """curated_species থেকে species-সারি (id start_id থেকে)।"""
    out = []
    for i, r in enumerate(CS.parse_curated()):
        sid = start_id + i
        gid = TAX.new_group(r["group_id"], r["class_id"], r["order_id"], r["family_id"])
        ginfo = GROUP_INDEX.get(gid, ("", "", "", "", "#2E7D32"))
        kp = KINGDOM_PHYLUM.get(gid, ("", ""))
        notes = r["notes_bn"]
        fact = r["fact_bn"]
        if notes and fact and fact.strip() in notes:
            notes = notes.replace(fact.strip(), "").strip()
        out.append({
            "id": sid,
            "bn_name": r["bn_name"], "en_name": r["en_name"],
            "sci_name": r["sci_name"], "authority": r["authority"],
            "class_id": r["class_id"], "order_id": r["order_id"],
            "family_id": r["family_id"], "group_id": gid,
            "region_bn": r["region_bn"],
            "region_key": GEN.region_key_of(r["region_bn"]),
            "habitat_bn": r["habitat_bn"],
            "habitat_key": GEN.habitat_key(r["habitat_bn"]),
            "diet_bn": r["diet_bn"], "iucn": r["iucn"], "size_bn": r["size_bn"],
            "venom_level": r["venom_level"], "venom_bn": r["venom_bn"],
            "repro_bn": r["repro_bn"], "fact_bn": fact,
            "extinct_bn": r["extinct_bn"], "notes_bn": notes,
            "is_demo": 1 if sid <= demo_upto else 0,
            "popularity": 99000 + (2000 - i * 4),
            "row_seq": sid,
            "extinct": 1 if (r["iucn"] in ("EX", "EW") or r["extinct_bn"]) else 0,
            "dangerous": 1 if r["venom_level"] >= 3 else 0,
        })
    return out


def _fill_names(rows, names):
    """ট্যাক্সোনমি-মিল যাচাই (বাংলা নাম DB-তে join থেকে আসে)।"""
    return rows


def _name_index():
    idx = {}
    for (gid, gbn, gen, emoji, color, cls_sci, cls_bn, ord_sci, ord_bn,
         fam_sci, fam_bn) in TAX.iter_taxonomy():
        idx[(gid, cls_sci, ord_sci, fam_sci)] = (cls_bn, ord_bn, fam_bn, fam_sci)
    return idx


def _row_tuple(r):
    return tuple(r[c] for c in SPECIES_COLUMNS)


# ---------------------------------------------------------------------------
# বিল্ড
# ---------------------------------------------------------------------------

def build(total=250000, out_dir=None, web_dir=None, demo_upto=500, verbose=True):
    t0 = time.time()
    out_dir = out_dir or os.path.join(ROOT, "data", "dist")
    os.makedirs(out_dir, exist_ok=True)
    db_path = os.path.join(out_dir, DB_NAME)
    dbz_path = os.path.join(out_dir, DBZ_NAME)
    for p in (db_path, dbz_path):
        if os.path.exists(p):
            os.remove(p)

    names = _name_index()
    conn = sqlite3.connect(db_path)
    conn.executescript(SCHEMA)

    insert_sql = "INSERT INTO species (%s) VALUES (%s)" % (
        ",".join(SPECIES_COLUMNS), ",".join("?" * len(SPECIES_COLUMNS)))

    fam_count = {}
    cls_count = {}
    ord_count = {}
    grp_count = {}
    inserted = 0
    batch = []

    def flush():
        nonlocal batch
        if not batch:
            return
        conn.executemany(insert_sql, batch)
        conn.commit()
        batch = []

    def tally(r):
        g = r["group_id"]; c = r["class_id"]; o = r["order_id"]; f = r["family_id"]
        grp_count[g] = grp_count.get(g, 0) + 1
        cls_count[(g, c)] = cls_count.get((g, c), 0) + 1
        ord_count[(g, c, o)] = ord_count.get((g, c, o), 0) + 1
        fam_count[(g, c, o, f)] = fam_count.get((g, c, o, f), 0) + 1

    def add(r):
        nonlocal inserted
        batch.append(_row_tuple(r))
        tally(r)
        inserted += 1
        if len(batch) >= CHUNK:
            flush()

    # ১) নির্বাচিত বাস্তব প্রজাতি (প্রথম id গুলো)
    curated = _fill_names(_curated_rows(1, demo_upto), names)
    for r in curated:
        add(r)
    nc = len(curated)
    if verbose:
        print("[%6.1fs] curated: %d" % (time.time() - t0, nc))

    # ২) জেনারেটরের বাকি প্রজাতি
    gen_total = max(0, total - nc)
    last = nc
    for r in GEN.iter_species(total=gen_total):
        r["id"] = nc + r["id"]
        r["row_seq"] = r["id"]
        r["notes_bn"] = ""
        r["is_demo"] = 1 if r["id"] <= demo_upto else 0
        r["extinct"] = 1 if (r.get("extinct_bn") or r.get("iucn") in ("EX", "EW")) else 0
        r["dangerous"] = 1 if r.get("venom_level", 0) >= 3 else 0
        for k in ("class_bn", "order_bn", "family_bn", "family_sci", "group_bn",
                  "kingdom", "phylum", "group_en", "emoji", "color", "region_en"):
            r.pop(k, None)
        _fill_names([r], names)
        add(r)
        if verbose and r["id"] - last >= 25000:
            last = r["id"]
            print("[%6.1fs] inserted %d" % (time.time() - t0, inserted))
    flush()
    if verbose:
        print("[%6.1fs] inserted total %d" % (time.time() - t0, inserted))

    # ৩) শ্রেণিবিন্যাস টেবিল
    write_taxonomy(conn, grp_count, cls_count, ord_count, fam_count, names)
    conn.commit()
    if verbose:
        print("[%6.1fs] taxonomy written" % (time.time() - t0))

    # ৪) FTS ইনডেক্স (species + ট্যাক্সোনমি join থেকে)
    fill_fts(conn)
    conn.commit()
    if verbose:
        print("[%6.1fs] FTS rebuilt" % (time.time() - t0))

    # ৫) মেটা
    write_meta(conn, inserted, nc, total)
    conn.commit()

    # ৬) ইনডেক্স + সংকোচন
    conn.executescript(INDEXES)
    conn.commit()
    conn.execute("ANALYZE")
    conn.commit()
    if verbose:
        print("[%6.1fs] indexes done" % (time.time() - t0))
    conn.execute("PRAGMA journal_mode = DELETE")
    conn.execute("VACUUM")
    conn.commit()
    conn.close()
    if verbose:
        print("[%6.1fs] vacuumed; db = %.1f MB" % (time.time() - t0,
                                                   os.path.getsize(db_path) / 1e6))

    # ৭) সংকুচিত .db.z (PKDB1 হেডার + gzip)
    compress_db(db_path, dbz_path)
    if verbose:
        print("[%6.1fs] compressed = %.1f MB" % (time.time() - t0,
                                                 os.path.getsize(dbz_path) / 1e6))

    # ৮) পরিসংখ্যান ও ওয়েব-প্রিভিউ
    stats = make_stats(db_path, inserted, nc)
    with open(os.path.join(out_dir, "stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=1)
    write_preview(db_path, out_dir, web_dir or os.path.join(ROOT, "web", "data"),
                  demo_upto)
    if verbose:
        print("[%6.1fs] DONE -> %s" % (time.time() - t0, dbz_path))
    return db_path, dbz_path, stats


def write_taxonomy(conn, grp_count, cls_count, ord_count, fam_count, names):
    grp_fams, grp_classes, grp_orders = {}, {}, {}
    for (g, c, o, f) in fam_count:
        grp_fams[g] = grp_fams.get(g, 0) + 1
        grp_classes.setdefault(g, set()).add(c)
        grp_orders.setdefault(g, set()).add((c, o))

    order_i = {g[0]: i for i, g in enumerate(GROUPS)}
    groups = []
    for gid, gbn, gen, emoji, color in GROUPS:
        groups.append((
            gid, gbn, gen, emoji, color or "#2E7D32",
            GROUP_BLURB.get(gid, ""),
            grp_count.get(gid, 0), len(grp_classes.get(gid, ())),
            len(grp_orders.get(gid, ())), grp_fams.get(gid, 0),
            order_i.get(gid, 99), GLOBAL_COUNT.get(gid, 0),
        ))
    conn.executemany(
        "INSERT INTO category_group VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", groups)
    conn.executemany(
        "INSERT INTO region VALUES (?,?,?)",
        [(k, bn, i) for i, (k, bn) in enumerate(GEN.REGIONS_LIST)])
    conn.executemany(
        "INSERT INTO habitat VALUES (?,?,?)",
        [(k, bn, i) for i, (k, bn) in enumerate(GEN.HABITATS_LIST)])

    cls_rows, ord_rows, fam_rows = [], [], []
    # শ্রেণি
    seen_cls = {}
    for (g, c), n in sorted(cls_count.items()):
        seen_cls.setdefault(g, []).append((c, n))
    for g, items in seen_cls.items():
        for i, (c, n) in enumerate(items):
            cls_rows.append((c, g, c, CLASS_BN.get(c, c), n,
                             sum(1 for (gg, cc, oo) in ord_count if gg == g and cc == c),
                             i))
    conn.executemany("INSERT INTO taxon_class VALUES (?,?,?,?,?,?,?)", cls_rows)

    seen_ord = {}
    for (g, c, o), n in sorted(ord_count.items()):
        seen_ord.setdefault((g, c), []).append((o, n))
    for (g, c), items in seen_ord.items():
        for i, (o, n) in enumerate(items):
            fams = sum(1 for (gg, cc, oo, ff) in fam_count
                       if gg == g and cc == c and oo == o)
            ord_rows.append((o, g, c, o, ORDER_BN.get(o, o.replace("_ord", "")),
                             n, fams, i))
    conn.executemany("INSERT INTO taxon_order VALUES (?,?,?,?,?,?,?,?)", ord_rows)

    seen_fam = {}
    for (g, c, o, f), n in sorted(fam_count.items()):
        seen_fam.setdefault((g, c, o), []).append((f, n))
    for (g, c, o), items in seen_fam.items():
        for i, (f, n) in enumerate(items):
            bn = names.get((g, c, o, f), ("", "", "", ""))[2] or f
            fam_rows.append((f, g, c, o, f, bn, n, i))
    conn.executemany("INSERT INTO taxon_family VALUES (?,?,?,?,?,?,?,?)", fam_rows)


def fill_fts(conn, batch=20000):
    """contentless FTS5 টেবিলে সারি ভরে। টুকরো টুকরো করে, মেমরি বাঁচাতে।"""
    sel = """
        SELECT s.id, s.bn_name, s.en_name, s.sci_name, s.authority,
               IFNULL(tc.bn_name, s.class_id), IFNULL(tord.bn_name, s.order_id),
               IFNULL(tf.bn_name, s.family_id), IFNULL(cg.bn_name, s.group_id),
               s.region_bn, s.habitat_bn, s.diet_bn, s.size_bn,
               s.venom_bn, s.repro_bn, s.fact_bn, s.extinct_bn, s.notes_bn
        FROM species s
        LEFT JOIN taxon_class  tc   ON tc.group_id = s.group_id
                                  AND tc.class_id = s.class_id
        LEFT JOIN taxon_order  tord ON tord.group_id = s.group_id
                                   AND tord.class_id = s.class_id
                                   AND tord.order_id = s.order_id
        LEFT JOIN taxon_family tf   ON tf.group_id = s.group_id
                                   AND tf.class_id = s.class_id
                                   AND tf.order_id = s.order_id
                                   AND tf.family_id = s.family_id
        LEFT JOIN category_group cg ON cg.group_id = s.group_id
        WHERE s.id > ? AND s.id <= ?
        ORDER BY s.id
    """
    ins = "INSERT INTO species_fts(rowid, %s) VALUES (%s)" % (
        ",".join(FTS_COLUMNS), ",".join("?" * (len(FTS_COLUMNS) + 1)))
    max_id = conn.execute("SELECT MAX(id) FROM species").fetchone()[0] or 0
    lo = 0
    while lo < max_id:
        hi = lo + batch
        rows = conn.execute(sel, (lo, hi)).fetchall()
        conn.executemany(ins, rows)
        lo = hi


def write_meta(conn, inserted, nc, total):
    rows = [
        ("schema_version", "3"),
        ("app_id", "com.prakriti.kosh"),
        ("db_name", DB_NAME),
        ("built_at", time.strftime("%Y-%m-%d %H:%M:%S")),
        ("species_total", str(inserted)),
        ("curated_total", str(nc)),
        ("planned_total", str(total)),
        ("fts_version", "5"),
        ("page_size", "20"),
        ("demo_upto", "500"),
        ("lang", "bn"),
        ("offline", "1"),
    ]
    conn.executemany("INSERT OR REPLACE INTO meta VALUES (?,?)", rows)


def compress_db(db_path, dbz_path):
    """PKDB1 হেডার + gzip স্ট্রিম লেখে।

    হেডার (৫+২+১+৮+৩২ = ৪৮ বাইট, অ্যাপের DatabaseManager-এর সঙ্গে হুবহু মিল):
        0..4    magic   5s  b'PKDB1'
        5..6    version H   1
        7       flags   B   0 = gzip স্ট্রিম
        8..15   raw_len Q   মূল ফাইলের আকার (little-endian)
        16..47  sha256  32s মূল ফাইলের হ্যাশ
    gzip স্ট্রিম শুরু হয় ঠিক ৪৮ নম্বর বাইটে।
    """
    h = hashlib.sha256()
    size = 0
    tmp = dbz_path + ".body"
    with open(db_path, "rb") as fin, open(tmp, "wb") as fout:
        with gzip.GzipFile(filename="", mode="wb", fileobj=fout,
                           compresslevel=9, mtime=0) as gz:
            while True:
                blk = fin.read(1 << 20)
                if not blk:
                    break
                h.update(blk)
                size += len(blk)
                gz.write(blk)
    header = MAGIC + struct.pack("<HBQ", 1, 0, size) + h.digest()
    assert len(header) == 48, len(header)
    with open(tmp, "rb") as fin, open(dbz_path, "wb") as fout:
        fout.write(header)
        while True:
            blk = fin.read(1 << 20)
            if not blk:
                break
            fout.write(blk)
    os.remove(tmp)
    return size, h.hexdigest()


def make_stats(db_path, inserted, nc):
    conn = sqlite3.connect(db_path)
    q = lambda s, *a: conn.execute(s, a).fetchall()  # noqa: E731
    stats = {
        "species_total": inserted,
        "curated_total": nc,
        "db_bytes": os.path.getsize(db_path),
        "by_group": {r[0]: r[1] for r in q(
            "SELECT group_id, COUNT(*) FROM species GROUP BY group_id")},
        "by_iucn": {r[0]: r[1] for r in q(
            "SELECT iucn, COUNT(*) FROM species GROUP BY iucn ORDER BY 2 DESC")},
        "by_venom": {str(r[0]): r[1] for r in q(
            "SELECT venom_level, COUNT(*) FROM species GROUP BY venom_level")},
        "extinct": q("SELECT COUNT(*) FROM species WHERE extinct = 1")[0][0],
        "dangerous": q("SELECT COUNT(*) FROM species WHERE dangerous = 1")[0][0],
        "classes": q("SELECT COUNT(*) FROM taxon_class")[0][0],
        "orders": q("SELECT COUNT(*) FROM taxon_order")[0][0],
        "families": q("SELECT COUNT(*) FROM taxon_family")[0][0],
        "regions": q("SELECT COUNT(DISTINCT region_bn) FROM species")[0][0],
    }
    # FTS বেঞ্চমার্ক
    for probe in ("বাঘ", "tiger", "Panthera", "মাছরাঙা"):
        t = time.time()
        n = q("SELECT COUNT(*) FROM species_fts WHERE species_fts MATCH ?",
              probe + "*")[0][0]
        stats.setdefault("fts_probe", {})[probe] = {
            "hits": n, "ms": round((time.time() - t) * 1000, 2)}
    conn.close()
    return stats


FULL_SELECT = """
SELECT s.*, IFNULL(tc.bn_name, s.class_id) AS class_bn,
       IFNULL(tord.bn_name, s.order_id) AS order_bn,
       IFNULL(tf.bn_name, s.family_id) AS family_bn,
       s.family_id AS family_sci,
       IFNULL(cg.bn_name, s.group_id) AS group_bn,
       IFNULL(cg.en_name, '') AS group_en,
       IFNULL(cg.emoji, '') AS emoji, IFNULL(cg.color, '#2E7D32') AS color
FROM species s
LEFT JOIN taxon_class  tc   ON tc.group_id = s.group_id AND tc.class_id = s.class_id
LEFT JOIN taxon_order  tord ON tord.group_id = s.group_id
                           AND tord.class_id = s.class_id
                           AND tord.order_id = s.order_id
LEFT JOIN taxon_family tf   ON tf.group_id = s.group_id
                           AND tf.class_id = s.class_id
                           AND tf.order_id = s.order_id
                           AND tf.family_id = s.family_id
LEFT JOIN category_group cg ON cg.group_id = s.group_id
"""


def write_preview(db_path, out_dir, web_dir, demo_upto, n_preview=600):
    """ওয়েব প্রোটোটাইপের জন্য ছোট JSON (সব ডেটা নয়)।"""
    os.makedirs(web_dir, exist_ok=True)
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    rows = conn.execute(FULL_SELECT + " WHERE s.is_demo=1"
                        " ORDER BY s.popularity DESC, s.id LIMIT ?",
                        (n_preview,)).fetchall()
    extra = conn.execute(FULL_SELECT + " WHERE s.is_demo=0"
                         " ORDER BY s.row_seq LIMIT ?",
                         (max(0, n_preview - len(rows)),)).fetchall()
    species = [dict(r) for r in list(rows) + list(extra)]
    with open(os.path.join(web_dir, "species.json"), "w", encoding="utf-8") as f:
        json.dump(species, f, ensure_ascii=False)

    tax = {"groups": [], "classes": [], "orders": [], "families": []}
    for r in conn.execute("SELECT * FROM category_group ORDER BY sort_order"):
        tax["groups"].append(dict(r))
    for tbl, key in (("taxon_class", "classes"), ("taxon_order", "orders"),
                     ("taxon_family", "families")):
        for r in conn.execute(
                "SELECT * FROM %s ORDER BY group_id, sort_order LIMIT 4000" % tbl):
            tax[key].append(dict(r))
    with open(os.path.join(web_dir, "taxonomy.json"), "w", encoding="utf-8") as f:
        json.dump(tax, f, ensure_ascii=False)

    with open(os.path.join(out_dir, "preview_species.json"), "w",
              encoding="utf-8") as f:
        json.dump(species[:300], f, ensure_ascii=False, indent=1)
    conn.close()
    return len(species)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--total", type=int, default=250000)
    ap.add_argument("--quick", action="store_true", help="৫০০০ সারির পরীক্ষামূলক বিল্ড")
    ap.add_argument("--out", default=None)
    ap.add_argument("--web", default=None)
    ap.add_argument("--demo", type=int, default=500)
    args = ap.parse_args()
    total = 5000 if args.quick else args.total
    db_path, dbz_path, stats = build(total=total, out_dir=args.out,
                                     web_dir=args.web, demo_upto=args.demo)
    print(json.dumps({k: v for k, v in stats.items() if k != "fts_probe"},
                     ensure_ascii=False, indent=1))
    print(json.dumps(stats["fts_probe"], ensure_ascii=False, indent=1))
    print("db :", db_path)
    print("dbz:", dbz_path)


if __name__ == "__main__":
    main()
