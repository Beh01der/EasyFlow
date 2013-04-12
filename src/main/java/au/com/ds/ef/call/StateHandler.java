package au.com.ds.ef.call;

import au.com.ds.ef.State;
import au.com.ds.ef.StatefulContext;

public interface StateHandler<C extends StatefulContext> {
	void call(State<C> state, C context) throws Exception;
}
