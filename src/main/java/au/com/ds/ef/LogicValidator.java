package au.com.ds.ef;

import au.com.ds.ef.err.DefinitionError;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

// TODO may be check additionally: 
// 1. every event and state have unique name
// 2. every event and state utilised in FSM
// 3. every callback assigned only once
public class LogicValidator <C extends StatefulContext> {
	private State<C> startState;
	private Set<Event<C>> events; 
	private Set<State<C>> states; 
	
	public LogicValidator(State<C> startState) {
		this.startState = startState;
		events = Sets.newHashSet();
		states = Sets.newHashSet();
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
