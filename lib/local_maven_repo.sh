#!/bin/bash
# JSBML - local maven dependencies
# This script installs the required java libraries not available via maven repositories from the 
# lib folder in the local maven repository 'cy3sbml-temp'.

# lib directory
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# spi-full-0.2.4
# wget http://sourceforge.net/p/jsbml/code/HEAD/tree/trunk/core/lib/spi-full-0.2.4.jar?format=raw -O $DIR/spi-full-0.2.4.jar
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=spi-full -Dversion=0.2.4 -Dfile=$DIR/spi-full-0.2.4.jar -Dpackaging=jar -DgeneratePom=true

# jigsaw-dateParser-0.1
# wget http://sourceforge.net/p/jsbml/code/HEAD/tree/trunk/core/lib/jigsaw-dateParser.jar?format=raw -O $DIR/jigsaw-dateParser-0.1.jar
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jigsaw-dateParser -Dversion=0.1 -Dfile=$DIR/jigsaw-dateParser-0.1.jar -Dpackaging=jar -DgeneratePom=true

# JSBML (generated in build_jsbml script)
# The registered jars have to correspond to the actual pom.xml, so changes here have to be synchronized!
# core
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml -Dversion=1.2-SNAPSHOT -Dfile=$DIR/jsbml-1.2-SNAPSHOT.jar -Dpackaging=jar -DgeneratePom=true
# qual
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-qual -Dversion=2.1-b1 -Dfile=$DIR/jsbml-qual-2.1-b1.jar -Dpackaging=jar -DgeneratePom=true
# layout
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-layout -Dversion=1.0-b1 -Dfile=$DIR/jsbml-layout-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true
# comp
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-comp -Dversion=1.0-b1 -Dfile=$DIR/jsbml-comp-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true
# fbc
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-fbc -Dversion=1.0-b1 -Dfile=$DIR/jsbml-fbc-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true
# distrib
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-distrib -Dversion=0.5 -Dfile=$DIR/jsbml-distrib-0.5.jar -Dpackaging=jar -DgeneratePom=true

