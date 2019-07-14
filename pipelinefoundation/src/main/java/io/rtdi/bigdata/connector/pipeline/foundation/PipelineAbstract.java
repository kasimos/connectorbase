package io.rtdi.bigdata.connector.pipeline.foundation;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;

import io.rtdi.bigdata.connector.pipeline.foundation.entity.ConsumerEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ConsumerMetadataEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ProducerEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ProducerMetadataEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.TopicPayload;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PipelineCallerException;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PipelineRuntimeException;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;
import io.rtdi.bigdata.connector.properties.ConsumerProperties;
import io.rtdi.bigdata.connector.properties.PipelineConnectionProperties;
import io.rtdi.bigdata.connector.properties.PipelineConnectionServerProperties;
import io.rtdi.bigdata.connector.properties.ProducerProperties;

/**
 * The main API definition. Every concrete implementation of a transaction log service is based on this. <BR/>
 * Each instance can be used for a single tenant only.
 *
 * @param <S> PipelineConnectionProperties
 * @param <T> TopicHandler
 * @param <P> ProducerSession
 * @param <C> ConsumerSession
 */
public abstract class PipelineAbstract<
				S extends PipelineConnectionProperties, 
				T extends TopicHandler, 
				P extends ProducerSession<T>, 
				C extends ConsumerSession<T>> implements Closeable, IPipelineAPI<S, T, P, C> {

	PipelineServerAbstract<? extends PipelineConnectionServerProperties,T,P,C> server;
	private File webinfdir;

	public PipelineAbstract() {
		super();
	}
	
	public void setServer(PipelineServerAbstract<? extends PipelineConnectionServerProperties,T,P,C> server) {
		this.server = server;
	}
	
	/**
	 * At this point in time no connections or anything else that might fail should be done.
	 * The class will be instantiated e.g. at start of the webserver and just because the
	 * topic server is not available, the web server should still start up.
	 * All connections should be created at {@link #open()}.
	 * 
	 * @param server
	 */
	public PipelineAbstract(PipelineServerAbstract<? extends PipelineConnectionServerProperties,T,P,C> server) {
		super();
		this.server = server;
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getSchema(java.lang.String)
	 */
	@Override
	public SchemaHandler getSchema(String schemaname) throws PropertiesException {
		return server.getSchema(new SchemaName(getTenantID(), schemaname));
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getSchema(int)
	 */
	@Override
	public Schema getSchema(int schemaid) throws PropertiesException {
		return server.getSchema(schemaid);
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getSchema(io.rtdi.bigdata.connector.pipeline.foundation.SchemaName)
	 */
	@Override
	public SchemaHandler getSchema(SchemaName schemaname) throws PropertiesException {
		if (schemaname.getTenant().equals(getTenantID())) {
			return server.getSchema(schemaname);
		} else {
			throw new PipelineRuntimeException("getSchema() request failed, the request is not within the current tenant");
		}
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#registerSchema(io.rtdi.bigdata.connector.pipeline.foundation.SchemaName, java.lang.String, org.apache.avro.Schema, org.apache.avro.Schema)
	 */
	@Override
	public SchemaHandler registerSchema(SchemaName schemaname, String description, Schema keyschema, Schema valueschema) throws PropertiesException {
		if (schemaname.getTenant().equals(getTenantID())) {
			return server.registerSchema(schemaname, description, keyschema, valueschema);
		} else {
			throw new PipelineRuntimeException("registerSchema() request failed, the request is not within the current tenant");
		}
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#registerSchema(java.lang.String, java.lang.String, org.apache.avro.Schema, org.apache.avro.Schema)
	 */
	@Override
	public SchemaHandler registerSchema(String schemaname, String description, Schema keyschema, Schema valueschema) throws PropertiesException {
		return server.registerSchema(new SchemaName(getTenantID(), schemaname), description, keyschema, valueschema);
	}

	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getSchemas()
	 */
	@Override
	public List<String> getSchemas() throws PropertiesException {
		return server.getSchemas(getTenantID());
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#topicCreate(io.rtdi.bigdata.connector.pipeline.foundation.TopicName, int, int, java.util.Map)
	 */
	@Override
	public T topicCreate(TopicName topic, int partitioncount, int replicationfactor, Map<String, String> configs) throws PropertiesException {
		if (topic.getTenant().equals(getTenantID())) {
			return server.createTopic(topic, partitioncount, replicationfactor, configs);
		} else {
			throw new PipelineRuntimeException("topicCreate() request failed, the request is not within the current tenant");
		}
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#topicCreate(io.rtdi.bigdata.connector.pipeline.foundation.TopicName, int, int)
	 */
	@Override
	public T topicCreate(TopicName topic, int partitioncount, int replicationfactor) throws PropertiesException {
		if (topic.getTenant().equals(getTenantID())) {
			return server.topicCreate(topic, partitioncount, replicationfactor);
		} else {
			throw new PipelineRuntimeException("topicCreate() request failed, the request is not within the current tenant");
		}
	}
	

	@Override
	public T topicCreate(String topic, int partitioncount, int replicationfactor, Map<String, String> configs) throws PropertiesException {
		return topicCreate(new TopicName(getTenantID(), topic), partitioncount, replicationfactor, configs);
	}


	@Override
	public T topicCreate(String topic, int partitioncount, int replicationfactor) throws PropertiesException {
		return topicCreate(topic, partitioncount, replicationfactor, null);
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getTopicOrCreate(java.lang.String, int, int, java.util.Map)
	 */
	@Override
	public synchronized T getTopicOrCreate(String name, int partitioncount, int replicationfactor, Map<String, String> configs) throws PropertiesException {
		T t = getTopic(name);
		if (t == null) {
			t = server.createTopic(new TopicName(getTenantID(), name), replicationfactor, replicationfactor, configs);
		} 
		return t;
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getTopicOrCreate(java.lang.String, int, int)
	 */
	@Override
	public final T getTopicOrCreate(String topicname, int partitioncount, int replicationfactor) throws PropertiesException {
		return getTopicOrCreate(topicname, partitioncount, replicationfactor, null);
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getTopic(java.lang.String)
	 */
	@Override
	public T getTopic(String topicname) throws PropertiesException {
		return server.getTopic(new TopicName(getTenantID(), topicname));
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getTopic(io.rtdi.bigdata.connector.pipeline.foundation.TopicName)
	 */
	@Override
	public T getTopic(TopicName topic) throws PropertiesException {
		if (topic.getTenant().equals(getTenantID())) {
			return server.getTopic(topic);
		} else {
			throw new PipelineRuntimeException("getTopic() request failed, the request is not within the current tenant");
		}	
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getTopics()
	 */
	@Override
	public List<String> getTopics() throws IOException {
		return server.getTopics(getTenantID());
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getLastRecords(java.lang.String, int)
	 */
	@Override
	public List<TopicPayload> getLastRecords(String topicname, int count) throws IOException {
		return server.getLastRecords(new TopicName(getTenantID(), topicname), count);
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getLastRecords(java.lang.String, long)
	 */
	@Override
	public List<TopicPayload> getLastRecords(String topicname, long timestamp) throws IOException {
		return server.getLastRecords(new TopicName(getTenantID(), topicname), timestamp);
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getLastRecords(io.rtdi.bigdata.connector.pipeline.foundation.TopicName, int)
	 */
	@Override
	public List<TopicPayload> getLastRecords(TopicName topicname, int count) throws IOException {
		if (topicname.getTenant().equals(getTenantID())) {
			return server.getLastRecords(topicname, count);
		} else {
			throw new PipelineRuntimeException("getLastRecords() request failed, the request is not within the current tenant");
		}	
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getLastRecords(io.rtdi.bigdata.connector.pipeline.foundation.TopicName, long)
	 */
	@Override
	public List<TopicPayload> getLastRecords(TopicName topicname, long timestamp) throws IOException {
		if (topicname.getTenant().equals(getTenantID())) {
			return server.getLastRecords(topicname, timestamp);
		} else {
			throw new PipelineRuntimeException("getLastRecords() request failed, the request is not within the current tenant");
		}	
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#createNewProducerSession(io.rtdi.bigdata.connector.properties.ProducerProperties)
	 */
	@Override
	public P createNewProducerSession(ProducerProperties properties) throws PropertiesException {
		if (properties == null) {
			throw new PipelineCallerException("ProducerSession requires a ProducerProperties object to get its name");
		} else {
			return createProducerSession(properties);
		}
	}

	/**
	 * Create a new ProducerSession based on the provided properties. <BR/>
	 * This method should not throw exceptions as it creates the object only.
	 * 
	 * @param properties Producer specific properties or null
	 * @return A new ProducerSession to be used for connecting against the server and producing records
	 * @throws PropertiesException 
	 */
	protected abstract P createProducerSession(ProducerProperties properties) throws PropertiesException;
	
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#createNewConsumerSession(io.rtdi.bigdata.connector.properties.ConsumerProperties)
	 */
	@Override
	public C createNewConsumerSession(ConsumerProperties properties) throws PropertiesException {
		if (properties == null) {
			throw new PropertiesException("ProducerSession requires a ProducerProperties object to get its name");
		} else {
			return createConsumerSession(properties);
		}
	}

	/**
	 * This factory method creates a new ConsumerSession object. <BR/>
	 * It should call {@link #setTopicHandlers(ConsumerSession) setTopicHandlers} as reference which concrete topics it does listen on. <BR/>
	 * This method should not throw exceptions as it creates the object only.
	 * 
	 * @param properties Mandatory parameter as it includes the topics to listen on
	 * @return A new ConsumerSession
	 * @throws PropertiesException 
	 */
	protected abstract C createConsumerSession(ConsumerProperties properties) throws PropertiesException;
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#open()
	 */
	@Override
	public void open() throws PropertiesException {
		server.open();
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#close()
	 */
	@Override
	public void close() {
		server.close();
	}
		
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#removeProducerMetadata(java.lang.String)
	 */
	@Override
	public void removeProducerMetadata(String producername) throws IOException {
		server.removeProducerMetadata(getTenantID(), producername);
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#removeConsumerMetadata(java.lang.String)
	 */
	@Override
	public void removeConsumerMetadata(String consumername) throws IOException {
		server.removeConsumerMetadata(getTenantID(), consumername);
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#addProducerMetadata(java.lang.String, java.util.Map)
	 */
	@Override
	public void addProducerMetadata(ProducerEntity producer) throws IOException {
		server.addProducerMetadata(getTenantID(), producer);
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#addConsumerMetadata(java.lang.String, java.util.Map)
	 */
	@Override
	public void addConsumerMetadata(ConsumerEntity consumer) throws IOException {
		server.addConsumerMetadata(getTenantID(), consumer);
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getProducerMetadata()
	 */
	@Override
	public ProducerMetadataEntity getProducerMetadata() throws IOException {
		return server.getProducerMetadata(getTenantID());
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getConsumerMetadata()
	 */
	@Override
	public ConsumerMetadataEntity getConsumerMetadata() throws IOException {
		return server.getConsumerMetadata(getTenantID());
	}
	
	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI#getAPIProperties()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public S getAPIProperties() {
		return (S) server.getAPIProperties();
	}

	@Override
	public void loadConnectionProperties(File webinfdir) throws PropertiesException {
		this.webinfdir = webinfdir;
		server.loadConnectionProperties(webinfdir);
	}

	@Override
	public void writeConnectionProperties() throws PropertiesException {
		server.writeConnectionProperties(webinfdir);
	}

	protected PipelineServerAbstract<? extends PipelineConnectionServerProperties, T, P, C> getServer() {
		return server;
	}
	
	@Override
	public String getHostName() {
		return IOUtils.getHostname();
	}

}