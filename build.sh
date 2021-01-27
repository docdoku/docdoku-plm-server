#!/usr/bin/env sh

set -e

VERSION=$(mvn -q -N org.codehaus.mojo:exec-maven-plugin:3.0.0:exec \
    -Dexec.executable='echo' \
    -Dexec.args='${project.version}')

mvn clean install
docker build -f docker/payara/Dockerfile -t docdoku/docdoku-plm-server-base:$VERSION .
docker build --build-arg VERSION=$VERSION -f docker/Dockerfile -t docdoku/docdoku-plm-server:$VERSION .
