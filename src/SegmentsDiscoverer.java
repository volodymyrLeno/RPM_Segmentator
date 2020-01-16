import data.DirectlyFollowsGraph;
import data.Edge;
import data.Event;
import data.Node;

import java.util.*;

import static java.util.Comparator.comparing;

public class SegmentsDiscoverer {

    public SegmentsDiscoverer() {}


    public Map<Integer, List<Event>> extractSegmentsFromDFG(DirectlyFollowsGraph dfg) {
        List<Edge> loops = discoverLoopsNaive(dfg);
        Map<Integer, List<Event>> segments = discoverSegments(dfg, loops);

        return segments;

    }

    private List<Edge> discoverLoopsNaive(DirectlyFollowsGraph dfg) {
        List<Edge> loops = new ArrayList<>();

        List<Node> nodes = dfg.getNodes();
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
                        System.out.println("DEBUG - found a loop edge ("+ depths.get(target) + ","+ depth +"): " + next.toString() + " - " + next.getFrequency() +
                                " logLength = " + next.getAvgLogLength() + ", topologicalDepth = " + Math.abs(depth - depths.get(target)));
                        loops.add(next);
                    }
                }
            }
            System.out.println("DEBUG - null (" + depth + ")");
        }

        int f = 0;
        for(Edge e : loops) f+= e.getFrequency();
        System.out.println("DEBUG - total loops discovered ("+ f +"): " + loops.size());

        return loops;
    }

    private Map<Integer, List<Event>> discoverSegments(DirectlyFollowsGraph dfg, List<Edge> loops){
        Map<Integer, List<Event>> segments = new HashMap<>();
        List<Event> uiLog = dfg.getEvents();
        Map<Integer, Integer> pairs;

        int eCounts = uiLog.size();
        Event next = null;

        uiLog.get(0).setStart(true);
        uiLog.get(eCounts-1).setEnd(true);

        Collections.sort(loops, comparing(Edge::getAvgLogLength));

        int lCount = 0;
        for(Edge loop : loops) {
            for(Event start : loop.getTargetEvents()) uiLog.get(start.getID()).setStart(true);
            for(Event end : loop.getSourceEvents()) uiLog.get(end.getID()).setEnd(true);
        }

        int i = 0;
        int caseID = 0;
        boolean within = false;
        List<Event> segment = null;
        int start;
        int totalLength = 0;
        do {
             next = uiLog.get(i);
             i++;

            if(within) {
                segment.add(next);
                if(next.isEnd()) {
                    segments.put(caseID, segment);
                    caseID++;
                    within = false;
                    totalLength+=segment.size();
                    System.out.println("DEBUG - discovered segment of length: " + segment.size());
                }
            } else if(next.isStart()) {
                segment = new ArrayList<>();
                segment.add(next);
                within = true;
            }

        } while(i!=eCounts);

        System.out.println("DEBUG - total segments discovered: " + caseID);
        System.out.println("DEBUG - total events ("+ i +") into segments: " + totalLength);
        return segments;
    }

    private List<Edge> rankByFrequency(List<Edge> edges){
        List<Edge> rankedEdges = new ArrayList<>(edges);
        Collections.sort(edges, comparing(Edge::getFrequency));
        return rankedEdges;
    }

    private List<Edge> rankByLogLength(List<Edge> edges){
        List<Edge> rankedEdges = new ArrayList<>(edges);
        Collections.sort(edges, comparing(Edge::getAvgLogLength));
        return rankedEdges;
    }

}
