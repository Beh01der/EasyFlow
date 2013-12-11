package au.com.ds.ef.call;

import au.com.ds.ef.*;

public interface StateHandler<C extends StatefulContext> extends Handler {
	void call(StateEnum state, C context) throws Exception;
}
