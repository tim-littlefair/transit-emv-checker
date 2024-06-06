#!/usr/bin/env sh

# This script converts the first 7 hex digits of the current
# commit hash into a positive decimal number which is
# interposed in place of the number "1" in the "versionCode 1"
# lines which appear in each of the build.gradle files

# This decimal number is then available to builds and can
# be rendered back to hex as part of a version string.

hash7_as_hex=$(git rev-parse HEAD | cut -c1-7)
hash7_as_decimal=$(printf "%d" 0x$hash7_as_hex)
/bin/echo git hash7 = $hash7_as_hex -\> $hash7_as_decimal

# For the moment we are only marking up the Android app but we could
# apply the same change to products in other directories in the future
for d in android-app ; do
  git checkout $d/build.gradle
  /usr/bin/sed -e "s/versionCode 1/versionCode $hash7_as_decimal/" --in-place $d/build.gradle
done