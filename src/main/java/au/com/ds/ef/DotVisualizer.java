package au.com.ds.ef;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DotVisualizer {

    private final EasyFlow flow;

    public DotVisualizer(EasyFlow flow) {
        this.flow = flow;
    }

    public void graph(String filename) {
        try {
            StringBuffer graph = new StringBuffer("digraph flow{ ")
                    .append("rankdir=LR;\n")
                    .append("\tnode [ shape =circle ]; \n");

            Set<StateEnum> statesVisited = new HashSet<>();
            Set<StateEnum> endStates = new HashSet<>();

            followGraph(flow.getStartState(), statesVisited, endStates, graph);

            graph.append("\t").append( flow.getStartState() ).append(" [style=filled, shape=circle,fillcolor=black, fontcolor=white];\n");

            for(StateEnum ends : endStates){
                graph.append("\t").append( ends ).append(" [style=filled, shape=doublecircle,fillcolor=black, fontcolor=white];\n");
            }


            graph.append("}");
            Files.write(Paths.get(filename), graph.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void followGraph(StateEnum stateFrom, Set<StateEnum> statesVisited, Set<StateEnum> endStates, StringBuffer graph) {

        if (statesVisited.add(stateFrom)) {
            List<Transition> transitions = flow.getAvailableTransitions(stateFrom);
            if (transitions.isEmpty()) {
                endStates.add(stateFrom);
            } else {
                for (Transition transistion : transitions) {
                    StateEnum stateTo = transistion.getStateTo();
                    EventEnum event = transistion.getEvent();

                    graph.append("\t")
                            .append(stateFrom).append(" -> ").append(stateTo)
                            .append(" [")
                            .append("label=\"").append(event)
                            .append("\"];\n");

                    followGraph(stateTo, statesVisited, endStates, graph);
                }
            }
        }
    }
}
