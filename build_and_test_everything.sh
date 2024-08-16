#! /bin/sh

./gradlew clean
./gradlew build
# ./gradlew defaultGoogleATDDebugAndroidTest
./gradlew connectedDebugAndroidTest

