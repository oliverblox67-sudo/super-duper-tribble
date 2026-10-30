#!/usr/bin/env bash
# Crea i due archivi di ogni versione in releases/v<versione>/:
#   1) TheGods-<v>-CurseForge.zip  -> da importare nell'app CurseForge (Crea profilo personalizzato > Importa)
#   2) TheGods-<v>-Segnaposto.zip  -> codice sorgente completo + SEGNAPOSTO.md, per riprendere il lavoro in un'altra chat
# Copia anche il .jar singolo (per chi vuole metterlo a mano nella cartella mods).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

prop() { grep "^$1=" gradle.properties | cut -d= -f2-; }
VERSION="$(prop mod_version)"
MC="$(prop minecraft_version)"
NEO="$(prop neo_version)"
NAME="TheGods-$VERSION"
OUT="releases/v$VERSION"

echo ">> Build $NAME (Minecraft $MC, NeoForge $NEO)"
./gradlew build --console=plain -q
JAR="build/libs/thegods-$VERSION.jar"
[ -f "$JAR" ] || { echo "jar non trovato: $JAR"; exit 1; }

mkdir -p "$OUT"
rm -f "$OUT"/*.zip "$OUT"/*.jar
cp "$JAR" "$OUT/thegods-$VERSION.jar"

STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

# ---------- 1) Profilo CurseForge
mkdir -p "$STAGE/cf/overrides/mods"
cp "$JAR" "$STAGE/cf/overrides/mods/"
cat > "$STAGE/cf/manifest.json" <<JSON
{
  "minecraft": {
    "version": "$MC",
    "modLoaders": [
      { "id": "neoforge-$NEO", "primary": true }
    ]
  },
  "manifestType": "minecraftModpack",
  "manifestVersion": 1,
  "name": "The Gods $VERSION",
  "version": "$VERSION",
  "author": "oliverblox67-sudo",
  "files": [],
  "overrides": "overrides"
}
JSON
cat > "$STAGE/cf/modlist.html" <<HTML
<ul>
<li>The Gods $VERSION (inclusa nel profilo)</li>
</ul>
HTML
(cd "$STAGE/cf" && zip -qr "$ROOT/$OUT/$NAME-CurseForge.zip" .)

# ---------- 2) Segnaposto (codice + stato del progetto)
git ls-files --cached --others --exclude-standard | grep -v '^releases/' > "$STAGE/files.txt"
zip -q "$OUT/$NAME-Segnaposto.zip" -@ < "$STAGE/files.txt"

echo ">> Fatto:"
ls -la "$OUT"
