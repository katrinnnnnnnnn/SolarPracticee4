#!/bin/sh
# Initial setup on macOS: download official Gradle, verify checksum, generate standard wrapper.
set -eu
cd "$(dirname "$0")"
if [ -z "${JAVA_HOME:-}" ] && [ -d '/Applications/Android Studio.app/Contents/jbr/Contents/Home' ]; then
  export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
fi
if [ -z "${ANDROID_HOME:-}" ]; then export ANDROID_HOME="$HOME/Library/Android/sdk"; fi
if [ ! -d "$ANDROID_HOME" ]; then printf 'Install Android SDK in Android Studio first. SDK path: %s\n' "$ANDROID_HOME"; exit 1; fi
cache="$HOME/.gradle/solarpractice-bootstrap"
mkdir -p "$cache"
if [ ! -x "$cache/gradle-8.9/bin/gradle" ]; then
  cd "$cache"
  curl --fail --location --retry 2 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -o gradle-8.9-bin.zip
  curl --fail --location --retry 2 'https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256' -o gradle-8.9-bin.zip.sha256
  printf '%s  gradle-8.9-bin.zip\n' "$(cat gradle-8.9-bin.zip.sha256)" | shasum -a 256 -c -
  unzip -qo gradle-8.9-bin.zip
  cd - >/dev/null
fi
"$cache/gradle-8.9/bin/gradle" wrapper --gradle-version 8.9
printf '\nWrapper generated. Open this project in Android Studio.\n'
