# script for building the latest jsbml core jar.
# ! register in local maven repository after build for using !

# JSBML should be checked out and environment variable $JSBMLCODE be set
# to the location of latest source code, i.e. 

# export $JSBMLCODE=$HOME/svn/jsbml-code 
# svn checkout svn://svn.code.sf.net/p/jsbml/code/trunk $JSBMLCODE

# now update to latest revision
cd $JSBMLCODE
svn update

# CORE
cd $JSBMLCODE/core   
ant jar 
cp $JSBMLCODE/core/build/*.jar $CY3SBML/cy3sbml/lib/core.jar

# QUAL
cd $JSBMLCODE/extensions/qual
ant jar 
cp $JSBMLCODE/extensions/qual/build/*.jar $CY3SBML/cy3sbml/lib/qual.jar

# LAYOUT
cd $JSBMLCODE/extensions/layout
ant jar 
cp $JSBMLCODE/extensions/layout/build/*.jar $CY3SBML/cy3sbml/lib/layout.jar

