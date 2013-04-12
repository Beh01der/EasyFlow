package au.com.ds.ef.call;

import au.com.ds.ef.Event;
import au.com.ds.ef.State;
import au.com.ds.ef.StatefulContext;

public interface EventHandler<C extends StatefulContext> {
	void call(Event<C> event, State<C> from, State<C> to, C context) throws Exception;
}
