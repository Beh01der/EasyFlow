package au.com.ds.ef;

import au.com.ds.ef.err.DefinitionError;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LogicValidator <C extends StatefulContext> {
	private State<C> startState;
	private Set<Event<C>> events; 
	private Set<State<C>> states; 
	
	public LogicValidator(State<C> startState) {
		this.startState = startState;
		events = new HashSet<Event<C>>();
		states = new HashSet<State<C>>();
	}
	
	public void validate() {
		validate(startState);
	}
	
	private void validate(State<C> state) {
		if (!states.contains(state)) {
			// haven't started with this state yet
			states.add(state);
			
			if (state.isFinal()) {
				if (!state.getTransitions().isEmpty()) {
					throw new DefinitionError("Some events defined for final State: " + state);
				}
			} else {
				if (state.getTransitions().isEmpty()) {
					throw new DefinitionError("No events defined for non-final State: " + state);
				}
			}
			
			for (Map.Entry<Event<C>, State<C>> e : state.getTransitions().entrySet()) {
				Event<C> event = e.getKey();
				State<C> stateTo = e.getValue();
				if (state.equals(stateTo)) {
					throw new DefinitionError("Circular Event usage: " + event);
				}
				validate(stateTo);
			}
		}
	}
}
