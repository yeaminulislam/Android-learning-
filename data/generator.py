# -*- coding: utf-8 -*-
"""প্রকৃতি কোষ — নির্ধারক (deterministic) প্রজাতি জেনারেটর।

একই ইনপুটে সবসময় একই ২,৫০,০০০ প্রজাতি তৈরি হয় (কোনো র‍্যান্ডম seed ফাইল নেই)।
প্রতিটি প্রজাতির বৈশিষ্ট্য তার id থেকে বের করা hash-চালিত পছন্দ তালিকা দিয়ে ঠিক হয়।

ব্যবহার:
    from generator import iter_species, TOTAL
    for sp in iter_species():
        ...
"""

from taxonomy import iter_taxonomy, family_count, GROUPS, GROUP_INFO
import name_banks as nb

TOTAL = 250_000

# ─────────────────────────────────────────────────────────────
# নির্ধারক এলোমেলো সংখ্যা
# ─────────────────────────────────────────────────────────────
_M1 = 0xFFFFFFFF
_M2 = 0x100000000


def _hash(seed):
    """FNV-1a ভিত্তিক নির্ধারক মিশ্রণ।"""
    h = 2166136261
    s = str(seed).encode("utf-8")
    for b in s:
        h ^= b
        h = (h * 16777619) & _M1
    return h


def rnd(seed, salt=0):
    """seed ও salt থেকে 0..1 মান (নির্ধারক)।"""
    h = _hash(seed ^ (salt * 0x9E3779B1))
    h = (h ^ (h >> 13)) * 0x5BD1E995 & _M1
    h ^= h >> 15
    return h / _M1


def pick(seed, salt, seq):
    """তালিকা থেকে নির্ধারক নির্বাচন।"""
    if not seq:
        return None
    return seq[int(rnd(seed, salt) * len(seq)) % len(seq)]


def wpick(seed, salt, pairs):
    """ভারযুক্ত নির্বাচন — pairs = [(item, weight), ...]"""
    total = sum(w for _, w in pairs)
    r = rnd(seed, salt) * total
    acc = 0
    for item, w in pairs:
        acc += w
        if r < acc:
            return item
    return pairs[-1][0]


def randint(seed, salt, lo, hi):
    return lo + int(rnd(seed, salt) * (hi - lo + 1)) % (hi - lo + 1)


BN_DIGIT_MAP = str.maketrans("0123456789", "০১২৩৪৫৬৭৮৯")


def bn_num(x):
    """ইংরেজি সংখ্যাকে বাংলা সংখ্যায় বদলায়।"""
    return str(x).translate(BN_DIGIT_MAP)


def lognum(seed, salt, lo, hi):
    """লগ-স্কেলে বাস্তবসম্মত মাপ — ছোট প্রাণী বেশি, দৈত্য কম।"""
    import math
    r = rnd(seed, salt)
    v = lo * math.exp(r * math.log(hi / lo))
    if hi >= 1000:
        return int(round(v / 10.0) * 10)
    if hi >= 100:
        return int(round(v))
    return max(lo, int(round(v)))


def fill_bn(tmpl, seed, salt, **vals):
    """টেমপ্লেটের {a},{b},… মান বাংলা সংখ্যায় বসায়।"""
    out = tmpl
    for i, k in enumerate("abcd"):
        if "{" + k + "}" in out:
            out = out.replace("{" + k + "}", bn_num(vals[k]))
    for k in ("n", "k"):
        if "{" + k + "}" in out and k in vals:
            out = out.replace("{" + k + "}", bn_num(vals[k]))
    return out


# ─────────────────────────────────────────────────────────────
# নাম তৈরি
# ─────────────────────────────────────────────────────────────
BAD_HAB_WORDS = ("সমুদ্র", "সাগর", "মহাসাগর", "প্রবাল", "গভীর", "মেরু", "তুষার",
                 "হাইড্রোথার্মাল", "কেল্প", "সি গ্রাস", "মোহনা", "উপকূল", "আর্কটিক",
                 "লোনা", "ম্যানগ্রোভ")

MICROBE_GROUPS = {"microbes", "viruses"}
AQUATIC_PLANT_GROUPS = {"plants"}


def family_noun(fam_bn):
    """পরিবারের বাংলা নাম থেকে মূল বিশেষ্য।"""
    if fam_bn in nb.NOUN_OVERRIDE:
        return nb.NOUN_OVERRIDE[fam_bn]
    core = fam_bn.split(" ও ")[0].split("-")[0].strip()
    core = core.replace(" পরিবার", "").replace(" জাতীয়", "").replace(" প্রজাপতি", " প্রজাপতি")
    for suf in (" ছত্রাক", " শৈবাল", " মাশরুম", " লাইকেন", " ভাইরাস", " ব্যাকটেরিয়া",
                " ডাইনোসর", " সরীসৃপ", " মাকড়সা", " বিচ্ছু", " মাকড়সা", " পোকা",
                " শামুক", " ঝিনুক", " কৃমি", " মাছ", " পাখি", " গাছ"):
        if core.endswith(suf):
            return core
    return core


