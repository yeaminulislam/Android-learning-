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
    tax = {}
    for gid, rows in _blocks():
        tax.setdefault(gid, {})
        for cls, order, fam_sci, fam_bn in rows:
            key = (cls, order, fam_sci)
            if key in seen:
                continue
            seen.add(key)
            tax[gid].setdefault(cls, {}).setdefault(order, []).append((fam_sci, fam_bn))
    # খালি গ্রুপ বাদ
    return {gid: classes for gid, classes in tax.items() if classes}


TAXONOMY = build_taxonomy()


# ── ব্লুপ্রিন্ট অনুযায়ী নতুন প্রধান বিভাগে রিম্যাপ ──────────────────────────
# সাপের পরিবার (Serpentes) — বাকি Squamata টিকটিকি/গুইসাপ
SNAKE_FAMS = {
    "Pythonidae", "Boidae", "Colubridae", "Elapidae", "Viperidae", "Natricidae",
    "Homalopsidae", "Acrochordidae", "Typhlopidae", "Leptotyphlopidae",
    "Pareatidae", "Xenodermatidae",
}
# পিঁপড়ার পরিবার — ব্লুপ্রিন্টে আলাদা প্রধান বিভাগ
ANT_FAMS = {"Formicinae_fam", "Myrmicinae_fam", "Oecophyllinae_fam"}

_MOLLUSK_CLASSES = {"Mollusca"}
_CRUSTACEAN_CLASSES = {"Crustacea", "Arthropoda_marine"}
_MICRO_CLASSES = {
    "Agaricomycetes", "Ascomycetes", "Zygomycetes", "Oomycetes", "Myxomycetes",
    "Lichenes", "Bacilli", "Gammaproteobacteria", "Alphaproteobacteria",
    "Betaproteobacteria", "Actinobacteria", "Clostridia", "Cyanobacteria",
    "Bacteroidia", "Apicomplexa", "Archaea", "Fusobacteria", "Spirochaetes",
}
_PLANT_CLASSES = {
    "Magnoliopsida", "Liliopsida", "Pinopsida", "Polypodiopsida", "Bryopsida",
    "Anthocerotopsida", "Cycadopsida", "Ginkgoopsida",
}


def new_group(old_gid, cls_sci, ord_sci, fam_sci):
    """পুরোনো গ্রুপ-আইডি + শ্রেণি/বর্গ/পরিবার → ব্লুপ্রিন্টের নতুন গ্রুপ-আইডি।"""
    if old_gid == "reptiles":
        if fam_sci in SNAKE_FAMS:
            return "snakes"
        return "lizards_turtles"
    if old_gid == "inverts":
        if cls_sci == "Insecta":
            return "ants" if fam_sci in ANT_FAMS else "insects"
        if cls_sci == "Arachnida":
            return "arachnids"
        if cls_sci in _MOLLUSK_CLASSES:
            return "mollusks"
        if cls_sci in _CRUSTACEAN_CLASSES:
            return "crustaceans"
        return "other_inverts"
    if old_gid == "marine":
        if cls_sci in _MOLLUSK_CLASSES:
            return "mollusks"
        if cls_sci in _CRUSTACEAN_CLASSES:
            return "crustaceans"
        if cls_sci == "Chordata_marine":
            return "fishes" if ord_sci == "Petromyzontiformes" else "other_inverts"
        return "other_inverts"
    if old_gid in ("fungi", "microbes", "viruses"):
        return "micro_life"
    if cls_sci in _MICRO_CLASSES:
        return "micro_life"
    if cls_sci in _PLANT_CLASSES or old_gid == "plants":
        return "plants"
    if old_gid == "amphibians":
        return "amphibians"
    return old_gid          # mammals / birds / fishes / dinosaurs অপরিবর্তিত


def iter_taxonomy():
    """(group_id, group_bn, group_en, emoji, color, class_sci, class_bn,
    order_sci, order_bn, family_sci, family_bn) জেনারেট করে — নতুন বিভাগ-আইডি সহ।
    একই (গ্রুপ, শ্রেণি, বর্গ, পরিবার) একবারের বেশি আসে না।"""
    from groups import GROUP_INFO, CLASS_BN, ORDER_BN
    seen = set()
    for old_gid in TAXONOMY:
        classes = TAXONOMY.get(old_gid)
        if not classes:
            continue
        for cls_sci, orders in classes.items():
            cls_bn = CLASS_BN.get(cls_sci, cls_sci)
            for ord_sci, fams in orders.items():
                ord_bn = ORDER_BN.get(ord_sci, ord_sci.replace("_ord", ""))
                for fam_sci, fam_bn in fams:
                    gid = new_group(old_gid, cls_sci, ord_sci, fam_sci)
                    key = (gid, cls_sci, ord_sci, fam_sci)
                    if key in seen:
                        continue
                    seen.add(key)
                    g = GROUP_INFO.get(gid, (gid, gid, gid, "", "#2E7D32"))
                    yield (gid, g[1], g[2], g[3], g[4], cls_sci, cls_bn,
                           ord_sci, ord_bn, fam_sci, fam_bn)


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
