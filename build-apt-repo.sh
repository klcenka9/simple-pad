#!/usr/bin/env bash
set -euo pipefail

root="$(dirname "$(readlink -f "$0")")"
repository="$root/docs"
package="$root/dist/simple-pad_1.0.3_all.deb"
key="EB1EB57949F6D557AF4C6F1A4DE60C0E4DDABBA5"
distribution="$repository/dists/stable"
binary="$distribution/main/binary-all"
pool="$repository/pool/main/s/simple-pad"

"$root/build-deb.sh"
rm -rf "$repository"
mkdir -p "$binary" "$pool"
install -m 644 "$package" "$pool/simple-pad_1.0.3_all.deb"

size="$(stat --format='%s' "$pool/simple-pad_1.0.3_all.deb")"
sha256="$(sha256sum "$pool/simple-pad_1.0.3_all.deb" | cut -d' ' -f1)"
printf 'Package: simple-pad\nVersion: 1.0.3\nArchitecture: all\nMaintainer: klcenka9\nDepends: openjdk-21-jre\nFilename: pool/main/s/simple-pad/simple-pad_1.0.3_all.deb\nSize: %s\nSHA256: %s\nDescription: Lightweight Batch-aware text editor\n Simple Pad is a lightweight editor with syntax highlighting for Batch files.\n' "$size" "$sha256" > "$binary/Packages"
gzip -9c "$binary/Packages" > "$binary/Packages.gz"

hash() {
    local file="$1"
    "${2}sum" "$file" | cut -d' ' -f1
}

release="$distribution/Release"
packages="main/binary-all/Packages"
packages_gz="main/binary-all/Packages.gz"
{
    printf 'Origin: Simple Pad\nLabel: Simple Pad\nSuite: stable\nCodename: stable\nArchitectures: all\nComponents: main\nDate: %s\n' "$(date -Ru)"
    printf 'MD5Sum:\n %s %s %s\n %s %s %s\n' "$(hash "$binary/Packages" md5)" "$(stat --format='%s' "$binary/Packages")" "$packages" "$(hash "$binary/Packages.gz" md5)" "$(stat --format='%s' "$binary/Packages.gz")" "$packages_gz"
    printf 'SHA256:\n %s %s %s\n %s %s %s\n' "$(hash "$binary/Packages" sha256)" "$(stat --format='%s' "$binary/Packages")" "$packages" "$(hash "$binary/Packages.gz" sha256)" "$(stat --format='%s' "$binary/Packages.gz")" "$packages_gz"
} > "$release"
gpg --batch --yes --local-user "$key" --armor --detach-sign --output "$distribution/Release.gpg" "$release"
gpg --batch --yes --local-user "$key" --clearsign --output "$distribution/InRelease" "$release"
gpg --batch --yes --output "$repository/simple-pad-archive-keyring.gpg" --export "$key"