def _place_bn(region_bn):
    """অঞ্চলের নাম থেকে বাংলা বর্ণনামূলক বিশেষণ।"""
    table = {
        "বাংলাদেশ": "বাংলার", "ভারত": "ভারতীয়", "পাকিস্তান": "পাকিস্তানি",
        "নেপাল": "নেপালি", "ভুটান": "ভুটানি", "শ্রীলঙ্কা": "সিংহলি",
        "মালদ্বীপ": "মালদ্বীপের", "আফগানিস্তান": "আফগান", "চীন": "চীনা",
        "জাপান": "জাপানি", "দক্ষিণ কোরিয়া": "কোরীয়", "উত্তর কোরিয়া": "কোরীয়",
        "মঙ্গোলিয়া": "মঙ্গোলীয়", "থাইল্যান্ড": "থাই", "ভিয়েতনাম": "ভিয়েতনামি",
        "মিয়ানমার": "বার্মিজ", "লাওস": "লাও", "কম্বোডিয়া": "খমের",
        "মালয়েশিয়া": "মালয়", "ইন্দোনেশিয়া": "ইন্দোনেশীয়", "ফিলিপাইন": "ফিলিপিনো",
        "সিঙ্গাপুর": "সিঙ্গাপুরি", "ব্রুনাই": "ব্রুনাই", "ইরান": "ইরানি",
        "ইরাক": "ইরাকি", "তুরস্ক": "তুর্কি", "সিরিয়া": "সিরীয়",
        "লেবানন": "লেবানিজ", "জর্ডান": "জর্ডানীয়", "ইসরায়েল": "ইসরায়েলি",
        "সৌদি আরব": "সৌদি", "ইয়েমেন": "ইয়েমেনি", "ওমান": "ওমানি",
        "কাজাখস্তান": "কাজাখ", "উজবেকিস্তান": "উজবেক", "তুর্কমেনিস্তান": "তুর্কমেন",
        "কিরগিজস্তান": "কিরগিজ", "তাজিকিস্তান": "তাজিক", "রাশিয়া": "রাশিয়ান",
        "মিশর": "মিশরীয়", "লিবিয়া": "লিবীয়", "তিউনিসিয়া": "তিউনিসীয়",
        "আলজেরিয়া": "আলজেরীয়", "মরক্কো": "মরক্কান", "সুদান": "সুদানি",
        "ইথিওপিয়া": "ইথিওপীয়", "কেনিয়া": "কেনীয়", "উগান্ডা": "উগান্ডীয়",
        "তানজানিয়া": "তানজানীয়", "কঙ্গো": "কঙ্গোলিজ", "গাবোন": "গ্যাবোনিজ",
        "ক্যামেরুন": "ক্যামেরুনি", "নাইজেরিয়া": "নাইজেরীয়", "ঘানা": "ঘানা",
        "সেনেগাল": "সেনেগালি", "মালি": "মালিয়ান", "নাইজার": "নাইজারীয়",
        "চাদ": "চাদ", "দক্ষিণ আফ্রিকা": "দক্ষিণ আফ্রিকান", "বতসোয়ানা": "বতসোয়ানা",
        "জিম্বাবুয়ে": "জিম্বাবুইয়ান", "জাম্বিয়া": "জাম্বিয়ান", "মোজাম্বিক": "মোজাম্বিকান",
        "মাদাগাস্কার": "মাদাগাস্কারিয়ান", "অ্যাঙ্গোলা": "অ্যাঙ্গোলান",
        "নামিবিয়া": "নামিবীয়", "মালাউই": "মালাউইয়ান", "গিনি": "গিনি",
        "লাইবেরিয়া": "লাইবেরীয়", "বেনিন": "বেনিনি", "টোগো": "টোগোলিজ",
        "যুক্তরাজ্য": "ব্রিটিশ", "আয়ারল্যান্ড": "আয়ারিশ", "ফ্রান্স": "ফরাসি",
        "জার্মানি": "জার্মান", "স্পেন": "স্প্যানিশ", "পর্তুগাল": "পর্তুগিজ",
        "ইতালি": "ইতালীয়", "গ্রিস": "গ্রিক", "নেদারল্যান্ডস": "ডাচ",
        "বেলজিয়াম": "বেলজিয়ান", "সুইজারল্যান্ড": "সুইস", "অস্ট্রিয়া": "অস্ট্রিয়ান",
        "পোল্যান্ড": "পোলিশ", "চেক প্রজাতন্ত্র": "চেক", "স্লোভাকিয়া": "স্লোভাক",
        "হাঙ্গেরি": "হাঙ্গেরীয়", "রোমানিয়া": "রোমানীয়", "বুলগেরিয়া": "বুলগেরীয়",
        "ক্রোয়েশিয়া": "ক্রোয়েশীয়", "স্লোভেনিয়া": "স্লোভেনীয়", "সার্বিয়া": "সার্বিয়ান",
        "বসনিয়া": "বসনিয়ান", "আলবেনিয়া": "আলবেনীয়", "সুইডেন": "সুইডিশ",
        "নরওয়ে": "নরওয়েজীয়", "ডেনমার্ক": "ড্যানিশ", "ফিনল্যান্ড": "ফিনিশ",
        "আইসল্যান্ড": "আইসল্যান্ডীয়", "এস্তোনিয়া": "এস্তোনীয়", "লাতভিয়া": "লাতভীয়",
        "লিথুয়ানিয়া": "লিথুয়ানীয়", "ইউক্রেন": "ইউক্রেনীয়", "বেলারুশ": "বেলারুশিয়ান",
        "মলদোভা": "মলদোভান", "সাইপ্রাস": "সাইপ্রিয়ট", "মাল্টা": "মাল্টীয়",
        "মার্কিন যুক্তরাষ্ট্র": "মার্কিন", "কানাডা": "কানাডীয়", "মেক্সিকো": "মেক্সিকান",
        "গুয়াতেমালা": "গুয়াতেমালান", "বেলিজ": "বেলিজিয়ান", "হন্ডুরাস": "হন্ডুরান",
        "নিকারাগুয়া": "নিকারাগুয়ান", "কোস্টা রিকা": "কোস্টারিকান", "পানামা": "পানামানিয়ান",
        "কিউবা": "কিউবান", "হাইতি": "হাইতিয়ান", "জ্যামাইকা": "জ্যামাইকান",
        "পুয়ের্তো রিকো": "পুয়ের্তো রিকান", "কলম্বিয়া": "কলম্বিয়ান",
        "ভেনেজুয়েলা": "ভেনেজুয়েলান", "গায়ানা": "গায়ানিজ", "সুরিনাম": "সুরিনামি",
        "ইকুয়েডর": "ইকুয়েডরীয়", "পেরু": "পেরুভিয়ান", "বলিভিয়া": "বলিভিয়ান",
        "ব্রাজিল": "ব্রাজিলীয়", "প্যারাগুয়ে": "প্যারাগুয়ান", "উরুগুয়ে": "উরুগুয়েয়ান",
        "আর্জেন্টিনা": "আর্জেন্টিনীয়", "চিলি": "চিলিয়ান", "অস্ট্রেলিয়া": "অস্ট্রেলীয়",
        "নিউজিল্যান্ড": "নিউজিল্যান্ডীয়", "পাপুয়া নিউ গিনি": "পাপুয়ান", "ফিজি": "ফিজিয়ান",
        "সলোমন দ্বীপপুঞ্জ": "সলোমন", "ভানুয়াটু": "ভানুয়াটু", "নিউ ক্যালেডোনিয়া": "নিউ ক্যালেডোনিয়ান",
        "সামোয়া": "সামোয়ান", "টোঙ্গা": "টোঙ্গান", "পালাউ": "পালাউয়ান",
        "আর্কটিক": "আর্কটিক", "অ্যান্টার্কটিকা": "অ্যান্টার্কটিক",
        "প্রশান্ত মহাসাগর": "প্রশান্ত", "আটলান্টিক মহাসাগর": "আটলান্টিক",
        "ভারত মহাসাগর": "ভারত মহাসাগরীয়", "দক্ষিণ মহাসাগর": "দক্ষিণ মহাসাগরীয়",
        "আরক্টিক মহাসাগর": "উত্তর মহাসাগরীয়", "গালাপাগোস": "গালাপাগোস",
        "হাওয়াই": "হাওয়াইয়ান", "কারিবীয় সমুদ্র": "কারিবীয়",
        "ভূমধ্যসাগর": "ভূমধ্যসাগরীয়", "লাল সমুদ্র": "লাল সমুদ্রের",
    }
    return table.get(region_bn, region_bn + "ের")


