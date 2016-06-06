#!/bin/bash
########################################################
# JSBML - local maven dependencies
# 
# This script installs the required java libraries not 
# available via maven repositories from the 
# lib folder in the local maven repository 'cy3sbml-dep'.
#
# If version numbers of the JSBML libraries change,
# these have to be updated in this script.
########################################################

# lib directory
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# spi-full-0.2.4
wget https://github.com/sbmlteam/jsbml/raw/master/core/lib/spi-full-0.2.4.jar -O $DIR/spi-full-0.2.4.jar
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=spi-full -Dversion=0.2.4 -Dfile=$DIR/spi-full-0.2.4.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

# jigsaw-dateParser-0.1
wget https://github.com/sbmlteam/jsbml/raw/master/core/lib/jigsaw-dateParser.jar -O $DIR/jigsaw-dateParser-0.1.jar
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jigsaw-dateParser -Dversion=0.1 -Dfile=$DIR/jigsaw-dateParser-0.1.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

# JSBML (generated in build_jsbml script)
# core
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml -Dversion=1.2-SNAPSHOT -Dfile=$DIR/jsbml-1.2-SNAPSHOT.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
# qual
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-qual -Dversion=2.1-b1 -Dfile=$DIR/jsbml-qual-2.1-b1.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
# layout
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-layout -Dversion=1.0-b1 -Dfile=$DIR/jsbml-layout-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
# comp
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-comp -Dversion=1.0-b1 -Dfile=$DIR/jsbml-comp-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
# fbc
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-fbc -Dversion=1.0-b1 -Dfile=$DIR/jsbml-fbc-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
# groups
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-groups -Dversion=0.4-b1 -Dfile=$DIR/jsbml-groups-0.4-b1.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
# distrib
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-distrib -Dversion=0.5 -Dfile=$DIR/jsbml-distrib-0.5.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
# tidy SBML
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-tidy -Dversion=1.2.1 -Dfile=$DIR/jsbml-tidy-1.2.1.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jtidy -Dversion=r938 -Dfile=$DIR/jtidy-r938.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

