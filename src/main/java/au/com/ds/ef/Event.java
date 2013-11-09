package au.com.ds.ef;

import au.com.ds.ef.call.EventHandler;
import au.com.ds.ef.err.DefinitionError;
import au.com.ds.ef.err.ExecutionError;
import au.com.ds.ef.err.LogicViolationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Event<C extends StatefulContext> {
	private static long idCounter = 1;
	private static Logger log = LoggerFactory.getLogger(Event.class);
	
	private String id;
    private Map<State<C>, State<C>> transitions = new HashMap<State<C>, State<C>>();
	private EasyFlow<C> runner;
    private boolean ignoreOutOfState = false;

	private EventHandler<C> onTriggeredHandler;

	public Event() {
		this.id = "Event_" + (idCounter++);
	}

	public Event(String id) {
		this.id = id;
	}
	
	public TransitionBuilder<C> to(State<C> stateTo) {
		return new TransitionBuilder<C>(this, stateTo);
	}
	
	public TransitionBuilder<C> finish(State<C> stateTo) {
		stateTo.setFinal(true);
		return new TransitionBuilder<C>(this, stateTo);
	}

	public Event<C> whenTriggered(EventHandler<C> onTriggered) {
        onTriggeredHandler = onTriggered;
		return this;
	}

    public Event<C> ignoreOutOfState() {
        ignoreOutOfState = true;
        return this;
    }

    public void trigger(final C context) {
        if (runner.isTrace())
		    log.debug("trigger {} for {}", this, context);

		if (context.isTerminated()) {
			return;
		}

		runner.execute(new Runnable() {
			@Override
			public void run() {
                State<C> stateFrom = context.getState();
                State<C> stateTo = transitions.get(stateFrom);

				try {
                    if (stateTo == null) {
                        if (!ignoreOutOfState)
                            throw new LogicViolationError("Invalid Event: " + Event.this.toString() +
                                " triggered while in State: " + context.getState() + " for " + context);
                    } else {
                        callOnTriggered(context, stateFrom, stateTo);
                        runner.callOnEventTriggered(Event.this, stateFrom, stateTo, context);
                        runner.setCurrentState(stateTo, context);
                    }
				} catch (Exception e) {
					runner.callOnError(new ExecutionError(stateFrom, Event.this, e,
							"Execution Error in [Event.trigger]", context));					
				}
			}
		});
	}

    protected void addTransition(State<C> from, State<C> to) {
        State<C> existingTransitionState = transitions.get(from);
        if (existingTransitionState != null) {
            if (existingTransitionState == to) {
                throw new DefinitionError("Duplicate transition[" + this + "] from " + from + " to " + to);
            } else {
                throw new DefinitionError("Ambiguous transition[" + this + "] from " + from + " to " + to + " and " +
                    existingTransitionState);

            }
        }

        transitions.put(from, to);
    }

	protected void setFlowRunner(EasyFlow<C> runner) {
		this.runner = runner;
	}

    private void callOnTriggered(C context, State<C> from, State<C> to) throws Exception {
        if (onTriggeredHandler != null) {
            onTriggeredHandler.call(Event.this, from, to, context);
        }
    }

	@Override
	public String toString() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event<C> other = (Event<C>) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