_DEMONYM = {
    "Belize": "Belizean", "Denmark": "Danish", "France": "French", "Netherlands": "Dutch",
    "Switzerland": "Swiss", "Spain": "Spanish", "Portugal": "Portuguese",
    "Thailand": "Thai", "Laos": "Lao", "Finland": "Finnish", "Poland": "Polish",
    "Ireland": "Irish", "Scotland": "Scottish", "England": "English", "Sweden": "Swedish",
    "Norway": "Norwegian", "Iceland": "Icelandic", "Greece": "Greek", "Turkey": "Turkish",
    "China": "Chinese", "Japan": "Japanese", "Israel": "Israeli", "Kazakhstan": "Kazakh",
    "Afghanistan": "Afghan", "Pakistan": "Pakistani", "Uzbekistan": "Uzbek",
    "Tajikistan": "Tajik", "Kyrgyzstan": "Kyrgyz", "Turkmenistan": "Turkmen",
    "Iran": "Iranian", "Iraq": "Iraqi", "Oman": "Omani", "Yemen": "Yemeni",
    "Qatar": "Qatari", "Bahrain": "Bahraini", "Kuwait": "Kuwaiti", "Jordan": "Jordanian",
    "Lebanon": "Lebanese", "Syria": "Syrian", "Egypt": "Egyptian", "Libya": "Libyan",
    "Tunisia": "Tunisian", "Algeria": "Algerian", "Morocco": "Moroccan",
    "Bangladesh": "Bangladeshi", "Nepal": "Nepalese", "Bhutan": "Bhutanese",
    "Myanmar": "Burmese", "Cambodia": "Cambodian", "Vietnam": "Vietnamese",
    "Malaysia": "Malaysian", "Indonesia": "Indonesian", "Philippines": "Philippine",
    "Singapore": "Singaporean", "Brunei": "Bruneian", "Mongolia": "Mongolian",
    "Korea": "Korean", "Sri Lanka": "Sri Lankan", "Maldives": "Maldivian",
    "Saudi Arabia": "Saudi", "United Arab Emirates": "Emirati", "Russia": "Russian",
    "Ukraine": "Ukrainian", "Belarus": "Belarusian", "Moldova": "Moldovan",
    "Georgia": "Georgian", "Armenia": "Armenian", "Azerbaijan": "Azerbaijani",
    "India": "Indian", "Timor-Leste": "Timorese", "Papua New Guinea": "Papuan",
    "New Zealand": "New Zealand", "Côte d'Ivoire": "Ivorian", "Cote d'Ivoire": "Ivorian",
    "Cape Verde": "Cape Verdean", "Czechia": "Czech", "Austria": "Austrian",
    "Hungary": "Hungarian", "Italy": "Italian", "Germany": "German", "Belgium": "Belgian",
    "Luxembourg": "Luxembourgish", "Croatia": "Croatian", "Serbia": "Serbian",
    "Bosnia": "Bosnian", "Albania": "Albanian", "Bulgaria": "Bulgarian",
    "Romania": "Romanian", "Slovakia": "Slovak", "Slovenia": "Slovenian",
    "Estonia": "Estonian", "Latvia": "Latvian", "Lithuania": "Lithuanian",
    "Brazil": "Brazilian", "Chile": "Chilean", "Peru": "Peruvian", "Bolivia": "Bolivian",
    "Colombia": "Colombian", "Venezuela": "Venezuelan", "Ecuador": "Ecuadorian",
    "Guyana": "Guyanese", "Suriname": "Surinamese", "Paraguay": "Paraguayan",
    "Uruguay": "Uruguayan", "Argentina": "Argentine", "Mexico": "Mexican",
    "Canada": "Canadian", "Cuba": "Cuban", "Jamaica": "Jamaican", "Haiti": "Haitian",
    "Kenya": "Kenyan", "Tanzania": "Tanzanian", "Uganda": "Ugandan", "Rwanda": "Rwandan",
    "Ethiopia": "Ethiopian", "Somalia": "Somali", "Sudan": "Sudanese",
    "Nigeria": "Nigerian", "Niger": "Nigerien", "Ghana": "Ghanaian", "Togo": "Togolese",
    "Benin": "Beninese", "Mali": "Malian", "Chad": "Chadian", "Gabon": "Gabonese",
    "Cameroon": "Cameroonian", "Angola": "Angolan", "Zambia": "Zambian",
    "Zimbabwe": "Zimbabwean", "Botswana": "Botswanan", "Namibia": "Namibian",
    "Mozambique": "Mozambican", "Madagascar": "Malagasy", "Mauritius": "Mauritian",
    "Seychelles": "Seychellois", "Australia": "Australian", "Fiji": "Fijian",
    "Samoa": "Samoan", "Tonga": "Tongan", "Vanuatu": "Ni-Vanuatu", "Nauru": "Nauruan",
    "Palau": "Palauan", "Andorra": "Andorran", "Monaco": "Monegasque", "Malta": "Maltese",
    "Cyprus": "Cypriot", "Nepal": "Nepalese", "Bhutan": "Bhutanese",
    "Mauritania": "Mauritanian", "Senegal": "Senegalese", "Gambia": "Gambian",
    "Guinea": "Guinean", "Liberia": "Liberian", "Sierra Leone": "Sierra Leonean",
    "Burkina Faso": "Burkinabe", "Eritrea": "Eritrean", "Djibouti": "Djiboutian",
    "Burundi": "Burundian", "Malawi": "Malawian", "Lesotho": "Basotho",
    "Eswatini": "Swazi", "South Africa": "South African",
    "Democratic Republic of the Congo": "Congolese", "Republic of the Congo": "Congolese",
    "Central African Republic": "Central African", "United States": "American",
    "United Kingdom": "British", "South Korea": "South Korean",
    "North Korea": "North Korean", "Solomon Islands": "Solomon Islander",
    "Costa Rica": "Costa Rican", "Panama": "Panamanian", "Nicaragua": "Nicaraguan",
    "Honduras": "Honduran", "Guatemala": "Guatemalan", "El Salvador": "Salvadoran",
    "Dominican Republic": "Dominican", "Puerto Rico": "Puerto Rican",
    "Trinidad and Tobago": "Trinidadian", "Antigua and Barbuda": "Antiguan",
    "Saint Lucia": "Saint Lucian", "Grenada": "Grenadian", "Barbados": "Barbadian",
    "Bahamas": "Bahamian", "Belize": "Belizean", "Micronesia": "Micronesian",
    "Kiribati": "I-Kiribati", "Marshall Islands": "Marshallese", "Tuvalu": "Tuvaluan",
    "Comoros": "Comorian", "South Sudan": "South Sudanese", "Kosovo": "Kosovar",
    "Montenegro": "Montenegrin", "North Macedonia": "Macedonian",
    "San Marino": "Sammarinese", "Liechtenstein": "Liechtensteiner",
    "Vatican City": "Vatican", "Palestine": "Palestinian", "Western Sahara": "Sahrawi",
    "Somaliland": "Somaliland", "Taiwan": "Taiwanese", "Tibet": "Tibetan",
    "Sri Lanka": "Sri Lankan", "Greenland": "Greenlandic", "Faroe Islands": "Faroese",
    "New Caledonia": "New Caledonian", "French Guiana": "Guianan", "Guiana": "Guianan",
    "Aruba": "Aruban", "Curaçao": "Curaçaoan", "Bermuda": "Bermudian",
    "Cayman Islands": "Caymanian", "Turks and Caicos": "Turks and Caicos",
    "British Virgin Islands": "British Virgin Islander", "US Virgin Islands": "Virgin Islander",
    "Guam": "Guamanian", "Northern Mariana Islands": "Mariana",
    "American Samoa": "American Samoan", "Cook Islands": "Cook Islander",
    "Niue": "Niuean", "Tokelau": "Tokelauan", "Wallis and Futuna": "Wallisian",
    "French Polynesia": "Polynesian", "Pitcairn": "Pitcairn Islander",
    "Saint Helena": "Saint Helenian", "Ascension": "Ascension",
    "Tristan da Cunha": "Tristan", "Anguilla": "Anguillan", "Montserrat": "Montserratian",
    "Saint Kitts and Nevis": "Kittitian", "Saint Vincent": "Vincentian",
    "Dominica": "Dominican", "Martinique": "Martinican", "Guadeloupe": "Guadeloupean",
    "Bonaire": "Bonairean", "Sint Maarten": "Sint Maarten", "Saba": "Saban",
    "Saint Barthelemy": "Barthelemois", "Saint Pierre": "Saint-Pierrais",
    "Mayotte": "Mahoran", "Reunion": "Reunionese", "Mauritania": "Mauritanian",
    "Antarctica": "Antarctic", "Arctic": "Arctic", "Arctic Ocean": "Arctic",
    "Atlantic Ocean": "Atlantic", "Pacific Ocean": "Pacific",
    "Indian Ocean": "Indian Ocean", "Southern Ocean": "Southern Ocean",
    "Caribbean Sea": "Caribbean", "Mediterranean Sea": "Mediterranean",
    "Red Sea": "Red Sea", "Bosnia and Herzegovina": "Bosnian",
    "DR Congo": "Congolese", "Galápagos": "Galapagos", "Hawaii": "Hawaiian",
    "Türkiye": "Turkish",
}


