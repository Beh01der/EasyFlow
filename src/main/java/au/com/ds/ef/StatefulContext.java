package au.com.ds.ef;

import au.com.ds.ef.err.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@SuppressWarnings("rawtypes")
public class StatefulContext implements Serializable {
	private static final long serialVersionUID = 2324535129909715649L;
	private static volatile long idCounter = 1;
	
	private final String id;
    private EasyFlow flow;
	private StateEnum state;
    private EventEnum lastEvent;
	private final AtomicBoolean terminated = new AtomicBoolean(false);
	private final AtomicBoolean stopped = new AtomicBoolean(false);
	private final CountDownLatch completionLatch = new CountDownLatch(1);

	public StatefulContext() {
		id = newId() + ":" + getClass().getSimpleName();
	}

	public StatefulContext(String aId) {
		id = aId + ":" + getClass().getSimpleName();
	}

	public String getId() {
		return id;
	}
	
	public void setState(StateEnum state){
		this.state = state;
	}
	
	public StateEnum getState() {
		return state;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatefulContext other = (StatefulContext) obj;
		if (id != other.id)
			return false;
		return true;
	}

    public void stop() {
        stopped.set(true);
        setTerminated();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public boolean safeTrigger(EventEnum event) {
        return flow.safeTrigger(event, this);
    }

    public void trigger(EventEnum event) throws LogicViolationError {
        flow.trigger(event, this);
    }

    protected void setFlow(EasyFlow<? extends StatefulContext> flow) {
        this.flow = flow;
    }
	
	protected long newId() {
		return idCounter++;
	}

    public boolean isTerminated() {
        return terminated.get();
    }

    public boolean isRunning() {
        return isStarted() && !terminated.get();
    }

    public boolean isStarted() {
        return state != null;
    }

	protected void setTerminated() {
		this.terminated.set(true);
		this.completionLatch.countDown();
	}

    public EventEnum getLastEvent() {
        return lastEvent;
    }

    void setLastEvent(EventEnum lastEvent) {
        this.lastEvent = lastEvent;
    }

    public List<Transition> getAvailableTransitions() {
        return flow.getAvailableTransitions(state);
    }

    protected void awaitTermination() {
        try {
            this.completionLatch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
