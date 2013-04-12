package au.com.ds.ef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import au.com.ds.ef.call.ExecutionErrorHandler;
import au.com.ds.ef.call.StateHandler;
import au.com.ds.ef.err.ExecutionError;
import au.com.ds.ef.err.LogicViolationError;

import com.google.common.collect.Lists;

public class RunSingleTest {
	private State<StatefulContext> START;
	private State<StatefulContext> STATE_1;
	private State<StatefulContext> STATE_2;
	private State<StatefulContext> STATE_3;
	private State<StatefulContext> STATE_4;
	
	private Event<StatefulContext> onEvent_1;
	private Event<StatefulContext> onEvent_2;
	private Event<StatefulContext> onEvent_3;
	private Event<StatefulContext> onEvent_4;
	private Event<StatefulContext> onEvent_5;
	
	@Before
	public void setUp() {
		START = FlowBuilder.state("START");
		STATE_1 = FlowBuilder.state("STATE_1");
		STATE_2 = FlowBuilder.state("STATE_2");
		STATE_3 = FlowBuilder.state("STATE_3");
		STATE_4 = FlowBuilder.state("STATE_4");
		
		onEvent_1 = FlowBuilder.event("onEvent_1");
		onEvent_2 = FlowBuilder.event("onEvent_2");
		onEvent_3 = FlowBuilder.event("onEvent_3");
		onEvent_4 = FlowBuilder.event("onEvent_4");
		onEvent_5 = FlowBuilder.event("onEvent_5");
	}
	
	@Test
	public void testInvalidEvent() throws LogicViolationError {
		final Exception[] exception = new Exception[]{null};
 		
		EasyFlow<StatefulContext> flow =
				
		FlowBuilder.from(START).transit(
				onEvent_1.to(STATE_1).transit(
						onEvent_2.finish(STATE_2)
				)
		).whenError(new ExecutionErrorHandler() {
			@Override
			public void call(ExecutionError error) {
				exception[0] = error;
			}
		});
		
		START.whenEnter(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
				onEvent_2.trigger(context);
			}
		});
		
		flow.validate().trace().start(new StatefulContext());
		flow.waitForCompletion();
		
		assertNotNull("Exception must be thrown during flow execution", exception[0]);
		assertTrue("Exception type should be ExecutionError", exception[0] instanceof ExecutionError);
		assertTrue("Exception cause should be LogicViolationError", exception[0].getCause() instanceof LogicViolationError);		
	}
	
	@Test
	public void testEventsOrder() throws LogicViolationError {
		EasyFlow<StatefulContext> flow =
				
		FlowBuilder.from(START).transit(
				onEvent_1.to(STATE_1).transit(
						onEvent_2.finish(STATE_2),
						onEvent_3.to(STATE_3).transit(
								onEvent_4.to(STATE_1),
								onEvent_5.finish(STATE_4)
						)
				)
		);
		
		final List<Integer> actualOrder = Lists.newArrayList();
		
		START.whenEnter(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) {
				actualOrder.add(1);
                onEvent_1.trigger(context);
			}
		}).whenLeave(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) {
				actualOrder.add(2);
			}
		});
		
		STATE_1.whenEnter(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) {
				actualOrder.add(3);
                onEvent_2.trigger(context);
			}
		}).whenLeave(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) {
				actualOrder.add(4);
			}
		});
		
		STATE_2.whenEnter(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) {
				actualOrder.add(5);
			}
		}).whenLeave(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) {
				throw new RuntimeException("It never leaaves the final state");
			}
		});
		
		flow.whenFinalState(new StateHandler<StatefulContext>() {
			@Override
			public void call(State<StatefulContext> state, StatefulContext context) {
				actualOrder.add(6);
			}
		});
		
		flow.validate().trace().start(new StatefulContext());
		flow.waitForCompletion();
		
		int i = 0;
		for (Integer order : actualOrder) {
			i++;
			if (order != i) {
				throw new RuntimeException("Events called not in order expected: " + i + " actual: " + order);
			}
		}
	}

    @Test
    public void testEventReuse() {
        EasyFlow<StatefulContext> flow =

            FlowBuilder.from(START).transit(
                onEvent_1.to(STATE_1).transit(
                    onEvent_3.to(START)
                ),
                onEvent_2.to(STATE_2).transit(
                    onEvent_2.finish(STATE_3),
                    onEvent_1.finish(STATE_4)
                )
            );

        START.whenEnter(new StateHandler<StatefulContext>() {
            @Override
            public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
                onEvent_2.trigger(context);
            }
        });

        STATE_2.whenEnter(new StateHandler<StatefulContext>() {
            @Override
            public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
                onEvent_2.trigger(context);
            }
        });

        flow.validate().trace().start(new StatefulContext());
        flow.waitForCompletion();

        assertEquals("Final state", STATE_3, flow.getContext().getState());
    }

    @Test
    public void testSyncExecutor() {
        EasyFlow<StatefulContext> flow =

            FlowBuilder.from(START).transit(
                onEvent_1.to(STATE_1).transit(
                    onEvent_3.to(START)
                ),
                onEvent_2.to(STATE_2).transit(
                    onEvent_2.finish(STATE_3),
                    onEvent_1.finish(STATE_4)
                )
            );

        START.whenEnter(new StateHandler<StatefulContext>() {
            @Override
            public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
                onEvent_2.trigger(context);
            }
        });

        STATE_2.whenEnter(new StateHandler<StatefulContext>() {
            @Override
            public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
                onEvent_2.trigger(context);
            }
        });

        flow
            .executor(new SyncExecutor())
            .trace()
            .start(new StatefulContext());

        assertEquals("Final state", STATE_3, flow.getContext().getState());
    }
}
