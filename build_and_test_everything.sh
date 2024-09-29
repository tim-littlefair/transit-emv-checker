#! /bin/sh

# Every build has a build_id which is composed of
# 7-hex-digit prefix of current HEAD git hash
# '-' separator
# 3-hex-digit prefix of SHA-256 hash of git diff on the working copy
# if the diff is empty, the separator and the diff hash are omitted
generate_build_id() {
  head_hash7=$(git rev-parse HEAD | cut -c1-7)
  diff_hash3=$(git diff | sha256sum - | cut -c1-3)
  if [ "$diff_hash3" = "e3b" ]
  then
    # If git diff returned an empty stream, $diff_hash3 will be 'e3b'
    # and the build_id is just $head_hash7
    build_id=$head_hash7
  else
    build_id=$head_hash7-$diff_hash3
  fi
}

build_debug_and_coverage() {
  ./gradlew clean
  ./gradlew build

  # Test on a VM managed by Gradle
  ./gradlew createManagedDeviceDebugAndroidTestCoverageReport

  cp -R tec-library/build/reports/tests/test $build_dir/tec-library_tests
  cp -R tec-library/build/reports/jacoco/test/html/ $build_dir/tec-library_coverage
  cp -R android-app/build/reports/lint-results-debug.html $build_dir/android-app_lint.html
  cp -R android-app/build/reports/androidTests/managedDevice/debug/defaultGoogleATD $build_dir/android-app_tests
  cp -R android-app/build/reports/coverage/androidTest/debug/managedDevice $build_dir/android-app_coverage
  find android-app/build -name *.png -exec cp -f {} $build_dir \;
}

build_release() {
    ./gradlew build bundleRelease lintVitalReportRelease
    echo Copying artifacts to $build_dir
    find android-app/build/outputs -name *.aab -exec cp {} $build_dir \;
    find android-app/build/outputs -name *.apk -exec cp {} $build_dir \;
    cp android-app/build/reports/*.html $build_dir
}

generate_build_id
export build_id
echo build_id=$build_id
build_dir=_work/tec-build-$build_id
mkdir $build_dir
git diff > $build_dir/$build_id.patch
export githash=$build_id

if [ "$1" = "--rehearse-release" ]
then
  # This option builds the software and the release directory
  # but skips the defaultGoogleATD test set which
  # speeds the build up considerably
  build_release
else
  build_debug_and_coverage
fi

if [ "$1" = "--execute-release" ]
then
  # build_debug_and_coverage will already have been done
  build_release
fi

