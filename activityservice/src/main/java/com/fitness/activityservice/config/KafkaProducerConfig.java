package com.fitness.activityservice.config;

import com.fitness.activityservice.models.Activity;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.ssl.ca-cert}")
    private String caCertPath;

    @Value("${kafka.ssl.service-cert}")
    private String serviceCertPath;
    @Bean
    public ProducerFactory<String, Activity> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        configProps.put("security.protocol", "SSL");
        configProps.put("ssl.truststore.type", "PEM");
        configProps.put("ssl.keystore.type", "PEM");
        configProps.put("ssl.truststore.location", caCertPath);
        configProps.put("ssl.keystore.location", serviceCertPath);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Activity> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}