package au.com.ds.ef;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("rawtypes")
public class StatefulContext implements Serializable {
	private static final long serialVersionUID = 2324535129909715649L;
	private static long idCounter = 1;
	
	private String id;
	private State state;
	private AtomicBoolean terminated = new AtomicBoolean(false);

	public StatefulContext() {
		id = newId() + ":" + getClass().getSimpleName();
	}

	public StatefulContext(String aId) {
		id = aId + ":" + getClass().getSimpleName();
	}

	public String getId() {
		return id;
	}
	
	public void setState(State state){
		this.state = state;
	}
	
	public State getState() {
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
	}

    @Override
    public String toString() {
        return id;
    }
}
