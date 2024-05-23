#!/bin/sh

variant=tec-library
version=0.1.0
# mainclass=net.heretical_camelid.transit_emv_checker.library.TapReplayConductor
# args=tec-library/src/main/resources/visa-exp2402-5406.xml

rm -rf -- */build

# Using the gradlew file generated as part of the IntelliJ IDEA project
gradle=./gradlew

$gradle clean

$gradle --warning-mode all :tec-library:clean :tec-library:uberJar # :android-app:build
gradle_status=$?
echo gradle_status=$gradle_status

