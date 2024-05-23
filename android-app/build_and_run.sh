#!/bin/sh

variant=android-app
version=0.1.0

# Using the gradlew file generated as part of the IntelliJ IDEA project
gradle=./gradlew

$gradle --warning-mode all :android-app:clean :android-app:collectLicenseFile :android-app:build
gradle_status=$?

# TODO: Install on an android device and run

echo gradle_status=$gradle_status

