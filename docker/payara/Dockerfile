FROM payara/server-full:5.194

ENV POSTGRES_CONNECTOR_URL https://jdbc.postgresql.org/download/postgresql-42.2.5.jar

USER root

# Add timezones (apt will fail without this)
RUN export DEBIAN_FRONTEND=noninteractive; \
    export DEBCONF_NONINTERACTIVE_SEEN=true; \
    echo 'tzdata tzdata/Areas select Etc' | debconf-set-selections; \
    echo 'tzdata tzdata/Zones/Etc select UTC' | debconf-set-selections;

# Update apt and install needed dependencies
RUN \
 apt-get update -qqy && apt-get install -qqy --no-install-recommends \
 tzdata \
 libreoffice  \
 libreoffice-writer  \
 ure  \
 libreoffice-java-common  \
 libreoffice-core \
 libreoffice-common \
 fonts-opensymbol \
 hyphen-fr \
 hyphen-de \
 hyphen-en-us \
 hyphen-it \
 hyphen-ru \
 fonts-dejavu \
 fonts-dejavu-core \
 fonts-dejavu-extra \
 fonts-noto \
 fonts-dustin \
 fonts-f500 \
 fonts-fanwood  \
 fonts-freefont-ttf \
 fonts-liberation \
 fonts-lmodern  \
 fonts-lyx  \
 fonts-sil-gentium \
 fonts-texgyre  \
 fonts-tlwg-purisa

# Create needed folders
RUN \
 mkdir -p /var/lib/docdoku/vault && \
 chown -R payara:payara /var/lib/docdoku/vault && \
 mkdir -p /opt/vault && \
 mkdir -p /opt/payara/appserver/glassfish/domains/production/lib

# Download dependencies
RUN \
 wget --quiet -O /opt/payara/appserver/glassfish/domains/production/lib/postgresql-42.2.5.jar ${POSTGRES_CONNECTOR_URL}

# Clean temp files and cache
RUN \
 apt-get clean && \
 rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Set permissions
RUN \
 chown -R payara:payara /opt

USER payara
