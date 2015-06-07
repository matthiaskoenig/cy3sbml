# script for building the latest jsbml core jar.

# JSBML should be checked out and environment variable $JSBMLCODE be set
# to the location of latest source code, i.e. 

# export $JSBMLCODE=$HOME/svn/jsbml-code 
# svn checkout svn://svn.code.sf.net/p/jsbml/code/trunk $JSBMLCODE

# now update to latest revision
cd $JSBMLCODE
svn update

# build the JSBML core.jar
cd $JSBMLCODE/core   
ant jar 

# copy cy3sbml lib folder
cp $JSBMLCODE/core/build/*.jar $CY3SBML/cy3sbml/lib/core.jar

# register in local repository
cd $CY3SBML/cy3sbml/lib
mvn install:install-file -DgroupId=cysbml-temp -DartifactId=core -Dversion=1.0 -Dfile=core.jar -Dpackaging=jar -DgeneratePom=true
