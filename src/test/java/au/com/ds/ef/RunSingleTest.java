package au.com.ds.ef;

import au.com.ds.ef.call.*;
import au.com.ds.ef.err.*;
import com.google.common.collect.*;
import org.junit.*;

import java.util.*;

import static au.com.ds.ef.FlowBuilder.*;
import static au.com.ds.ef.RunSingleTest.Events.*;
import static au.com.ds.ef.RunSingleTest.States.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class RunSingleTest {
    public enum States implements StateEnum {
        START, STATE_1, STATE_2, STATE_3, STATE_4
    }

    public enum Events implements EventEnum {
        event_1, event_2, event_3, event_4, event_5
    }

    @Test
    public void testInvalidEvent() throws LogicViolationError {
        final Exception[] exception = new Exception[]{null};

        // state machine definition
        final EasyFlow<StatefulContext> flow =

            from(START).transit(
                on(event_1).to(STATE_1).transit(
                    on(event_2).finish(STATE_2)
                )
            );

        // handlers definition
        flow
            .whenError(new ExecutionErrorHandler() {
                @Override
                public void call(ExecutionError error, StatefulContext context) {
                    exception[0] = error;
                }
            })

            .whenEnter(START, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) throws Exception {
                    context.trigger(event_2);
                }
            });

        StatefulContext ctx = new StatefulContext();
        flow.trace().start(ctx);
        ctx.awaitTermination();

        assertNotNull("Exception must be thrown during flow execution", exception[0]);
        assertTrue("Exception type should be ExecutionError", exception[0] instanceof ExecutionError);
        assertTrue("Exception cause should be LogicViolationError", exception[0].getCause() instanceof LogicViolationError);
    }

	@Test
	public void testEventsOrder() throws LogicViolationError {
		EasyFlow<StatefulContext> flow =

		    from(START).transit(
				on(event_1).to(STATE_1).transit(
                    on(event_2).finish(STATE_2),
                    on(event_3).to(STATE_3).transit(
                        on(event_4).to(STATE_1),
                        on(event_5).finish(STATE_4)
                    )
                )
		    );

		final List<Integer> actualOrder = Lists.newArrayList();

        flow
		    .whenEnter(START, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) throws Exception {
                    actualOrder.add(1);
                    context.trigger(event_1);
                }
            })
            .whenLeave(START, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) {
                    actualOrder.add(2);
                }
            })

            .whenEnter(STATE_1, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) throws Exception {
                    actualOrder.add(3);
                    context.trigger(event_2);
                }
            }).whenLeave(STATE_1, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) {
                    actualOrder.add(4);
                }
            })

            .whenEnter(STATE_2, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) {
                    actualOrder.add(5);
                }
            }).whenLeave(STATE_2, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) {
                    throw new RuntimeException("It never leaves the final state");
                }
            })

            .whenFinalState(new StateHandler<StatefulContext>() {
                @Override
                public void call(StateEnum state, StatefulContext context) {
                    actualOrder.add(6);
                }
            });

        StatefulContext ctx = new StatefulContext();
        flow.trace().start(ctx);
        ctx.awaitTermination();

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

            from(START).transit(
                on(event_1).to(STATE_1).transit(
                    on(event_3).to(START)
                ),
                on(event_2).to(STATE_2).transit(
                    on(event_2).finish(STATE_3),
                    on(event_1).finish(STATE_4)
                )
            );

        flow
            .whenEnter(START, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) throws Exception {
                    context.trigger(event_2);
                }
            })
            .whenEnter(STATE_2, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) throws Exception {
                    context.trigger(event_2);
                }
            });

        StatefulContext ctx = new StatefulContext();
        flow.trace().start(ctx);
        ctx.awaitTermination();

        assertEquals("Final state", STATE_3, ctx.getState());
    }

    @Test
    public void testSyncExecutor() {
        EasyFlow<StatefulContext> flow =

            from(START).transit(
                on(event_1).to(STATE_1).transit(
                    on(event_3).to(START)
                ),
                on(event_2).to(STATE_2).transit(
                    on(event_2).finish(STATE_3),
                    on(event_1).finish(STATE_4)
                )
            );

        flow
            .whenEnter(START, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) throws Exception {
                    context.trigger(event_2);
                }
            })
            .whenEnter(STATE_2, new ContextHandler<StatefulContext>() {
                @Override
                public void call(StatefulContext context) throws Exception {
                    context.trigger(event_2);
                }
            });


        StatefulContext ctx = new StatefulContext();
        flow
            .executor(new SyncExecutor())
            .trace()
            .start(ctx);

        assertEquals("Final state", STATE_3, ctx.getState());
    }
}
