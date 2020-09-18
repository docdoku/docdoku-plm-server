#!/usr/bin/env sh

IMAGE_NAME=docdoku/docdoku-plm-server
VERSION=$(mvn -q -N org.codehaus.mojo:exec-maven-plugin:3.0.0:exec \
    -Dexec.executable='echo' \
    -Dexec.args='${project.version}')

mvn clean install && \
 docker build -f docker/payara/Dockerfile -t docdoku/docdoku-plm-server-base:latest .  && \
 docker tag docdoku/docdoku-plm-server-base:latest docdoku/docdoku-plm-server-base:$VERSION  && \
 docker build -f docker/Dockerfile -t $IMAGE_NAME:latest .  && \
 docker tag $IMAGE_NAME:latest $IMAGE_NAME:$VERSION
