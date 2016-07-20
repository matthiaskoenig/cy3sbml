#!/bin/bash

# Here a solution should be implemented which deploys jsbml into a local
# mvn repository.
# This would allow a simple update of the mvn dependency without a very
# complicated pom.xml.

# <exec executable="mvn">
# 		<arg value="deploy:deploy-file"/>
# 			<arg value="-DpomFile=./dev/maven/pom.xml"/>
# 			<arg value="-Dfile=./dev/maven/jsbml-pom.jar"/>
# 			<arg value="-Durl=${maven.repo.url}"/>
# 			<arg value="-DrepositoryId=jsbml-maven-repo-sf"/>
# 		</exec>

MVN_REPO_URL="/home/mkoenig/tmp"
JSBML_DIR="/home/mkoenig/git/jsbml"
mvn deploy:deploy-file -DpomFile=${JSBML_DIR}/dev/maven/pom.xml -Dfile=${JSBML_DIR}/dev/maven/jsbml-pom.jar -Durl=${MVN_REPO_URL}
