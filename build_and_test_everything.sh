#! /bin/sh

./gradlew clean
./gradlew build

# Test on a VM managed by Gradle - presently does not collect coverage
#./gradlew defaultGoogleATDDebugAndroidTest

# Test on a physical device - will fail if no devices connected
./gradlew createDebugCoverageReport

firefox \
  tec-library/build/reports/tests/test/index.html \
  tec-library/build/reports/jacoco/test/html/index.html \
  android-app/build/reports/lint-results-debug.html \
  android-app/build/reports/androidTests/managedDevice/debug/allDevices/index.html \
  android-app/build/reports/coverage/androidTest/debug/managedDevice/index.html \
&


