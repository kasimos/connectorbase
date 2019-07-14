package io.rtdi.bigdata.connector.connectorframework.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.rtdi.bigdata.connector.connectorframework.exceptions.ConnectorRuntimeException;
import io.rtdi.bigdata.connector.pipeline.foundation.IControllerState;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ErrorEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.entity.ErrorListEntity;
import io.rtdi.bigdata.connector.pipeline.foundation.enums.ControllerExitType;
import io.rtdi.bigdata.connector.pipeline.foundation.enums.ControllerRequestedState;
import io.rtdi.bigdata.connector.pipeline.foundation.enums.ControllerState;


/**
 * An abstract class implementing the common logic for all controllers.<br/>
 * A controller is a robust implementation that tries to keep its children running, provides
 * monitoring information, restarts automatically etc.
 *
 * @param <C> The concrete type of the child Controller
 */
public abstract class Controller<C extends Controller<?>> implements IControllerState {
	protected final Logger logger;
	protected ControllerState state = ControllerState.STOPPED;
	protected HashMap<String, C> childcontrollers = new HashMap<>();
	private String name;
	protected ErrorListEntity errors = new ErrorListEntity();
	/**
	 * If the controller is not enabled it should not be started. This is not an error, so parent controllers should ignore that.
	 */
	protected ControllerRequestedState requestedstate = ControllerRequestedState.DISABLE;

	public Controller(String name) {
		super();
		this.name = name;
		logger = LogManager.getLogger(this.getClass().getName());
	}

	/**
	 * Starts the controller in a new thread.
	 * 
	 * @return Thread running this controller
	 * @throws IOException 
	 */
	public void startController() throws IOException {
		requestedstate = ControllerRequestedState.RUN;
		state = ControllerState.STARTING;
		startControllerImpl();
		startChildController(); // in the thread-less controller the children have to be started here. ThreadBasedController has its own implementation 
	}

	protected void startChildController() throws IOException {
		for (Controller<?> c : childcontrollers.values()) {
			c.startController();
		}
	}

	/**
	 * @return name of the controller as provided in the constructor; usually the same as the properties name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the requested state which is either STARTED or DISABLED
	 */
	public ControllerRequestedState getRequestedState() {
		return requestedstate;
	}
	
	/**
	 * @return implementers return the name of the controller type; used to set the thread name
	 */
	protected abstract String getControllerType();
	
	/**
	 * Signal this controller to stop and hence does stop all child controllers
	 * 
	 * @param exittype
	 */
	public void stopController(ControllerExitType exittype) {
		if (exittype == ControllerExitType.DISABLE) {
			requestedstate = ControllerRequestedState.DISABLE;
		} else if (requestedstate != ControllerRequestedState.DISABLE) {
			requestedstate = ControllerRequestedState.STOP;
		}
		state = ControllerState.STOPPING;
		stopControllerImpl(exittype);
		stopChildControllers(exittype);
	}

	/**
	 * Signal all child controllers to stop
	 * 
	 * @param exittype
	 */
	protected void stopChildControllers(ControllerExitType exittype) {
		for (Controller<?> c : childcontrollers.values()) {
			c.stopController(exittype);
		}
	}

	/**
	 * @param exittype
	 * @return true if all children and this controller have been stopped successfully within a time depending on the exittype
	 */
	public boolean joinAll(ControllerExitType exittype) {
		boolean allstopped = joinAllImpl(exittype);
		allstopped &= joinChildControllers(exittype);
		state = ControllerState.STOPPED;
		return allstopped;
	}

	protected boolean joinAllImpl(ControllerExitType exittype) {
		return true;
	}

	/**
	 * @param exittype
	 * @return true if all children have been stopped successfully within a time depending on the exittype
	 */
	protected boolean joinChildControllers(ControllerExitType exittype) {
		boolean allstopped = true;
		for (Controller<?> c : childcontrollers.values()) {
			allstopped = allstopped & c.joinAll(exittype);
		}
		return allstopped;
	}

	/**
	 * Allows the individual controller implementations to execute own code at start.
	 * 
	 * @throws IOException
	 */
	protected abstract void startControllerImpl() throws IOException;
	
	/**
	 * Allows the individual controller implementations to execute own code at stop.
	 * 
	 * @param exittype
	 */
	protected abstract void stopControllerImpl(ControllerExitType exittype);

	/**
	 * All child controllers should be added here so the {@link #stopController(ControllerExitType)} can stop the children.
	 * 
	 * @param name of the child
	 * @param controller instance to add
	 * @throws ConnectorRuntimeException 
	 */
	public void addChild(String name, C controller) throws ConnectorRuntimeException {
		if (childcontrollers.containsKey(name)) {
			throw new ConnectorRuntimeException("Trying to add the same child controller again");
		} else {
			childcontrollers.put(name, controller);
		}
	}
	
	/**
	 * @return list of recent errors
	 */
	public List<ErrorEntity> getErrorList() {
		return errors.getErrors();
	}
	
	/**
	 * Most Controllers retry temporary connector exceptions only, hence the default implementation returns false always. 
	 * Override and return true else.
	 * 
	 * @return
	 */
	protected boolean retryPipelineTemporaryExceptions() {
		return false;
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.pipeline.foundation.IControllerState#isRunning()
	 */
	@Override
	public boolean isRunning() {
		return (requestedstate == ControllerRequestedState.RUN);
	}

	/**
	 * @return true if all children are running, direct and indirect children
	 * @throws Exception 
	 */
	public boolean checkChildren() throws ConnectorRuntimeException {
		for (String childname : childcontrollers.keySet()) {
			Controller<?> t = childcontrollers.get(childname);
			if (!t.isRunning()) {
				if (t instanceof ThreadBasedController) {
					Exception ex = ((ThreadBasedController<?>) t).lastexception;
					if (ex != null) {
						throw new ConnectorRuntimeException("A child controller got an exception", null, null, this.getName());
					}
				}
				return false;
			} else if (!(t instanceof ThreadBasedController) && !t.checkChildren()) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see io.rtdi.bigdata.connector.connectorframework.controller.IControllerState#getState()
	 */
	@Override
	public ControllerState getState() {
		return state;
	}
	
	@Override
	public String toString() {
		return getControllerType() + " for " + getName();
	}

	/**
	 * @return all registered child controllers
	 */
	protected HashMap<String, C> getChildControllers() {
		return childcontrollers;
	}
}