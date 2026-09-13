# -*- coding: utf-8 -*-
"""প্রকৃতি কোষ — শ্রেণিবিন্যাস একত্রীকরণ।

data/tax_animals.py ও data/tax_other.py-এর লাইন-ভিত্তিক তালিকা থেকে
১২টি প্রধান বিভাগের পরিবার-কাঠামো তৈরি করে।

প্রতিটি লাইনের গঠন:  শ্রেণি|বর্গ|পরিবার(বৈজ্ঞানিক)|পরিবার(বাংলা)
"""

import os

import tax_animals, tax_other
from groups import GROUPS, GROUP_INFO, CLASS_BN, ORDER_BN



# কোন তালিকার কোন বিভাগে যাবে: (ব্লক, গ্রুপ)
_BLOCK_GROUPS = [
    ("MAMMALS", "mammals"),
    ("BIRDS", "birds"),
    ("REPTILES", "reptiles"),
    ("AMPHIBIANS", "amphibians"),
    ("FISHES", "fishes"),
    ("INVERTS", "inverts"),
    ("DINOSAURS", "dinosaurs"),
    ("FUNGI", "fungi"),
    ("PLANTS", "plants"),
    ("MICROBES", "microbes"),
    ("VIRUSES", "viruses"),
]

# এই শ্রেণিগুলো "সামুদ্রিক জীব" বিভাগে যাবে (যদি আগে অন্য বিভাগে না গিয়ে থাকে)
_MARINE_CLASSES = {
    "Porifera", "Ctenophora_ord", "Phoronida_ord", "Sipuncula_ord", "Brachiopoda_ord",
    "Arthropoda_marine", "Chordata_marine", "Chlorophyta_ord", "Phaeophyceae_ord",
    "Rhodophyta_ord", "Dinophyceae_ord", "Nemertea_ord", "Platyhelminthes",
    "Annelida", "Mollusca", "Cnidaria", "Echinodermata",
}

# এই শ্রেণিগুলো সবসময় "অমেরুদণ্ডী" বিভাগে
_ALWAYS_INVERTS = {"Insecta", "Arachnida", "Crustacea", "Nematoda", "Rotifera",
                   "Bryozoa", "Tardigrada", "Ascidiacea_ord", "Thaliacea_ord",
                   "Branchiostoma_ord", "Hemichordata_ord", "Chaetognatha_ord",
                   "Acoelomorpha_ord"}


def _parse(block):
    out = []
    for raw in block.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|")
        if len(parts) != 4:
            continue
        out.append(tuple(p.strip() for p in parts))
    return out


def _marine_rows():
    """সামুদ্রিক শ্রেণির সারি — সব ব্লক থেকে সংগ্রহ (প্রথমবার যেখানে পাবে)।"""
    out, seen = [], set()
    blocks = [(getattr(tax_animals, n, "")) for n, _ in _BLOCK_GROUPS]
    blocks.append(tax_other.MICROBES)
    for block in blocks:
        for cls, order, fam_sci, fam_bn in _parse(block):
            if cls in _MARINE_CLASSES and (cls, order, fam_sci) not in seen:
                seen.add((cls, order, fam_sci))
                out.append((cls, order, fam_sci, fam_bn))
    return out


def _blocks():
    """[(group_id, rows), ...] ক্রম অনুযায়ী। সামুদ্রিক আগে, যাতে নকল না হয়।"""
    yield "marine", _marine_rows()
    for name, gid in _BLOCK_GROUPS:
        yield gid, _parse(getattr(tax_animals, name, "") or getattr(tax_other, name, ""))


def build_taxonomy():
    """গ্রুপ → শ্রেণি → বর্গ → [(পরিবার, বাংলা)] কাঠামো।"""
    seen = set()
    tax = {gid: {} for gid, _, _, _, _ in GROUPS}
    for gid, rows in _blocks():
        for cls, order, fam_sci, fam_bn in rows:
            key = (cls, order, fam_sci)
            if key in seen:
                continue
            seen.add(key)
            tax[gid].setdefault(cls, {}).setdefault(order, []).append((fam_sci, fam_bn))
    # খালি গ্রুপ বাদ
    return {gid: classes for gid, classes in tax.items() if classes}


TAXONOMY = build_taxonomy()


def iter_taxonomy():
    """(group_id, group_bn, group_en, emoji, color, class_sci, class_bn,
    order_sci, order_bn, family_sci, family_bn) জেনারেট করে।"""
    for gid, gbn, gen, emoji, color in GROUPS:
        classes = TAXONOMY.get(gid)
        if not classes:
            continue
        for cls_sci, orders in classes.items():
            cls_bn = CLASS_BN.get(cls_sci, cls_sci)
            for ord_sci, fams in orders.items():
                ord_bn = ORDER_BN.get(ord_sci, ord_sci.replace("_ord", ""))
                for fam_sci, fam_bn in fams:
                    yield (gid, gbn, gen, emoji, color,
                           cls_sci, cls_bn, ord_sci, ord_bn, fam_sci, fam_bn)


def family_count():
    return sum(len(f) for g in TAXONOMY.values() for o in g.values() for f in o.values())


def stats():
    return {
        "groups": len(TAXONOMY),
        "classes": sum(len(g) for g in TAXONOMY.values()),
        "orders": sum(len(o) for g in TAXONOMY.values() for o in g.values()),
        "families": family_count(),
    }


if __name__ == "__main__":
    import json
    print(json.dumps(stats(), ensure_ascii=False, indent=1))
    per_group = {gid: sum(len(f) for o in c.values() for f in o.values())
                 for gid, c in TAXONOMY.items()}
    print(json.dumps(per_group, ensure_ascii=False, indent=1))
    for i, row in enumerate(iter_taxonomy()):
        if i < 3 or 500 < i < 503:
            print(row)
