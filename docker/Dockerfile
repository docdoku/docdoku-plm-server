FROM docdoku/docdoku-plm-server-base:latest

COPY docdoku-plm-server-ear/target/docdoku-plm-server-ear.ear $DEPLOY_DIR
COPY docker/entrypoint.sh /entrypoint.sh
COPY docker/asadmin.commands /asadmin.commands

USER root

RUN chmod +x /entrypoint.sh && \
 chown payara:payara /asadmin.commands

USER payara

ENTRYPOINT ["/entrypoint.sh"]
