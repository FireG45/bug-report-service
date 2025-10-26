package ru.bre.storage.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.bre.storage.kafka.KafkaProducer;
import ru.bre.storage.kafka.KafkaProducerImpl;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${kafka.config.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${kafka.config.security.protocol:SASL_SSL}")
    private String securityProtocol;

    @Value("${kafka.config.login.module}")
    private String loginModule;

    @Value("${kafka.config.sasl.mechanism:PLAIN}")
    private String saslMechanism;

    @Value("${kafka.config.username}")
    private String username;

    @Value("${kafka.config.password}")
    private String password;

    @Value("${kafka.report.topic:bre.report.message}")
    private String reportTopic;

    @Value("${kafka.feedback.topic:bre.feedback.message}")
    private String feedbackTopic;

    @Bean("kafkaProducerFactory")
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        configProps.putAll(getSaslConfig());

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean("messageTemplate")
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.putAll(getSaslConfig());

        return new KafkaAdmin(configProps);
    }

    @Bean
    public KafkaProducer kafkaProducer(
            ObjectMapper objectMapper,
            @Qualifier("messageTemplate") KafkaTemplate<String, String> kafkaTemplate
    ) {
        return new KafkaProducerImpl(kafkaTemplate, objectMapper);
    }

    private Map<String, Object> getSaslConfig() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        configProps.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        configProps.put(SaslConfigs.SASL_JAAS_CONFIG, loginModule + " required " +
                "username=\"" + username + "\" " +
                "password=\"" + password + "\";");

        return configProps;
    }

    @Bean
    public NewTopic reportTopic() {
        return TopicBuilder.name(reportTopic).build();
    }

    @Bean
    public NewTopic feedbackTopic() {
        return TopicBuilder.name(feedbackTopic).build();
    }
}
