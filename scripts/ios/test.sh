#!/usr/bin/env bash
# Usage: scripts/ios/test.sh
#
# Generates the iOS project (scripts/ios/generate-project.sh) and runs every iOS test on an
# iPhone simulator: the built-bundle checks (iosAppTests) and the launch UI tests
# (iosAppUITests). The "iOS build and tests" CI job runs exactly this.
#
# A simulator's first boot on a fresh CI machine takes minutes. The script therefore starts the
# boot before building the app and tests and waits for it only once they are built, and it
# prints the time of each phase (docs/ci_cd.md §5).
#
# IOS_SIMULATOR and IOS_SIMULATOR_OS choose the simulator. The default exists in Xcode 26 or
# later and on the CI image.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"

simulator="${IOS_SIMULATOR:-iPhone 17e}"
os="${IOS_SIMULATOR_OS:-26.5}"
derived_data=build/ios-derived-data

# The available simulator with that name on that iOS version.
udid="$(xcrun simctl list devices available --json | python3 -c '
import json, sys
name, version = sys.argv[1], sys.argv[2]
runtime = "com.apple.CoreSimulator.SimRuntime.iOS-" + version.replace(".", "-")
devices = json.load(sys.stdin)["devices"].get(runtime, [])
print(next((device["udid"] for device in devices if device["name"] == name), ""))
' "$simulator" "$os")"
if [ -z "$udid" ]; then
  echo "FAIL: no available '$simulator' simulator on iOS $os." >&2
  echo "      Pick one from 'xcrun simctl list devices available' with IOS_SIMULATOR and IOS_SIMULATOR_OS." >&2
  exit 1
fi

phase_start=$SECONDS
scripts/ios/generate-project.sh
echo "TIME: project generated in $((SECONDS - phase_start)) s"

# Boots the simulator, or returns at once if it is already booted, in the background. Started after
# Gradle, because on a 3-core CI runner a boot alongside the Gradle build slowed it by minutes.
boot_log="$(mktemp)"
xcrun simctl bootstatus "$udid" -b > "$boot_log" 2>&1 &
boot_pid=$!

destination="platform=iOS Simulator,id=$udid"
phase_start=$SECONDS
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "$destination" \
  -derivedDataPath "$derived_data" -onlyUsePackageVersionsFromResolvedFile -quiet build-for-testing
echo "TIME: app and tests built in $((SECONDS - phase_start)) s"

phase_start=$SECONDS
if ! wait "$boot_pid"; then
  cat "$boot_log" >&2
  echo "FAIL: the '$simulator' simulator ($udid) did not boot" >&2
  exit 1
fi
echo "TIME: waited $((SECONDS - phase_start)) s more for the simulator to boot"

phase_start=$SECONDS
# A failed snapshot writes its actual image here, which CI uploads (TEST_RUNNER_ variables reach the tests).
rm -rf build/snapshot-artifacts
mkdir -p build/snapshot-artifacts
TEST_RUNNER_SNAPSHOT_ARTIFACTS="$root/build/snapshot-artifacts" \
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "$destination" \
  -derivedDataPath "$derived_data" -quiet test-without-building
echo "TIME: tests ran in $((SECONDS - phase_start)) s"
echo "OK:   iOS tests passed on $simulator, iOS $os ($udid)"
