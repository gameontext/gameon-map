package org.gameontext.map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
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

        HealthCheckResponseBuilder builder = HealthCheckResponse.named(MapResource.class.getSimpleName())
        .withData("mapRepository", mapRepository.connectionReady() ? "available" : "down")
        .withData("playerClient", playerClient.isHealthy() ? "available" : "down")
        .withData("kafka", kafka.isHealthy() ? "available" : "down");

        if ( mapRepositoryReady() && playerClientReady() && kafkaReady() ) {
            return builder.up().build();
        }

        return builder.down().build();
    }

    private boolean mapRepositoryReady() {
        return mapRepository != null && mapRepository.connectionReady();
    }

    private boolean playerClientReady() {
        return playerClient != null && playerClient.isHealthy();
    }

    private boolean kafkaReady() {
        return kafka != null && kafka.isHealthy();
    }
}
