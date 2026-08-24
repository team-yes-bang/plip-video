package com.plip.video.global.config;

import com.plip.video.adapter.out.messaging.kafka.dto.DiaryVideoUploadedKafkaEvent;
import com.plip.video.adapter.out.messaging.kafka.dto.VideoUploadedKafkaEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaProducerConfig {

	@Value("${spring.kafka.bootstrap-servers:localhost:9092}")
	private String bootstrapServers;

	@Bean
	public ProducerFactory<String, VideoUploadedKafkaEvent> videoUploadedProducerFactory() {
		return new DefaultKafkaProducerFactory<>(producerProps());
	}

	@Bean
	public KafkaTemplate<String, VideoUploadedKafkaEvent> videoUploadedKafkaTemplate(
			ProducerFactory<String, VideoUploadedKafkaEvent> videoUploadedProducerFactory
	) {
		return new KafkaTemplate<>(videoUploadedProducerFactory);
	}

	@Bean
	public ProducerFactory<String, DiaryVideoUploadedKafkaEvent> diaryVideoUploadedProducerFactory() {
		return new DefaultKafkaProducerFactory<>(producerProps());
	}

	@Bean
	public KafkaTemplate<String, DiaryVideoUploadedKafkaEvent> diaryVideoUploadedKafkaTemplate(
			ProducerFactory<String, DiaryVideoUploadedKafkaEvent> diaryVideoUploadedProducerFactory
	) {
		return new KafkaTemplate<>(diaryVideoUploadedProducerFactory);
	}

	private Map<String, Object> producerProps() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
		return props;
	}
}
