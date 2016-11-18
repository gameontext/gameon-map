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

import java.io.IOException;
import java.security.Key;

import javax.ws.rs.WebApplicationException;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.gameontext.map.auth.PlayerClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

public class PlayerClientTest {
    @Mocked
    Key key;
    PlayerClient pc = new PlayerClient();
    
    @Mocked HttpClientBuilder builder;
    @Mocked Jwts jwts;

    @Before
    public void injectResources() {
        //setup the injected resources.. 
        Deencapsulation.setField(pc, "signingKey", key);
        Deencapsulation.setField(pc, "playerLocation", "playerURL");
        Deencapsulation.setField(pc, "SYSTEM_ID", "SYSTEM_ID");
        Deencapsulation.setField(pc, "registrationSecret", "registrationSecret");
        Deencapsulation.setField(pc, "sweepId", "SweepId");
        Deencapsulation.setField(pc, "sweepSecret", "SweepSecret");
    }

    @Test
    public void testSweepSecret() throws IOException {
        String secret = pc.getSecretForId("SweepId");
        Assert.assertEquals(secret,"SweepSecret");
    }

    @Test
    public void testRegistrationSecret() throws IOException {
        String secret = pc.getSecretForId("SYSTEM_ID");
        Assert.assertEquals(secret,"registrationSecret");
    }

    @Test
    public void testSuccess(@Mocked CloseableHttpClient httpClient,
            @Mocked CloseableHttpResponse response, @Mocked JwtBuilder jwtBuilder,
            @Mocked BasicResponseHandler responseHandler) throws IOException {

        new Expectations() {{
            //setup enough expectations to allow us to return the result that would have 
            //happened via http get.
            HttpClientBuilder.create(); result = builder;
            builder.build(); result = httpClient;
            httpClient.execute((HttpGet)any); result = response;
            responseHandler.handleResponse(response); result = "{\"credentials\":{\"sharedSecret\":\"WIBBLE\"}}";
            
            //setup enough expectations to allow jwt generation to end up with a known result.
            Jwts.builder(); result = jwtBuilder;
            jwtBuilder.setHeaderParam((String)any,any); result = jwtBuilder;
            jwtBuilder.setClaims((Claims)any); result = jwtBuilder;
            jwtBuilder.signWith((SignatureAlgorithm)any, key); result = jwtBuilder;
            jwtBuilder.compact(); result = "<<BUILTJWT>>";
        }};

        String secret = pc.getSecretForId("fish");
        Assert.assertEquals(secret,"WIBBLE");
        
        new Verifications() {{
            
           HttpGet httpGet;
           httpClient.execute(httpGet = withCapture());
           //check if jwt header was set on request.
           Assert.assertEquals("<<BUILTJWT>>", httpGet.getFirstHeader("gameon-jwt").getValue());
           Assert.assertEquals("playerURL/fish", httpGet.getURI().toString());
        }};
    }
    
    @Test(expected = WebApplicationException.class)
    public void testFailure(@Mocked CloseableHttpClient httpClient,
            @Mocked CloseableHttpResponse response, @Mocked JwtBuilder jwtBuilder,
            @Mocked BasicResponseHandler responseHandler) throws IOException {

        new Expectations() {{
            //setup enough expectations to allow us to return the result that would have 
            //happened via http get.
            HttpClientBuilder.create(); result = builder;
            builder.build(); result = httpClient;
            
            //trigger failure path.. =)
            httpClient.execute((HttpGet)any); result = new HttpResponseException(500,"test case failure");
            
            //setup enough expectations to allow jwt generation to end up with a known result.
            Jwts.builder(); result = jwtBuilder;
            jwtBuilder.setHeaderParam((String)any,any); result = jwtBuilder;
            jwtBuilder.setClaims((Claims)any); result = jwtBuilder;
            jwtBuilder.signWith((SignatureAlgorithm)any, key); result = jwtBuilder;
            jwtBuilder.compact(); result = "<<BUILTJWT>>";
        }};

        pc.getSecretForId("fish");
    }

}