def _place_en(region_en):
    """অঞ্চলের ইংরেজি নাম → বিশেষণ (demonym)।"""
    if region_en in _DEMONYM:
        return _DEMONYM[region_en]
    words = region_en.split()
    last = words[-1]
    if last in _DEMONYM:
        return _DEMONYM[last]
    if last.endswith("ia"):
        return last[:-1] + "n"
    if last.endswith(("istan", "stan")):
        return last
    return last + "n"


def _latinize(word):
    """সাধারণ ইংরেজি/বাংলা শব্দকে লাতিনীয় গণ-নামের মতো গঠন দেয়।"""
    w = "".join(ch for ch in word if ch.isalpha())
    if not w:
        return "Species"
    w = w[0].upper() + w[1:].lower()
    return w


# গ্রুপভিত্তিক মাপের সীমা: a=দেহদৈর্ঘ্য, c=দ্বিতীয় মাপ, d=ওজন
# (a_lo, a_hi, c_lo, c_hi, d_lo, d_hi)
SIZE_RANGE = {
    "mammals":    (10, 600, 5, 90, 1, 3000),
    "birds":      (8, 150, 5, 30, 3, 9000),
    "reptiles":   (8, 600, 3, 40, 5, 800),
    "amphibians": (12, 150, 2, 20, 1, 300),
    "fishes":     (3, 300, 2, 40, 1, 900),
    "inverts":    (2, 180, 1, 30, 1, 60),
    "fungi":      (1, 35, 1, 25, 1, 12),
    "plants":     (1, 40, 2, 60, 1, 300),
    "dinosaurs":  (1, 35, 1, 18, 1, 70),
    "microbes":   (1, 10, 1, 30, 1, 150),
    "viruses":    (20, 350, 10, 200, 1, 900),
    "marine":     (2, 300, 1, 40, 1, 700),
}

