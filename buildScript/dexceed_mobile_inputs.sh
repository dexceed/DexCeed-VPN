#!/usr/bin/env bash
# Prepare the pinned native binaries and their Java API for the branded 1.1.x builds.
# Run from a clean disposable checkout with Go 1.27.0, JDK 17 and the Android SDK.
set -euo pipefail
root=$(pwd)
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
work=${1:?Specify a new scratch directory}
mkdir "$work"
work=$(cd "$work" && pwd)
test -f core-dependencies.json
test ! -e app/libs/RawlineCore.aar
test ! -e app/libs/RawlineCore.jar
version=rawline-1.1.1
sha=f9ed39c3a20640873b958d838fea625c7bc1b8a224603c8f733bd278f0807208
ref=$(sed -n 's/^THRONE_AAR_REF=//p' nb4a.properties)
test "$ref" = 4247933586e2b047ed84b73fd6a9c2932ec4b0db
apk="$work/upstream.apk"
curl -fL --retry 3 "https://github.com/ttrroyy/RawlineMobile/releases/download/$version/RawlineMobile-1.1.1-universal.apk" -o "$apk"
echo "$sha  $apk" | sha256sum -c -
# The native libraries come from this APK while the Java bindings are generated from THRONE_AAR_REF: both must be the
# same core build, so the APK's BuildConfig.THRONE_CORE_REF has to name the pinned ref.
# grep -c reads to the end: with pipefail, grep -q would stop early and fail unzip with SIGPIPE.
test "$(unzip -p "$apk" 'classes*.dex' | grep -ac "$ref")" -gt 0 || { echo "upstream $version was not built from core $ref" >&2; exit 1; }
git init -q "$work/core-src"
git -C "$work/core-src" fetch --depth=1 https://github.com/ttrroyy/qThrone.git "$ref"
git -C "$work/core-src" checkout -q --detach FETCH_HEAD
for step in prepare_mobile_core patch_mobile_trusttunnel patch_mobile_socks patch_mobile_xray_socks patch_mobile_obfs patch_mobile_warp; do
    python3 "scripts/$step.py" "$work/core-src/core"
done
GOBIN="$work/bin" go install github.com/sagernet/gomobile/cmd/gobind@v0.1.13
(
    cd "$work/core-src/core"
    tags=$(sed -n 's/^TAGS="\(.*\)"/\1/p' ../script/build_android.sh)
    test -n "$tags"
    GOOS=android GOARCH=arm64 CGO_ENABLED=1 CC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang" \
      "$work/bin/gobind" -lang=java -javapkg=app.rawline.core -libname=rawline -outdir "$work/glue" -tags "$tags" ./mobile
)
mkdir -p "$work/classes" "$root/app/libs"
find "$work/glue/java" -name '*.java' > "$work/java-sources"
javac --release 8 -classpath "$ANDROID_HOME/platforms/android-35/android.jar" -d "$work/classes" @"$work/java-sources"
jar cf app/libs/RawlineCore.jar -C "$work/classes" .
python3 - "$apk" <<'PY'
import sys
from pathlib import Path
from shutil import copyfileobj
from zipfile import ZipFile
with ZipFile(sys.argv[1]) as archive:
    for member in archive.infolist():
        name = member.filename
        if member.is_dir():
            continue
        if name.startswith(("assets/routeprofiles/", "assets/sb-dashboard/", "assets/carrier-licenses/", "assets/schema/")):
            output = Path("app/src/main", name)
        elif name.startswith("lib/") and name.endswith(".so"):
            output = Path("deployment/carriers", name.removeprefix("lib/"))
        else:
            continue
        output.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(member) as source, output.open("wb") as target:
            copyfileobj(source, target)
PY
