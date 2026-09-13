#!/usr/bin/env python3
"""test_sql.py — Repository.java-র প্রতিটি SQL আসল ডেটাবেসে কম্পাইল হয় কি না।

কেন দরকার
----------
ডিভাইসে চালানোর আগে SQL-এর syntax ভুল ধরার কোনো উপায় ছিল না — ফলে একটি
**কমা missing** (`s.extinct`-এর পরে) সরাসরি ব্যবহারকারীর ফোনে গিয়ে ধরা পড়েছিল:
`SQLiteException: near "(": syntax error`।

এই স্ক্রিপ্টটি:

১. `Repository.java` থেকে SELECT_BASE / SELECT_EXTRA / JOIN_TAXONOMY ধ্রুবকগুলো
   **সরাসরি পাঠে** নেয় (একক উৎস — Java বদলালে পরীক্ষাও বদলায়)
২. বাকি কুয়েরি-টেমপ্লেটগুলো এখানে হুবহু আয়নায় রাখা হয় (টীকাসহ)
৩. প্রতিটি কুয়েরি আসল SQLite ডেটাবেসে `EXPLAIN` করে দেখা হয় — syntax ভুল থাকলে
   সঙ্গে সঙ্গে ধরা পড়ে
৪. কয়েকটি গুরুত্বপূর্ণ কুয়েরি আসল ডেটাতে চালিয়ে কলাম ও ফলও দেখা হয়

    python3 tools/test_sql.py [ডেটাবেসের পথ]
    (ডিফল্ট: data/dist/prakriti_kosh.db)
"""
import os
import re
import sqlite3
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO_JAVA = os.path.join(ROOT, 'android', 'app', 'src', 'main', 'java',
                         'com', 'prakriti', 'kosh', 'db', 'Repository.java')

CONST_RE = re.compile(
    r'public static final String (\w+) =\s*((?:"[^"]*"\s*\+?\s*)+);', re.S)
STR_RE = re.compile(r'"((?:[^"\\]|\\.)*)"')

PAGE = 25          # Repository.PAGE_SIZE


def java_const(name):
    src = open(REPO_JAVA, encoding='utf-8').read()
    for cname, body in CONST_RE.findall(src):
        if cname == name:
            parts = STR_RE.findall(body)
            return ''.join(p.replace('\\"', '"').replace('\\n', '\n')
                           for p in parts)
    raise SystemExit('✗ Repository.java-তে ধ্রুবক পাওয়া যায়নি: ' + name)


# ── Java থেকে আসা একক উৎস ────────────────────────────────────────────
SELECT_BASE = java_const('SELECT_BASE')
SELECT_EXTRA = java_const('SELECT_EXTRA')
JOIN_TAXONOMY = java_const('JOIN_TAXONOMY')
SELECT_FULL = SELECT_BASE + SELECT_EXTRA + ' FROM species s' + JOIN_TAXONOMY

# ── আয়নায় রাখা কুয়েরি-টেমপ্লেট (Repository.java-এর হুবহু) ────────────
SCOPE = {
    'ALL': '',
    'GROUP': ' AND s.group_id=?',
    'CLASS': ' AND s.group_id=? AND s.class_id=?',
    'ORDER': ' AND s.group_id=? AND s.class_id=? AND s.order_id=?',
    'FAMILY': ' AND s.group_id=? AND s.class_id=? AND s.order_id=?'
              ' AND s.family_id=?',
    'THREATENED': " AND s.iucn IN ('VU','EN','CR')",
    'EXTINCT': ' AND s.extinct=1',
    'VENOMOUS': ' AND s.dangerous=1',
    'DEMO': ' AND s.is_demo=1',
}


def page_seq(mode):
    return ('SELECT s.id, s.row_seq FROM species s WHERE s.row_seq>?'
            + SCOPE[mode] + ' ORDER BY s.row_seq LIMIT ' + str(PAGE + 1))


def page_pop(mode):
    return ('SELECT s.id, s.popularity FROM species s WHERE (s.popularity<?'
            ' OR (s.popularity=? AND s.id>?))' + SCOPE[mode]
            + ' ORDER BY s.popularity DESC, s.id LIMIT ' + str(PAGE + 1))


def count_of(mode):
    return 'SELECT COUNT(*) FROM species s WHERE 1=1' + SCOPE[mode]


def by_ids(n):
    parts = ' UNION ALL '.join('SELECT %d AS k, ? AS sid' % i for i in range(n))
    return (SELECT_BASE + SELECT_EXTRA + ', o.k FROM (' + parts + ') AS o'
            + ' JOIN species s ON s.id = o.sid' + JOIN_TAXONOMY
            + ' ORDER BY o.k')


