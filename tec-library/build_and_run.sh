#!/bin/sh

variant=tec-library
version=0.1.0
mainclass=net.heretical_camelid.transit_emv_checker.library.Main

# Using the gradlew file generated as part of the IntelliJ IDEA project
gradle=./gradlew

$gradle clean

$gradle uberJar
gradle_status=$?

if [ "$gradle_status" -eq "0 " ]
then
    # java -cp $variant/target/$variant-$version.jar:$variant/target/dependency/*:$variant $mainclass
    java -cp $variant/build/libs/$variant-$version-uber.jar $mainclass
else
    echo gradle_status=$gradle_status
fi
