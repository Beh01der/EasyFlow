package au.com.ds.ef.call;

import au.com.ds.ef.err.ExecutionError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultErrorHandler implements ExecutionErrorHandler {
	private static Logger log = LoggerFactory.getLogger(DefaultErrorHandler.class);
	@Override
	
	public void call(ExecutionError error) {
		String msg = "Execution Error in State [" + error.getState() + "] ";
		if (error.getEvent() != null) {
			msg += "on Event [" + error.getEvent() + "] ";
		}
		msg += "with Context [" + error.getContext() + "] ";
		
		Exception e = new Exception(msg, error);
		log.error("Error", e);
	}
}
