FROM websphere-liberty:beta

LABEL maintainer="Erin Schnabel <schnabel@us.ibm.com> (@ebullientworks)"

ENV ETCD_VERSION 2.2.2

RUN apt-get update \
  && apt-get install -y curl \
  \
# setup etcd
  && wget https://github.com/coreos/etcd/releases/download/v${ETCD_VERSION}/etcd-v${ETCD_VERSION}-linux-amd64.tar.gz -q \
  && tar xzf etcd-v${ETCD_VERSION}-linux-amd64.tar.gz etcd-v${ETCD_VERSION}-linux-amd64/etcdctl --strip-components=1 \
  && rm etcd-v${ETCD_VERSION}-linux-amd64.tar.gz \
  && mv etcdctl /usr/local/bin/etcdctl

# Install required features
RUN /opt/ibm/wlp/bin/installUtility install  --acceptLicense \
      apiDiscovery-1.0 \
      bluemixLogCollector-1.1 \
      cdi-1.2 \
      concurrent-1.0 \
      couchdb-1.0 \
      localConnector-1.0 \
      jaxrs-2.0 \
      jndi-1.0 \
      jsonp-1.0 \
      ssl-1.0 \
      websocket-1.1

ENV SERVERDIRNAME map

COPY ./startup.sh /opt/startup.sh
ADD ./servers/gameon-map /opt/ibm/wlp/usr/servers/defaultServer/

CMD ["/opt/startup.sh"]

# Generous start time for constrained systems
HEALTHCHECK \
  --timeout=10s \
  --start-period=2m \
  CMD wget -q -O /dev/null http://localhost:9080/map/v1/health
