#!/bin/bash

# Configure our link to etcd based on shared volume with secret
if [ ! -z "$ETCD_SECRET" ]; then
  . /data/primordial/setup.etcd.sh /data/primordial $ETCD_SECRET
fi

export CONTAINER_NAME=map

SERVER_PATH=/opt/ibm/wlp/usr/servers/defaultServer

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

  mkdir -p ${SERVER_PATH}/resources/security
  cd ${SERVER_PATH}/resources/
  etcdctl get /proxy/third-party-ssl-cert > cert.pem
  openssl pkcs12 -passin pass:keystore -passout pass:keystore -export -out cert.pkcs12 -in cert.pem
  keytool -import -v -trustcacerts -alias default -file cert.pem -storepass truststore -keypass keystore -noprompt -keystore security/truststore.jks
  keytool -genkey -storepass testOnlyKeystore -keypass wefwef -keyalg RSA -alias endeca -keystore security/key.jks -dname CN=rsssl,OU=unknown,O=unknown,L=unknown,ST=unknown,C=CA
  keytool -delete -storepass testOnlyKeystore -alias endeca -keystore security/key.jks
  keytool -v -importkeystore -srcalias 1 -alias 1 -destalias default -noprompt -srcstorepass keystore -deststorepass testOnlyKeystore -srckeypass keystore -destkeypass testOnlyKeystore -srckeystore cert.pkcs12 -srcstoretype PKCS12 -destkeystore security/key.jks -deststoretype JKS
  cd ${SERVER_PATH}

  export COUCHDB_SERVICE_URL=$(etcdctl get /couchdb/url)
  export COUCHDB_USER=$(etcdctl get /couchdb/user)
  export COUCHDB_PASSWORD=$(etcdctl get /passwords/couchdb)

  export MAP_KEY=$(etcdctl get /passwords/map-key)

  export PLAYER_SERVICE_URL=$(etcdctl get /player/url)

  export LOGMET_HOST=$(etcdctl get /logmet/host)
  export LOGMET_PORT=$(etcdctl get /logmet/port)
  export LOGMET_TENANT=$(etcdctl get /logmet/tenant)
  export LOGMET_PWD=$(etcdctl get /logmet/pwd)

  export SYSTEM_ID=$(etcdctl get /global/system_id)
  export SWEEP_ID=$(etcdctl get /npc/sweep/id)
  export SWEEP_SECRET=$(etcdctl get /npc/sweep/password)

  GAMEON_MODE=$(etcdctl get /global/mode)
  export GAMEON_MODE=${GAMEON_MODE:-production}
  export TARGET_PLATFORM=$(etcdctl get /global/targetPlatform)

  export KAFKA_SERVICE_URL=$(etcdctl get /kafka/url)

  #to run with message hub, we need a jaas jar we can only obtain
  #from github, and have to use an extra config snippet to enable it.
  export MESSAGEHUB_USER=$(etcdctl get /kafka/user)
  export MESSAGEHUB_PASSWORD=$(etcdctl get /passwords/kafka)
  wget https://github.com/ibm-messaging/message-hub-samples/raw/master/java/message-hub-liberty-sample/lib-message-hub/messagehub.login-1.0.0.jar

else
  if [ "$GAMEON_MODE" == "development" ]; then
    # LOCAL DEVELOPMENT!
    # We do not want to ruin the cloudant admin party, but our code is written to expect
    # that creds are required, so we should make sure the required user/password exist

    AUTH_HOST="http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@couchdb:5984"

    echo "** Testing connection to ${COUCHDB_SERVICE_URL}"
    curl --fail -X GET ${AUTH_HOST}/_config/admins/${COUCHDB_USER}
    RC=$?

    # RC=7 means the host isn't there yet. Let's do some re-trying until it
    # does start / is ready
    while [ $RC -eq 7 ]; do
      sleep 15

      # recheck condition
      echo "** Re-testing connection to ${COUCHDB_SERVICE_URL}"
      curl --fail -X GET ${AUTH_HOST}/_config/admins/${COUCHDB_USER}
      RC=$?
    done

    # RC=22 means the user doesn't exist
    if [ $RC -eq 22 ]; then
      echo "** Creating ${COUCHDB_USER}"
      curl -X PUT ${COUCHDB_SERVICE_URL}/_config/admins/${COUCHDB_USER} -d \"${COUCHDB_PASSWORD}\"
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
  fi
fi

# Start the server in the foreground
exec /opt/ibm/wlp/bin/server run defaultServer
