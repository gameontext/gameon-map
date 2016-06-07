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
package org.gameontext.signed;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.gameontext.signed.SignedRequestMap.MVSO_StringMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class SignedRequestHmacTest {
    static final String id = "MyUserId";
    static final String secret = "fish";

    MultivaluedHashMap<String, Object> internalMap = new MultivaluedHashMap<>();
    MVSO_StringMap headers = new MVSO_StringMap(internalMap);

    String dateString = "Sat, 21 May 2016 19:14:54 GMT";

    @Rule
    public TestName test = new TestName();

    @Before
    public void before() {
        System.out.println("\n====== " + test.getMethodName());
    }

    @Test
    public void testSignRequestNoBody() {

        // GET /map/v1/sites/aRoomId HTTP/1.1
        // gameon-id: MyUserId
        // gameon-date: Sat, 21 May 2016 19:14:54 GMT
        // gameon-signature: mYsWeiZm9oyUmJXo1uCwq1AHoHSm5eLrblU9q35EjOU=

        String method = "GET";
        String path = "/map/v1/sites/aRoomId";

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, method, path)
                .setDate(dateString);

        System.out.println(clientHmac);
        clientHmac.signRequest(headers);
        printMessage(clientHmac);

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_SIGNATURE
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));
        Assert.assertEquals("mYsWeiZm9oyUmJXo1uCwq1AHoHSm5eLrblU9q35EjOU=",
                headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));

        // --- now validate the hmac signature ----

        SignedRequestHmac serverHmac = new SignedRequestHmac(id, secret, method, path)
                .checkHeaders(headers)
                .verifyRequestHeaderHashes(headers, null)
                .verifyFullSignature();

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }
    }

    @Test
    public void testSignRequestBody() throws Exception {

        // POST /map/v1/sites HTTP/1.1
        // gameon-id: MyUserId
        // gameon-date: Sat, 21 May 2016 19:14:54 GMT
        // gameon-sig-body: AWRN0wv343B7k7Ucp1sipeM2U9hZLVlMzPNA6uUiyug=
        // gameon-signature: jblpGaN8bjd4SmhsK341EP1x7e2w8sZ3L1T64YB+mrQ=
        // Content-Type: application/json
        // Content-Length: 12
        //
        // {id: 'test'}

        String method = "POST";
        String path = "/map/v1/sites";
        byte[] content = "{id: 'test'}".getBytes(SignedRequestHmac.UTF8);

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, method, path)
                .setDate(dateString)
                .generateBodyHash(headers, content);

        System.out.println(clientHmac);
        clientHmac.signRequest(headers);
        printMessage(clientHmac);

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_SIGNATURE,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_PARAMETERS
                              ));

        Assert.assertEquals("AWRN0wv343B7k7Ucp1sipeM2U9hZLVlMzPNA6uUiyug=",
                headers.getFirst(SignedRequestHmac.GAMEON_SIG_BODY));

        Assert.assertEquals("jblpGaN8bjd4SmhsK341EP1x7e2w8sZ3L1T64YB+mrQ=",
                headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));

        // --- now validate the hmac signature ----

        SignedRequestHmac serverHmac = new SignedRequestHmac(id, secret, method, path)
                .checkHeaders(headers)
                .verifyRequestHeaderHashes(headers, null)
                .verifyBodyHash(content)
                .verifyFullSignature();

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }
     }

    @Test
    public void testSignRequestHeaders() throws Exception {

        // POST /map/v1/sites HTTP/1.1
        // gameon-id: MyUserId
        // gameon-date: Sat, 21 May 2016 19:14:54 GMT
        // gameon-sig-headers: Content-Type;Content-Length;47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=
        // gameon-sig-body: AWRN0wv343B7k7Ucp1sipeM2U9hZLVlMzPNA6uUiyug=
        // gameon-signature: 3E3+YFH6qd30WlujaOellykNWxH0AOMecFvuHyYV42k=
        // Content-Type: application/json
        // Content-Length: 12
        //
        // {id: 'test'}

        String method = "POST";
        String path = "/map/v1/sites";
        byte[] content = "{id: 'test'}".getBytes(SignedRequestHmac.UTF8);

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, method, path)
                .setDate(dateString)
                .generateRequestHeaderHashes(headers, Arrays.asList("Content-Type", "Content-Length"), null, null)
                .generateBodyHash(headers, content);

        System.out.println(clientHmac);
        clientHmac.signRequest(headers);
        printMessage(clientHmac);

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_SIGNATURE,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_PARAMETERS
                              ));

        Assert.assertEquals("AWRN0wv343B7k7Ucp1sipeM2U9hZLVlMzPNA6uUiyug=",
                headers.getFirst(SignedRequestHmac.GAMEON_SIG_BODY));

        Assert.assertEquals("3E3+YFH6qd30WlujaOellykNWxH0AOMecFvuHyYV42k=",
                headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));

        // --- now validate the hmac signature ----

        SignedRequestHmac serverHmac = new SignedRequestHmac(id, secret, method, path)
                .checkHeaders(headers)
                .verifyRequestHeaderHashes(headers, null)
                .verifyBodyHash(content)
                .verifyFullSignature();

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }
    }

    @Test
    public void testSignRequestParameters() throws Exception {
        // GET /map/v1/sites?owner=MyUserId HTTP/1.1
        // gameon-id: MyUserId
        // gameon-date: Sat, 21 May 2016 19:14:54 GMT
        // gameon-sig-params: owner;HkP19XXoI90rtg6yWMTACQ20rWZQhbGmgFDMjHSU2qg=
        // gameon-signature: bb0otJw4jDitSf7DXNWMjQEwsoaZqjXlSrE8Wkvkf6s=

        String method = "GET";
        String path = "/map/v1/sites";
        String pString = "owner=MyUserId";
        SignedRequestMap parameters = new SignedRequestMap.QueryParameterMap(pString);


        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, method, path)
                .setDate(dateString)
                .generateRequestHeaderHashes(headers, null, parameters, Arrays.asList("owner"));

        System.out.println(clientHmac);
        clientHmac.signRequest(headers);
        printMessage(clientHmac, "?owner=MyUserId");

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_SIGNATURE
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));


        // --- now validate the hmac signature ----

        SignedRequestHmac serverHmac = new SignedRequestHmac(id, secret, method, path)
                .checkHeaders(headers)
                .verifyRequestHeaderHashes(headers, parameters)
                .verifyBodyHash(null)
                .verifyFullSignature();

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }
    }

    @Test
    public void testOldHmac() throws Exception {

        String method = "GET";
        String path = "/map/v1/sites";

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, method, path)
                .setOldStyleDate(Instant.now());

        System.out.println(clientHmac);
        clientHmac.signRequest(headers);
        printMessage(clientHmac);

        new SignedRequestHmac(id, secret, method, path)
                .checkHeaders(headers)
                .checkExpiry()
                .verifyBodyHash(null)
                .verifyFullSignature();
    }

    @Test
    public void testWebSocket() {

        MultivaluedMap<String, Object> m2 = new MultivaluedHashMap<>();
        SignedRequestMap map = new SignedRequestMap.MVSO_StringMap(m2);

        System.out.println("Create and sign client request");
        // WS client: baseUri, date --> signature
        SignedRequestHmac clientHmac = new SignedRequestHmac("", secret, "", "/ws/uri");

        clientHmac.signRequest(headers);
        System.out.println("client="+clientHmac);

        System.out.println("\n==== Create server hmac and verify signature, client message: ");
        printMessage(clientHmac);

        // WS server verify: baseUri, date --> signature (verify)
        SignedRequestHmac serverHmac = new SignedRequestHmac("", secret, "", "/ws/uri")
                .checkHeaders(headers);

        serverHmac.verifyFullSignature();
        System.out.println("server="+serverHmac);

        System.out.println("\n=== Resign outbound response, headers to resign");
        printMessage(serverHmac);

        // WS server sign: received signature, new date --> signing signature
        serverHmac.wsResignRequest(map);
        System.out.println("server="+serverHmac);
        System.out.println(map);

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_SIGNATURE
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));
        Assert.assertEquals(serverHmac.dateString, map.getAll(SignedRequestHmac.GAMEON_DATE, ""));
        Assert.assertEquals(serverHmac.signature, map.getAll(SignedRequestHmac.GAMEON_SIGNATURE, ""));

        System.out.println("\n=== Client verify resigned signature, server message: ");
        printMessage(serverHmac);

        System.out.println("server="+serverHmac);
        System.out.println("client="+clientHmac);
        clientHmac.wsVerifySignature(map);
    }

    @Test
    public void testOldWebSocket() {

        MultivaluedMap<String, Object> m2 = new MultivaluedHashMap<>();
        SignedRequestMap map = new SignedRequestMap.MVSO_StringMap(m2);

        // WS client: date, baseUri --> signature
        SignedRequestHmac clientHmac = new SignedRequestHmac("", secret, "", "/ws/uri")
                .setOldStyleDate(Instant.now());

        System.out.println(clientHmac);
        clientHmac.signRequest(headers);
        printMessage(clientHmac);

        // WS server verify: date, baseUri --> signature (verify)
        SignedRequestHmac serverHmac = new SignedRequestHmac("", secret, "", "/ws/uri")
                .checkHeaders(headers);

        System.out.println(serverHmac);
        serverHmac.verifyFullSignature();
        printMessage(serverHmac);

        // WS server sign: received signature, new date --> signing signature
        serverHmac.wsResignRequest(map);
        printMessage(serverHmac);

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_SIGNATURE
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));
    }

    @Test
    public void testFilterNoId() {

        // GET /map/v1/sites/aRoomId HTTP/1.1
        String method = "GET";
        String path = "/map/v1/sites/aRoomId";

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(null, secret, method, path)
                .setDate(dateString);

        System.out.println(clientHmac);
        clientHmac.signRequest(headers);
        printMessage(clientHmac);

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_SIGNATURE
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));
        Assert.assertEquals("SD8LztqvmB3xcNlV3/U//4n4qLMT7xJeHgyCrEr5qg8=",
                headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));

        // --- now validate the hmac signature ----

        SignedRequestHmac serverHmac = new SignedRequestHmac(null, secret, method, path)
                .checkHeaders(headers)
                .verifyRequestHeaderHashes(headers, null)
                .verifyFullSignature();

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }
    }

    void assertHeaders(List<String> set, List<String> unset) {
        for(String key : set) {
            Assert.assertNotNull(key  + " should be set : " + headers.getAll(key, ""), headers.getAll(key, null));
        }
        for(String key : unset) {
            Assert.assertNull(key  + " should not be set : " + headers.getAll(key, ""), headers.getAll(key, null));
        }
    }

    void printMessage(SignedRequestHmac hmac) {
        printMessage(hmac,"", null);
    }

    void printMessage(SignedRequestHmac hmac, String query) {
        printMessage(hmac, query, null);
    }

    void printMessage(SignedRequestHmac hmac, String query, byte[] body) {
        System.out.println("-----------------");
        System.out.format("%s %s%s HTTP/1.1\n", hmac.method, hmac.baseUri, query);
        if ( !hmac.userId.isEmpty() )
            System.out.println("gameon-id: " + hmac.userId);
        System.out.println("gameon-date: " + hmac.dateString);
        if ( hmac.sigHeaders != null && !hmac.sigHeaders.isEmpty())
            System.out.println("gameon-sig-headers: " + hmac.sigHeaders);
        if ( hmac.sigParameters != null && !hmac.sigParameters.isEmpty() )
            System.out.println("gameon-sig-params: " + hmac.sigParameters);
        if ( hmac.sigBody != null && !hmac.sigBody.isEmpty() )
            System.out.println("gameon-sig-body: " + hmac.sigBody);
        System.out.println("gameon-signature: " + hmac.signature);
        if ( body != null ) {
            System.out.println("Content-Type: application/json");
            System.out.println("Content-Length: " + body.length);
            System.out.println();
            System.out.println((new String(body)));
        }
        System.out.println("-----------------");
    }
}
