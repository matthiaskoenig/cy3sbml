#!/bin/bash
########################################################
# Necessary to install some dependency jars.
#
# see https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html
########################################################

# ols-client
# https://github.com/PRIDE-Utilities/ols-client
OLSCODE=$HOME/git/ols-client
OLS_VERSION=2.5-SNAPSHOT


# necessary to also provide the pom with the dependencies in the install
# at least 2.5 so that pom file is used
# mvn -Dplugin=install help:describe

# mvn install:install-file -DgroupId=uk.ac.ebi.pride.utilities -DartifactId=ols-client -Dversion=$OLS_VERSION -Dfile=$OLSCODE/target/ols-client-$OLS_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true -DpomFile=$OLSCODE/pom.xml
mvn install:install-file -Dfile=$OLSCODE/target/ols-client-$OLS_VERSION.jar
