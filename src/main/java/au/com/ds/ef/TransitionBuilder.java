package au.com.ds.ef;

import au.com.ds.ef.err.DefinitionError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitionBuilder<C extends StatefulContext> {
    private static final Logger log = LoggerFactory.getLogger(TransitionBuilder.class);

	private Event<C> event;
	private State<C> stateTo;
	
	protected TransitionBuilder(Event<C> event, State<C> stateTo) {
		this.event = event;
		this.stateTo = stateTo;
	}
	
	protected Event<C> getEvent() {
		return event;
	}

	protected State<C> getStateTo() {
		return stateTo;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TransitionBuilder transit(TransitionBuilder... transitions) {
		for (TransitionBuilder<C> transition : transitions) {
            transition.getEvent().addTransition(stateTo, transition.getStateTo());
			stateTo.addEvent(transition.getEvent(), transition.getStateTo());
		}
		
		return this;
	}
}
