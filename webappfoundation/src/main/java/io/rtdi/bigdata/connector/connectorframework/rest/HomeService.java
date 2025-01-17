package io.rtdi.bigdata.connector.connectorframework.rest;

import java.io.IOException;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.rtdi.bigdata.connector.connectorframework.IConnectorFactory;
import io.rtdi.bigdata.connector.connectorframework.WebAppController;
import io.rtdi.bigdata.connector.connectorframework.controller.ConnectorController;
import io.rtdi.bigdata.connector.connectorframework.servlet.ServletSecurityConstants;
import io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI;


@Path("/")
public class HomeService {
	
	@Context
    private Configuration configuration;

	@Context 
	private ServletContext servletContext;
		
	@GET
	@Path("/home")
    @Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(ServletSecurityConstants.ROLE_VIEW)
    public Response getConnectionProperties() {
		try {
			IPipelineAPI<?, ?, ?, ?> api = WebAppController.getPipelineAPI(servletContext);
			ConnectorController controller = WebAppController.getConnector(servletContext);
			IConnectorFactory<?, ?, ?> factory = WebAppController.getConnectorFactory(servletContext);
			return Response.ok(new HomeEntity(api, controller, factory)).build();
		} catch (Exception e) {
			return JAXBErrorResponseBuilder.getJAXBResponse(e);
		}
	}

	public static class HomeEntity {
		private int connectioncount;
		private int servicecount;
		private int topiccount;
		private int schemacount;
		private int producercount;
		private int consumercount;
		private long rowsprocessedcount;
		private Long lastprocessed = null;
		private boolean supportsconnections = true;
		private boolean supportsservices = true;
		
		public HomeEntity() {
			super();
		}
		
		public HomeEntity(IPipelineAPI<?, ?, ?, ?> api, ConnectorController controller, IConnectorFactory<?, ?, ?> factory) throws IOException {
			if (controller != null) {
				connectioncount = controller.getConnections().size();
				producercount = controller.getProducerCount();
				consumercount = controller.getConsumerCount();
				rowsprocessedcount = controller.getRowsProcessed();
				lastprocessed = controller.getLastProcessed();
				servicecount = controller.getServices().size();
				if (factory != null) {
					supportsconnections = factory.supportsConnections();
					supportsservices = factory.supportsServices();
				}
			} else {
				connectioncount = 0;
				servicecount = 0;
			}
			if (api != null) {
				List<String> l = api.getTopics();
				if (l != null) {
					topiccount = l.size();
				} else {
					topiccount = 0;
				}
				l = api.getSchemas();
				if (l != null) {
					schemacount = l.size();
				} else {
					schemacount = 0;
				}
			}
		}
		
		public int getConnectioncount() {
			return connectioncount;
		}
		public int getServicecount() {
			return servicecount;
		}
		public int getTopiccount() {
			return topiccount;
		}
		public int getSchemacount() {
			return schemacount;
		}
		public int getProducercount() {
			return producercount;
		}

		public int getConsumercount() {
			return consumercount;
		}

		public long getRowsprocessedcount() {
			return rowsprocessedcount;
		}
		public Long getLastprocessed() {
			return lastprocessed;
		}
		
		public boolean isSupportconnections() {
			return supportsconnections;
		}
		public boolean isSupportservices() {
			return supportsservices;
		}
	}
}
