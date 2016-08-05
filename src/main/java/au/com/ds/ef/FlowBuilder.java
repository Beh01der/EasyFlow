package au.com.ds.ef;

import java.util.Collection;
import java.util.concurrent.Executor;

public class FlowBuilder<C extends StatefulContext> {

    private StateEnum startState;
    private boolean skipValidation;
    private Executor executor;

    public static class ToHolder {
        private EventEnum event;

        private ToHolder(EventEnum event) {
            this.event = event;
        }

        public Transition to(StateEnum state) {
            return new Transition(event, state, false);
        }

        public Transition finish(StateEnum state) {
            return new Transition(event, state, true);
        }
    }

	private FlowBuilder(StateEnum startState) {
        this.startState = startState;
	}

	public static <C extends StatefulContext> FlowBuilder<C> from(StateEnum startState) {
		return new FlowBuilder<C>(startState);
	}

	public static <C extends StatefulContext> EasyFlow<C> fromTransitions(StateEnum startState,
                                                                          Collection<Transition> transitions, boolean skipValidation) {
        EasyFlow<C> flow = new EasyFlow<C>(startState);
        flow.setTransitions(transitions, skipValidation);
        return flow;
    }

    public static ToHolder on(EventEnum event) {
        return new ToHolder(event);
    }

    public FlowBuilder<C> transit(Transition... transitions) {
        return transit(false, transitions);
    }

	public FlowBuilder<C> transit(boolean skipValidation, Transition... transitions) {
        for (Transition transition : transitions) {
            transition.setStateFrom(startState);
        }
        this.skipValidation = skipValidation;
        return this;
	}

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> build() {
        EasyFlow<C1> flow = new EasyFlow<C1>(startState);
        flow.processAllTransitions(skipValidation);
        if (executor != null) flow.executor(executor);
        return flow;
    }
}
