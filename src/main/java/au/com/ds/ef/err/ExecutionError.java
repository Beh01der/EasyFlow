package au.com.ds.ef.err;

import au.com.ds.ef.*;

public class ExecutionError extends Exception {
	private static final long serialVersionUID = 4362053831847081229L;
	private StateEnum state;
	private EventEnum event;
	private StatefulContext context;
	
	public ExecutionError(StateEnum state, EventEnum event, Exception error, String message, StatefulContext context) {
		super(message, error);
		
		this.state = state;
		this.event = event;
		this.context = context;
	}

	public StateEnum getState() {
		return state;
	}

	public EventEnum getEvent() {
		return event;
	}

	public <C extends StatefulContext> C getContext() {
		return (C) context;
	}
}
