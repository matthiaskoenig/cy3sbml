#!/bin/bash
############################################################
# build with tests and log all output
############################################################
mvn clean install | tee ./logs/cy3sbml_build.log

