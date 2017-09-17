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
package org.gameontext.map.kafka;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.WakeupException;
import org.gameontext.map.Log;
import org.gameontext.map.model.Site;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class Kafka {

   @Resource(lookup="kafkaUrl")
   protected String kafkaUrl;

   private Producer<String,String> producer=null;
   private Consumer<String, String> consumer=null;

   private volatile boolean keepGoing = true;

   /** CDI injection of Java EE7 Managed thread factory */
   @Resource
   protected ManagedThreadFactory threadFactory;

   public Kafka(){
   }

   public boolean isHealthy() {
       return producer != null && consumer != null;
   }

   private boolean multipleHosts(){
       //this is a cheat, we need to enable ssl when talking to message hub, and not to kafka locally
       //the easiest way to know which we are running on, is to check how many hosts are in kafkaUrl
       //locally for kafka there'll only ever be one, and messagehub gives us a whole bunch..
       return kafkaUrl.indexOf(",") != -1;
   }

   @PostConstruct
   public void init(){

     try{
         try{
             //Kafka client expects this property to be set and pointing at the
             //jaas config file.. except when running in liberty, we don't need
             //one of those.. thankfully, neither does kafka client, it just doesn't
             //know that.. so we'll set this to an empty string to bypass the check.
             if(System.getProperty("java.security.auth.login.config")==null){
               System.setProperty("java.security.auth.login.config", "");
             }

             Properties producerProps = new Properties();
             Properties consumerProps = new Properties();

             //this is a cheat, we need to enable ssl when talking to message hub, and not to kafka locally
             //the easiest way to know which we are running on, is to check how many hosts are in kafkaUrl
             //locally for kafka there'll only ever be one, and messagehub gives us a whole bunch..
             boolean multipleHosts = multipleHosts();
             if(multipleHosts){
               Log.log(Level.INFO, this, "Initializing SSL Config for MessageHub");
               producerProps.put("security.protocol","SASL_SSL");
               producerProps.put("ssl.protocol","TLSv1.2");
               producerProps.put("ssl.enabled.protocols","TLSv1.2");
               Path p = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
               producerProps.put("ssl.truststore.location", p.toString());
               producerProps.put("ssl.truststore.password","changeit");
               producerProps.put("ssl.truststore.type","JKS");
               producerProps.put("ssl.endpoint.identification.algorithm","HTTPS");
             }

             // duplicate common properties right now.
             consumerProps.putAll(producerProps);

             Log.log(Level.INFO, this, "Initializing kafka producer for url {0}", kafkaUrl);

             producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
             producerProps.put(ProducerConfig.ACKS_CONFIG,"-1");
             producerProps.put(ProducerConfig.CLIENT_ID_CONFIG,"gameon-map");
             producerProps.put(ProducerConfig.RETRIES_CONFIG,0);
             producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG,16384);
             producerProps.put(ProducerConfig.LINGER_MS_CONFIG,1);
             producerProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG,33554432);
             producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
             producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");

             producer = new KafkaProducer<>(producerProps);

             Log.log(Level.INFO, this, "Initializing kafka consumer for url {0}", kafkaUrl);

             consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
             consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "gameon-map");
             consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
             consumerProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
             consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
             consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

             consumer = new KafkaConsumer<>(consumerProps);

         }catch(KafkaException k){
             Throwable cause = k.getCause();
             if(cause != null && cause.getMessage().contains("DNS resolution failed for url") && multipleHosts()){
                 Log.log(Level.SEVERE, this, "Error during Kafka Init. Kafka will be unavailable. You may need to restart all linked containers.", cause);
             }else{
                 throw k;
             }
         }
     }catch(Exception e){
         Log.log(Level.SEVERE, this, "Unknown error during kafka init, please report ", e);
     }
   }

   public void publishMessage(String topic, String key, String message){
     if(producer!=null){
         Log.log(Level.FINER, this, "Publishing Event {0} {1} {2}",topic,key,message);
         ProducerRecord<String,String> pr = new ProducerRecord<>(topic, key, message);
         producer.send(pr);
         Log.log(Level.FINER, this, "Published Event");
     }else{
         Log.log(Level.FINER, this, "Kafka Unavailable, ignoring event {0} {1} {2}",topic,key,message);
     }
   }

   protected final ObjectMapper mapper = new ObjectMapper();
   public enum SiteEvent {UPDATE,CREATE,DELETE};
   public void publishSiteEvent(SiteEvent eventType, Site site){
       try{
           //note that messagehub topics are charged, so we must only
           //create them via the bluemix ui, to avoid accidentally
           //creating a thousand topics =)
           String topic = "siteEvents";
           //siteEvents are keyed by site id.
           String key = site.getId();

           ObjectNode rootNode = mapper.createObjectNode();
           rootNode.put("type", eventType.name());
           rootNode.set("site", mapper.valueToTree(site));

           String message = mapper.writeValueAsString(rootNode);

           publishMessage(topic, key, message);
       }catch(JsonProcessingException e){
           Log.log(Level.SEVERE, this, "Error during event publish, could not build json for site with id "+site.getId(),e);
       }
   }

   public void subscribe(KafkaEventHandler eventHandler) {
       if (consumer == null || eventHandler == null) {
           return;
       }

       List<String> topics = Collections.singletonList("playerEvents");
       Log.log(Level.INFO, this, "Kafka Consumer SUBSCRIBED to {0}", topics);

       Runnable consumerThread = () -> {
           consumer.subscribe(topics);

           // Dedicated thread sending messages to the room as fast
           // as it can take them: maybe we batch these someday.
           while (keepGoing) {
               try {
                   final ConsumerRecords<String, String> consumerRecords = consumer.poll(1000);
                   final String desiredType = eventHandler.getEventType();

                   if (consumerRecords.count()==0) {
                       continue;
                   }

                   consumerRecords.forEach(record -> {
                       System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                               record.key(), record.value(),
                               record.partition(), record.offset());

                       JsonNode tree;
                        try {
                            tree = mapper.readTree(record.value());
                            String type = tree.get("type").asText();

                            if ( type.equals(desiredType) ) {
                                eventHandler.handleEvent(record.key(), tree);
                            }
                        } catch (IOException e) {
                            Log.log(Level.INFO, this, "Exception parsing JSON: {0}", e.getMessage());
                            Log.log(Level.SEVERE, this, "Error consuming event {0}"+record.key(),record.value());
                        }
                   });
               } catch (WakeupException e) {
                  // Ignore exception if closing
                  if (!keepGoing) throw e;
               } catch (KafkaException ex) {
                   Log.log(Level.SEVERE, this, "Exception working with Kafka, closing: {0}", ex.getMessage());
                   Log.log(Level.FINEST, this, "Exception working with Kafka", ex);
                   keepGoing = false;
               }
           }
           consumer.close();
           Log.log(Level.INFO, this, "Kafka Consumer CLOSED");
       };

       // Start container-managed consumer thread
       threadFactory.newThread(consumerThread);
   }

   @PreDestroy
   protected void stopConsumer() {
       Log.log(Level.INFO, this, "Stopping Kafka Consumer");
       keepGoing = false;
       consumer.wakeup();
   }
}
