package au.com.ds.ef;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Paths;

import static au.com.ds.ef.FlowBuilder.on;
import static org.junit.Assert.assertEquals;

public class DotVisualizerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    enum State implements StateEnum {
        START, MIDDLE, END
    }

    enum Event implements EventEnum {
        S1, S2
    }

    @Test
    public void testGraph() throws Exception {
        EasyFlow flow = FlowBuilder.from(State.START).transit(
                on(Event.S1).to(State.MIDDLE).transit(
                        on(Event.S2).finish(State.END)
                )
        );
        String dotFile = folder.newFile().getCanonicalPath();
        new DotVisualizer(flow).graph(dotFile);

        byte[] allBytes = Files.readAllBytes(Paths.get(dotFile));
        String actualGraph = new String(allBytes).replaceAll("[\\t\\n]","").replaceAll("(\\ +)"," ");

        String expectedGraph = "" +
        "digraph flow{ rankdir=LR;"+
            "node [ shape =circle ]; "+
            "START -> MIDDLE [label=\"S1\"];" +
            "MIDDLE -> END [label=\"S2\"];"+
            "START [style=filled, shape=circle,fillcolor=black, fontcolor=white];"+
            "END [style=filled, shape=doublecircle,fillcolor=black, fontcolor=white];"+
        "}";

        assertEquals(expectedGraph,actualGraph);
    }


}