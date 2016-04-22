#!/bin/bash

if [ "$SERVERDIRNAME" == "" ]; then
  SERVERDIRNAME=defaultServer
else
  # Share the configuration directory via symlink
  ln -s /opt/ibm/wlp/usr/servers/defaultServer /opt/ibm/wlp/usr/servers/$SERVERDIRNAME

  # move the convenience output dir link to the new output location
  rm /output
  ln -s $WLP_OUTPUT_DIR/$SERVERDIRNAME /output
fi

if [ "$ETCDCTL_ENDPOINT" != "" ]; then
  echo Setting up etcd...
  echo "** Testing etcd is accessible"
  etcdctl --debug ls
  RC=$?

  while [ $RC -ne 0 ]; do
      sleep 15

      # recheck condition
      echo "** Re-testing etcd connection"
      etcdctl --debug ls
      RC=$?
  done
  echo "etcdctl returned sucessfully, continuing"

  mkdir -p /opt/ibm/wlp/usr/servers/$SERVERDIRNAME/resources/security
  cd /opt/ibm/wlp/usr/servers/$SERVERDIRNAME/resources/
  etcdctl get /proxy/third-party-ssl-cert > cert.pem
  openssl pkcs12 -passin pass:keystore -passout pass:keystore -export -out cert.pkcs12 -in cert.pem
  keytool -import -v -trustcacerts -alias default -file cert.pem -storepass truststore -keypass keystore -noprompt -keystore security/truststore.jks
  keytool -genkey -storepass testOnlyKeystore -keypass wefwef -keyalg RSA -alias endeca -keystore security/key.jks -dname CN=rsssl,OU=unknown,O=unknown,L=unknown,ST=unknown,C=CA
  keytool -delete -storepass testOnlyKeystore -alias endeca -keystore security/key.jks
  keytool -v -importkeystore -srcalias 1 -alias 1 -destalias default -noprompt -srcstorepass keystore -deststorepass testOnlyKeystore -srckeypass keystore -destkeypass testOnlyKeystore -srckeystore cert.pkcs12 -srcstoretype PKCS12 -destkeystore security/key.jks -deststoretype JKS

  export COUCHDB_URL=$(etcdctl get /couchdb/url)
  export COUCHDB_USER=$(etcdctl get /couchdb/user)
  export COUCHDB_PASSWORD=$(etcdctl get /passwords/couchdb)
  export MAP_KEY=$(etcdctl get /passwords/map-key)
  export MAP_PLAYER_URL=$(etcdctl get /player/url)
  export LOGSTASH_ENDPOINT=$(etcdctl get /logstash/endpoint)
  export LOGMET_HOST=$(etcdctl get /logmet/host)
  export LOGMET_PORT=$(etcdctl get /logmet/port)
  export LOGMET_TENANT=$(etcdctl get /logmet/tenant)
  export LOGMET_PWD=$(etcdctl get /logmet/pwd)
  export SYSTEM_ID=$(etcdctl get /player/system_id)
  export SWEEP_ID=$(etcdctl get /npc/sweep/id)
  export SWEEP_SECRET=$(etcdctl get /npc/sweep/password)
  
  # Softlayer needs a logstash endpoint so we set up the server
  # to run in the background and the primary task is running the
  # forwarder. In ICS, Liberty is the primary task so we need to
  # run it in the foreground
  if [ "$LOGSTASH_ENDPOINT" != "" ]; then
    /opt/ibm/wlp/bin/server start $SERVERDIRNAME
    echo Starting the logstash forwarder...
    sed -i s/PLACEHOLDER_LOGHOST/${LOGSTASH_ENDPOINT}/g /opt/forwarder.conf
    cd /opt
    chmod +x ./forwarder
    etcdctl get /logstash/cert > logstash-forwarder.crt
    etcdctl get /logstash/key > logstash-forwarder.key
    sleep 0.5
    ./forwarder --config ./forwarder.conf
  else
    /opt/ibm/wlp/bin/server run $SERVERDIRNAME
  fi
else
  # LOCAL DEVELOPMENT!
  # We do not want to ruin the cloudant admin party, but our code is written to expect
  # that creds are required, so we should make sure the required user/password exist

  AUTH_HOST="http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@couchdb:5984"
  SERVER_PATH=/opt/ibm/wlp/usr/servers/$SERVERDIRNAME

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
      echo "Updating firstroom.json ${SERVER_PATH}/firstRoom.json"
      sed "s/game-on.org/${SYSTEM_ID}/g" ${SERVER_PATH}/firstRoom.json > ${SERVER_PATH}/firstRoom.withid.json
      echo "Adding firstroom to db"
      curl -X POST -H "Content-Type: application/json" --data @${SERVER_PATH}/firstRoom.withid.json ${AUTH_HOST}/map_repository
  fi

  exec /opt/ibm/wlp/bin/server run $SERVERDIRNAME
fi
