package au.com.ds.ef;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import au.com.ds.ef.call.StateHandler;

/**
 * With original synchronize()/notifyAll() approach this test never finishes
 * (thread(s) still waiting on notification)
 */
public class SynchronizationTest {
  void doRun() {
    ExecutorService threadPool = Executors.newCachedThreadPool();
    for(int i = 0; i < 5; i++) {
      Node n = new Node();
      threadPool.execute(n);
    }
    threadPool.shutdown();
  }

  public static void main(String[] args) {
    new SynchronizationTest().doRun();
  }
  
  static class Node implements Runnable {
    final Event<StatefulContext> onInitialize = FlowBuilder.event("onInitialize");
    final Event<StatefulContext> onTerminate = FlowBuilder.event("onTerminate");

    final EasyFlow<StatefulContext> flow;
    
    Node() {
      final State<StatefulContext> uninitialized = FlowBuilder.state("unitialized");
      final State<StatefulContext> running = FlowBuilder.state("running");
      final State<StatefulContext> done = FlowBuilder.state("done");

      uninitialized.whenEnter(new StateHandler<StatefulContext>() {
        @Override
        public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
          System.out.println(getThreadName()+" unitialized:Enter");
          unitializedEnter(state, context);
        }
      });
      running.whenEnter(new StateHandler<StatefulContext>() {
        @Override
        public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
          System.out.println(getThreadName()+" runnig:Enter");
          runningEnter(state, context);
        }
      });

      this.flow = FlowBuilder.from(uninitialized)
          .transit(onInitialize.to(running).transit(onTerminate.finish(done)));
      this.flow.executor(new SyncExecutor());
      
      flow.whenFinalState(new StateHandler<StatefulContext>() {
        @Override
        public void call(State<StatefulContext> state, StatefulContext context) throws Exception {
          System.out.println(getThreadName()+" final");
        }
      });
    }

    void unitializedEnter(State<StatefulContext> state, StatefulContext context) {
      onInitialize.trigger(context);
    }

    void runningEnter(State<StatefulContext> state, StatefulContext context) {
      for(int i = 0; i < 10; i++) {
        System.out.println(getThreadName()+" running");
        sleep(1000);
      }
      onTerminate.trigger(context);
    }
    
    @Override
    public void run() {
      flow.validate().trace().start(new StatefulContext());
      flow.waitForCompletion();
      System.out.println("Run method completed");
    }
  }
  
  static String getThreadName() {
    return Thread.currentThread().getName();
  }
  
  static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.interrupted();
    }
  }
}
