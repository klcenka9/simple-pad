#!/usr/bin/env bash
set -euo pipefail

root="$(dirname "$(readlink -f "$0")")"
package="$root/packaging"
output="$root/dist"

rm -rf "$output"
mkdir -p "$output"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
stage="$work/package"
mkdir -p "$stage"
cp -a "$package/." "$stage"
mkdir -p "$stage/usr/share/simple-pad"
javac --release 21 -d "$stage/usr/share/simple-pad" "$root/src/Notepad.java"
chmod 755 "$stage/usr/bin/simple-pad"
printf '2.0\n' > "$work/debian-binary"
tar --owner=0 --group=0 -C "$stage/DEBIAN" -cJf "$work/control.tar.xz" .
tar --owner=0 --group=0 --exclude='./DEBIAN' -C "$stage" -cJf "$work/data.tar.xz" .
ar r "$output/simple-pad_1.0.0_all.deb" "$work/debian-binary" "$work/control.tar.xz" "$work/data.tar.xz"
