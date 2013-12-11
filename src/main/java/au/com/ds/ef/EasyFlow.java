package au.com.ds.ef;

import au.com.ds.ef.call.*;
import au.com.ds.ef.err.*;
import com.google.common.base.*;
import org.slf4j.*;

import java.util.concurrent.*;

import static au.com.ds.ef.HandlerCollection.*;


public class EasyFlow<C extends StatefulContext> {
    static Logger log = LoggerFactory.getLogger(EasyFlow.class);

    private StateEnum startState;
    private TransitionCollection transitions;

    private Executor executor;

    private HandlerCollection handlers = new HandlerCollection();
    private boolean trace = false;

    protected EasyFlow(StateEnum startState) {
        this.startState = startState;
        this.handlers.setHandler(HandlerCollection.EventType.ERROR, null, null, null);
    }

    protected void processAllTransitions(boolean skipValidation) {
        transitions = new TransitionCollection(Transition.consumeTransitions(), !skipValidation);
    }

    private void prepare() {
        if (executor == null) {
            executor = new AsyncExecutor();
        }
    }

    public void start(final C context) {
        prepare();
        context.setFlow(this);

        if (context.getState() == null) {
            setCurrentState(startState, context);
        }
    }

//    public void start(final StateHolder<C> startState, final C context) {
//        // TODO do we need it?
//        validate();
//        this.context = context;
//
//        if (context.getState() == null) {
//            setCurrentState(startState, context);
//        }
//    }

    protected void setCurrentState(final StateEnum state, final C context) {
        execute(new Runnable() {
            @Override
            public void run() {
                if (isTrace())
                    log.debug("setting current state to {} for {} <<<", state, context);

                StateEnum prevState = context.getState();
                if (prevState != null) {
                    leave(context, prevState);
                }

                context.setState(state);
                enter(context, state);

                if (isTrace())
                    log.debug("setting current state to {} for {} >>>", state, context);
            }
        });
    }

    protected void execute(Runnable task) {
        executor.execute(task);
    }

