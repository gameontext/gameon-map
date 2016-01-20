#!/bin/bash

if [ "$ETCDCTL_ENDPOINT" != "" ]; then
  echo Setting up etcd...
  wget https://github.com/coreos/etcd/releases/download/v2.2.2/etcd-v2.2.2-linux-amd64.tar.gz -q
  tar xzf etcd-v2.2.2-linux-amd64.tar.gz etcd-v2.2.2-linux-amd64/etcdctl --strip-components=1
  rm etcd-v2.2.2-linux-amd64.tar.gz
  mv etcdctl /usr/local/bin/etcdctl

  export REGISTRATION_SECRET=$(etcdctl get /passwords/concierge-key)
  export QUERY_SECRET=$(etcdctl get /passwords/concierge-key)

  /opt/ibm/wlp/bin/server start defaultServer
  echo Starting the logstash forwarder...
  sed -i s/PLACEHOLDER_LOGHOST/$(etcdctl get /logstash/endpoint)/g /opt/forwarder.conf
  cd /opt
  chmod +x ./forwarder
  etcdctl get /logstash/cert > logstash-forwarder.crt
  etcdctl get /logstash/key > logstash-forwarder.key
  sleep 0.5
  ./forwarder --config ./forwarder.conf
else
  # LOCAL DEVELOPMENT!
  # We do not want to ruin the cloudant admin party, but our code is written to expect
  # that creds are required, so we should make sure the required user/password exist
  export AUTH_HOST="http://${COUCHDB_MAP_USER}:${COUCHDB_MAP_PASSWORD}@map_couchdb:5984"
  curl --fail -v -X GET ${AUTH_HOST}/_config/admins/${COUCHDB_MAP_USER}
  if [ $? -eq 22 ]; then
      curl -X PUT ${COUCHDB_MAP_URL}/_config/admins/${COUCHDB_MAP_USER} -d \"${COUCHDB_MAP_PASSWORD}\"
      curl -X PUT $AUTH_HOST/map_repository
  fi

  /opt/ibm/wlp/bin/server run defaultServer
fi
