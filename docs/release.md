# cy3sbml Release Instructions

- build latest version `mvn clean install -DskipTests` resulting in `./target/cy3sbml-0.*.*.jar`
- update release notes in `release-notes`
- commit all changes to develop branch
- make new github release, create new tag, add release notes, attach `./target/cy3sbml-0.*.*.jar`
- upload the app to the app store (https://apps.cytoscape.org/apps/cy3sbml), update markdown with readme, upload jar, update release notes
- increase version in pom.xml
- update symlink: 
- create empty release notes for next version
```bash
ln -s /home/mkoenig/git/cy3sbml/target/cy3sbml-0.4.1.jar $HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar
```
