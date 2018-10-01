package org.gameontext.map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.ektorp.CouchDbConnector;
import org.gameontext.map.auth.PlayerClient;
import org.gameontext.map.db.MapRepository;
import org.gameontext.map.kafka.Kafka;

@Health
@ApplicationScoped
public class MapHealth implements HealthCheck {
    
    @Inject
    protected MapRepository mapRepository;
    
    @Inject
    Kafka kafka;

    @Inject
    PlayerClient playerClient;
    
    @Override
    public HealthCheckResponse call() {
        if ( mapRepository != null && mapRepository.connectionReady()
             && playerClient != null && playerClient.isHealthy()
             && kafka != null && kafka.isHealthy() ) {
            return HealthCheckResponse.named(MapResource.class.getSimpleName())
            .withData("mapRepository", mapRepository.connectionReady() ? "available" : "down")
            .withData("playerClient", playerClient.isHealthy() ? "available" : "down")
            .withData("kafka", kafka.isHealthy() ? "available" : "down")
            .up().build();
        }
        return HealthCheckResponse.named(MapResource.class.getSimpleName())
        .withData("mapRepository", mapRepository.connectionReady() ? "available" : "down")
        .withData("playerClient", playerClient.isHealthy() ? "available" : "down")
        .withData("kafka", kafka.isHealthy() ? "available" : "down")
        .down().build();
    }
}