    public <C1 extends StatefulContext> EasyFlow<C1> whenEvent(EventEnum event, ContextHandler<C1> onEvent) {
        handlers.setHandler(EventType.EVENT_TRIGGER, null, event, onEvent);
        return (EasyFlow<C1>) this;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> whenEvent(EventHandler<C1> onEvent) {
        handlers.setHandler(EventType.EVENT_TRIGGER, null, null, onEvent);
        return (EasyFlow<C1>) this;
    }

//    protected void callOnEventTriggered(EventHolder<C> event, StateHolder<C> from, StateHolder<C> to, C context) throws Exception {
//        if (onEventTriggeredHandler != null) {
//            try {
//                if (isTrace())
//                    log.debug("when triggered {} in {} for {} <<<", event, from, context);
//
//                onEventTriggeredHandler.call(event, from, to, context);
//
//                if (isTrace())
//                    log.debug("when triggered {} in {} for {} >>>", event, from, context);
//            } catch (Exception e) {
//                callOnError(new ExecutionError(from, event, e,
//                    "Execution Error in [EasyFlow.whenEventTriggered] handler", context));
//            }
//        }
//    }

    public <C1 extends StatefulContext> EasyFlow<C1> whenEnter(StateEnum state, ContextHandler<C1> onEnter) {
        handlers.setHandler(EventType.STATE_ENTER, state, null, onEnter);
        return (EasyFlow<C1>) this;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> whenEnter(StateHandler<C1> onEnter) {
        handlers.setHandler(EventType.STATE_ENTER, null, null, onEnter);
        return (EasyFlow<C1>) this;
    }

//    protected void callOnStateEnter(final StateHolder<C> state, final C context) {
//        if (onStateEnterHandler != null) {
//            try {
//                if (isTrace())
//                    log.debug("when enter state {} for {} <<<", state, context);
//
//                onStateEnterHandler.call(state, context);
//
//                if (isTrace())
//                    log.debug("when enter state {} for {} >>>", state, context);
//            } catch (Exception e) {
//                callOnError(new ExecutionError(state, null, e,
//                    "Execution Error in [EasyFlow.whenStateEnter] handler", context));
//            }
//        }
//    }

    public <C1 extends StatefulContext> EasyFlow<C1> whenLeave(StateEnum state, ContextHandler<C1> onEnter) {
        handlers.setHandler(EventType.STATE_LEAVE, state, null, onEnter);
        return (EasyFlow<C1>) this;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> whenLeave(StateHandler<C1> onEnter) {
        handlers.setHandler(EventType.STATE_LEAVE, null, null, onEnter);
        return (EasyFlow<C1>) this;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> whenError(ExecutionErrorHandler<C1> onError) {
        handlers.setHandler(EventType.ERROR, null, null, onError);
        return (EasyFlow<C1>) this;
    }

//    protected void callOnStateLeave(final StateHolder<C> state, final C context) {
//        if (onStateLeaveHandler != null) {
//            try {
//                if (isTrace())
//                    log.debug("when leave state {} for {} <<<", state, context);
//
//                onStateLeaveHandler.call(state, context);
//
//                if (isTrace())
//                    log.debug("when leave state {} for {} >>>", state, context);
//            } catch (Exception e) {
//                callOnError(new ExecutionError(state, null, e,
//                    "Execution Error in [EasyFlow.whenStateLeave] handler", context));
//            }
//        }
//    }
//
    public <C1 extends StatefulContext> EasyFlow<C1> whenFinalState(StateHandler<C1> onFinalState) {
        handlers.setHandler(EventType.FINAL_STATE, null, null, onFinalState);
        return (EasyFlow<C1>) this;
    }

//    protected void callOnFinalState(final StateHolder<C> state, final C context) {
//        try {
//            if (onFinalStateHandler != null) {
//                if (isTrace())
//                    log.debug("when final state {} for {} <<<", state, context);
//
//                onFinalStateHandler.call(state, context);
//
//                if (isTrace())
//                    log.debug("when final state {} for {} >>>", state, context);
//            }
//
//            doOnTerminate(context);
//        } catch (Exception e) {
//            callOnError(new ExecutionError(state, null, e,
//                "Execution Error in [EasyFlow.whenFinalState] handler", context));
//        }
//    }

//    public EasyFlow<C> whenError(ExecutionErrorHandler onError) {
//        this.onError = onError;
//        return this;
//    }
//
//    public EasyFlow<C> whenTerminate(ContextHandler onTerminateHandler) {
//        this.onTerminateHandler = onTerminateHandler;
//        return this;
//    }

    public void waitForCompletion(C context) {
      context.awaitTermination();
    }

    public <C1 extends StatefulContext> EasyFlow<C1> executor(Executor executor) {
        this.executor = executor;
        return (EasyFlow<C1>) this;
    }

    public <C1 extends StatefulContext> EasyFlow<C1> trace() {
        trace = true;
        return (EasyFlow<C1>) this;
    }

    public void trigger(final C context, final EventEnum event) {
        if (isTrace())
            log.debug("trigger {} for {}", this, context);

        if (context.isTerminated()) {
            return;
        }

        execute(new Runnable() {
            @Override
            public void run() {
                StateEnum stateFrom = context.getState();
                Optional<Transition> transition = transitions.getTransition(stateFrom, event);

                try {
                    if (transition.isPresent()) {
                        StateEnum stateTo = transition.get().getStateTo();
                        handlers.callOnEventTriggered(context, event, stateFrom, stateTo);
                        setCurrentState(stateTo, context);
                    } else {
                        throw new LogicViolationError("Invalid EventHolder: " + event +
                            " triggered while in StateHolder: " + context.getState() + " for " + context);
                    }
                } catch (Exception e) {
                    doOnError(new ExecutionError(stateFrom, event, e,
                        "Execution Error in [trigger]", context));
                }
            }
        });
    }

    private void enter(final C context, final StateEnum state) {
        if (context.isTerminated()) {  // TODO do we need this check?
            return;
        }

        try {
            // first enter state
            handlers.callOnStateEntered(context, state);

            if (transitions.isFinal(state)) {
                doOnTerminate(context, state);
            }

//            if (onEnterHandler != null) {
//                if (flow.isTrace())
//                    log.debug("when enter {} for {} <<<", StateHolder.this, context);
//
//                onEnterHandler.call(StateHolder.this, context);
//
//                if (flow.isTrace())
//                    log.debug("when enter {} for {} >>>", StateHolder.this, context);
//            }

//            if (isFinal) {
//                flow.callOnFinalState(StateHolder.this, context);
//            }
        } catch (Exception e) {
            doOnError(new ExecutionError(state, null, e,
                "Execution Error in [StateHolder.whenEnter] handler", context));
        }
    }

    private void leave(final C context, StateEnum state) {
        if (context.isTerminated()) {
            return;
        }

        try {
            handlers.callOnStateLeaved(context, state);
        } catch (Exception e) {
            doOnError(new ExecutionError(state, null, e,
                "Execution Error in [StateHolder.whenEnter] handler", context));
        }
//        if (onLeaveHandler != null) {
//            try {
//                if (flow.isTrace())
//                    log.debug("when leave {} for {} <<<", StateHolder.this, context);
//
//                onLeaveHandler.call(StateHolder.this, context);
//
//                if (flow.isTrace())
//                    log.debug("when leave {} for {} >>>", StateHolder.this, context);
//            } catch (Exception e) {
//                flow.callOnError(new ExecutionError(StateHolder.this, null, e,
//                    "Execution Error in [StateHolder.whenEnter] handler", context));
//            }
//        }
//        flow.callOnStateLeave(this, context);
    }

    protected boolean isTrace() {
        return trace;
    }

    protected void doOnError(final ExecutionError error) {
        handlers.callOnError(error);
        doOnTerminate((C) error.getContext(), error.getState());
    }

    protected StateEnum getStartState() {
        return startState;
    }

    protected void doOnTerminate(final C context, StateEnum state) {
        if (!context.isTerminated()) {
            try {
                if (isTrace())
                    log.debug("terminating context {}", context);

                context.setTerminated();
                handlers.callOnFinalState(context, state);
            } catch (Exception e) {
                log.error("Execution Error in [EasyFlow.whenTerminate] handler", e);
            }
        }
    }
}
