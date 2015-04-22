# get the file

wget http://sourceforge.net/projects/jsbml/files/jsbml/1.0/jsbml-1.0-with-dependencies.jar/download -O jsbml-with-dependencies-1.0.jar

# install in local repository
mvn install:install-file -DgroupId=org.sbml.jsbml -DartifactId=jsbml-with-dependencies -Dversion=1.0 -Dfile=jsbml-with-dependencies-1.0.jar -Dpackaging=jar -DgeneratePom=true
