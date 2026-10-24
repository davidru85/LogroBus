#!/usr/bin/env bash
# Usage: scripts/ci/install-swiftlint.sh <install-dir>
#
# Installs the pinned SwiftLint release for Linux x86-64 into <install-dir>, after checking
# its SHA-256: third-party tools are pinned (docs/ci_cd.md §6). The SwiftLint CI job uses
# it; macOS developers install SwiftLint with Homebrew (docs/code_style.md §3.1). Bump the
# version and the checksum together.
set -euo pipefail

version="0.65.1"
sha256="caeed6f4a679c35539ffaf124f6c4ab4a8416917f7d8796279dc52b74026059d"
dest="${1:?Usage: $0 <install-dir>}"

archive="$(mktemp -d)/swiftlint.zip"
curl --fail --silent --show-error --location --output "$archive" \
  "https://github.com/realm/SwiftLint/releases/download/$version/swiftlint_linux_amd64.zip"
echo "$sha256  $archive" | sha256sum --check --quiet
mkdir -p "$dest"
unzip -q -o "$archive" -d "$dest"
chmod +x "$dest/swiftlint"
echo "OK:   SwiftLint $("$dest/swiftlint" version) installed in $dest"
