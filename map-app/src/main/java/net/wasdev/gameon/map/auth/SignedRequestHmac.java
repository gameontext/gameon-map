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
package net.wasdev.gameon.map.auth;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

public class SignedRequestHmac {
    static final String UTF8 = "UTF-8";

    static final String HMAC_ALGORITHM = "HmacSHA256";
    static final String SHA_256 = "SHA-256";
    static final String GAMEON_PFX = "gameon-";
    static final String GAMEON_ID = "gameon-id";
    static final String GAMEON_DATE = "gameon-date";
    static final String GAMEON_HEADERS = "gameon-sig-headers";
    static final String GAMEON_PARAMETERS = "gameon-sig-parameters";
    static final String GAMEON_SIG_BODY = "gameon-sig-body";
    static final String GAMEON_SIGNATURE = "gameon-signature";

    /** expiry time for requests in ms */
    static final Duration EXPIRES_REQUEST_MS = Duration.ofMinutes(5);

    /** how long to retain replays for, must be longer than the valid request period */
    static final Duration EXPIRES_REPLAY_MS = EXPIRES_REQUEST_MS .plus(Duration.ofMinutes(1));

    protected final String userId;
    protected final String dateString;
    protected final String method;
    protected final String baseUri;

    protected String hmacHeader;
    protected String bodyHashHeader;
    protected String sigHeadersHeader;
    protected String sigParamsHeader;

    protected String body = "";
    protected byte[] bodyBytes = null;
    protected String secret = null;
    protected boolean requestHasBody = false;

    MultivaluedMap<String, String> headers = null;
    MultivaluedMap<String, String> parameters = null;

    // Temporary: cope with sigs that don't have method/uri path in them
    boolean oldStyle = true;

    /**
     * Validation of an incoming client request. This is created in the
     * {@link SignedRequestFilter}, when we still have easy access to
     * the request context. Signature validation is done in two steps:
     * a pre-check, which can always be done in the filter, and then
     * the extra validation that includes a hash of the body. If the
     * message has a body, validation should be postponed to the
     * interceptor.
     *
     * @param context
     * @see #precheck(PlayerClient)
     * @see #validate()
     * @see #readRequestBody(InputStream)
     * @see #getBodyInputStream()
     * @see SignedRequestFilter
     * @see SignedRequestInterceptor
     */
    public SignedRequestHmac(ContainerRequestContext context) {
        this.userId = context.getHeaderString(GAMEON_ID);
        this.dateString = context.getHeaderString(GAMEON_DATE);
        this.hmacHeader = context.getHeaderString(GAMEON_SIGNATURE);
        this.sigHeadersHeader = context.getHeaderString(GAMEON_HEADERS);
        this.sigParamsHeader = context.getHeaderString(GAMEON_PARAMETERS);
        this.bodyHashHeader = context.getHeaderString(GAMEON_SIG_BODY);
        this.requestHasBody = context.getLength() >= 0;
        this.method = context.getMethod();
        this.baseUri = context.getUriInfo().getAbsolutePath().getPath();

        if ( sigHeadersHeader != null )
            headers = context.getHeaders();
        if ( sigParamsHeader != null )
            parameters = context.getUriInfo().getQueryParameters();
    }

