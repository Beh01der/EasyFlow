package au.com.ds.ef;

import au.com.ds.ef.err.DefinitionError;

import java.util.*;

/**
 * User: andrey
 * Date: 6/12/2013
 * Time: 2:08 PM
 */
final class TransitionCollection {
    private Map<StateEnum, Map<EventEnum, Transition>> transitionFromState =
        new HashMap<StateEnum, Map<EventEnum, Transition>>();
    private Set<StateEnum> finalStates = new HashSet<StateEnum>();

    protected TransitionCollection(Collection<Transition> transitions, boolean validate) {
        if (transitions != null) {
            for (Transition transition : transitions) {
                Map<EventEnum, Transition> map = transitionFromState.get(transition.getStateFrom());
                if (map == null) {
                    map = new HashMap<EventEnum, Transition>();
                    transitionFromState.put(transition.getStateFrom(), map);
                }
                map.put(transition.getEvent(), transition);
                if (transition.isFinal()) {
                    finalStates.add(transition.getStateTo());
                }
            }
        }

        if (validate) {
            if (transitions == null || transitions.isEmpty()) {
                throw new DefinitionError("No transitions defined");
            }

            Set<Transition> processedTransitions = new HashSet<Transition>();
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

    public Transition getTransition(StateEnum stateFrom, EventEnum event) {
        Map<EventEnum, Transition> transitionMap = transitionFromState.get(stateFrom);
        return transitionMap == null ? null : transitionMap.get(event);
    }

    public List<Transition> getTransitions(StateEnum stateFrom) {
        Map<EventEnum, Transition> transitionMap = transitionFromState.get(stateFrom);
        return transitionMap == null ? Collections.<Transition>emptyList() : new ArrayList<Transition>(transitionMap.values());
    }

    protected boolean isFinal(StateEnum state) {
        return finalStates.contains(state);
    }
}
