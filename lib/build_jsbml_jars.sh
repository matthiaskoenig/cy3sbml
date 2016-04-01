#!/bin/bash
########################################################
# Script for building latest JSBML (core & packages)
#
# JSBML should be checked out and environment variable $JSBMLCODE be set
# to the location of latest source code, i.e. 
#
# check if the necessary enviroment variables are set
# The environment variable can be set via
# export JSBMLCODE=$HOME/svn/jsbml-code
#
# ! After building the local maven repository in lib & pom file
#   have to be updated manually !
#
########################################################
: "${JSBMLCODE:?The JSBML environment variable must be set to the jsbml-code svn directory.}"

# cy3sbml/lib directory
LIBDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# now update to latest revision
cd $JSBMLCODE
svn update 
rm -r $JSBMLCODE/build

# build core and extensions
ant jar 

# copy CORE
cp $JSBMLCODE/core/build/*.jar $LIBDIR

# copy EXTENSIONS
cp $JSBMLCODE/build/*.jar $LIBDIR

