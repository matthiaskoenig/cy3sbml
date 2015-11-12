# script for building the latest jsbml core jar.
# ! register in local maven repository after build for using !

# JSBML should be checked out and environment variable $JSBMLCODE be set
# to the location of latest source code, i.e. 

# export JSBMLCODE=$HOME/svn/jsbml-code 
# svn checkout svn://svn.code.sf.net/p/jsbml/code/trunk $JSBMLCODE

export JSBMLCODE=$HOME/svn/jsbml-code

# now update to latest revision
cd $JSBMLCODE
svn update 
rm -r build

# build core and extensions
ant jar 

# CORE
cp $JSBMLCODE/core/build/*.jar $CY3SBML/lib/

# EXTENSIONS
cp $JSBMLCODE/build/*.jar $CY3SBML/lib/

