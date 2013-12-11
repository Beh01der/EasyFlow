package au.com.ds.ef.call;

import au.com.ds.ef.*;
import au.com.ds.ef.err.ExecutionError;

public interface ExecutionErrorHandler<C extends StatefulContext> extends Handler {
	void call(ExecutionError error, C context);
}
