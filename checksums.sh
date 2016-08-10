#!/bin/bash
############################################################
# Calculates checksums for release file
############################################################
# lib directory
LIBDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd target

MD5_FILE=md5sum.txt
SHA1_FILE=sha1sum.txt
SHA256_FILE=sha256sum.txt

# create checksums
echo "---------------------------------"
echo Checksums
echo "---------------------------------"
echo MD5
md5sum cy3sbml-*.jar | tee ${MD5_FILE}
echo SHA1
sha1sum cy3sbml-*.jar | tee ${SHA1_FILE}
echo SHA256
sha256sum cy3sbml-*.jar | tee ${SHA256_FILE}
echo "---------------------------------"

# check checksums
md5sum -c ${MD5_FILE}
sha1sum -c ${SHA1_FILE}
sha256sum -c ${SHA256_FILE}

