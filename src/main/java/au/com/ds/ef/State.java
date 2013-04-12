package au.com.ds.ef;

import au.com.ds.ef.call.StateHandler;
import au.com.ds.ef.err.ExecutionError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class State<C extends StatefulContext> {
	private static Logger log = LoggerFactory.getLogger(State.class);
	private static long idCounter = 1;
	
	private boolean isFinal = false;
	private EasyFlow<C> runner;
	private String id;
	private Map<Event<C>, State<C>> transitions = new HashMap<Event<C>, State<C>>();
	private StateHandler<C> onEnterHandler;
	private StateHandler<C> onLeaveHandler;
	
	public State() {
		this.id = "State_" + (idCounter++);
	}
	
	public State(String id) {
		this.id = id;
	}
	
	public State<C> whenEnter(StateHandler<C> onEnterListener) {
        this.onEnterHandler = onEnterListener;
        return this;
    }

    public State<C> whenLeave(StateHandler<C> onLeaveListener){
        this.onLeaveHandler = onLeaveListener;
		return this;
	}

    public boolean hasEvent(Event<C> event) {
        return transitions.containsKey(event);
    }

    public boolean hasTransitionTo(State<C> state) {
        return transitions.containsValue(state);
    }

    @Override
	public String toString() {
		return id;
	}

	protected void addEvent(Event<C> event, State<C> stateTo) {
//		log.debug("add transition: {} --- {} --> {}", id, event, stateTo);
		transitions.put(event, stateTo);
	}

	protected void enter(final C context) {
		if (context.isTerminated()) {
			return;
		}

        try {
            // first enter state
            runner.callOnStateEnter(State.this, context);

            if (onEnterHandler != null) {
                if (runner.isTrace())
                    log.debug("when enter {} for {} <<<", State.this, context);

                onEnterHandler.call(State.this, context);

                if (runner.isTrace())
                    log.debug("when enter {} for {} >>>", State.this, context);
            }

            if (isFinal) {
                runner.callOnFinalState(State.this, context);
            }
        } catch (Exception e) {
            runner.callOnError(new ExecutionError(State.this, null, e,
                    "Execution Error in [State.whenEnter] handler", context));
        }
	}

	protected void leave(final C context) {
		if (context.isTerminated()) {
			return;
		}
		
		// then leave the state
		if (onLeaveHandler != null) {
            try {
                if (runner.isTrace())
                    log.debug("when leave {} for {} <<<", State.this, context);

                onLeaveHandler.call(State.this, context);

                if (runner.isTrace())
                    log.debug("when leave {} for {} >>>", State.this, context);
            } catch (Exception e) {
                runner.callOnError(new ExecutionError(State.this, null, e,
                        "Execution Error in [State.whenEnter] handler", context));
            }
		}
		runner.callOnStateLeave(this, context);
	}
	
	protected Map<Event<C>, State<C>> getTransitions() {
		return transitions;
	}

	public boolean isFinal() {
		return isFinal;
	}

	protected void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}
	
	protected void setFlowRunner(EasyFlow<C> runner) {
		this.runner = runner;
		
		for (Event<C> event : transitions.keySet()) {
			event.setFlowRunner(runner);
		}
		
		for (State<C> nextState : transitions.values()) {
			if (nextState.runner == null) {
				nextState.setFlowRunner(runner);
			}
		}
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
		State<C> other = (State<C>) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