TAX = ('mammals', 'mammalia', 'carnivora', 'felidae')

QUERIES = [
    ('SELECT_FULL (বিস্তারিত পর্দা)', SELECT_FULL + ' WHERE s.id=?', (1,), True),
    ('groups (প্রধান পর্দা)',
     'SELECT group_id, bn_name, en_name, emoji, color, blurb_bn,'
     ' species_count, class_count, order_count, family_count, sort_order'
     ' FROM category_group ORDER BY sort_order', (), True),
    ('group',
     'SELECT group_id, bn_name, en_name, emoji, color, blurb_bn,'
     ' species_count, class_count, order_count, family_count, sort_order'
     ' FROM category_group WHERE group_id=?', ('mammals',), True),
    ('classes',
     'SELECT class_id, group_id, sci_name, bn_name, species_count,'
     ' order_count, sort_order FROM taxon_class WHERE group_id=?'
     ' ORDER BY sort_order', ('mammals',), True),
    ('orders',
     'SELECT order_id, group_id, class_id, sci_name, bn_name, species_count,'
     ' family_count, sort_order FROM taxon_order WHERE group_id=?'
     ' AND class_id=? ORDER BY sort_order', ('mammals', 'mammalia'), True),
    ('families',
     'SELECT family_id, group_id, class_id, order_id, sci_name, bn_name,'
     ' species_count, sort_order FROM taxon_family WHERE group_id=?'
     ' AND class_id=? AND order_id=? ORDER BY sort_order', TAX[:3], True),
    ('familyIds',
     'SELECT s.id FROM species s WHERE s.group_id=? AND s.class_id=?'
     ' AND s.order_id=? AND s.family_id=?'
     ' ORDER BY s.popularity DESC, s.row_seq LIMIT ?', TAX + (30,), True),
    ('related',
     'SELECT id FROM species WHERE group_id=? AND class_id=? AND order_id=?'
     ' AND family_id=? AND id<>? ORDER BY popularity DESC, row_seq LIMIT ?',
     TAX + (1, 8), True),
    ('neighbours',
     'SELECT s.id FROM species s WHERE s.row_seq>?' + SCOPE['FAMILY']
     + ' ORDER BY s.row_seq LIMIT 4', (0,) + TAX, True),
    ('search: হুবহু বাংলা', 'SELECT id FROM species WHERE bn_name=? LIMIT ?',
     ('বাঘ', 5), True),
    ('search: হুবহু ইংরেজি',
     'SELECT id FROM species WHERE LOWER(en_name)=? LIMIT ?', ('tiger', 5), True),
    ('search: হুবহু বৈজ্ঞানিক',
     'SELECT id FROM species WHERE LOWER(sci_name)=? LIMIT ?',
     ('panthera tigris', 5), True),
    ('search: prefix বাংলা',
     'SELECT id FROM species WHERE bn_name>=? AND bn_name<?'
     ' ORDER BY popularity DESC LIMIT ?', ('বাঘ', 'বাঘ\uffff', 5), True),
    ('search: prefix ইংরেজি',
     'SELECT id FROM species WHERE LOWER(en_name)>=? AND LOWER(en_name)<?'
     ' ORDER BY popularity DESC LIMIT ?', ('tiger', 'tiger\uffff', 5), True),
    ('search: FTS5',
     'SELECT s.id FROM species_fts f JOIN species s ON s.id=f.rowid'
     ' WHERE species_fts MATCH ? ORDER BY rank LIMIT ?', ('"বাঘ"*', 15), True),
    ('countAll', 'SELECT COUNT(*) FROM species', (), True),
]

# ── ব্যবহারকারীর নিজস্ব ডেটাবেসের কুয়েরি (আলাদা ফাইলে টেবিল বানায়) ──────
USER_QUERIES = [
    ('favorites', 'SELECT species_id FROM favorite ORDER BY added_at DESC'
                  ' LIMIT 2000', ()),
    ('favorite: আছে কি না', 'SELECT 1 FROM favorite WHERE species_id=?', (1,)),
    ('favorite: গণনা', 'SELECT COUNT(*) FROM favorite', ()),
    ('notes', 'SELECT species_id, body FROM note', ()),
    ('note: একটি', 'SELECT body FROM note WHERE species_id=?', (1,)),
    ('note: গণনা', 'SELECT COUNT(*) FROM note', ()),
    ('pref', 'SELECT value FROM pref WHERE key=?', ('intro_seen',)),
]

