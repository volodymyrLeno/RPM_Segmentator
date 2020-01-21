import data.DirectlyFollowsGraph;
import data.Edge;
import data.Event;
import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class SegmentsDiscoverer {

    public SegmentsDiscoverer() {}


    public Map<Integer, List<Event>> extractSegmentsFromDFG(DirectlyFollowsGraph dfg) {
        //List<Edge> loops = dfg.discoverLoops();
        //List<Edge> loops = dfg.identifyBackEdges(dfg.getAdjacencyMatrix(), 0);
        //Map<Integer, List<Event>> segments = discoverSegments(dfg, loops);

        List<Edge> loops = dfg.getLoops();

        var segments = discoverSegments(dfg, loops);
        return segments;
    }

    private Map<Integer, List<Event>> discoverSegments(DirectlyFollowsGraph dfg, List<Edge> loops){
        Map<Integer, List<Event>> segments = new HashMap<>();
        List<Event> uiLog = dfg.getEvents();
        Map<Integer, Integer> pairs;

        int eCounts = uiLog.size();
        Event next = null;

        uiLog.get(0).setStart(true);
        uiLog.get(eCounts-1).setEnd(true);

        //rankByFrequency(loops);
        //rankByLogLength(loops);
        rankByGraphDistance(loops, dfg);

        //var backEdges = dfg.identifyBackEdges(dfg.getAdjacencyMatrix(), 0);

        var sccs = dfg.getSCComponents(dfg.getAdjacencyMatrix());

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
        rankedEdges.sort(comparing(Edge::getFrequency).reversed());
        return rankedEdges;
    }

    private List<Edge> rankByLogLength(List<Edge> edges){
        List<Edge> rankedEdges = new ArrayList<>(edges);
        rankedEdges.sort(comparing(Edge::getAvgLogLength).reversed());
        return rankedEdges;
    }

    private List<Edge> rankByGraphDistance(List<Edge> edges, DirectlyFollowsGraph dfg){

        //DirectlyFollowsGraph acyclicDFG = new DirectlyFollowsGraph(dfg);
        //acyclicDFG.removeLoops();
        //acyclicDFG.convertIntoDOT();

        HashMap<Edge, Integer> longestDistance = new HashMap<>();

        for(var edge: edges)
            longestDistance.put(edge, dfg.getLongestPath(edge.getTarget(), edge.getSource()));

        /*
        for(var edge: edges)
            dijkstrasDistance.put(edge, dfg.getShortestPath(edge.getTarget(), edge.getSource()));
            */

        Map<Edge, Integer> sorted = longestDistance.entrySet().stream().sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        return new ArrayList<>(sorted.keySet());
    }
}