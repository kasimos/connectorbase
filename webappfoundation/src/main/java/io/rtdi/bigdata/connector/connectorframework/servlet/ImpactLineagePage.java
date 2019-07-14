package io.rtdi.bigdata.connector.connectorframework.servlet;

import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;

@WebServlet("/ui5/ImpactLineage")
public class ImpactLineagePage extends UI5ServletAbstract {

	private static final long serialVersionUID = 122608943464963L;

	public ImpactLineagePage() {
		super("ImpactLineage", "ImpactLineage");
	}
	
	@Override
	protected void printCustomHeader(PrintWriter out) {
		out.println("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.js\"></script>");
		out.println("<link href=\"https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis-network.min.css\" rel=\"stylesheet\" type=\"text/css\" />");
	}

}