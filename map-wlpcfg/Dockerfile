FROM gameontext/docker-liberty-custom:master-29

ENV SERVERDIRNAME map

USER 0
ADD https://raw.githubusercontent.com/gameontext/gameon/master/bin/init_couchdb.sh /opt/init_couchdb.sh
ADD https://raw.githubusercontent.com/gameontext/gameon/master/bin/gen-keystore.sh /opt/gen-keystore.sh
RUN chmod g+rwx /opt/init_couchdb.sh /opt/gen-keystore.sh
COPY ./servers/gameon-map /opt/ol/wlp/usr/servers/defaultServer/
RUN mkdir -p /opt/ol/wlp/usr/servers/defaultServer/resources/security && chown -R 1001:0 /opt/ol/wlp/usr/servers/defaultServer && chmod -R g+rwx /opt/ol/wlp/usr/servers/defaultServer
USER 1001
COPY ./startup.sh /opt/startup.sh
COPY ./site.json /opt/site.json
COPY ./firstRoom.json /opt/firstRoom.json

CMD ["/opt/startup.sh"]

# Generous start time for constrained systems
HEALTHCHECK \
  --timeout=10s \
  --start-period=2m \
  CMD wget -q -O /dev/null http://localhost:9080/map/v1/health
