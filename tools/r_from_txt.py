#!/usr/bin/env python3
"""r_from_txt.py — aapt2-এর R.txt থেকে **আসল** R.java বানায়।

কেন এটি জরুরি
--------------
`tools/gen_r.py` আগে নিজে নিজে কাল্পনিক id (0x7f010001, 0x7f010002 …) বসিয়ে দিত।
কিন্তু APK-র ভেতরের `resources.arsc` তৈরি হয় `aapt2 link` দিয়ে, যেখানে id বসে
টাইপ-অনুযায়ী (0x7f02… drawable, 0x7f04… string, 0x7f05… style)। ফলে কম্পাইল হওয়া
কোডের সব রিসোর্স-id ভুল হত → অ্যাপ খোলার সঙ্গে সঙ্গে
`Resources$NotFoundException`-এ ক্র্যাশ।

তাই বিল্ডের ধাপ বদলানো হয়েছে: আগে `aapt2 link --output-text-symbols` চালিয়ে
R.txt নেওয়া হয়, তারপর এই স্ক্রিপ্ট সেই **আসল** id দিয়ে R.java বানায়, তারপর
javac (ECJ) ও D8 চলে।

    python3 tools/r_from_txt.py R.txt build/gen/com/prakriti/kosh/R.java [প্যাকেজ]

R.txt-এর লাইন: `int <type> <name> 0x7f050005`
              `int[] <type> <name> {0x…,…}`  (stylable — বাদ পড়ে)
"""
import os
import re
import sys

IDENT = re.compile(r'[^0-9a-zA-Z_$]')
LINE = re.compile(r'^int\s+(\S+)\s+(\S+)\s+(0x[0-9a-fA-F]+)\s*$')


def safe(name):
    """রিসোর্স-নাম → জাভা আইডেন্টিফায়ার (যেমন app-name → app_name)।"""
    s = IDENT.sub('_', name)
    if s[:1].isdigit():
        s = '_' + s
    return s


def parse(path):
    groups = {}
    bad = []
    with open(path, encoding='utf-8') as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith('int[]'):
                continue                      # stylable অ্যারে — আমাদের লাগে না
            m = LINE.match(line)
            if not m:
                if line.startswith('int '):
                    bad.append(line)
                continue
            typ, name, val = m.group(1), m.group(2), int(m.group(3), 16)
            groups.setdefault(typ, {})[safe(name)] = val
    if bad:
        sys.stderr.write('সতর্কতা: %dটি লাইন পড়া যায়নি, যেমন: %s\n' % (len(bad), bad[0]))
    return groups


def render(pkg, groups):
    out = ['/* এই ফাইল tools/r_from_txt.py দিয়ে aapt2-এর R.txt থেকে তৈরি।',
           ' * এতে resources.arsc-এর **আসল** রিসোর্স-id আছে — হাতে বদলাবেন না।',
           ' */',
           'package %s;' % pkg, '',
           'public final class R {', '    private R() {', '    }']
    for typ in sorted(groups):
        out.append('')
        out.append('    public static final class %s {' % typ)
        for name in sorted(groups[typ], key=lambda n: groups[typ][n]):
            out.append('        public static final int %s = 0x%08x;'
                       % (name, groups[typ][name]))
        out.append('    }')
    out.append('}')
    return '\n'.join(out) + '\n'


def main():
    if len(sys.argv) < 3:
        sys.stderr.write(__doc__)
        return 2
    txt, out_java = sys.argv[1], sys.argv[2]
    pkg = sys.argv[3] if len(sys.argv) > 3 else 'com.prakriti.kosh'
    if not os.path.exists(txt):
        sys.stderr.write('✗ R.txt পাওয়া যায়নি: %s\n' % txt)
        return 1
    groups = parse(txt)
    if not groups:
        sys.stderr.write('✗ R.txt-এ কোনো রিসোর্স নেই — aapt2 link ব্যর্থ?\n')
        return 1
    java = render(pkg, groups)
    d = os.path.dirname(out_java)
    if d:
        os.makedirs(d, exist_ok=True)
    with open(out_java, 'w', encoding='utf-8') as f:
        f.write(java)
    total = sum(len(v) for v in groups.values())
    print('  ✓ %s: %d টাইপ, %dটি রিসোর্স-id (আসল)' % (out_java, len(groups), total))
    for typ in sorted(groups):
        vals = sorted(groups[typ].values())
        print('      %-9s %3dটি  0x%08x … 0x%08x'
              % (typ, len(vals), vals[0], vals[-1]))
    return 0


if __name__ == '__main__':
    sys.exit(main())
