#!/bin/bash

if [ "$ETCDCTL_ENDPOINT" != "" ]; then
  echo Setting up etcd...
  wget https://github.com/coreos/etcd/releases/download/v2.2.2/etcd-v2.2.2-linux-amd64.tar.gz -q
  tar xzf etcd-v2.2.2-linux-amd64.tar.gz etcd-v2.2.2-linux-amd64/etcdctl --strip-components=1
  rm etcd-v2.2.2-linux-amd64.tar.gz
  mv etcdctl /usr/local/bin/etcdctl

  export COUCHDB_URL=$(etcdctl get /couchdb/url)
  export COUCHDB_USER=$(etcdctl get /couchdb/user)
  export COUCHDB_PASSWORD=$(etcdctl get /passwords/couchdb)

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

  AUTH_HOST="http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@couchdb:5984"
  SERVER_PATH=/opt/ibm/wlp/usr/servers/defaultServer

  echo "** Testing connection to ${COUCHDB_URL}"
  curl --fail -X GET ${AUTH_HOST}/_config/admins/${COUCHDB_USER}
  RC=$?

  # RC=7 means the host isn't there yet. Let's do some re-trying until it
  # does start / is ready
  while [ $RC -eq 7 ]; do
      sleep 15

      # recheck condition
      echo "** Re-testing connection to ${COUCHDB_URL}"
      curl --fail -X GET ${AUTH_HOST}/_config/admins/${COUCHDB_USER}
      RC=$?
  done

  # RC=22 means the user doesn't exist
  if [ $RC -eq 22 ]; then
      echo "** Creating ${COUCHDB_USER}"
      curl -X PUT ${COUCHDB_URL}/_config/admins/${COUCHDB_USER} -d \"${COUCHDB_PASSWORD}\"
  fi

  echo "** Checking database"
  curl --fail -X GET ${AUTH_HOST}/map_repository
  if [ $? -eq 22 ]; then
      curl -X PUT $AUTH_HOST/map_repository
  fi

  echo "** Checking design documents"
  curl -v --fail -X GET ${AUTH_HOST}/map_repository/_design/site
  if [ $? -eq 22 ]; then
      curl -v -X PUT -H "Content-Type: application/json" --data @${SERVER_PATH}/site.json ${AUTH_HOST}/map_repository/_design/site
  fi

  echo "** Checking firstroom"
  curl --fail -X GET ${AUTH_HOST}/map_repository/firstroom
  if [ $? -eq 22 ]; then
      curl -X POST -H "Content-Type: application/json" --data @${SERVER_PATH}/firstRoom.json ${AUTH_HOST}/map_repository
  fi


  exec /opt/ibm/wlp/bin/server run defaultServer
fi