# d (ওজন) স্কেল: কেজি না গ্রাম
WEIGHT_AS_GRAM = {"birds", "amphibians", "inverts"}


def make_size(seed, group_id):
    lo_a, hi_a, lo_c, hi_c, lo_d, hi_d = SIZE_RANGE.get(group_id, SIZE_RANGE["inverts"])
    tmpl = pick(seed, 91, nb.SIZES.get(group_id, nb.SIZES["inverts"]))
    a = lognum(seed, 92, lo_a, hi_a)
    b = max(a + 1, lognum(seed, 93, a, hi_a))
    c = lognum(seed, 94, lo_c, hi_c)
    d = max(c + 1, lognum(seed, 95, c, hi_d))
    return fill_bn(tmpl, seed, 96, a=a, b=b, c=c, d=d)


REPRO_RANGE = {
    "mammals":    (30, 700, 1, 8),
    "birds":      (3, 10, 1, 8),
    "reptiles":   (2, 40, 30, 90),
    "amphibians": (200, 4000, 1, 1),
    "fishes":     (2, 30, 1, 20),
    "inverts":    (4, 60, 1, 40),
    "fungi":      (1, 40, 1, 1),
    "plants":     (2, 12, 1, 1),
    "dinosaurs":  (30, 120, 1, 25),
    "microbes":   (20, 180, 1, 1),
    "viruses":    (4, 72, 1, 1),
    "marine":     (3, 90, 1, 30),
}


def make_repro(seed, group_id):
    lo_n, hi_n, lo_k, hi_k = REPRO_RANGE.get(group_id, (2, 60, 1, 10))
    tmpl = pick(seed, 101, nb.REPRO.get(group_id, nb.REPRO["inverts"]))
    n = lognum(seed, 102, lo_n, hi_n)
    k = randint(seed, 103, lo_k, hi_k)
    return fill_bn(tmpl, seed, 104, n=n, k=k)


