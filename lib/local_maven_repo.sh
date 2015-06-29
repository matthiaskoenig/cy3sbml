# JSBML - local maven dependencies
# This script installs the libraries in the local maven repository 

# change in the lib directory
cd $CY3SBML/lib

# spi-full-0.2.4
# wget http://sourceforge.net/p/jsbml/code/HEAD/tree/trunk/core/lib/spi-full-0.2.4.jar?format=raw -O spi-full-0.2.4.jar
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=spi-full -Dversion=0.2.4 -Dfile=spi-full-0.2.4.jar -Dpackaging=jar -DgeneratePom=true

# jigsaw-dateParser-0.1
# wget http://sourceforge.net/p/jsbml/code/HEAD/tree/trunk/core/lib/jigsaw-dateParser.jar?format=raw -O jigsaw-dateParser-0.1.jar
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jigsaw-dateParser -Dversion=0.1 -Dfile=jigsaw-dateParser-0.1.jar -Dpackaging=jar -DgeneratePom=true

# JSBML (generated in build_jsbml script)
# This has to be in line with the pom.xml
# mvn install:install-file -DgroupId=cysbml-temp -DartifactId=jsbml -Dversion=1.0 -Dfile=core.jar -Dpackaging=jar -DgeneratePom=true
# mvn install:install-file -DgroupId=cysbml-temp -DartifactId=jsbml-qual -Dversion=1.0 -Dfile=qual.jar -Dpackaging=jar -DgeneratePom=true
# mvn install:install-file -DgroupId=cysbml-temp -DartifactId=jsbml-layout -Dversion=1.0 -Dfile=layout.jar -Dpackaging=jar -DgeneratePom=true

mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml -Dversion=1.1-dev -Dfile=jsbml-1.1-dev.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-qual -Dversion=2.1-b1 -Dfile=jsbml-qual-2.1-b1.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-layout -Dversion=1.0-b1 -Dfile=jsbml-layout-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-comp -Dversion=1.0-b1 -Dfile=jsbml-comp-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -DgroupId=cy3sbml-temp -DartifactId=jsbml-fbc -Dversion=1.0-b1 -Dfile=jsbml-fbc-1.0-b1.jar -Dpackaging=jar -DgeneratePom=true
