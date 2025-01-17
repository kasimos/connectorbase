package io.rtdi.bigdata.connector.connectorframework.controller;

import java.io.IOException;

import io.rtdi.bigdata.connector.connectorframework.Consumer;
import io.rtdi.bigdata.connector.connectorframework.IConnectorFactory;
import io.rtdi.bigdata.connector.connectorframework.exceptions.ConnectorTemporaryException;
import io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ConsumerEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ServiceEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.enums.ControllerExitType;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PipelineRuntimeException;
import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PipelineTemporaryException;
import io.rtdi.bigdata.connector.properties.ConnectionProperties;
import io.rtdi.bigdata.connector.properties.ConsumerProperties;

public class ConsumerInstanceController extends ThreadBasedController<Controller<?>> {

	private ConsumerController consumercontroller;
	private long rowsprocessed = 0;
	private Long lastdatatimestamp = null;
	private long lastoffset = 0;
	private int fetchcalls = 0;
	private boolean updateConsumerMetadata;

	public ConsumerInstanceController(String name, ConsumerController consumercontroller) {
		super(name);
		this.consumercontroller = consumercontroller;
	}

	@Override
	protected void startThreadControllerImpl() throws PipelineRuntimeException {
	}

	@Override
	protected void stopThreadControllerImpl(ControllerExitType exittype) {
	}

	@Override
	public void runUntilError() throws IOException {
		do {
			updateConsumerMetadata = true;
			try (Consumer<?,?> consumer = getConnectorFactory().createConsumer(this);) {
				long flushtime = getProperties().getFlushMaxTime();
				long maxrows = getProperties().getFlushMaxRecords();
				long timeout = System.currentTimeMillis() + flushtime;
				long rowslimit = rowsprocessed + maxrows;
				consumer.setTopics();
				while (isRunning() && checkChildren()) {
					logger.info("Fetching Data");
					fetchcalls++;
					int rowsfetched = consumer.fetchBatch();
					if (rowsfetched != 0) {
						rowsprocessed += rowsfetched;
						lastdatatimestamp = System.currentTimeMillis();
						lastoffset = consumer.getLastOffset();
					}
					if (System.currentTimeMillis() > timeout || rowsprocessed >= rowslimit) {
						timeout = System.currentTimeMillis() + flushtime;
						rowslimit = rowsprocessed + maxrows;
						consumer.flushData(); // flush/commit.
					}
					if (updateConsumerMetadata) {
						getPipelineAPI().addConsumerMetadata(
								new ConsumerEntity(
										consumercontroller.getName(),
										consumercontroller.getConnectionProperties().getName(),
										this.getPipelineAPI(),
										consumer.getTopics()));
						String bs = getPipelineAPI().getBackingServerConnectionLabel();
						if (bs != null) {
							getPipelineAPI().addServiceMetadata(
									new ServiceEntity(
											bs,
											bs,
											getPipelineAPI().getConnectionLabel(),
											null,
											null));
						}
						updateConsumerMetadata = false;
					}
				}
			} catch (PipelineTemporaryException | ConnectorTemporaryException e) { 
				errors.addError(e, null, null);
				logger.error("Consumer got error, retrying", e);
			}
		} while (isRunning());
	}
	
	private ConsumerProperties getProperties() {
		return consumercontroller.getConsumerProperties();
	}
	
	@Override
	protected String getControllerType() {
		return "ConsumerInstanceController";
	}

	public ConsumerProperties getConsumerProperties() {
		return consumercontroller.getConsumerProperties();
	}

	public ConnectionProperties getConnectionProperties() {
		return consumercontroller.getConnectionProperties();
	}

	public IPipelineAPI<?, ?, ?, ?> getPipelineAPI() {
		return consumercontroller.getPipelineAPI();
	}

	public IConnectorFactory<?, ?, ?> getConnectorFactory() {
		return consumercontroller.getConnectorFactory();
	}

	public long getRowsFetched() {
		return rowsprocessed;
	}
	
	public Long getLastOffset() {
		return lastoffset;
	}
	
	public int getFetchCalls() {
		return fetchcalls;
	}

	public Long getLastProcessed() {
		return lastdatatimestamp;
	}

	@Override
	protected void updateLandscape() {
		this.updateConsumerMetadata = true;
	}

	@Override
	protected void updateSchemaCache() {
	}
	
}
