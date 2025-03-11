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

### Install Cytoscape  

Download and install the latest **Cytoscape 3** version (≥ 3.10.3) from:  
[http://www.cytoscape.org](http://www.cytoscape.org)  

### Install Maven  

Cytoscape apps are built using **Maven (version 3 or higher)**. Follow the installation guide for your platform:  
[Apache Maven Installation Guide](https://maven.apache.org/install.html)  

To verify the installation, check the Maven version:  
```bash
mvn -v
```

### Install Java JDK 17

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

### JavaFX Setup

[JavaFX](https://openjfx.io/index.html) is no longer bundled with the JDK, so it must be installed separately and configured in your IDE.

#### **1. Installing JavaFX**
On Linux, install JavaFX using the package manager:
```bash
sudo apt-get install openjfx
```

#### **2. Configuring JavaFX in IntelliJ IDEA**
To resolve missing JavaFX symbols in IntelliJ IDEA, add the JavaFX library manually:
1. Open **IntelliJ IDEA**.
2. Navigate to **File → Project Structure → Project Settings → Libraries**.
3. Click **+ (Add Library)** and select **Java**.
4. Add the JavaFX library path:
   ```
   /usr/share/openjfx/lib/
   ```
5. Apply and save the changes.

For further troubleshooting, refer to this [Stack Overflow discussion](https://stackoverflow.com/questions/27178895/cannot-resolve-symbol-javafx-application-in-intellij-idea-ide).


## Build cy3sbml

### Setup repository

Clone the `cy3sbml` repository from GitHub:

```bash
git clone https://github.com/matthiaskoenig/cy3sbml.git
```
The latest development branch is `develop`. Switch to it using:

```bash
git checkout develop
```

### Build with Maven

To build `cy3sbml` without running tests:

```bash
mvn clean install -DskipTests
```

To build and run tests (note: this may take a long time):

```bash
mvn clean install
```

The built `.jar` file will be located in:

```bash
./target/cy3sbml-0.*.*.jar
```

With these steps completed, `cy3sbml` is ready for development and testing.

### cy3sbml Install
The last step is installing the app. To update the app cy3sbml automatically after every build, set a symbolic link of the to the build cy3sbml jar in the Cytoscape installed apps folder.
```bash
ln -s $CY3SBML/target/cy3sbml-0.*.*.jar $HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar
```
e.g. 
```bash
ln -s /home/mkoenig/git/cy3sbml/target/cy3sbml-0.4.0.jar $HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar
```
The link has to be updated with increasing versions.

## Development

### Setup IDE
Open the GitHub Repository with Intellij
- In Intellij, go to the File menu and choose Open.
- Navigate to the cloned git repository and open it.

### Launch Cytoscape
The cy3sbml app should now be installed via a symlink (see above).
Cytoscape can be launched from the command line in either **normal mode** or **debug mode**.

#### Windows
Open a command prompt and run:
```bash
cytoscape.bat         # Normal mode
cytoscape.bat debug   # Debug mode
```

#### Linux / macOS
Run the following command:
```bash
cytoscape.sh debug
```

#### Configuring Cytoscape Debug Mode in IntelliJ IDEA
You can set up a debug start configuration in **IntelliJ IDEA** to streamline the development process.

##### 1. Create a New Run Configuration
1. Open **IntelliJ IDEA**.
2. From the main menu, navigate to **Run → Edit Configurations...**.
3. In the upper-left corner, click **+** to add a new configuration.
4. Select **Shell Script** as the configuration type.

##### 2. Configure the Debug Start Script
In the **Run/Debug Configuration** dialog:
- **Name**: Set to an appropriate label (e.g., **Cytoscape 3 Debug**).
- **Script**: Navigate to the location of `cytoscape.sh` (e.g., `/path/to/cytoscape.sh`).
- **Interpreter Path**: Set to `/bin/bash` (or the appropriate shell for your system).
- **Program Arguments**: Set to `debug` if debugging, or leave blank for normal mode.

Click **Apply** and **Close**.

##### 3. Run Cytoscape in Debug Mode
1. Open **Run → Cytoscape 3 Debug** from the main menu.
2. Cytoscape will launch, and the **Run panel** will display debug output, including:

The output after starting Cytoscape in debug mode should be one of the following:
```bash
Listening for transport dt_socket at address: 12345
```
You are now ready to debug and develop `cy3sbml` within Cytoscape.


### Debugging cy3sbml
#### Debugging in IntelliJ IDEA

To debug `cy3sbml`, we will configure **IntelliJ IDEA** to attach to Cytoscape's remote JVM for debugging.

#### 1. Create a Remote Debugging Configuration
1. Open **IntelliJ IDEA**.
2. From the main menu, navigate to **Run → Edit Configurations...**.
3. Click **+** to add a new configuration.
4. Select **Remote JVM Debug**.

#### 2. Configure the Debugging Session
In the **Run/Debug Configuration** dialog:
- **Name**: Set an appropriate name, e.g., **Cytoscape 3: cy3sbml**.
- **Debugger Mode**: Select **Attach to Remote JVM**.
- **Host**: `localhost`
- **Port**: `12345`
- **Command Line Args**:
  ```bash
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=12345
  ```
- **Use module classpath**: Select `<no module>`.
- **Before launch**: Click **+**, then select **Run Maven Goal**.
   - **Working directory**: `/home/mkoenig/git/cy3sbml`
   - **Command line**: `compile`

#### 3. Start Debugging
1. From the main menu, go to **Run → Cytoscape 3: cy3sbml**.
2. Cytoscape should now be running with debugging enabled.
3. Set breakpoints in your code, and IntelliJ IDEA will pause execution when those breakpoints are hit.

#### **4. IntelliJ Debugger Guide**
For a detailed guide on using the IntelliJ debugger, refer to:  
[Java Debugging with IntelliJ IDEA](https://www.jetbrains.com/help/idea/debugging-code.html).

### **Hotswap: Live App Updates**
Java, along with Cytoscape, supports a feature called **Hotswap**, which allows automatic updates to your App without restarting Cytoscape.

- When the `cy3sbml` JAR file is placed in the following directory:
  ```bash
  ~/CytoscapeConfiguration/3/apps/installed/
  ```
- Cytoscape will detect the change and automatically reload the updated App.
- This allows for **faster testing and debugging** without restarting Cytoscape.
- The howswap can be triggered via `mvn install -DskipTests`

For more details, see the official documentation:  
[Cytoscape Java Hotswap](https://github.com/cytoscape/cytoscape/wiki/Java-Hotswap).


## Advanced topics

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
