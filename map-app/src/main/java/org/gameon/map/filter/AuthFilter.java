package org.gameon.map.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

@WebFilter(
        filterName = "mapAuthFilter",
        urlPatterns = {"/*"}
          )
public class AuthFilter implements Filter {

    /**
     * If any player secret comes back as this string, they have been revoked by admin, and 
     * will be treated as unable to authenticate.
     */
    private static final String ACCESS_DENIED = "ACCESS_DENIED";

    private Map<String,TimestampedKey> apiKeyForId = Collections.synchronizedMap( new HashMap<String,TimestampedKey>() );
    
    /** CDI injection of client for Player CRUD operations */
    @Inject
    PlayerClient playerClient;
    
    @Resource(lookup="registrationSecret")
    String registrationSecret;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    /**
     * We need to hash the request body.. which is read via an input stream that can only be read once
     * so if we need to read it, then we need to keep hold of it so the client servlet can read it too.
     * Thus we use a ServletRequestWrapper..
     */
    public static class ServletAuthWrapper extends HttpServletRequestWrapper{
        private final HttpServletRequest req;
        private final String body;
        public ServletAuthWrapper (HttpServletRequest req) throws IOException{
            super(req);
            this.req = req;
            
            try (BufferedReader buffer = new BufferedReader(
                    new InputStreamReader(req.getInputStream(),"UTF-8"))) {
                body = buffer.lines().collect(Collectors.joining("\n"));
            }
        }
        public String getId(){
            return req.getHeader("gameon-id");
        }
        public String getDate(){
            return req.getHeader("gameon-date");           
        }
        public String getSigBody(){
            return req.getHeader("gameon-sig-body");
        }
        public String getSignature(){
            return req.getHeader("gameon-signature");
        }
        public String getBody(){
            return body;
        }
        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes("UTF-8"));
            ServletInputStream inputStream = new ServletInputStream() {
                public int read () 
                    throws IOException {
                    return byteArrayInputStream.read();
                }
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available()==0;
                }
                @Override
                public boolean isReady() {
                    return true;
                }
                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new RuntimeException("Not implemented");            
                }
            };
            return inputStream;
        }
    }
    
    private String buildHmac(List<String> stuffToHash, String key) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException{
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256"));
        
        StringBuffer hashData = new StringBuffer();
        for(String s: stuffToHash){
            hashData.append(s);            
        }
        
        return Base64.getEncoder().encodeToString( mac.doFinal(hashData.toString().getBytes("UTF-8")) );
    }
    
    private String buildHash(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes("UTF-8")); 
        byte[] digest = md.digest();
        return Base64.getEncoder().encodeToString( digest );
    }
    
    /**
     * Timestamped Key
     * Equality / Hashcode is determined by key string alone.
     * Sort order is provided by key timestamp.
     */
    private final static class TimestampedKey implements Comparable<TimestampedKey> {
        private final String apiKey;
        private final Long time;
        public TimestampedKey(String a){
            this.apiKey=a; this.time=System.currentTimeMillis();
        }
        public TimestampedKey(String a,Long t){
            this.apiKey=a; this.time=t;
        }
        @Override
        public int compareTo(TimestampedKey o) {
            return o.time.compareTo(time);
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((apiKey == null) ? 0 : apiKey.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TimestampedKey other = (TimestampedKey) obj;
            if (apiKey == null) {
                if (other.apiKey != null)
                    return false;
            } else if (!apiKey.equals(other.apiKey))
                return false;
            return true;
        }
    }
    
    /**
     * Obtain the apiKey for the given id, using a local cache to avoid hitting couchdb too much.
     */
    private String getKeyForId(String id){
        //first.. handle our built-in key
        if("game-on.org".equals(id)){
            return registrationSecret;
        }
        
        String key = null;
        //check cache for this id.
        TimestampedKey t = apiKeyForId.get(id);
        if(t!=null){
            //cache hit, but is the key still valid?
            long current = System.currentTimeMillis();
            current -= t.time;          
            //if the key is older than this time period.. we'll consider it dead.
            boolean valid = current < TimeUnit.DAYS.toMillis(1);    
            if(valid){                          
                //key is good.. we'll use it.
                System.out.println("Map using cached key for "+id);
                key = t.apiKey;
            }else{
                //key has expired.. forget it.
                System.out.println("Map expired cached key for "+id);
                apiKeyForId.remove(id);
                t=null;
            }
        }
        if(t == null){
            //key was not in cache, or was expired..
            //go obtain the apiKey via player Rest endpoint.
            try{
                System.out.println("Map asking player service for key for id "+id);
                key = playerClient.getApiKey(id);
            }catch(Exception e){
                System.out.println("Map unable to get key for id "+id);
                e.printStackTrace();
                key=null;
            }           
            //got a key ? add it to the cache.
            if(key!=null){
                t = new TimestampedKey(key);
                apiKeyForId.put(id, t);
            }
        }
        return key;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if(request instanceof HttpServletRequest){
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            
            String requestUri = httpRequest.getRequestURI();
                      
            if(requestUri.startsWith("/map/v1/health")){
                //no auth needed for health.
                chain.doFilter(request, response);
                return;
            }
            
            if(requestUri.startsWith("/map/v1/sites")){
                //auth needed for sites endpoints.               
                ServletAuthWrapper saw = new ServletAuthWrapper(httpRequest);
                
                String id = saw.getId();
                if ( id == null ){
                    id = "game-on.org";
                }
                String gameonDate = saw.getDate();                
                try{
                    //we protect Map, and our requirements vary per http method
                    switch(httpRequest.getMethod()){
                        case "GET":{
                            //if there's no auth.. we continue but set id to null.
                            if(saw.getId() == null){
                                id = null;
                            }else{
                                if(!validateHeaderBasedAuth(response, saw, id, gameonDate, false)){
                                    return;
                                }
                            }
                            break;
                        }
                        case "POST":{
                            if(!validateHeaderBasedAuth(response, saw, id, gameonDate, true)){
                                return;
                            }
                            break;
                        }
                        case "DELETE":{
                            if(!validateHeaderBasedAuth(response, saw, id, gameonDate, false)){
                                return;
                            }
                            break;
                        }
                        case "PUT":{
                            if(!validateHeaderBasedAuth(response, saw, id, gameonDate, true)){
                                return;
                            }
                            break;
                        }
                        default:{
                            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported Http Method "+httpRequest.getMethod());
                            return;
                        }                    
                    }
                }catch(UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e){
                    e.printStackTrace();
                    ((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing headers "+e.getMessage());
                    return;
                }
                     
                request.setAttribute("player.id", id);
       
                //pass our request wrapper to the chain.. NOT the original request, because we may have 
                //already burnt the input stream by reading it to hash the body.
                chain.doFilter(saw, response);
                return;
            }          
            if(requestUri.toLowerCase().startsWith("/map/logview")){
                //logview manages its own auth.
                chain.doFilter(request, response);
                return;
            }
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN, "Request made to unknown url pattern. "+httpRequest.getRequestURI());         
        }else{
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Only supports http servlet requests");
        }
    }

    private boolean validateHeaderBasedAuth(ServletResponse response, ServletAuthWrapper saw, String id, String gameonDate, boolean postData)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, IOException {        
        String secret = getKeyForId(id);  
        if(secret == null){            
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Unable to obtain shared secret for player "+id+" from player service");
            return false;
        }
        if(ACCESS_DENIED.equals(secret)){
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Auth key revoked and disabled by admin");
            return false;
        }
        String body = postData ? saw.getBody() : "";
        String bodyHash = postData ? buildHash(body) : "";
        String bodyHashHeader = postData ? saw.getSigBody() : "";
        if(bodyHash!=null && bodyHash.equals(bodyHashHeader)){
            String hmac = buildHmac(Arrays.asList(
                    new String[] { id,gameonDate,bodyHashHeader} ), secret);
            String hmacHeader = saw.getSignature();
            if(hmac!=null && hmac.equals(hmacHeader)){
                Instant now = Instant.now();
                Instant then = Instant.parse(gameonDate);    
                try{
                if(Duration.between(now,then).toMillis() > 5000){
                    //fail.. time delta too much.
                    ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Time delta of "+Duration.between(now,then).toMillis()+"ms is too great.");
                    return false;
                }else{                    
                    //TODO: add replay check.. 
                    System.out.println("Allowing invocation for id "+id);
                    //otherwise.. we're done here.. auth is good, we'll come out the switch
                    //and pass control to the original method.
                    return true;
                }
                }catch(DateTimeParseException e){
                    ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Unable to parse gameon-date header");
                    return false;
                }
                
            }else{
                System.out.println("Had hmac "+hmacHeader+" and calculated "+hmac+" using key first2chars '"+secret.substring(0, 2)+"' for id "+id);
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Had hmac '"+hmacHeader+"' and first 4chars of calculated were '"+hmac.substring(0,4)+"' using key with first 2chars '"+secret.substring(0, 2)+"' for id "+id);
                return false;
            }                                
        }else{
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Bad gameon-sig-body value");
            return false;
        }
    }

    @Override
    public void destroy() {
    }

}
