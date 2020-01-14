import data.DirectlyFollowsGraph;
import data.Edge;
import data.Event;
import data.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmentsDiscoverer {

    public SegmentsDiscoverer() {}


    public Map<Integer, List<Event>> extractSegmentsFromDFG(DirectlyFollowsGraph dfg) {
        Map<Integer, List<Event>> segments = new HashMap<>();
        List<Edge> loops = new ArrayList<>();

        List<Node> nodes = dfg.getNodes();
        List<Event> uiLog = dfg.getEvents();
        Map<Node, List<Edge>> outgoings = dfg.getOutgoingEdges();

        Map<Node, Integer> depths = new HashMap<>();
        Node source = nodes.get(0);
        Node target;
        Edge next;

        int depth = 0;
        depths.put(source, depth);
        System.out.println("DEBUG - ("+ depth +") node: " + source.toString());

        ArrayList<Edge> unexplored = new ArrayList<>();
        for(Edge e : outgoings.get(source))
            unexplored.add(e);

        while(!unexplored.isEmpty()) {
            depth++;
            unexplored.add(null);
            while((next = unexplored.remove(0)) != null) {
                target = next.getTarget();
                if( depths.get(target) == null ) {
                    depths.put(target, depth);
                    System.out.println("DEBUG - ("+ depth +") node: " + target.toString());
                    for(Edge e : outgoings.get(target)) unexplored.add(e);
                } else {
                    if( depth < depths.get(target) ) {
                        System.out.println("ERROR 0001 - this should not happen.");
                    } else {
                        System.out.println("DEBUG - found a loop edge ("+ depths.get(target) + ","+ depth +"): " + next.toString() );
                        loops.add(next);
                    }
                }
            }
            System.out.println("DEBUG - null (" + depth + ")");
        }

        return segments;
    }

}
