#!/usr/bin/env bash
# creates the extension bundle
echo "Create extension bundle"
jar -cfm org.cy3javascript.extension-0.0.1.jar manifest.mf
mv org.cy3javascript.extension-0.0.1.jar ../src/main/resources/extension/