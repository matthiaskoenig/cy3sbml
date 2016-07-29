#!/bin/bash

# lib directory
LIB_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

########################
# identifiers-org-code
########################
# > svn checkout http://svn.code.sf.net/p/identifiers-org/code/trunk identifiers-org-code
# > cd identifiers-org-code/registry-lib
# > ant jar
# > cp registry-lib-1.1.1.jar ~/git/cy3sbml/lib

REGISTRY_GROUPID=org.identifiers
REGISTRY_ARTIFACTID=registry-lib
REGISTRY_VERSION=1.1.1

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -DgroupId=${REGISTRY_GROUPID} -DartifactId=${REGISTRY_ARTIFACTID} -Dversion=${REGISTRY_VERSION} -Dfile=${REGISTRY_ARTIFACTID}-${REGISTRY_VERSION}.jar -DlocalRepositoryPath=${LIB_DIR} -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true