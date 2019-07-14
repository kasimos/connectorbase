package io.rtdi.bigdata.connector.connectorframework.rest.service;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.rtdi.bigdata.connector.connectorframework.WebAppController;
import io.rtdi.bigdata.connector.connectorframework.servlet.ServletSecurityConstants;
import io.rtdi.bigdata.connector.pipeline.foundation.IPipelineAPI;
import io.rtdi.bigdata.connector.properties.atomic.PropertyRoot;


@Path("/")
public class PipelineConnectionService {
	
	@Context
    private Configuration configuration;

	@Context 
	private ServletContext servletContext;
		
	@GET
    @Produces(MediaType.APPLICATION_JSON)
	@Path("/PipelineConnection")
	@RolesAllowed(ServletSecurityConstants.ROLE_CONFIG)
    public Response getPipelineConnection() {
		try {
			IPipelineAPI<?, ?, ?, ?> api = WebAppController.getPipelineAPI(servletContext);
			return Response.ok(api.getAPIProperties().getPropertyGroup()).build();
		} catch (Exception e) {
			return JAXBErrorResponseBuilder.getJAXBResponse(e);
		}
	}

	@POST
    @Produces(MediaType.APPLICATION_JSON)
	@Path("/PipelineConnection")
	@RolesAllowed(ServletSecurityConstants.ROLE_CONFIG)
    public Response postPipelineConnection(PropertyRoot data) {
		try {
			IPipelineAPI<?, ?, ?, ?> api = WebAppController.getPipelineAPI(servletContext);
			api.getAPIProperties().setValue(data);
			api.writeConnectionProperties();
			return Response.ok("created").build();
		} catch (Exception e) {
			return JAXBErrorResponseBuilder.getJAXBResponse(e);
		}
	}
}