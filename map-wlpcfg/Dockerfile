FROM gameontext/docker-liberty-custom:master-9

ENV SERVERDIRNAME map

COPY ./startup.sh /opt/startup.sh
ADD ./servers/gameon-map /opt/ol/wlp/usr/servers/defaultServer/

CMD ["/opt/startup.sh"]

# Generous start time for constrained systems
HEALTHCHECK \
  --timeout=10s \
  --start-period=2m \
  CMD wget -q -O /dev/null http://localhost:9080/map/v1/health
