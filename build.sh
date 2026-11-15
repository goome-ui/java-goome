#!/usr/bin/env bash
# ./build.sh          builds the native lib into build/natives/<platform>/
# ./build.sh test     same, then runs the gradle tests
# ./build.sh win      goome.dll with mingw from linux
set -euo pipefail
cd "$(dirname "$0")"
what="${1:-}"
flags=(-O2 -std=c99 -Wall -Wextra -Wno-unused-parameter -Wno-missing-field-initializers ${WERROR:+-Werror})
os="$(uname -s)"
case "$(uname -m)" in
aarch64 | arm64) arch=arm64 ;;
*) arch=x64 ;;
esac
if [ "$what" = win ]; then
  out=build/natives/windows-x64
  mkdir -p "$out"
  "${CC:-x86_64-w64-mingw32-gcc}" "${flags[@]}" -shared -static-libgcc -Wl,--export-all-symbols -o "$out/goome.dll" native/goome_lib.c
  echo "built $out/goome.dll"
  exit 0
fi
case "$os" in
MINGW* | MSYS* | CYGWIN*)
  out=build/natives/windows-$arch
  mkdir -p "$out"
  "${CC:-gcc}" "${flags[@]}" -shared -static-libgcc -Wl,--export-all-symbols -o "$out/goome.dll" native/goome_lib.c
  lib="$out/goome.dll"
  ;;
Darwin)
  out=build/natives/macos-$arch
  mkdir -p "$out"
  "${CC:-cc}" "${flags[@]}" -fPIC -dynamiclib -mmacosx-version-min=11.0 -o "$out/libgoome.dylib" native/goome_lib.c
  lib="$out/libgoome.dylib"
  ;;
*)
  out=build/natives/linux-$arch
  mkdir -p "$out"
  "${CC:-cc}" "${flags[@]}" -fPIC -shared -o "$out/libgoome.so" native/goome_lib.c
  lib="$out/libgoome.so"
  ;;
esac
echo "built $lib"
if [ "$what" = test ]; then ./gradlew --no-daemon :goome:test :goome:testLwjgl :minecraft:common:test; fi
