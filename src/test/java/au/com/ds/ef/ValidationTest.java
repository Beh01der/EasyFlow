package au.com.ds.ef;

import org.junit.Before;
import org.junit.Test;

import au.com.ds.ef.err.DefinitionError;

public class ValidationTest {
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
	private Event<StatefulContext> onEvent_6;
	
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
		onEvent_6 = FlowBuilder.event("onEvent_6");
	}
	
	@Test(expected = DefinitionError.class)
	public void testLooseEnd1() {
		EasyFlow<StatefulContext> flow =
				
		FlowBuilder.from(START).transit();
		
		flow.validate();
	}
	
	@Test(expected = DefinitionError.class)
	public void testLooseEnd2() {
		EasyFlow<StatefulContext> flow =
				
		FlowBuilder.from(START).transit(
				onEvent_1.to(STATE_1)
		);
		
		flow.validate();
	}
	
	@Test(expected = DefinitionError.class)
	public void testLooseEnd3() {
		EasyFlow<StatefulContext> flow =
		
		FlowBuilder.from(START).transit(
				onEvent_1.to(STATE_1).transit()
		);
		
		flow.validate();
	}
	
	@Test(expected = DefinitionError.class)
	public void testUnreachableEvent() {
		EasyFlow<StatefulContext> flow =
				
		FlowBuilder.from(START).transit(
				onEvent_1.finish(STATE_1).transit(
						onEvent_2.to(STATE_2)
				)
		);
		
		flow.validate();
	}
	
	@Test
	public void testReuseEvent() {
		EasyFlow<StatefulContext> flow =
				
		FlowBuilder.from(START).transit(
				onEvent_1.to(STATE_1).transit(
						onEvent_1.to(STATE_2).transit(
                            onEvent_1.finish(STATE_3)
                        )
				)
		);
		
		flow.validate();
	}

	@Test(expected = DefinitionError.class)
	public void testAmbiguousEvent() {
		EasyFlow<StatefulContext> flow =

		FlowBuilder.from(START).transit(
			onEvent_1.to(STATE_1),
            onEvent_1.to(STATE_2)
		);

		flow.validate();
	}

	@Test(expected = DefinitionError.class)
	public void testDuplicateEvent() {
		EasyFlow<StatefulContext> flow =

		FlowBuilder.from(START).transit(
			onEvent_1.to(STATE_1),
            onEvent_1.to(STATE_1)
		);

		flow.validate();
	}

	@Test(expected = DefinitionError.class)
	public void testCircularEvent() {
		EasyFlow<StatefulContext> flow =
		
		FlowBuilder.from(START).transit(
				onEvent_1.to(START)
		);
		
		flow.validate();
	}
	
	@Test
	public void testValid() {
		EasyFlow<StatefulContext> flow =
			
		FlowBuilder.from(START).transit(
				onEvent_1.to(STATE_1).transit(
						onEvent_3.to(STATE_2),
						onEvent_6.finish(STATE_4)
				),
				onEvent_2.to(STATE_2).transit(
						onEvent_4.to(STATE_3).transit(
								onEvent_5.to(START)
						)
				)
		);
		
		flow.validate();
	}
}