    /**
     * Create a SignedRequestHmac for building/constructing headers
     * for an outbound message.
     *
     * @param userId
     * @param context
     * @see #setRequestBody(byte[])
     * @see #prepareForSigning(PlayerClient, MultivaluedMap)
     * @see #prepareForSigning(PlayerClient, MultivaluedMap, List)
     * @see #prepareForSigning(PlayerClient, MultivaluedMap, List, MultivaluedMap, List)
     * @see #signRequest(MultivaluedMap)
     */
    public SignedRequestHmac(String userId, ClientRequestContext context) {
        this(userId,
             DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()),
             context.getMethod(),
             context.getUri().getPath());
    }

    public SignedRequestHmac(String userId, String dateString, String method, String baseUri) {
        this.userId = userId;
        this.method = method;
        this.baseUri = baseUri;
        this.dateString = dateString;
        System.out.println("HEYLA!! BASE URI = " +  baseUri);
    }

    public String getUserId() {
        return userId;
    }

    public boolean requestBodyRequired() {
        return bodyHashHeader != null;
    }

    public void readRequestBody(InputStream bis) throws IOException, WebApplicationException {
        BufferedReader buffer = new BufferedReader(new InputStreamReader(bis,UTF8));
        this.body = buffer.lines().collect(Collectors.joining("\n"));
    }

    public InputStream getBodyInputStream() throws IOException {
        if ( bodyBytes != null )
            return new ByteArrayInputStream(bodyBytes);

        return new ByteArrayInputStream(body.getBytes(UTF8));
    }

    public void setRequestBody(byte[] data) {
        requestHasBody = true;
        bodyBytes = data;
    }

    /**
     * Sniff test to make sure required attributes are there to perform
     * hmac checking
     *
     * @param playerClient
     * @return WebApplicationException if request fails tests, null if ok
     */
    public void precheck(PlayerClient playerClient) throws WebApplicationException {
        if ( userId == null || userId.isEmpty() )
            throw new WebApplicationException("Request requires a Game On! ID", Status.FORBIDDEN);

        if ( hmacHeader == null || hmacHeader.isEmpty() ) {
            throw new WebApplicationException("Invalid signature (hmac)", Status.FORBIDDEN);
        }

        if ( bodyHashHeader != null && !requestHasBody ) {
            throw new WebApplicationException("Invalid signature (body)", Status.FORBIDDEN);
        }

        try {
            if ( sigHeadersHeader != null && mismatchedHashOfNamedValues(sigHeadersHeader, headers) ) {
                throw new WebApplicationException("Invalid signature (headers)", Status.FORBIDDEN);
            }

            if ( sigParamsHeader != null && mismatchedHashOfNamedValues(sigParamsHeader, parameters) ) {
                throw new WebApplicationException("Invalid signature (parameters)", Status.FORBIDDEN);
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new WebApplicationException("Invalid signature", Status.FORBIDDEN);
        }

        // Get the secret for later.
        secret = playerClient.getSecretForId(userId);
    }

    /**
     * Check signature against recently seen signatures to guard against
     * replay attacks.
     *
     * @param method
     * @param timedCache
     * @return WebApplicationException if request is a duplicate, null if ok
     */
    public void checkDuplicate(String method, SignedRequestTimedCache timedCache) throws WebApplicationException {

        if ( "POST".equals(method) && timedCache.isDuplicate(hmacHeader, EXPIRES_REPLAY_MS) ) {
            throw new WebApplicationException("Duplicate request", Status.FORBIDDEN);
        }

    }

    /**
     * Ensure request has been made within the last 5 minutes UTC
     * @return WebApplicationException if request has expired, null if ok
     */
    public void checkExpiry() throws WebApplicationException {
        Instant now = Instant.now();
        Instant then = parseValue(dateString);

        if (then == null) {
            throw new WebApplicationException("Invalid signature (date)", Status.FORBIDDEN);
        } else if( Duration.between(then,now).compareTo(EXPIRES_REQUEST_MS) > 0) {
            throw new WebApplicationException("Signature expired", Status.FORBIDDEN);
        }
    }

    /**
     * Validate the signature using the previously fetched headers, etc.
     * Return an error response if the hmac isn't valid.
     * @return WebApplicationException if request fails hash validation, null if ok
     */
    public void validate() throws WebApplicationException {
        if ( secret == null || secret.isEmpty() ) {
            throw new WebApplicationException("Invalid or unretrievable shared secret", Status.FORBIDDEN);
        }

        try {
            List<String> stuffToHash = new ArrayList<String>();

            if ( oldStyle ) {
                stuffToHash.add(method); // (1)
                stuffToHash.add(baseUri);// (2)
            }
            stuffToHash.add(userId);     // (3)
            stuffToHash.add(dateString); // (4)
            stuffToHash.add(sigHeadersHeader != null ? sigHeadersHeader : ""); // (5)
            stuffToHash.add(sigParamsHeader != null ? sigParamsHeader : "");   // (6)

            if ( bodyHashHeader != null ) {
                String h_bodyHash = buildHash(body);
                if ( !bodyHashHeader.equals(h_bodyHash) ) {
                    throw new WebApplicationException("Invalid signature (bodyHash)", Status.FORBIDDEN);
                }
                stuffToHash.add(bodyHashHeader); // (7)
            }

            String h_hmac = buildHmac(stuffToHash, secret);
            if ( !hmacHeader.equals(h_hmac) ) {
                throw new WebApplicationException("Invalid signature (hmacCompare)", Status.FORBIDDEN);
            }

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            throw new WebApplicationException("Invalid signature", Status.FORBIDDEN);
        }
    }


    /**
     * Prepare a request for signing, when no extra headers or parameters
     * are required
     * @param playerClient
     * @param headers Message headers, always present
     * @return WebApplicationException if badness ensues, null if all is well
     */
    public void prepareForSigning(PlayerClient playerClient,
                                  MultivaluedMap<String, String> headers) throws WebApplicationException {
        prepareForSigning(playerClient, headers, null, null, null);
    }

    /**
     * Prepare a request for signing when extra headers are required
     * @param playerClient
     * @param headers Message headers, always present
     * @param header_names Names of extra required headers (optional)
     * @return WebApplicationException if badness ensues, null if all is well
     */
    public void prepareForSigning(PlayerClient playerClient,
                                  MultivaluedMap<String, String> headers,
                                  List<String> header_names) throws WebApplicationException{

        prepareForSigning(playerClient, headers, header_names, null, null);
    }

    /**
     * Prepare a request for signing when extra headers and/or parameters are required
     * @param playerClient
     * @param headers Message headers, always present
     * @param header_names Names of extra required headers (optional)
     * @param parameters Query parameters (optional)
     * @param parameter_names Names of extra required parameters (optional)
     * @return WebApplicationException if badness ensues, null if all is well
     */
    public void prepareForSigning(PlayerClient playerClient,
                                  MultivaluedMap<String, String> headers,
                                  List<String> header_names,
                                  MultivaluedMap<String, String> parameters,
                                  List<String> parameter_names) throws WebApplicationException {

        // Get the secret for later.
        secret = playerClient.getSecretForId(userId);

        try {
            if ( header_names != null && !header_names.isEmpty() ) {
                sigHeadersHeader = hashOfNamedValues(header_names, headers);
                headers.add(GAMEON_HEADERS, sigHeadersHeader);
            }

            if ( parameter_names != null && !parameter_names.isEmpty() ) {
                sigParamsHeader = hashOfNamedValues(parameter_names, parameters);
                headers.add(GAMEON_PARAMETERS, sigParamsHeader);
            }

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            // this is our fault.
            throw new WebApplicationException("Unable to generate signature", Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate the HMAC signature for the request, including the message body
     * @param headers Message headers, always present
     * @return WebApplicationException if badness ensues, null if all is well
     */
    public void signRequest(MultivaluedMap<String, String> headers) throws WebApplicationException {
        if ( userId == null || userId.isEmpty() )
            throw new WebApplicationException("Request requires a Game On! ID", Status.INTERNAL_SERVER_ERROR);

        if ( secret == null || secret.isEmpty() )
            throw new WebApplicationException("Request requires a Secret", Status.INTERNAL_SERVER_ERROR);


        List<String> stuffToHash = new ArrayList<String>();

        stuffToHash.add(method);        // (1)
        stuffToHash.add(baseUri);       // (2)

        headers.add(GAMEON_ID, userId);
        stuffToHash.add(userId);        // (3)

        headers.add(GAMEON_DATE, dateString);
        stuffToHash.add(dateString);    // (4)

        stuffToHash.add(sigHeadersHeader == null ? "" : sigHeadersHeader); // (5)
        stuffToHash.add(sigParamsHeader == null ? "" : sigParamsHeader);   // (6)

        try {
            //hash the body
            if ( requestHasBody ) {
                if ( bodyBytes == null && body != null) {
                    bodyBytes = body.getBytes(UTF8);
                }
                bodyHashHeader = buildHash(bodyBytes);
                headers.add(GAMEON_SIG_BODY, bodyHashHeader);
                stuffToHash.add(bodyHashHeader);                 // (7)
            }

            // Finally, the HMAC!
            hmacHeader = buildHmac(stuffToHash, secret);
            headers.add(GAMEON_SIGNATURE, hmacHeader);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            // this is our fault.
            throw new WebApplicationException("Unable to generate signature", Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected String hashOfNamedValues(List<String> names, MultivaluedMap<String, String> map)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        List<String> values = new ArrayList<String>();

        // Make a list of values for named elements
        // concatenate multiple values with no spaces
        Iterator<String> i = names.iterator();
        while ( i.hasNext()) {
            String key = i.next();
            if ( key.startsWith(GAMEON_PFX) ) {
                i.remove();
                continue;
            }

            List<String> keyValues = map.get(key);
            if ( keyValues != null )
                values.add(String.join("", keyValues));
        }

        // Create a hash of the gathered values
        String hash = buildHash(values);
        // Set the header to the list of names and the hash of their values
        return String.join(";", names) + ";" + hash;
    }

    protected boolean mismatchedHashOfNamedValues(String header, MultivaluedMap<String, String> map)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        int rpos = header.lastIndexOf(';');
        String oldHash = header.substring(rpos+1);
        List<String> names = Arrays.asList(header.substring(0, rpos).split(";"));

        List<String> values = new ArrayList<String>();
        for(String key : names ) {
            List<String> keyValues = map.get(key);
            if ( keyValues != null )
                values.add(String.join("", keyValues));
        }
        String newHash = buildHash(values);

        return !oldHash.equals(newHash);
    }

    protected String buildHmac(List<String> stuffToHash, String key)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key.getBytes(UTF8), HMAC_ALGORITHM));

        if ( oldStyle ) {
            StringBuilder hashData = new StringBuilder();
            for(String s: stuffToHash){
                hashData.append(s);
            }
            mac.update(hashData.toString().getBytes(UTF8));
        } else {
            for(String s: stuffToHash){
                mac.update(s.getBytes(UTF8));
            }
        }

        return Base64.getEncoder().encodeToString( mac.doFinal() );
    }

    protected String buildHash(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        return buildHash(data.getBytes(UTF8));
    }

    protected String buildHash(byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        MessageDigest md = MessageDigest.getInstance(SHA_256);
        md.update(data);
        byte[] digest = md.digest();
        return Base64.getEncoder().encodeToString( digest );
    }

    protected String buildHash(List<String> values) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        MessageDigest md = MessageDigest.getInstance(SHA_256);
        for( String value : values ) {
            md.update(value.getBytes(UTF8));
        }
        return Base64.getEncoder().encodeToString( md.digest() );
    }

    private Instant parseValue(String dateString) {
        try {
            ZonedDateTime then = ZonedDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME);
            return then.toInstant();
        } catch(DateTimeParseException e) {
            try {
                Instant then = Instant.parse(dateString);
                oldStyle = false; // TEMPORARY: skip method and URI when parsing signature
                return then;
            } catch(DateTimeParseException ne) {
                return null;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SignedRequestHmac [userId=").append(userId)
            .append(", dateString=").append(dateString)
            .append(", method=").append(method)
            .append(", baseUri=").append(baseUri)
            .append(", hmacHeader=").append(hmacHeader)
            .append(", bodyHashHeader=").append(bodyHashHeader)
            .append(", sigHeadersHeader=").append(sigHeadersHeader)
            .append(", sigParamsHeader=").append(sigParamsHeader)
            .append(", body=").append(body)
            .append(", bodyBytes=").append(Arrays.toString(bodyBytes))
            .append(", requestHasBody=").append(requestHasBody)
            .append(", headers=").append(headers)
            .append(", parameters=").append(parameters)
            .append(", oldStyle=").append(oldStyle).append("]");
        return builder.toString();
    }


}
