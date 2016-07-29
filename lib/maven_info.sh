#!/bin/bash
########################################################
# Necessary to install some dependency jars.
#
# see https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html
# http://maven.apache.org/plugins/maven-install-plugin/usage.html
#
# necessary to also provide the pom with the dependencies in the install
# at least 2.5 so that pom file is used
# mvn -Dplugin=install help:describe

# information for plugin
# mvn install:help -Ddetail=true -Dgoal=install-file

# installing plugins
# http://maven-plugins.sourceforge.net/installing.html
#
########################################################

###############
# ols-client
###############
# https://github.com/PRIDE-Utilities/ols-client

# lib directory
LIB_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# The local install does not resolve the dependencies.
# see http://stackoverflow.com/questions/27554781/maven-install-file-isnt-resolving-dependencies
# This is a known issue, is fixed, but not available in a release version.
# available in install-plugin:2.6.0
# See: https://issues.apache.org/jira/browse/MINSTALL-110

# necessary to set the fully qualified path to goal to get > 2.5
OLS_CODE=$HOME/git/ols-client/
OLS_VERSION=2.5-SNAPSHOT
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=${OLS_CODE}/target/ols-client-${OLS_VERSION}.jar -DpomFile=$OLS_CODE/pom.xml -DlocalRepositoryPath=${LIB_DIR}



#############################
# JSBML
##############################

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

# The problem with install is that it does not resolve the maven dependencies:
# The local install does not resolve the dependencies.
# see http://stackoverflow.com/questions/27554781/maven-install-file-isnt-resolving-dependencies
# This is a known issue, is fixed, but not available in a release version.
# available in install-plugin:2.6.0
# See: https://issues.apache.org/jira/browse/MINSTALL-110

MVN_REPO_URL="/home/mkoenig/tmp"
JSBML_DIR="/home/mkoenig/git/jsbml"
mvn deploy:deploy-file -DpomFile=${JSBML_DIR}/dev/maven/pom.xml -Dfile=${JSBML_DIR}/dev/maven/jsbml-pom.jar -Durl=${MVN_REPO_URL}