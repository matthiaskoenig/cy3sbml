# `cy3sbml` Build Instructions
This document describes how to setup the requirements for cy3sbml and build the cy3sbml app from source code. Information and documentation for cy3sbml is available from the project page
https://github.com/matthiaskoenig/cy3sbml/

## Build Requirements
To build cy3sbml git, Java™ and Maven have to be available. 
To use the app Cytoscape 3 has to be installed. 
The example workflow was tested on Ubuntu 18.04LTS and OSX 10.11.1.

### git
Follow install instructions from https://git-scm.com/.
#### Ubuntu
```
sudo apt-get install git
```

### Java JDK
Cytoscape apps are build with Oracle Java 8. Follow the instructions for your platform. 

Check your java version via
```
java -version
```

#### Ubuntu
```{bash}
# Remove openjdk
sudo apt-get purge openjdk*
# install oracle java
sudo -E add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
set java version for system
sudo update-java-alternatives -s java-8-oracle
# test installation
java -version
javac -version

# set JAVA_HOME environment variable
sudo gedit /etc/bash.bashrc
export JAVA_HOME=/usr/lib/jvm/java-8-oracle
```

### Maven
Cytoscape apps are build with maven version 3 or higher. Follow the instructions for your platform to install maven (https://maven.apache.org/). The version information is available via
```
mvn -v
```
#### Ubuntu
```
sudo apt-get install maven
```

### Cytoscape
Download and install the latest Cytoscape 3 version (>3.6.1) from http://www.cytoscape.org.

## Build cy3sbml
### git Repository
Clone the repository from github
```
git clone https://github.com/matthiaskoenig/cy3sbml.git
```
If the repository exists pull the latest code via
```
cd cy3sbml
git pull
```
An overview over the available branches is available via
```
git branch -a
```
The master branch contains the stable releases, with development code in the develop branch. All development work is done in the development branch. To work with the development branch, you'll need to create a local tracking branch:
```
git checkout -b develop origin/develop
```
To build the development version, checkout the develop branch
```
git checkout develop
```

### cy3sbml Build
After providing the maven dependencies you can build cy3sbml via
```
mvn clean install
```
To skip the tests (which take a long time and performed in continuous integration) use
```
mvn clean install -DskipTests
```
The target jar is located in
```
./target/
```

### cy3sbml Install
The last step is installing the app. You can install cy3sbml as app with the created jar file directly within Cytoscape
```
Apps → App Manager → Install Apps
```
Select `Install from File` and use the `cy3sbml-*.jar` located in the `cy3sbml/target/` folder of the git repository.

To manually install the cy3sbml jar remove all old cy3sbml jars from
``` 
$HOME/CytoscapeConfiguration/3/apps/installed/
```
and copy the new jar in the respective folder.

To update the app cy3sbml automatically after every build, set a symbolic link of the to the build cy3sbml jar in the Cytoscape installed apps folder
```
ln -s $CY3SBML/target/cy3sbml-0.*.*.jar $HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar
```
The link has to be updated with increasing versions.

## Advanced topics (core developers)

### Update JSBML dependencies
For installation one can setup an environment variable referring to the cy3sbml source folder. This will simplify the subsequent steps
```
export CY3SBML=$HOME/git/cy3sbml
```
All necessary jars are available in the lib folder and the provided source code is tested and developed against the provided versions. So rebuild the JSBML jars only if you know what you are doing.

Clone JSBML repository and set environment variable
```
cd $HOME/git 
git clone https://github.com/sbmlteam/jsbml.git
export JSBMLCODE=$HOME/git/jsbml
Update existing repository
cd $JSBMLCODE
git pull
```

Build the JSBML jars with the provided script and register in local mvn repository
```
$CY3SBML/lib/build_jsbml_jars.sh
```
If the version numbers change of JSBML or the extensions change, the build script and the respective versions in the
`pom.xml` have to be updated.
