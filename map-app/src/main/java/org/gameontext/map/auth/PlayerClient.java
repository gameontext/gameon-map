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
package org.gameontext.map.auth;

import java.io.FileInputStream;
import java.io.IOException;
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

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.SSLContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.gameontext.map.Log;
import org.gameontext.signed.SignedRequestSecretProvider;
import org.gameontext.signed.TimestampedKey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * Obtain the key we'll use to sign the jwts we use to talk to Player endpoints.
     *
     * @throws IOException
     *             if there are any issues with the keystore processing.
     */
    private synchronized void getKeyStoreInfo() {
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
    private String getClientJwtForId(String playerId) throws IOException{
        // grab the key if needed
        if (signingKey == null)
            getKeyStoreInfo();

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
            playerSecret = getPlayerSecret(id);
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
     * Obtain sharedSecret for player id.
     *
     * @param playerId
     *            The player id
     * @return The apiKey for the player
     */
    private String getPlayerSecret(String playerId) throws WebApplicationException {

        try {
            String jwt = getClientJwtForId(playerId);

            HttpClient client = null;
            if("development".equals(System.getenv("MAP_PLAYER_MODE"))){
                System.out.println("Using development mode player connection. (DefaultSSL,NoHostNameValidation)");
                HttpClientBuilder b = HttpClientBuilder.create();

                //use the default ssl context, we have a trust store configured for player cert.
                SSLContext sslContext = SSLContext.getDefault();

                //use a very trusting truststore.. (not needed..)
                //SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();

                b.setSSLContext( sslContext);

                //disable hostname validation, because we'll need to access the cert via a different hostname.
                b.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);

                client = b.build();
            }else{
                client = HttpClientBuilder.create().build();
            }

            HttpGet hg = new HttpGet(playerLocation+"/"+playerId);
            hg.addHeader("gameon-jwt", jwt);

            Log.log(Level.FINEST, this, "Building web target: {0}", hg.getURI().toString());

            // Make GET request using the specified target, get result as a
            // string containing JSON
            HttpResponse r = client.execute(hg);
            String result = new BasicResponseHandler().handleResponse(r);

            // Parse the JSON response, and retrieve the apiKey field value.
            ObjectMapper om = new ObjectMapper();
            JsonNode jn = om.readValue(result,JsonNode.class);

            Log.log(Level.FINER, this, "Got player record for {0} from player service", playerId);

            JsonNode creds = jn.get("credentials").get("sharedSecret");
            return creds.textValue();

        } catch (HttpResponseException hre) {
            Log.log(Level.FINEST, this, "Error communicating with player service: {0} {1}", hre.getStatusCode(), hre.getMessage());
            throw new WebApplicationException("Error communicating with Player service", Response.Status.INTERNAL_SERVER_ERROR);
        } catch ( IOException | NoSuchAlgorithmException e ) {
            Log.log(Level.FINEST, this, "Unexpected exception getting secret from playerService: {0}", e);
            throw new WebApplicationException("Error communicating with Player service", Response.Status.INTERNAL_SERVER_ERROR);
        } catch (WebApplicationException wae) {
            Log.log(Level.FINEST, this, "Error processing response: {0}", wae.getResponse());
            throw wae;
        }
    }

}
