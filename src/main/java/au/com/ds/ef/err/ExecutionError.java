package au.com.ds.ef.err;

import au.com.ds.ef.Event;
import au.com.ds.ef.State;
import au.com.ds.ef.StatefulContext;

@SuppressWarnings("rawtypes")
public class ExecutionError extends Exception {
	private static final long serialVersionUID = 4362053831847081229L;
	private State state;
	private Event event;
	private StatefulContext context;
	
	public ExecutionError(State state, Event event, Exception error, String message, StatefulContext context) {
		super(message, error);
		
		this.state = state;
		this.event = event;
		this.context = context;
	}

	public State getState() {
		return state;
	}

	public Event getEvent() {
		return event;
	}

	public StatefulContext getContext() {
		return context;
	}
}
