package au.com.ds.ef;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class SyncExecutor implements Executor {
    private ArrayList<Runnable> queue = Lists.newArrayList();
    private boolean running = false;

	@Override
	public void execute(Runnable task) {
        queue.add(task);

        if (!running) {
            while (!queue.isEmpty()) {
                Runnable nextTask = queue.remove(queue.size() - 1);
                running = true;
                nextTask.run();
                running = false;
            }
        }
    }
}
