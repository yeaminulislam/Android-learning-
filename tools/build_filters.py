#!/usr/bin/env python3
"""
Shield Browser — offline filter-list packer.

Downloads the latest public filter lists and produces the compact bundles that
ship inside app/src/main/assets/filters/. The Android engine (AdBlockEngine)
understands this format directly (hosts lines + Adblock Plus filter syntax).

Usage:
    python3 tools/build_filters.py [workdir]

If the lists already exist in <workdir> they are reused (offline mode).
"""

import os
import shutil
import sys
import tarfile
import tempfile
import urllib.request
from datetime import datetime, timezone

WORKDIR = sys.argv[1] if len(sys.argv) > 1 else os.path.join(tempfile.gettempdir(), "filterlists")
OUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "filters",
)

STEVENBLACK_TGZ = "https://codeload.github.com/StevenBlack/hosts/tar.gz/refs/heads/master"
EASYLIST_TGZ = "https://codeload.github.com/easylist/easylist/tar.gz/refs/heads/master"

# files we pull out of the easylist repo tarball
EASYLIST_PARTS = {
    "easylist.txt": [
        "easylist-master/easylist/easylist_adservers.txt",
        "easylist-master/easylist/easylist_general_block.txt",
        "easylist-master/easylist/easylist_thirdparty.txt",
        "easylist-master/easylist/easylist_allowlist.txt",
        "easylist-master/easylist/easylist_general_hide.txt",
        "easylist-master/easylist/easylist_specific_hide.txt",
        "easylist-master/easylist/easylist_allowlist_general_hide.txt",
    ],
    "easyprivacy.txt": [
        "easylist-master/easyprivacy/easyprivacy_general.txt",
        "easylist-master/easyprivacy/easyprivacy_thirdparty.txt",
        "easylist-master/easyprivacy/easyprivacy_specific.txt",
        "easylist-master/easyprivacy/easyprivacy_allowlist.txt",
    ],
    "annoyances.txt": [
        "easylist-master/fanboy-addon/fanboy_annoyance_general_hide.txt",
        "easylist-master/easylist_cookie/easylist_cookie_general_hide.txt",
    ],
}

# options our engine does not implement -> drop those rules entirely
UNSUPPORTED = (
    "$popup", ",popup", "$badfilter", ",badfilter", "$csp", ",csp",
    "$redirect", ",redirect", "$rewrite", ",rewrite", "$removeparam",
    ",removeparam", "$replace", ",replace", "$generichide", ",generichide",
    "$genericblock", ",genericblock", "$elemhide", ",elemhide", "$jsinject",
    ",jsinject", "$sitekey", ",sitekey", "$donottrack", ",donottrack",
    "$webrtc", ",webrtc", "$prefetch", ",prefetch", "$header", ",header",
    "$method", ",method", "$match-case", ",match-case", "$denyallow",
    ",denyallow", "$cookie", ",cookie",
)

BAD_SELECTOR_PARTS = (
    ":-abp-", "-abp-", ":xpath", ":style(", ":matches-css", ":remove",
    ":if(", ":if-not(", ":has-text(", ":contains(", ":upward", "+js(",
    "script:contains", ":properties(", ":watch-attr", ":min-text-length",
    ":nth-ancestor", ":has(",
)


