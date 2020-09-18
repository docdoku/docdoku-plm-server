#!/bin/bash

find $DEPLOY_DIR -maxdepth 1 -iname '*.ear' -exec echo "deploy {}" >> /asadmin.commands \; && \
echo 'list-applications --type ejb' >> /asadmin.commands && \
asadmin start-domain --debug ${DEBUG} -v --postbootcommandfile /asadmin.commands ${PAYARA_DOMAIN}
