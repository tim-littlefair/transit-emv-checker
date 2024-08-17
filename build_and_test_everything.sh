#! /bin/sh

./gradlew clean
./gradlew build

# Test on a VM managed by Gradle - presently does not collect coverage
./gradlew defaultGoogleATDDebugAndroidTest

# Test on a physical device - will fail if no devices connected
# ./gradlew createDebugCoverageReport

