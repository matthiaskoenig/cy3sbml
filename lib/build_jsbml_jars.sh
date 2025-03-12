#!/bin/bash
########################################################
# Script for building JSBML (core & packages) from 
# the repository and install as local dependency.
#
# 1. Get the repository code
#	cd $HOME/git 
#	git clone https://github.com/sbmlteam/jsbml.git
#	
# 2. Export environment variable
# 	export JSBMLCODE=$HOME/git/jsbml
#
# After the build of the latest JSBML jars these are updated
# within the code location.
# 
# If version numbers of the JSBML libraries change,
# these have to be updated in this script.
#
# The maven repository files have to be deleted to 
# to force the update.
#
# This is the alternative solution to the maven central deploys.

# Usage: 
# 	./build_jsbml_jars.sh 2>&1 | tee ./build_jsbml_jars.log
#
########################################################
CORE_VERSION=1.6-SNAPSHOT
QUAL_VERSION=2.1-b1
LAYOUT_VERSION=1.0-b1
COMP_VERSION=1.0-b1
FBC_VERSION=1.0-b1
GROUPS_VERSION=0.4-b1
DISTRIB_VERSION=0.5
TIDY_VERSION=1.6-SNAPSHOT
JTIDY_VERSION=r938
########################################################
echo "Building jsbml in local repository"
date

# JSBML code directory
: "${JSBMLCODE:?The JSBML environment variable must be set to the jsbml-code directory.}"

# lib directory
LIBDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# update to latest commit
cd $JSBMLCODE
git pull
echo "*commit*"
git rev-parse HEAD


# clean old build files
rm -r $JSBMLCODE/build

# build core and extensions
# core available from $JSBMLCODE/core/build/
# extensions from $JSBMLCODE/build/
ant jar
cd $LIBDIR

# remove old versions from local repository
echo "Remove old versions from mvn repository"
rm -r ~/.m2/repository/org/sbml/

########################################################
# install in the local repository
echo "Install JSBML in mvn repository"
cd $LIBDIR
# TIMESTAMP=$(date +%Y%m%d.%H%M%S)
mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml -Dversion=$CORE_VERSION -Dfile=$JSBMLCODE/core/build/jsbml-$CORE_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-qual -Dversion=$QUAL_VERSION -Dfile=$JSBMLCODE/build/jsbml-qual-$QUAL_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-layout -Dversion=$LAYOUT_VERSION -Dfile=$JSBMLCODE/build/jsbml-layout-$LAYOUT_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-comp -Dversion=$COMP_VERSION -Dfile=$JSBMLCODE/build/jsbml-comp-$COMP_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-fbc -Dversion=$FBC_VERSION -Dfile=$JSBMLCODE/build/jsbml-fbc-$FBC_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-groups -Dversion=$GROUPS_VERSION -Dfile=$JSBMLCODE/build/jsbml-groups-$GROUPS_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-distrib -Dversion=$DISTRIB_VERSION -Dfile=$JSBMLCODE/build/jsbml-distrib-$DISTRIB_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jsbml-tidy -Dversion=$TIDY_VERSION -Dfile=$JSBMLCODE/build/jsbml-tidy-$TIDY_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

mvn install:install-file -DgroupId=cy3sbml-dep -DartifactId=jtidy -Dversion=$JTIDY_VERSION -Dfile=$JSBMLCODE/build/jtidy-$JTIDY_VERSION.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

