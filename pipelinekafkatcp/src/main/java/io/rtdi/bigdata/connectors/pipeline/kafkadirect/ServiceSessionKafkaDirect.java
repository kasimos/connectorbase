package io.rtdi.bigdata.connectors.pipeline.kafkadirect;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import io.rtdi.bigdata.connector.pipeline.foundation.MicroServiceTransformation;
import io.rtdi.bigdata.connector.pipeline.foundation.ServiceSession;
import io.rtdi.bigdata.connector.pipeline.foundation.TopicName;
import io.rtdi.bigdata.connector.pipeline.foundation.avro.JexlGenericData.JexlRecord;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;
import io.rtdi.bigdata.connector.properties.ServiceProperties;

public class ServiceSessionKafkaDirect extends ServiceSession {
	private KafkaStreams stream;
	private KafkaAPIdirect api;
	private ServiceProperties<?> properties;

	public ServiceSessionKafkaDirect(ServiceProperties<?> properties, KafkaAPIdirect api) {
		this.api = api;
		this.properties = properties;
	}

	@Override
	public void start() throws PropertiesException {
		if (properties.getMicroServices() != null && properties.getMicroServices().size() > 0) {
			Properties streamsConfiguration = new Properties();
			streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "rtdi.io microservice");
		    streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, properties.getName());
		    streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, api.getAPIProperties().getKafkaBootstrapServers());
		    streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
		    streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
		    streamsConfiguration.put(GenericAvroSerde.SCHEMA_PROVIDER_CONFIG, api);
	
		    TopicName source = new TopicName(api.getTenantID(), properties.getSourceTopic());
		    TopicName target = new TopicName(api.getTenantID(), properties.getTargetTopic());
		    StreamsBuilder builder = new StreamsBuilder();
		    KStream<byte[], JexlRecord> input = builder.stream(source.getTopicFQN());
	
		    KStream<byte[], JexlRecord> microservice = input;
		    
		    for (MicroServiceTransformation m : properties.getMicroServices()) {
				microservice = microservice.mapValues(new ValueMapperMicroService(m)); // chain the objects together starting with input
		    }
	
			microservice.to(target.getTopicFQN());
	
		    stream = new KafkaStreams(builder.build(), streamsConfiguration);
	
			stream.start();
		} else {
			throw new PropertiesException("No microservices configured for this service", (Exception) null, null, properties.getName());
		}
	}

	@Override
	public void stop() {
		if (stream != null) {
			stream.close();
		}
	}

	@Override
	public long getRowsProcessed() {
		if (properties.getMicroServices() != null && properties.getMicroServices().size() > 0) {
			return properties.getMicroServices().get(0).getRowProcessed();
		} else {
			return 0;
		}
	}

	@Override
	public Long getLastRowProcessed() {
		if (properties.getMicroServices() != null && properties.getMicroServices().size() > 0) {
			return properties.getMicroServices().get(0).getLastRowProcessed();
		} else {
			return null;
		}
	}

}
