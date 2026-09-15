# Attribution & Licenses

Shield Browser bundles community-maintained filter lists and build tooling. Thanks to
all the maintainers of these projects.

## Filter lists (in `app/src/main/assets/filters/`)

| List | Source | License |
|---|---|---|
| **StevenBlack hosts** | https://github.com/StevenBlack/hosts | [MIT](https://github.com/StevenBlack/hosts/blob/master/license.txt) |
| **EasyList** (adservers, general block/hide, third-party, specific hide, allowlists) | https://github.com/easylist/easylist | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) / [GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) |
| **EasyPrivacy** | https://github.com/easylist/easylist | CC BY-SA 4.0 / GPLv3 |
| **Fanboy's Annoyance general-hide** & **EasyList Cookie general-hide** | https://github.com/easylist/easylist | CC BY-SA 4.0 / GPLv3 |

The bundled files were merged/cleaned by `tools/build_filters.py`. Re-running that
script re-downloads the latest upstream versions. In-app updates fetch the same
canonical sources:

- `https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts`
- `https://easylist-downloads.adblockplus.org/easylist.txt`
- `https://easylist-downloads.adblockplus.org/easyprivacy.txt`
- `https://secure.fanboy.co.nz/fanboy-annoyance.txt`

## Build tooling

| Component | Source | License |
|---|---|---|
| Gradle wrapper (`gradle-wrapper.jar`, `gradlew`, `gradlew.bat`) | Gradle / Square Okio repo snapshot | [Apache 2.0](https://github.com/square/okio/blob/main/LICENSE.txt) |

## App icon

`graphics/*.png`, `res/**/ic_launcher*` — AI-generated for this project; free to use
within it.
