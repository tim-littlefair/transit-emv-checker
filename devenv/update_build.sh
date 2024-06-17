#!/usr/bin/env sh

# This script converts the first 7 hex digits of the current
# commit hash into a positive decimal number which is
# interposed in place of the number "1" in the "versionCode 1"
# lines which appear in each of the build.gradle files

# This decimal number is then available to builds and can
# be rendered back to hex as part of a version string.

hash7_as_hex=$(git rev-parse HEAD | cut -c1-7)
hash7_as_decimal=$(printf "%d" "0x$hash7_as_hex")
/bin/echo "git hash7 = $hash7_as_hex -\> $hash7_as_decimal"

short_osname=$(uname -s)
if [ "$short_osname" = "Linux" ] ; then
  sed_inplace_arg="--in-place"
elif [ "$short_osname" = "Darwin" ] ; then
  # See https://github.com/koalaman/shellcheck/wiki/SC2089
  set -- -i ''
  sed_inplace_arg=$0
else
  echo "Unexpected OS name $short_osname"
  return 2> /dev/null
  exit 1
fi

# For the moment we are only marking up the Android app but we could
# apply the same change to products in other directories in the future
# shellcheck disable=SC2043
for d in android-app ; do
  git restore $d/build.gradle
  # Using '#' as sed delimiter to avoid a lot of escaping in file path in second command
  /usr/bin/sed -r -e "/versionName/s#-dirty#-$hash7_as_hex#" "$sed_inplace_arg" $d/build.gradle
  /usr/bin/sed -r -e "s#replace_with_keystore_properties_path#/tmp/hc_keys/hc-playstore-upload-2024_keystore.properties#" "$sed_inplace_arg" $d/build.gradle
done

git diff -U0 | grep -e versionName -e versionCode -e keystorePropertiesFile
