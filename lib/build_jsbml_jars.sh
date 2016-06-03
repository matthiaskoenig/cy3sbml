#!/bin/bash
########################################################
# Script for building JSBML (core & packages) from 
# the repository
#
# 1. Get the repository code
#	cd $HOME/git 
#	git clone https://github.com/sbmlteam/jsbml.git
#	
# 2. Export environment variable
# 	export JSBMLCODE=$HOME/git/jsbml
#
# After building the latest JSBML jars these have to be 
# updated in the local maven repository with the script
# 	local_maven_repo.sh
#
########################################################
: "${JSBMLCODE:?The JSBML environment variable must be set to the jsbml-code svn directory.}"

# cy3sbml/lib directory
LIBDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# now update to latest revision
cd $JSBMLCODE
git pull 
rm -r $JSBMLCODE/build

# build core and extensions
ant jar 

# copy CORE
cp $JSBMLCODE/core/build/*.jar $LIBDIR

# copy EXTENSIONS
cp $JSBMLCODE/build/*.jar $LIBDIR

