package au.com.ds.ef;

import au.com.ds.ef.err.*;
import com.google.common.base.*;
import com.google.common.collect.*;

import java.util.*;

/**
 * User: andrey
 * Date: 6/12/2013
 * Time: 2:08 PM
 */
final class TransitionCollection {
    private static final class TransitionWithEvent implements Predicate<Transition> {
        private EventEnum event;

        private TransitionWithEvent(EventEnum event) {
            this.event = event;
        }

        @Override
        public boolean apply(Transition transition) {
            return transition.getEvent() == event;
        }
    }

    private Multimap<StateEnum, Transition> transitionFromState = HashMultimap.create();
    private Set<StateEnum> finalStates = Sets.newHashSet();

    protected TransitionCollection(Collection<Transition> transitions, boolean validate) {
        if (transitions != null) {
            for (Transition transition : transitions) {
                transitionFromState.put(transition.getStateFrom(), transition);
                if (transition.isFinal()) {
                    finalStates.add(transition.getStateTo());
                }
            }
        }

        if (validate) {
            if (transitions == null || transitions.isEmpty()) {
                throw new DefinitionError("No transitions defined");
            }

            Set<Transition> processedTransitions = Sets.newHashSet();
            for (Transition transition : transitions) {
                StateEnum stateFrom = transition.getStateFrom();
                if (finalStates.contains(stateFrom)) {
                    throw new DefinitionError("Some events defined for final State: " + stateFrom);
                }

                if (processedTransitions.contains(transition)) {
                    throw new DefinitionError("Ambiguous transitions: " + transition);
                }

                StateEnum stateTo = transition.getStateTo();
                if (!finalStates.contains(stateTo) &&
                        !transitionFromState.containsKey(stateTo)) {
                    throw new DefinitionError("No events defined for non-final State: " + stateTo);
                }

                if (stateFrom.equals(stateTo)) {
                    throw new DefinitionError("Circular transition: " + transition);
                }

                processedTransitions.add(transition);
            }
        }
    }

    public Optional<Transition> getTransition(StateEnum stateFrom, EventEnum event) {
        return FluentIterable
            .from(transitionFromState.get(stateFrom))
            .firstMatch(new TransitionWithEvent(event));
    }

    protected boolean isFinal(StateEnum state) {
        return finalStates.contains(state);
    }
}
