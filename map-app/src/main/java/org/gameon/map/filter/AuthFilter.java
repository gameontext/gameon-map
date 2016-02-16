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
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    private static final long EXPIRES_REQUEST_MS = TimeUnit.MINUTES.toMillis(5);                            //expiry time for requests in ms
    private static final long EXPIRES_PLAYERID_MS = TimeUnit.DAYS.toMillis(1);      //expiry time for player ID before checking for revocation (in ms)
    private static final long EXPIRES_REPLAY_MS = EXPIRES_REQUEST_MS + TimeUnit.MINUTES.toMillis(1);        //how long to retain replays for, must be longer than the valid request period
    private static final long TRIGGER_CLEANUP_DEPTH = 1000;                         //number of requests before a cleanup is triggered
    private static ConcurrentMap<String,TimestampedKey> apiKeyForId = new ConcurrentHashMap<>();
    private static final String instanceID = UUID.randomUUID().toString();
    public static final String INSTANCE_ID = "gameon-instanceID";
    private static final boolean instanceCheckingEnabled = false;                   //remove this to enforce map instance checking to counter replays
    //this map contains all the received messages, it is thread safe
    private static ConcurrentMap<String,TimestampedKey> requests = new ConcurrentHashMap<>();
    
    /** CDI injection of client for Player CRUD operations */
    @Inject
    PlayerClient playerClient;
    
    @Resource(lookup="registrationSecret")
    String registrationSecret;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        filterConfig.getServletContext().setAttribute(INSTANCE_ID, instanceID);
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
        public String getMapID() {
            return req.getHeader(INSTANCE_ID);
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
     * Obtain the apiKey for the given id, using a local cache to avoid hitting couchdb too much.
     */
    private String getKeyForId(String id){
        //first.. handle our built-in key
        if("game-on.org".equals(id)){
            return registrationSecret;
        }
        
        TimestampedKey t = new TimestampedKey(EXPIRES_PLAYERID_MS);
        TimestampedKey result =  apiKeyForId.putIfAbsent(id, t);    //check cache for this id.
        if(result != null) {
            //the id has been seen, so check to see if it has expired
            if(!result.hasExpired()) {
                System.out.println("Map using cached key for "+id);
                return result.getKey();                
            }
            System.out.println("Map expired cached key for "+id);
            apiKeyForId.replace(id, t); //replace old entry with new one to be initialised
        }
        System.out.println("Map asking player service for key for id "+id);
        try {
            String key = playerClient.getApiKey(id);
            t.setKey(key);
            return key;
        } catch (Exception e) {
            System.out.println("Map unable to get key for id "+id);
            e.printStackTrace();
            return null;
        }
    }
    
    
    //checks to see if the HMAC has previously been processed by the server
    private boolean isDuplicate(String hmac) {
        if(requests.size() > TRIGGER_CLEANUP_DEPTH) {
            System.out.println("Clearing down expired messages");
            long count = 0;
            /*
             * This will do for the moment, however it will still be possible the multiple 
             * requests are doing a clean up at the same time. However the use of ConcurrentMaps
             * and weakly consistent iterators means that ConcurrentModificationExceptions will not occur
             * (it just isn't very efficient at the moment).
             */
            for(Entry<String, TimestampedKey> request : requests.entrySet()) {
                if(request.getValue().hasExpired()) {
                    //safe as concurrent maps don't throw concurrent modification exceptions
                    requests.remove(request.getKey());
                    count++;
                }
            }
            if(count > 0) {
                System.out.println("Cleared down " + count + " messages");
            } else {
                System.out.println("No messages were cleared down");
            }
        }
        TimestampedKey t = new TimestampedKey(hmac, EXPIRES_REPLAY_MS);
        return requests.putIfAbsent(hmac, t) != null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if(request instanceof HttpServletRequest){
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            
            String requestUri = httpRequest.getRequestURI();
                       
            if(requestUri.startsWith("/map/v1/health") || requestUri.startsWith("/map/v1/app")) {
                System.out.println("No auth needed for health or app info");
                //no auth needed for health.
                chain.doFilter(request, response);
                return;
            }
            
            if(requestUri.startsWith("/map/v1/sites")){

                //auth needed for sites endpoints.               
                ServletAuthWrapper saw = new ServletAuthWrapper(httpRequest);
                
                String id = saw.getId();
                if ( id == null )
                    id = "game-on.org";
                String gameonDate = saw.getDate();    
                String mapID = saw.getMapID();
                try{
                    //we protect Map, and our requirements vary per http method
                    switch(httpRequest.getMethod()){
                        case "GET":{
                            //if there's no auth.. we continue but set id to null.
                            if(saw.getId() == null){
                                id = null;
                            }else{
                                if(!validateHeaderBasedAuth(response, saw, id, gameonDate, false, mapID)){
                                    return;
                                }
                            }
                            break;
                        }
                        case "POST":{
                            if(!validateHeaderBasedAuth(response, saw, id, gameonDate, true, mapID)) {
                                return;
                            }
                            break;
                        }
                        case "DELETE":{
                            if(!validateHeaderBasedAuth(response, saw, id, gameonDate, false, mapID)) {
                                return;
                            }
                            return;
                        }
                        case "PUT":{
                            if(!validateHeaderBasedAuth(response, saw, id, gameonDate, true, mapID)){
                                return;
                            }
                            return;
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
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN, "Request made to unknown url pattern. "+httpRequest.getRequestURI());
        }else{
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Only supports http servlet requests");
        }
    }

    private boolean validateHeaderBasedAuth(ServletResponse response, ServletAuthWrapper saw, String id, String gameonDate, boolean postData, String mapID)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, IOException {       
        String hmacHeader = saw.getSignature();
        if((hmacHeader == null) || (hmacHeader.length() == 0)) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid signature received");
            return false;
        } 
        if(isDuplicate(hmacHeader)) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Duplicate request received");
            return false;
        }
        if(instanceCheckingEnabled || ((mapID != null) && (mapID.length() > 0))) {
            if(!instanceID.equals(mapID)) {
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid map instance ID supplied in header");
                return false;
            }
        } else {
            mapID = "";     //checking is disabled, so remove mapID
        }
        String secret = getKeyForId(id);  
        if(secret == null){            
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Unable to obtain shared secret for player "+id+" from player service");
            return false;
        }
        String body = postData ? saw.getBody() : "";
        String bodyHash = postData ? buildHash(body) : "";
        String bodyHashHeader = postData ? saw.getSigBody() : "";
        if(bodyHash!=null && bodyHash.equals(bodyHashHeader)){
            String hmac = buildHmac(Arrays.asList(
                    new String[] { mapID, id,gameonDate,bodyHashHeader} ), secret);
            
            if(hmac!=null && hmac.equals(hmacHeader)){
                Instant now = Instant.now();
                Instant then = Instant.parse(gameonDate);    
                try{
                if(Duration.between(now,then).toMillis() > EXPIRES_REQUEST_MS) {
                    //fail.. time delta too much.
                    ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Time delta of "+Duration.between(now,then).toMillis()+"ms is too great.");
                    return false;
                }else{                    
                    return true;
                }
                }catch(DateTimeParseException e){
                    ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Unable to parse gameon-date header");
                    return false;
                }
                
            }else{
                System.out.println("Had hmac "+hmacHeader+" and calculated "+hmac+" using key(first2chars) "+secret.substring(0, 2)+" for id "+id);
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN,"Had hmac "+hmacHeader+" and calculated (first 4chars) "+hmac.substring(0,4)+" using key(first2chars) "+secret.substring(0, 2)+" for id "+id);
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
