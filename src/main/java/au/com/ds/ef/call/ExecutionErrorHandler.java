package au.com.ds.ef.call;

import au.com.ds.ef.err.ExecutionError;

public interface ExecutionErrorHandler {
	void call(ExecutionError error);
}
