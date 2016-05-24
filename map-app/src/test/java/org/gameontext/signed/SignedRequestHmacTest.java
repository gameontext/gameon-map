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
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import mockit.Expectations;
import mockit.Mocked;
import net.wasdev.gameon.map.auth.PlayerClient;

public class SignedRequestHmacTest {
    static final String id = "MyUserId";
    static final String secret = "fish";

    MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
    String dateString = "Sat, 21 May 2016 19:14:54 GMT";

    @Rule
    public TestName test = new TestName();

    @Before
    public void before() {
        System.out.println("\n====== " + test.getMethodName());
    }

    @Test
    public void testSignRequestNoBody(@Mocked ContainerRequestContext containerContext,
                                      @Mocked PlayerClient playerClient,
                                      @Mocked SignedRequestTimedCache timedCache) {

        // GET /map/v1/sites/aRoomId HTTP/1.1
        // gameon-id: MyUserId
        // gameon-date: Sat, 21 May 2016 19:14:54 GMT
        // gameon-signature: mYsWeiZm9oyUmJXo1uCwq1AHoHSm5eLrblU9q35EjOU=

        String method = "GET";
        String path = "/map/v1/sites/aRoomId";

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, dateString, method, path);

        clientHmac.prepareForSigning(secret, headers);
        System.out.println(headers);
        assertHeaders(
                Collections.emptyList(),
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_SIGNATURE,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));

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

        new Expectations() {{
            playerClient.getSecretForId(id); returns(secret);
            containerContext.getMethod(); returns(method);
            containerContext.getUriInfo().getAbsolutePath().getPath(); returns(path);
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_ID); returns(headers.getFirst(SignedRequestHmac.GAMEON_ID));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_DATE); returns(headers.getFirst(SignedRequestHmac.GAMEON_DATE));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIGNATURE); returns(headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));
        }};

        SignedRequestHmac serverHmac = new SignedRequestHmac(containerContext);

        serverHmac.precheck(playerClient);

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }

        serverHmac.validate();
    }

    @Test
    public void testSignRequestBody(@Mocked ContainerRequestContext containerContext,
                                    @Mocked PlayerClient playerClient,
                                    @Mocked SignedRequestTimedCache timedCache) throws Exception {

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
        String content = "{id: 'test'}";

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, dateString, method, path);

        clientHmac.prepareForSigning(secret, headers);
        System.out.println(headers);

        assertHeaders(
                Collections.emptyList(),
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_SIGNATURE,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));

        clientHmac.setRequestBody(content.getBytes(SignedRequestHmac.UTF8));
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

        new Expectations() {{
            playerClient.getSecretForId(id); returns(secret);
            containerContext.getMethod(); returns(method);
            containerContext.getUriInfo().getAbsolutePath().getPath(); returns(path);
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_ID); returns(headers.getFirst(SignedRequestHmac.GAMEON_ID));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_DATE); returns(headers.getFirst(SignedRequestHmac.GAMEON_DATE));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIG_BODY); returns(headers.getFirst(SignedRequestHmac.GAMEON_SIG_BODY));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIGNATURE); returns(headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));
        }};

        SignedRequestHmac serverHmac = new SignedRequestHmac(containerContext);

        // transfer the request body...
        serverHmac.readRequestBody(clientHmac.getBodyInputStream());

        serverHmac.precheck(playerClient);

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }

        serverHmac.validate();
     }

    @Test
    public void testSignRequestHeaders(@Mocked ContainerRequestContext containerContext,
                                       @Mocked PlayerClient playerClient,
                                       @Mocked SignedRequestTimedCache timedCache) throws Exception {

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
        String content = "{id: 'test'}";

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id,  secret, dateString, method, path);

        clientHmac.prepareForSigning(secret,
                                     headers,
                                     Arrays.asList("Content-Type", "Content-Length"));
        System.out.println(headers);
        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_HEADERS
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_PARAMETERS,
                              SignedRequestHmac.GAMEON_SIGNATURE,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));

        clientHmac.setRequestBody(content.getBytes(SignedRequestHmac.UTF8));
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

        new Expectations() {{
            playerClient.getSecretForId(id); returns(secret);
            containerContext.getMethod(); returns(method);
            containerContext.getUriInfo().getAbsolutePath().getPath(); returns(path);
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_ID); returns(headers.getFirst(SignedRequestHmac.GAMEON_ID));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_DATE); returns(headers.getFirst(SignedRequestHmac.GAMEON_DATE));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_HEADERS); returns(headers.getFirst(SignedRequestHmac.GAMEON_HEADERS));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIG_BODY); returns(headers.getFirst(SignedRequestHmac.GAMEON_SIG_BODY));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIGNATURE); returns(headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));
        }};

        SignedRequestHmac serverHmac = new SignedRequestHmac(containerContext);

        // transfer the request body...
        serverHmac.readRequestBody(clientHmac.getBodyInputStream());

        serverHmac.precheck(playerClient);

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }

        serverHmac.validate();
    }

    @Test
    public void testSignRequestParameters(@Mocked ContainerRequestContext containerContext,
                                          @Mocked PlayerClient playerClient,
                                          @Mocked SignedRequestTimedCache timedCache,
                                          @Mocked UriInfo uriInfo) throws Exception {
        // GET /map/v1/sites?owner=MyUserId HTTP/1.1
        // gameon-id: MyUserId
        // gameon-date: Sat, 21 May 2016 19:14:54 GMT
        // gameon-sig-params: owner;HkP19XXoI90rtg6yWMTACQ20rWZQhbGmgFDMjHSU2qg=
        // gameon-signature: bb0otJw4jDitSf7DXNWMjQEwsoaZqjXlSrE8Wkvkf6s=

        String method = "GET";
        String path = "/map/v1/sites";

        MultivaluedHashMap<String, Object> parameters = new MultivaluedHashMap<>();
        parameters.add("owner", "MyUserId");

        // --- create the hmac signature ----

        SignedRequestHmac clientHmac = new SignedRequestHmac(id, secret, dateString, method, path);

        clientHmac.prepareForSigning(secret,
                                     headers,
                                     null,
                                     parameters,
                                     Arrays.asList("owner"));

        assertHeaders(
                Arrays.asList(SignedRequestHmac.GAMEON_PARAMETERS
                              ),
                Arrays.asList(SignedRequestHmac.GAMEON_ID,
                              SignedRequestHmac.GAMEON_DATE,
                              SignedRequestHmac.GAMEON_HEADERS,
                              SignedRequestHmac.GAMEON_SIGNATURE,
                              SignedRequestHmac.GAMEON_SIG_BODY
                              ));

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

        new Expectations() {{
            playerClient.getSecretForId(id); returns(secret);
            containerContext.getMethod(); returns(method);
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_ID); returns(headers.getFirst(SignedRequestHmac.GAMEON_ID));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_DATE); returns(headers.getFirst(SignedRequestHmac.GAMEON_DATE));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_HEADERS); returns(headers.getFirst(SignedRequestHmac.GAMEON_HEADERS));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_PARAMETERS); returns(headers.getFirst(SignedRequestHmac.GAMEON_PARAMETERS));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIG_BODY); returns(headers.getFirst(SignedRequestHmac.GAMEON_SIG_BODY));
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIGNATURE); returns(headers.getFirst(SignedRequestHmac.GAMEON_SIGNATURE));
            containerContext.getUriInfo(); returns(uriInfo);
            uriInfo.getQueryParameters(); returns(parameters);
            uriInfo.getAbsolutePath().getPath(); returns(path);
        }};

        SignedRequestHmac serverHmac = new SignedRequestHmac(containerContext);

        // transfer the request body...
        serverHmac.readRequestBody(clientHmac.getBodyInputStream());

        serverHmac.precheck(playerClient);

        try {
            serverHmac.checkExpiry();
            Assert.fail("Expiry check should have failed");
        } catch(WebApplicationException e) {
            Assert.assertTrue("Exception should indicate token expired", e.getMessage().contains("expire"));
        }

        serverHmac.validate();
    }

    @Test
    public void testOldHmac(@Mocked ContainerRequestContext containerContext,
                     @Mocked PlayerClient playerClient,
                     @Mocked SignedRequestTimedCache timedCache) throws Exception {

        String method = "GET";
        String path = "/map/v1/sites";
        String dateString = Instant.now().toString(); // OLD Date format

        SignedRequestHmac tmpHmac = new SignedRequestHmac(id, secret, dateString, method, path);
        String hmac = tmpHmac.buildHmac(Arrays.asList(id, dateString), secret);

        new Expectations() {{
            playerClient.getSecretForId(id); returns(secret);
            containerContext.getMethod(); returns(method);
            containerContext.getUriInfo().getAbsolutePath().getPath(); returns(path);
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_ID); returns(id);
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_DATE); returns(dateString);
            containerContext.getHeaderString(SignedRequestHmac.GAMEON_SIGNATURE); returns(hmac);
        }};

        SignedRequestHmac serverHmac = new SignedRequestHmac(containerContext);

        serverHmac.precheck(playerClient);
        serverHmac.checkExpiry();
        serverHmac.validate();

    }

    void assertHeaders(List<String> set, List<String> unset) {
        for(String key : set) {
            Assert.assertNotNull(key  + " should be set : " + headers.get(key), headers.get(key));
        }
        for(String key : unset) {
            Assert.assertNull(key  + " should not be set : " + headers.get(key), headers.get(key));
        }
    }

    void printMessage(SignedRequestHmac hmac) {
        printMessage(hmac,"");
    }

    void printMessage(SignedRequestHmac hmac, String query) {
        System.out.println("-----------------");
        System.out.format("%s %s%s HTTP/1.1\n", hmac.method, hmac.baseUri, query);
        System.out.println("gameon-id: " + id);
        System.out.println("gameon-date: " + dateString);
        if ( hmac.sigHeadersHeader != null )
            System.out.println("gameon-sig-headers: " + hmac.sigHeadersHeader);
        if ( hmac.sigParamsHeader != null )
            System.out.println("gameon-sig-params: " + hmac.sigParamsHeader);
        if ( hmac.bodyHashHeader != null )
            System.out.println("gameon-sig-body: " + hmac.bodyHashHeader);
        System.out.println("gameon-signature: " + hmac.hmacHeader);
        if ( hmac.bodyBytes != null ) {
            System.out.println("Content-Type: application/json");
            System.out.println("Content-Length: " + hmac.bodyBytes.length);
            System.out.println();
            System.out.println((new String(hmac.bodyBytes)));
        }
        System.out.println("-----------------");
    }
}
