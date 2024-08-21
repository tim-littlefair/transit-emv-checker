#! /bin/sh

# Every build has a build_id which is composed of
# 7-hex-digit prefix of current HEAD git hash
# '-' separator
# 3-hex-digit prefix of SHA-256 hash of git diff on the working copy
# if the diff is empty, the separator and the diff hash are omitted
generate_build_id() {
  head_hash7=$(git rev-parse HEAD | cut -c1-7)
  diff_text=$(git diff)
  if [ -z "$diff_text" ]
  then
    build_id=$head_hash7
  else
    build_id=$head_hash7-$(git diff | sha256sum - | cut -c1-3)
  fi
}

generate_build_id
export build_id
echo build_id=$build_id
build_dir=_work/tec-build-$build_id
export githash=$build_id

if [ "$1" = "--rehearse-release" ]
then
    ./gradlew clean build bundleRelease lintVitalReportRelease
    mkdir $build_dir
    echo Copying artifacts to $build_dir
    find android-app/build/outputs/bundle -name *.aab -exec cp {} $build_dir \;
    find android-app/build/reports/*.html -exec cp {} $build_dir \;
else
  ./gradlew clean
  ./gradlew build

  # Test on a VM managed by Gradle - presently does not collect coverage
  #./gradlew defaultGoogleATDDebugAndroidTest

  # Test on a physical device - will fail if no devices connected
  ./gradlew createDebugCoverageReport

  cp -R tec-library/build/reports/tests/test $build_dir/tec-library_tests
  cp -R tec-library/build/reports/jacoco/test/html/ $build_dir/tec-library_coverage
  cp -R android-app/build/reports/lint-results-debug.html $build_dir/android-app_lint.html
  cp -R android-app/build/reports/androidTests/managedDevice/debug/allDevices android-app_tests
  cp -R android-app/build/reports/coverage/androidTest/debug/managedDevice android-app_coverage
fi


echo "firefox --new-instance $build_dir 2> /dev/null"