def make_habitat(seed, group_id):
    pool = nb.HABITATS.get(group_id, nb.HABITATS["inverts"])
    h = pick(seed, 111, pool)
    if group_id in MICROBE_GROUPS:
        # অণুজীব/ভাইরাস — বাসস্থান ঠিকই আছে
        return h
    bad = 0
    while bad_word_in(h, group_id) and bad < 6:
        bad += 1
        h = pick(seed, 111 + bad, pool)
    return h


def bad_word_in(h, group_id):
    if group_id in ("marine", "fishes"):
        return False
    if group_id in AQUATIC_PLANT_GROUPS:
        # উদ্ভিদ — গভীর সমুদ্র/হাইড্রোথার্মাল বাদ
        for w in ("গভীর সমুদ্র", "হাইড্রোথার্মাল", "প্রবাল প্রাচীর", "মেরু হিমশীতল"):
            if w in h:
                return True
        return False
    for w in BAD_HAB_WORDS:
        if w in h:
            return True
    return False


# ─────────────────────────────────────────────────────────────
# প্রধান জেনারেটর
# ─────────────────────────────────────────────────────────────
def iter_species(total=TOTAL):
    """প্রজাতির dict ধারায় ধারায় জেনারেট করে (id 1..total)।

    পরিবারের ওপর সমানভাবে ভাগ করা হয়: প্রথম পরিবার আগে, তারপর পরেরটি —
    ফলে প্রতি পরিবারে প্রায় total/families টি প্রজাতি পড়ে।
    """
    fams = list(iter_taxonomy())
    nf = len(fams)
    per = max(1, total // nf)
    rem = total - per * nf

    idx = 0          # প্রজাতির ক্রমিক (row_seq)
    # পরিবারগুলোকে জনপ্রিয়তা অনুযায়ী আগে রাখি (স্তন্যপায়ী/পাখি আগে)
    group_order = {g[0]: i for i, g in enumerate(GROUPS)}
    fams.sort(key=lambda r: (group_order.get(r[0], 99), r[5], r[7], r[9]))

    used_bn = {}
    used_en = {}
    used_sci = {}

    for fi, fam in enumerate(fams):
        (gid, gbn, gen, emoji, color, cls_sci, cls_bn, ord_sci, ord_bn, fam_sci, fam_bn) = fam
        count = per + (1 if fi < rem else 0)
        noun_bn = family_noun(fam_bn)
        habitats = nb.HABITATS.get(gid, nb.HABITATS["inverts"])

        for j in range(count):
            idx += 1
            sid = idx
            seed = (fi * 1000003 + j * 7919 + 104729) & _M1

            # অঞ্চল (ভারযুক্ত — এশিয়া আগে)
            reg = wpick(seed, 1, [(r, nb.REGION_WEIGHT.get(r[2], 8)) for r in nb.REGIONS])
            region_bn, region_en = reg[0], reg[1]
            place_bn = _place_bn(region_bn)
            place_en = _place_en(region_en)

            habitat_bn = make_habitat(seed, gid)

            # বাংলা নাম
            form = wpick(seed, 2, [(0, 45), (1, 15), (2, 25), (3, 15)])
            mod = pick(seed, 3, nb.COLORS_BN + nb.PATTERNS_BN + nb.SIZE_BN)
            hab_pref = pick(seed, 4, nb.HABITAT_PREFIX_BN)
            if form == 0:
                bn = mod + " " + noun_bn
            elif form == 1:
                mod2 = pick(seed, 5, nb.COLORS_BN + nb.SIZE_BN)
                bn = mod + "-" + mod2 + " " + noun_bn
            elif form == 2:
                bn = hab_pref + " " + noun_bn
            else:
                bn = place_bn + " " + mod + " " + noun_bn
            key = (gid, bn)
            n = used_bn.get(key, 0)
            if n:
                bn = bn + " " + _bn_ordinal(n + 1)
            used_bn[key] = n + 1
            bn = bn if not bn.endswith(" ") else bn.strip()

            # ইংরেজি নাম
            eform = wpick(seed, 6, [(t, w) for t, w in nb.EN_NAME_FORMS])
            adj = pick(seed, 7, nb.EN_ADJ)
            adj2 = pick(seed, 8, nb.EN_ADJ)
            en_noun = pick(seed, 9, nb.group_en_nouns(gid, fam_bn))
            person = pick(seed, 10, ["Anderson", "Blyth", "Hodgson", "Jerdon", "Gould",
                                     "Swinhoe", "Tikader", "Hora", "Boulenger", "Smith",
                                     "Gray", "Moore", "Peters", "Meyrick", "Hampson"])
            e = eform
            en = (e.replace("{adj}", adj).replace("{adj2}", adj2 if adj2 != adj else pick(seed, 11, nb.EN_ADJ))
                   .replace("{noun}", en_noun).replace("{place}", place_en)
                   .replace("{person}", person))
            en = " ".join(w.capitalize() if w.islower() else w for w in en.split())
            if en.startswith("Of"):
                en = en[3:]
            ekey = (gid, en.lower())
            n = used_en.get(ekey, 0)
            if n:
                en = en + " " + _roman(n + 2)
            used_en[ekey] = n + 1

            # বৈজ্ঞানিক নাম
            genus = pick(seed, 12, nb.GENERA) + pick(seed, 13, nb.GENERA_SUFFIX).replace("2", "")
            epithet = pick(seed, 14, nb.SPECIFIC_EPITHETS)
            sci = genus + " " + epithet
            skey = sci.lower()
            n = used_sci.get(skey, 0)
            if n:
                sci = genus + "-" + str(n + 2) + " " + epithet
            used_sci[skey] = n + 1

            authority = pick(seed, 15, nb.AUTHOR_SUFFIX)

            # খাদ্য
            diet_bn = pick(seed, 16, nb.DIETS.get(gid, nb.DIETS["inverts"]))

            # IUCN / বিলুপ্তি
            iucn = wpick(seed, 17, [(k, w) for k, w in nb.IUCN_WEIGHT.items()])
            extinct_bn = ""
            extinct = 0
            if iucn in ("EX", "EW"):
                extinct = 1
                year = pick(seed, 18, nb.EXTINCT_YEARS)
                where = "বন্য পরিবেশে" if iucn == "EW" else "সম্পূর্ণভাবে"
                extinct_bn = "%s বিলুপ্ত (%s সাল); শেষ নিশ্চিত দেখা %s" % (
                    where, bn_num(year), region_bn)
            elif iucn == "CR":
                if rnd(seed, 19) < 0.35:
                    extinct_bn = "সম্ভবত বিলুপ্ত; গত ৩০ বছরে কোনো নিশ্চিত দেখা নেই"

            # বিষাক্ততা
            venom_level = 0
            if gid in ("reptiles",):
                venom_level = wpick(seed, 20, [(0, 30), (1, 18), (2, 22), (3, 22), (4, 8)])
            elif gid in ("inverts", "marine"):
                venom_level = wpick(seed, 20, [(0, 55), (1, 20), (2, 15), (3, 8), (4, 2)])
            elif gid in ("amphibians", "fungi", "plants"):
                venom_level = wpick(seed, 20, [(0, 70), (1, 15), (2, 10), (3, 4), (4, 1)])
            elif gid in ("microbes", "viruses"):
                venom_level = wpick(seed, 20, [(0, 10), (1, 20), (2, 35), (3, 25), (4, 10)])
            else:
                venom_level = wpick(seed, 20, [(0, 88), (1, 8), (2, 3), (3, 1), (4, 0)])
            venom_bn = nb.VENOM_LEVELS[min(venom_level, 4)][1]

            size_bn = make_size(seed, gid)
            repro_bn = make_repro(seed, gid)
            fact_bn = pick(seed, 21, nb.FACTS.get(gid, nb.FACTS["inverts"]))
            if "{n}" in fact_bn:
                fact_bn = fact_bn.replace("{n}", bn_num(randint(seed, 22, 2, 200)))

            popularity = int(rnd(seed, 23) * 100000) % 100000
            # পরিচিত প্রজাতি (বড় বিভাগ) একটু বেশি জনপ্রিয়
            if gid in ("mammals", "birds"):
                popularity = min(99999, popularity + 20000)
            if iucn in ("EN", "CR", "EX", "EW"):
                popularity = min(99999, popularity + 9000)

            yield {
                "id": sid,
                "bn_name": bn,
                "en_name": en,
                "sci_name": sci,
                "authority": authority,
                "kingdom": _kingdom(gid),
                "phylum": _phylum(cls_sci, gid),
                "class_id": cls_sci,
                "class_bn": cls_bn,
                "order_id": ord_sci,
                "order_bn": ord_bn,
                "family_id": fam_sci,
                "family_bn": fam_bn,
                "family_sci": fam_sci,
                "group_id": gid,
                "group_bn": gbn,
                "group_en": gen,
                "emoji": emoji,
                "color": color,
                "region_bn": region_bn,
                "region_en": region_en,
                "habitat_bn": habitat_bn,
                "diet_bn": diet_bn,
                "iucn": iucn,
                "size_bn": size_bn,
                "venom_level": venom_level,
                "venom_bn": venom_bn,
                "repro_bn": repro_bn,
                "fact_bn": fact_bn,
                "extinct_bn": extinct_bn,
                "extinct": extinct,
                "is_demo": 1 if sid <= 300 else 0,
                "popularity": popularity,
                "row_seq": sid,
            }


BN_DIGITS = "০১২৩৪৫৬৭৮৯"
BN_WORDS = ["", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম", "ষষ্ঠ", "সপ্তম", "অষ্টম", "নবম",
            "দশম", "একাদশ", "দ্বাদশ", "ত্রয়োদশ", "চতুর্দশ", "পঞ্চদশ", "ষোড়শ", "সপ্তদশ",
            "অষ্টাদশ", "উনবিংশ", "বিংশ", "একবিংশ", "দ্বাবিংশ", "ত্রয়োবিংশ"]


def _bn_ordinal(n):
    if n < len(BN_WORDS):
        return BN_WORDS[n]
    return "".join(BN_DIGITS[int(d)] for d in str(n))


_ROMAN = ["", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
          "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"]


def _roman(n):
    if n < len(_ROMAN):
        return _ROMAN[n]
    return "No." + str(n)


def _kingdom(gid):
    return {
        "mammals": "প্রাণীজগৎ (Animalia)", "birds": "প্রাণীজগৎ (Animalia)",
        "reptiles": "প্রাণীজগৎ (Animalia)", "amphibians": "প্রাণীজগৎ (Animalia)",
        "fishes": "প্রাণীজগৎ (Animalia)", "inverts": "প্রাণীজগৎ (Animalia)",
        "marine": "প্রাণীজগৎ (Animalia)", "dinosaurs": "প্রাণীজগৎ (Animalia)",
        "fungi": "ছত্রাকজগৎ (Fungi)", "plants": "উদ্ভিদজগৎ (Plantae)",
        "microbes": "অণুজীব (Microbia/Archaea)", "viruses": "ভাইরাস (Viruses)",
    }.get(gid, "প্রাণীজগৎ (Animalia)")


def _phylum(cls_sci, gid):
    m = {
        "Mammalia": "কর্ডাটা (Chordata)", "Aves": "কর্ডাটা (Chordata)",
        "Reptilia": "কর্ডাটা (Chordata)", "Amphibia": "কর্ডাটা (Chordata)",
        "Actinopterygii": "কর্ডাটা (Chordata)", "Chondrichthyes": "কর্ডাটা (Chordata)",
        "Sauropsida": "কর্ডাটা (Chordata)", "Synapsida": "কর্ডাটা (Chordata)",
        "Amphibia_pre": "কর্ডাটা (Chordata)",
        "Insecta": "সন্ধিপদী (Arthropoda)", "Arachnida": "সন্ধিপদী (Arthropoda)",
        "Crustacea": "সন্ধিপদী (Arthropoda)", "Arthropoda_marine": "সন্ধিপদী (Arthropoda)",
        "Mollusca": "মলুস্কা (Mollusca)", "Annelida": "অ্যানেলিডা (Annelida)",
        "Cnidaria": "নিডারিয়া (Cnidaria)", "Echinodermata": "একাইনোডার্মাটা",
        "Porifera": "পরিফেরা (Porifera)", "Nematoda": "নিমাটোডা (Nematoda)",
        "Platyhelminthes": "প্লাটিহেলমিন্থেস", "Rotifera": "রোটিফেরা",
        "Bryozoa": "ব্রায়োজোয়া", "Tardigrada": "টার্ডিগ্রেডা",
        "Ctenophora_ord": "ক্টেনোফোরা", "Phoronida_ord": "ফোরোনিডা",
        "Sipuncula_ord": "সিপাঙ্কুলা", "Brachiopoda_ord": "ব্রাকিওপডা",
        "Nemertea_ord": "নিমার্টিয়া", "Acoelomorpha_ord": "অ্যাসিলোমর্ফা",
        "Chordata_marine": "কর্ডাটা (Chordata)",
        "Ascidiacea_ord": "কর্ডাটা (Chordata)", "Thaliacea_ord": "কর্ডাটা (Chordata)",
        "Branchiostoma_ord": "কর্ডাটা (Chordata)", "Hemichordata_ord": "হেমিকর্ডাটা",
        "Chaetognatha_ord": "ক্যাটোগ্নাথা",
        "Magnoliopsida": "ট্রাকিওফাইটা (Tracheophyta)", "Liliopsida": "ট্রাকিওফাইটা",
        "Pinopsida": "ট্রাকিওফাইটা", "Polypodiopsida": "ট্রাকিওফাইটা",
        "Bryopsida": "ব্রায়োফাইটা (Bryophyta)", "Anthocerotopsida": "ব্রায়োফাইটা",
        "Cycadopsida": "ট্রাকিওফাইটা", "Ginkgoopsida": "ট্রাকিওফাইটা",
        "Agaricomycetes": "ব্যাসিডিওমাইকোটা", "Ascomycetes": "অ্যাসকোমাইকোটা",
        "Zygomycetes": "জাইগোমাইকোটা", "Oomycetes": "ওমাইকোটা",
        "Myxomycetes": "অ্যামোবোজোয়া", "Lichenes": "ব্যাসিডিওমাইকোটা (সহজীবী)",
    }
    if cls_sci in m:
        return m[cls_sci]
    if gid == "microbes":
        return "ব্যাকটেরিয়া/আর্কিয়া"
    if gid == "viruses":
        return "ভাইরাস (অকোষীয়)"
    if gid in ("marine",):
        return "সামুদ্রিক প্রাণীজগৎ"
    if cls_sci.endswith("phyceae_ord") or cls_sci.endswith("phyta_ord"):
        return "শৈবাল (Algae)"
    return "—"


if __name__ == "__main__":
    import sys, json, time
    t0 = time.time()
    n = 0
    sample = []
    for sp in iter_species():
        n += 1
        if n <= 3 or n % 60000 == 0:
            sample.append(sp)
        if n >= TOTAL:
            break
    print("generated:", n, "time: %.1fs" % (time.time() - t0))
    for sp in sample[:4]:
        print(json.dumps({k: sp[k] for k in
                          ("id", "bn_name", "en_name", "sci_name", "authority", "group_id",
                           "class_bn", "order_bn", "family_bn", "region_bn", "habitat_bn",
                           "diet_bn", "iucn", "size_bn", "venom_bn", "repro_bn", "fact_bn",
                           "extinct_bn", "popularity")}, ensure_ascii=False, indent=1))