def download(url: str, dest: str) -> None:
    if os.path.exists(dest):
        print(f"  reuse {os.path.basename(dest)}")
        return
    print(f"  download {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "shield-browser-build/1.0"})
    with urllib.request.urlopen(req, timeout=300) as resp, open(dest, "wb") as fh:
        shutil.copyfileobj(resp, fh)


def norm_line(raw: str) -> str:
    return raw.strip()


def pass_network_rule(line: str) -> bool:
    """Cheap pre-filter for ABP network rules (engine revalidates)."""
    low = line.lower()
    for bad in UNSUPPORTED:
        if bad in low:
            return False
    return True


def pass_cosmetic(line: str) -> bool:
    if ":-abp" in line or "+js(" in line:
        return False
    marker = "##" if "##" in line else "#@#"
    sel = line.split(marker, 1)[1]
    for bad in BAD_SELECTOR_PARTS:
        if bad in sel:
            return False
    if "{" in sel or "}" in sel or ";" in sel:
        return False
    return True


def process_list(text: str, out, stats):
    for raw in text.splitlines():
        line = norm_line(raw)
        if not line or line[0] == "!" or line[0] == "[":
            continue
        if line.startswith("##") or line.startswith("#@#") or "##" in line or "#@#" in line:
            if pass_cosmetic(line):
                out.write(line + "\n")
                stats["cosmetic"] += 1
            else:
                stats["dropped"] += 1
        else:
            body = line[2:] if line.startswith("@@") else line
            if pass_network_rule(body):
                out.write(line + "\n")
                stats["network"] += 1
            else:
                stats["dropped"] += 1


def main() -> None:
    os.makedirs(WORKDIR, exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)

    # ---- 1. StevenBlack hosts ------------------------------------------------
    hosts_file = os.path.join(WORKDIR, "hosts-master", "hosts")
    if not os.path.exists(hosts_file):
        tgz = os.path.join(WORKDIR, "stevenblack.tgz")
        download(STEVENBLACK_TGZ, tgz)
        with tarfile.open(tgz, "r:gz") as tar:
            for m in tar.getmembers():
                if m.name == "hosts-master/hosts":
                    tar.extract(m, WORKDIR)
    hosts_out = os.path.join(OUT_DIR, "hosts.txt")
    host_count = 0
    seen_hosts = set()
    with open(hosts_file, encoding="utf-8", errors="replace") as fh, \
            open(hosts_out, "w", encoding="utf-8") as out:
        for raw in fh:
            line = raw.strip()
            if not line.startswith(("0.0.0.0 ", "127.0.0.1 ")):
                continue
            host = line[8:].split()[0].lower()
            if host in seen_hosts:
                continue
            seen_hosts.add(host)
            out.write("0.0.0.0 " + host + "\n")
            host_count += 1
    print(f"hosts.txt: {host_count} domains")

    # ---- 2. easylist repo parts ----------------------------------------------
    el_tgz = os.path.join(WORKDIR, "easylist.tgz")
    download(EASYLIST_TGZ, el_tgz)
    wanted = {p for parts in EASYLIST_PARTS.values() for p in parts}
    with tarfile.open(el_tgz, "r:gz") as tar:
        for m in tar.getmembers():
            if m.name in wanted:
                tar.extract(m, WORKDIR)

    for out_name, parts in EASYLIST_PARTS.items():
        out_path = os.path.join(OUT_DIR, out_name)
        stats = {"network": 0, "cosmetic": 0, "dropped": 0}
        with open(out_path, "w", encoding="utf-8") as out:
            out.write(f"! Shield Browser bundle - merged from {len(parts)} lists\n")
            for part in parts:
                full = os.path.join(WORKDIR, part)
                if not os.path.exists(full):
                    print(f"  MISSING {part}")
                    continue
                with open(full, encoding="utf-8", errors="replace") as fh:
                    process_list(fh.read(), out, stats)
        print(f"{out_name}: {stats['network']} network, {stats['cosmetic']} cosmetic, "
              f"{stats['dropped']} dropped")

    # ---- 3. hand-written extras ----------------------------------------------
    extras_path = os.path.join(OUT_DIR, "extras.txt")
    with open(extras_path, "w", encoding="utf-8") as out:
        out.write("! Shield Browser extras - hand-curated rules\n")
        out.write(EXTRAS)
    print(f"extras.txt: {EXTRAS.count(chr(10))} lines")

    # ---- 4. version stamp -----------------------------------------------------
    stamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    with open(os.path.join(OUT_DIR, "VERSION.txt"), "w", encoding="utf-8") as out:
        out.write("bundled " + stamp + "\n")
    print("VERSION.txt:", stamp)


EXTRAS = """! --- YouTube ad containers (belt & braces on top of youtube.js) ---
youtube.com##ytd-ad-slot-renderer
youtube.com##ytd-display-ad-renderer
youtube.com##ytd-promoted-video-renderer
youtube.com##ytd-compact-promoted-video-renderer
youtube.com##ytd-banner-promo-renderer
youtube.com##ytd-statement-banner-renderer
youtube.com##ytd-in-feed-ad-layout-renderer
youtube.com##ytd-promoted-sparkles-web-renderer
youtube.com##ytd-promoted-sparkles-text-search-renderer
youtube.com##yt-mealbar-promo-renderer
youtube.com###player-ads
youtube.com###masthead-ad
m.youtube.com##.ad-slot
! --- google search ads ---
google.com##div[data-text-ad]
google.com##.commercial-unit-mobile-top
! --- common leftovers ---
##.adsbygoogle
##ins.adsbygoogle
##div[id^="div-gpt-ad"]
##.ad-banner
||doubleclick.net^
||googlesyndication.com^
||googleadservices.com^
||adservice.google.com^
||pagead2.googlesyndication.com^
||ads.youtube.com^
||analytics.youtube.com^
||scorecardresearch.com^
||amazon-adsystem.com^
||ads.yahoo.com^
||advertising.com^
||outbrain.com^
||taboola.com^
||criteo.com^
||criteo.net^
||pubmatic.com^
||rubiconproject.com^
||openx.net^
||appnexus.com^
||adnxs.com^
||quantserve.com^
||adcolony.com^
||admob.com^
||moatads.com^
||inmobi.com^
||chartbeat.com^
||hotjar.com^
||fullstory.com^
||mouseflow.com^
"""


if __name__ == "__main__":
    main()
