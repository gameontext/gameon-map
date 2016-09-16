/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.map.clients;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gameontext.map.JsonProvider;
import org.gameontext.map.Log;
import org.gameontext.signed.SignedRequestSecretProvider;
import org.gameontext.signed.TimestampedKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * A wrapped/encapsulation of outbound REST requests to the player service.
 * <p>
 * The URL for the player service is injected via CDI: {@code <jndiEntry />}
 * elements defined in server.xml maps the environment variable to the JNDI
 * value.
 * </p>
 * <p>
 * CDI will create this (the {@code PlayerClient} as an application scoped bean.
 * This bean will be created when the application starts, and can be injected
 * into other CDI-managed beans for as long as the application is valid.
 * </p>
 *
 * @see ApplicationScoped
 */
@ApplicationScoped
public class PlayerClient implements SignedRequestSecretProvider {

    private static final Duration hours24 = Duration.ofHours(24);

    /** The Key to Sign JWT's with (once it's loaded) */
    private Key signingKey = null;

    /** Cache of player API keys */
    private ConcurrentMap<String,TimestampedKey> playerSecrets = new ConcurrentHashMap<>();

    /**
     * The player URL injected from JNDI via CDI.
     *
     * @see {@code playerUrl} in
     *      {@code /map-wlpcfg/servers/gameon-map/server.xml}
     */
    @Resource(lookup = "playerUrl")
    String playerLocation;

    // Keystore info for jwt parsing / creation.
    @Resource(lookup = "jwtKeyStore")
    String keyStore;

    @Resource(lookup = "jwtKeyStorePassword")
    String keyStorePW;

    @Resource(lookup = "jwtKeyStoreAlias")
    String keyStoreAlias;

    @Resource(lookup="registrationSecret")
    String registrationSecret;

    @Resource(lookup="systemId")
    String SYSTEM_ID;

    @Resource(lookup="sweepId")
    String sweepId;

    @Resource(lookup="sweepSecret")
    String sweepSecret;

    /**
     * The root target used to define the root path and common query parameters
     * for all outbound requests to the player service.
     *
     * @see WebTarget
     */
    WebTarget root;


    /**
     * The {@code @PostConstruct} annotation indicates that this method should
     * be called immediately after the {@code PlayerClient} is instantiated
     * with the default no-argument constructor.
     *
     * @see PostConstruct
     * @see ApplicationScoped
     */
    @PostConstruct
    public void initClient() {

        if ( playerLocation == null ) {
            Log.log(Level.SEVERE, this, "Player client can not be initialized, 'playerUrl' is not defined");
            throw new IllegalStateException("Unable to initialize PlayerClient");
        }

        try {
            // load up the keystore..
            FileInputStream is = new FileInputStream(keyStore);
            KeyStore signingKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            signingKeystore.load(is, keyStorePW.toCharArray());

            // grab the key we'll use to sign
            signingKey = signingKeystore.getKey(keyStoreAlias, keyStorePW.toCharArray());
        } catch (KeyStoreException |
                NoSuchAlgorithmException |
                CertificateException |
                UnrecoverableKeyException |
                IOException e) {
            throw new IllegalStateException("Unable to get required keystore: " + keyStore, e);
        }

        Client client = ClientBuilder.newBuilder()
                                     .property("com.ibm.ws.jaxrs.client.ssl.config", "DefaultSSLSettings")
                                     .property("com.ibm.ws.jaxrs.client.disableCNCheck", true)
                                     .build();

        client.register(JsonProvider.class);

        this.root = client.target(playerLocation);

        Log.log(Level.FINER, this, "Player client initialized with {0}", playerLocation);
    }

    /**
     * Obtain a JWT for the player id that can be used to invoke player REST services.
     *
     * We can create this, because we have access to the private certificate
     * required to sign such a JWT.
     *
     * @param playerId The id to build the JWT for
     * @return The JWT as a string.
     * @throws IOException
     */
    private String getClientJwtForId(String playerId) {

        Claims onwardsClaims = Jwts.claims();

        // Set the subject using the "id" field from our claims map.
        onwardsClaims.setSubject(playerId);

        // We'll use this claim to know this is a user token
        onwardsClaims.setAudience("client");

        // we set creation time to 24hrs ago, to avoid timezone issues
        // client JWT has 24 hrs validity from now.
        Instant timePoint = Instant.now();
        onwardsClaims.setIssuedAt(Date.from(timePoint.minus(hours24)));
        onwardsClaims.setExpiration(Date.from(timePoint.plus(hours24)));

        // finally build the new jwt, using the claims we just built, signing it
        // with our signing key, and adding a key hint as kid to the encryption
        // header, which is optional, but can be used by the receivers of the
        // jwt to know which key they should verifiy it with.
        String newJwt = Jwts.builder()
                .setHeaderParam("kid", "playerssl")
                .setClaims(onwardsClaims)
                .signWith(SignatureAlgorithm.RS256, signingKey)
                .compact();


        return newJwt;
    }

    /**
     * Obtain the apiKey for the given id, using a local cache to avoid hitting couchdb too much.
     */
    @Override
    public String getSecretForId(String id) {
        //first.. handle our built-in key
        if (SYSTEM_ID.equals(id)) {
            return registrationSecret;
        } else if (sweepId.equals(id)) {
            return sweepSecret;
        }

        // TODO: hystrix around player.

        String playerSecret = null;

        TimestampedKey timedKey =  playerSecrets.get(id);
        if ( timedKey != null ) {
            playerSecret = timedKey.getKey();
            if ( !timedKey.hasExpired() ) {
                // CACHED VALUE! the id has been seen, and hasn't expired. Shortcut!
                Log.log(Level.FINER,"Map using cached key for {0}",id);
                return playerSecret;
            }
        }

        TimestampedKey newKey = new TimestampedKey(hours24);
        Log.log(Level.FINER,"Map asking player service for key for id {0}",id);

        try {
            playerSecret = getPlayerSecret(id, getClientJwtForId(id));
            newKey.setKey(playerSecret);
        } catch (WebApplicationException e) {
            if ( playerSecret != null ) {
                // we have a stale value, return it
                return playerSecret;
            }

            // no dice at all, rethrow
            throw e;
        }

        // replace expired timedKey with newKey always.
        playerSecrets.put(id, newKey);

        // return fetched playerSecret
        return playerSecret;
    }

    /**
     * Get shared secret for player
     * @param playerId
     * @param jwt
     * @param oldRoomId
     * @param newRoomId
     * @return
     */
    public String getPlayerSecret(String playerId, String jwt) {
        WebTarget target = this.root.path("{playerId}").resolveTemplate("playerId", playerId);

        Log.log(Level.FINER, this, "requesting shared secret using {0}", target.getUri().toString());

        try {
            // Make PUT request using the specified target, get result as a
            // string containing JSON
            Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
            builder.header("Content-type", "application/json");
            builder.header("gameon-jwt", jwt);
            String result = builder.get(String.class);

            JsonReader p = Json.createReader(new StringReader(result));
            JsonObject j = p.readObject();
            JsonObject creds = j.getJsonObject("credentials");
            return creds.getString("sharedSecret");

        } catch (ResponseProcessingException rpe) {
            Response response = rpe.getResponse();
            Log.log(Level.FINER, this, "Exception obtaining shared secret for player,  uri: {0} resp code: {1} data: {2}",
                    target.getUri().toString(),
                    response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase(),
                    response.readEntity(String.class));

            Log.log(Level.FINEST, this, "Exception obtaining shared secret for player", rpe);
        } catch (ProcessingException | WebApplicationException ex) {
            Log.log(Level.FINEST, this, "Exception obtaining shared secret for player (" + target.getUri().toString() + ")", ex);
        }

        // Sadly, badness happened while trying to get the shared secret
        return null;
    }

}
