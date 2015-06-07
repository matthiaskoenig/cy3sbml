# script for building the latest jsbml core jar.

# JSBML should be checked out and environment variable $JSBMLCODE be set
# to the location of latest source code, i.e. 

# export $JSBMLCODE=$HOME/svn/jsbml-code 
# svn checkout svn://svn.code.sf.net/p/jsbml/code/trunk $JSBMLCODE

# now update to latest revision
cd $JSBMLCODE
svn update

# build the JSBML core.jar, copy to cy3lib folder and register in local repository
# CORE
# cd $JSBMLCODE/core   
# ant jar 
# cp $JSBMLCODE/core/build/*.jar $CY3SBML/cy3sbml/lib/core.jar
# cd $CY3SBML/cy3sbml/lib

# QUAL
cd $JSBMLCODE/extensions/qual
ant jar 
cp $JSBMLCODE/extensions/qual/build/*.jar $CY3SBML/cy3sbml/lib/qual.jar

# LAYOUT
cd $JSBMLCODE/extensions/layout
ant jar 
cp $JSBMLCODE/extensions/layout/build/*.jar $CY3SBML/cy3sbml/lib/layout.jar

cd $CY3SBML/cy3sbml/lib
# mvn install:install-file -DgroupId=cysbml-temp -DartifactId=core -Dversion=1.0 -Dfile=core.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=cysbml-temp -DartifactId=qual -Dversion=1.0 -Dfile=qual.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=cysbml-temp -DartifactId=layout -Dversion=1.0 -Dfile=layout.jar -Dpackaging=jar -DgeneratePom=true
