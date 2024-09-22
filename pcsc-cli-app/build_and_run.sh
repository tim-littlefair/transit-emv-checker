#!/bin/sh

variant=pcsc-cli-app
version=0.2.0
mainclass=net.heretical_camelid.transit_emv_checker.pcsc_cli_app.Main

if [ ! "$1" = "--run-only" ]
then
  rm -rf */build

  # Using the gradlew file generated as part of the IntelliJ IDEA project
  gradle=./gradlew

  $gradle clean
  $gradle build
  $gradle uberJar
  gradle_status=$?
else
  shift
  gradle_status=0
fi

if [ "$gradle_status" -eq "0 " ]
then
    java -cp $variant/build/libs/$variant-$version-uber.jar $mainclass $*
else
    echo gradle_status=$gradle_status
fi
