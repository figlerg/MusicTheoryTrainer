#!/usr/bin/env bash
# Signs an unsigned release APK with the app's old key and the owner's shared key, with a proof of
# rotation from old to new. Android 9 and later then treat the shared key as the app's key and still
# install the APK as an update over versions signed with the old key. Older devices see the old key.
#
#   tools/sign-rotated.sh unsigned.apk signed.apk
#
# Old key: OLD_KS, OLD_ALIAS, OLD_STORE_PASS, OLD_KEY_PASS (falls back to the store password).
# New key: NEW_KS, NEW_ALIAS, NEW_STORE_PASS, NEW_KEY_PASS (falls back to the store password).
set -euo pipefail

unsigned=$1
signed=$2
export OLD_KEY_PASS="${OLD_KEY_PASS:-$OLD_STORE_PASS}"
export NEW_KEY_PASS="${NEW_KEY_PASS:-$NEW_STORE_PASS}"

tools=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

old=(--ks "$OLD_KS" --ks-key-alias "$OLD_ALIAS" --ks-pass env:OLD_STORE_PASS --key-pass env:OLD_KEY_PASS)
new=(--ks "$NEW_KS" --ks-key-alias "$NEW_ALIAS" --ks-pass env:NEW_STORE_PASS --key-pass env:NEW_KEY_PASS)

"$tools/apksigner" rotate --out "$work/lineage" --old-signer "${old[@]}" --new-signer "${new[@]}"
"$tools/zipalign" -f -p 4 "$unsigned" "$work/aligned.apk"
# 28 is where Android learned key rotation; below it the old key's v1/v2 signatures apply.
"$tools/apksigner" sign "${old[@]}" --next-signer "${new[@]}" \
  --lineage "$work/lineage" --rotation-min-sdk-version 28 --out "$signed" "$work/aligned.apk"
"$tools/apksigner" verify --verbose --print-certs "$signed" | grep -E "Verified using|Signer|SHA-256" || true
