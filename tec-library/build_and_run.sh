#!/bin/sh

variant=tec-library
version=0.1.0
mainclass=net.heretical_camelid.transit_emv_checker.library.Main

mvn=/usr/bin/mvn

if [ ! -f $mvn ]
then
  # On Tim Littlefair's Mac
  mvn=~/Applications/apache-maven-3.8.8/bin/mvn
fi

$mvn clean dependency:copy-dependencies package -rf :$variant
mvn_status=$?

if [ "$mvn_status" -eq "0 " ]
then
    java -cp $variant/target/$variant-$version.jar:$variant/target/dependency/*:$variant $mainclass
else
    echo mvn_status=$mvn_status
fi