for mode in SCOPE:
    QUERIES.append(('page seq [%s]' % mode, page_seq(mode),
                    (0,) + (('mammals',) if mode == 'GROUP'
                            else TAX[:{'CLASS': 2, 'ORDER': 3,
                                       'FAMILY': 4}.get(mode, 0)]),
                    mode in ('ALL', 'GROUP')))
    QUERIES.append(('page pop [%s]' % mode, page_pop(mode),
                    (999999999, 999999999, 0) + (('mammals',) if mode == 'GROUP'
                                                 else TAX[:{'CLASS': 2, 'ORDER': 3,
                                                            'FAMILY': 4}.get(mode, 0)]),
                    mode == 'ALL'))
    QUERIES.append(('count [%s]' % mode, count_of(mode),
                    (('mammals',) if mode == 'GROUP'
                     else TAX[:{'CLASS': 2, 'ORDER': 3,
                                'FAMILY': 4}.get(mode, 0)]),
                    mode in ('ALL', 'THREATENED', 'EXTINCT')))
    QUERIES.append(('page seq+iucn [%s]' % mode,
                    page_seq(mode).replace(' LIMIT %d' % (PAGE + 1),
                                            ' AND s.iucn=? LIMIT %d' % (PAGE + 1)),
                    (0, 'EN') + (('mammals',) if mode == 'GROUP'
                                 else TAX[:{'CLASS': 2, 'ORDER': 3,
                                            'FAMILY': 4}.get(mode, 0)]),
                    False))

QUERIES.append(('byIds (২৪টি)', by_ids(24), tuple(range(1, 25)), True))
QUERIES.append(('byIds (১টি)', by_ids(1), (7,), True))

USER_DDL = [
    'CREATE TABLE IF NOT EXISTS favorite ('
    'species_id INTEGER PRIMARY KEY, added_at INTEGER NOT NULL)',
    'CREATE TABLE IF NOT EXISTS note ('
    'species_id INTEGER PRIMARY KEY, body TEXT NOT NULL DEFAULT \'\','
    ' updated_at INTEGER NOT NULL)',
    'CREATE TABLE IF NOT EXISTS pref ('
    'key TEXT PRIMARY KEY, value TEXT NOT NULL)',
]


def main():
    db_path = sys.argv[1] if len(sys.argv) > 1 else \
        os.path.join(ROOT, 'data', 'dist', 'prakriti_kosh.db')
    if not os.path.exists(db_path):
        sys.stderr.write('✗ ডেটাবেস নেই: %s\n' % db_path)
        return 1
    conn = sqlite3.connect(db_path)
    conn.execute('PRAGMA query_only=ON')
    bad = 0
    print('SQL পরীক্ষা: %s\n' % db_path)
    for name, sql, args, run in QUERIES:
        try:
            conn.execute('EXPLAIN ' + sql, args)
        except Exception as e:
            bad += 1
            print('  ✗ %-28s %s' % (name, e))
            print('      SQL: %s…' % sql[:160])
            continue
        if run:
            try:
                cur = conn.execute(sql, args)
                rows = cur.fetchmany(3)
                ncol = len(cur.description) if cur.description else 0
                print('  ✓ %-28s কলাম=%-2d নমুনা-সারি=%d' % (name, ncol, len(rows)))
            except Exception as e:
                bad += 1
                print('  ✗ %-28s চালানো যায়নি: %s' % (name, e))
        else:
            print('  ✓ %-28s (syntax ঠিক)' % name)

    # ব্যবহারকারীর ডেটাবেসের DDL + তার কুয়েরি আলাদা ফাইলে পরীক্ষা
    uconn = sqlite3.connect(':memory:')
    for ddl in USER_DDL:
        try:
            uconn.execute(ddl)
            print('  ✓ user-db DDL: %s…' % ddl[30:60])
        except Exception as e:
            bad += 1
            print('  ✗ user-db DDL: %s' % e)
    for name, sql, args in USER_QUERIES:
        try:
            uconn.execute(sql, args)
            print('  ✓ %-28s (user-db)' % name)
        except Exception as e:
            bad += 1
            print('  ✗ %-28s (user-db) %s' % (name, e))
    uconn.close()
    conn.close()
    print('')
    if bad:
        print('❌ %dটি SQL ভুল — অ্যাপে এই কুয়েরিগুলো ক্র্যাশ করবে' % bad)
        return 1
    print('✅ %dটি কুয়েরির সবগুলো আসল ডেটাবেসে কম্পাইল হয়েছে' % len(QUERIES))
    return 0


if __name__ == '__main__':
    sys.exit(main())
