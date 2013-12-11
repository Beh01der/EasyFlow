package au.com.ds.ef;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import au.com.ds.ef.call.*;
import sun.management.resources.*;

import static au.com.ds.ef.SynchronizationTest.Events.*;
import static au.com.ds.ef.SynchronizationTest.States.*;
import static au.com.ds.ef.FlowBuilder.*;

/**
 * With original synchronize()/notifyAll() approach this test never finishes
 * (thread(s) still waiting on notification)
 */
public class SynchronizationTest {
    private static final int THREAD_NUM = 5;

    public enum Events implements EventEnum {
        initialize, terminate
    }

    public enum States implements StateEnum {
        UNINITIALIZED,
        RUNNING,
        DONE
    }

    void doRun() throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_NUM);
        for (int i = 0; i < THREAD_NUM; i++) {
            Node n = new Node(threadPool);
            threadPool.submit(n);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new SynchronizationTest().doRun();
    }

    static class Node implements Runnable {
        private static AtomicInteger runningInstances = new AtomicInteger(THREAD_NUM);
        final EasyFlow<StatefulContext> flow;
        ExecutorService executor;

        Node(ExecutorService executor) {
            this.executor = executor;

            flow =

                from(UNINITIALIZED).transit(
                    on(initialize).to(RUNNING).transit(
                        on(terminate).finish(DONE)
                    )
                );

            flow
                .whenEnter(UNINITIALIZED, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        System.out.println(getThreadName() + " unitialized:Enter");
                        context.trigger(initialize);
                    }
                })

                .whenEnter(RUNNING, new ContextHandler<StatefulContext>() {
                    @Override
                    public void call(StatefulContext context) throws Exception {
                        System.out.println(getThreadName() + " runnig:Enter");
                        for (int i = 0; i < 10; i++) {
                            System.out.println(getThreadName() + " running");
                            Thread.sleep(1000);
                        }
                        context.trigger(terminate);
                    }
                })

                .whenFinalState(new StateHandler<StatefulContext>() {
                    @Override
                    public void call(StateEnum state, StatefulContext context) throws Exception {
                        System.out.println(getThreadName() + " final");
                    }
                });
        }

        @Override
        public void run() {
            StatefulContext ctx = new StatefulContext();

            flow.start(ctx);
//      This is not required when using SyncExecutor
//      By the time we get here, flow is already completed
//      SyncExecutor runs on the same thread on which "start" is called
            ctx.awaitTermination();
            System.out.println("Run method completed");

            if (runningInstances.decrementAndGet() == 0) {
                System.out.println("All threads completed");
                executor.shutdownNow();
                System.exit(0);
            }
        }
    }

    static String getThreadName() {
        return Thread.currentThread().getName();
    }
}
