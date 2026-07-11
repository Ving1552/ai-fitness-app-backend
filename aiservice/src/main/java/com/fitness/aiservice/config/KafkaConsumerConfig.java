package com.fitness.aiservice.config;

import com.fitness.aiservice.model.Activity;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.ssl.ca-cert}")
    private String caCertPath;

    @Value("${kafka.ssl.service-cert}")
    private String serviceCertPath;

    @Bean
    public ConsumerFactory<String, Activity> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "activity-processor-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.fitness.aiservice.model.Activity");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        configProps.put("security.protocol", "SSL");
        configProps.put("ssl.truststore.type", "PEM");
        configProps.put("ssl.keystore.type", "PEM");
        configProps.put("ssl.truststore.location", caCertPath);
        configProps.put("ssl.keystore.location", serviceCertPath);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Activity> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Activity> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}