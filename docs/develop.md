# cy3sbml Development Instructions

This document provides instructions for setting up the development environment, building the `cy3sbml` app, and contributing to its development.

`cy3sbml` is a Cytoscape app, and standard development practices for Cytoscape apps apply.  
For more details, refer to the official Cytoscape developer documentation:  

- [Cytoscape Developer Documentation](https://cytoscape.org/documentation_developers.html)  
- [Cytoscape App Development Guide](https://github.com/cytoscape/cytoscape/wiki/Cytoscape-App-Ladder)  

## Requirements  

To develop `cy3sbml`, the following dependencies must be installed:  

- **Git** (for source code versioning)  
- **Java JDK 17** (required for compilation) 
- **JavaFX**
- **Maven (version 3 or higher)** (build automation tool)  
- **Cytoscape 3 (≥ 3.10.3)** (required for testing and usage)  

## Installation Steps  

### 1. Install Cytoscape  

Download and install the latest **Cytoscape 3** version (≥ 3.10.3) from:  
[http://www.cytoscape.org](http://www.cytoscape.org)  

### 2. Install Maven  

Cytoscape apps are built using **Maven (version 3 or higher)**. Follow the installation guide for your platform:  
[Apache Maven Installation Guide](https://maven.apache.org/install.html)  

To verify the installation, check the Maven version:  
```bash
mvn -v
```

### 3. Install Java JDK 17

Cytoscape apps require **Java JDK 17**. Follow the installation instructions based on your operating system.

#### **Ubuntu (Debian-based systems)**
Install **OpenJDK 17** with:
```bash
sudo apt install openjdk-17-jdk
```

Set the `JAVA_HOME` environment variable:
```bash
sudo gedit /etc/bash.bashrc
```
Add the following line at the end of the file:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```
Save and close the file, then apply the changes:
```bash
source /etc/bash.bashrc
```

Verify the Java installation:
```bash
java -version
```

### JavaFx
JavaFx (https://openjfx.io/index.html) is no longer included in the JDK. Therefor it must be installed separately and 
the idea must be made aware of the installation

For instance on linux
```bash
sudo apt-get install openjfx
```
This has to be setup correctly:
https://stackoverflow.com/questions/27178895/cannot-resolve-symbol-javafx-application-in-intellij-idea-ide
In idea add the `/usr/share/openjfx/lib/` to the project via `File -> Project Structure -> Project Settings -> Libraries`.


With these dependencies installed, you are ready to start developing `cy3sbml`.


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

### maven build
The clean build can be run via
```bash
mvn clean install -DskipTests
```

To run the tests (which take a long time and performed in continuous integration) use
```bash
mvn clean install
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

## Development

### Setup IDE
Open the GitHub Repository with Intellij
- In Intellij, go to the File menu and choose Open.
- Navigate to the cloned git repository and open it.

https://github.com/cytoscape/cytoscape/wiki/Launch-and-Debug-from-Intellij


### Launch and debug app
I presume you have installed your App using a symlink.

Launch Cytoscape from the commandline in either normal mode or debug mode
In Windows:
```
cytoscape.bat
cytoscape.bat debug
```
In Linux/Mac:
```
cytoscape.sh debug
```

...from the Intellij IDE

    From the main menu select Run and Edit Configurations...
    In the upper left corner select '+', you will be prompted to Add a New Configuration, select Bash (This requires the BashSupport plugin)
    You will be provided with a dialog for the configuration.
        Set the name to something appropriate: Cytoscape 3
        Script: navigate to location of cytoscape.sh
        Interpreter path: /bin/bash
        Program arguments: debug or leave blank if you are not debugging your App
        Apply and Close
    From the main menu select Run and Cytoscape 3

Cytoscape should launch and you should see the console output in the Run panel.

Debugging your App
...from the Intellij IDE

We will be defining a configuration for debugging our App.

    From the main menu select Run and Edit Configurations...
    In the upper left corner select '+', you will be prompted to Add a New Configuration, select Remote
    You will be provided with a dialog for the configuration.
        Set the name to something appropriate: Cytoscape 3: Create Network View
        Debugger mode: Attach to remote JVM
        Host: localhost, Port: 12345
        Command line args: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=12345
        Use module classpath: <no module>
        Before launch: +
            Run Maven Goal
            Working directory: .../cytoscape-app-samples/sample-create-network-view
            Command line: compile
    From the main menu select Run and Debug Cytoscape 3

The Intellij Debugger

Here is a nice tutorial on how to use the Intellij debugger: Java Debugging with Intellij

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
