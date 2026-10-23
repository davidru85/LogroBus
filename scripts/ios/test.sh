#!/usr/bin/env bash
# Usage: scripts/ios/test.sh
#
# Generates the iOS project (scripts/ios/generate-project.sh) and runs every iOS test on an
# iPhone simulator: the built-bundle checks (iosAppTests) and the launch UI tests
# (iosAppUITests). The "iOS build and tests" CI job runs exactly this. IOS_DESTINATION
# overrides the simulator; the default exists in Xcode 26 or later and on the CI image.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"

scripts/ios/generate-project.sh

destination="${IOS_DESTINATION:-platform=iOS Simulator,name=iPhone 17e,OS=26.5}"
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "$destination" \
  -derivedDataPath build/ios-derived-data -quiet test
echo "OK:   iOS tests passed on $destination"
