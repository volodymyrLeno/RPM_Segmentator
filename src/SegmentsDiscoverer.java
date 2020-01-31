import data.DirectlyFollowsGraph;
import data.Edge;
import data.Event;
import data.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class SegmentsDiscoverer {

    public SegmentsDiscoverer() {}


    public Map<Integer, List<Event>> extractSegmentsFromDFG(DirectlyFollowsGraph dfg) {
        System.out.println("\nExtracting segments...\n");

        var domMap = dfg.getDominatorsMap();

        /* Approach based on loop nested forest */

        List<Edge> loops = new ArrayList<>();
        discoverBackEdges(dfg, domMap, loops, 0);
        System.out.println("DEBUG - Back edges identified:");
        for(var loop: loops){
            var longestDistance = dfg.getLongestPath(loop.getTarget(), loop.getSource());
            //var longestPath = dfg.getAllPaths(loop.getTarget(), loop.getSource()).stream().filter(el -> el.size() == longestDistance + 1).collect(Collectors.toList());
            System.out.println("DEBUG - " + loop + " (frequency = " + loop.getFrequency() + ", longestDistance = " + longestDistance + ")");
        }
        System.out.println();

        /* Approach based on identification of back edges during creation of DFG */

        //List<Edge> loops = new ArrayList<>(dfg.getLoops());

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

        var rank1 = rankByFrequency(loops);
        var rank2 = rankByLogLength(loops);
        var rank3 = rankByGraphDistance(loops, dfg);

        List<List<Edge>> rankings = new ArrayList<>(){{ add(rank1); add(rank2); add(rank3); }};
        var overallRank = getAggregatedRanking(rankings);

        int lCount = 0;

        HashMap<Event, List<Event>> startMatches = new HashMap<>();


        for(Edge loop: loops){
            for(Event start: uiLog){
                if(start.getEventType().equals(loop.getTarget().getEventType()) && start.context.equals(loop.getTarget().getContext())){
                    uiLog.get(start.getID()).setStart(true);
                    if(!startMatches.containsKey(start))
                        startMatches.put(start, new ArrayList<>(loop.getSourceEvents().stream().filter(event ->
                                event.getTimestamp().compareTo(start.getTimestamp()) > 0).collect(Collectors.toList())));
                    else
                        startMatches.put(start, new ArrayList<>(Stream.concat(startMatches.get(start).stream(),
                                loop.getSourceEvents().stream().filter(event ->
                                        event.getTimestamp().compareTo(start.getTimestamp()) > 0)).collect(Collectors.toList())));
                }
            }
            for(Event end: uiLog){
                if(end.getEventType().equals(loop.getSource().getEventType()) && end.context.equals(loop.getSource().getContext()))
                    uiLog.get(end.getID()).setEnd(true);
            }
        }

        int i = 0;
        int caseID = 0;
        boolean within = false;
        List<Event> segment = null;
        Event start = null;
        int totalLength = 0;
        do {
             next = uiLog.get(i);
             i++;

            if(within) {
                segment.add(next);
                if(next.isEnd() && (startMatches.get(start).contains(next) || i == eCounts)) {
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
                start = next;
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
        HashMap<Edge, Integer> longestDistance = new HashMap<>();

        for(var edge: edges)
            longestDistance.put(edge, dfg.getLongestPath(edge.getTarget(), edge.getSource()));

        Map<Edge, Integer> sorted = longestDistance.entrySet().stream().sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        return new ArrayList<>(sorted.keySet());
    }

    private List<Edge> getAggregatedRanking(List<List<Edge>> rankings){
        List<Edge> edges = new ArrayList<>(rankings.get(0));
        HashMap<Edge, Double> scores = new HashMap<>();
        for(var edge: edges){
            Double score = 0.0;
            for(var ranking: rankings)
                score += ranking.indexOf(edge);
            scores.put(edge, score/rankings.size());
        }
        Map<Edge, Double> sorted = scores.entrySet().stream().sorted(comparingByValue())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        return new ArrayList<>(sorted.keySet());
    }

    /*

    private List<Edge> discoverBackEdges(DirectlyFollowsGraph dfg, HashMap<Node, List<Node>> dominatorsMap, List<Edge> loops){
        List<DirectlyFollowsGraph> sccs = dfg.getSCComponents(dfg.getAdjacencyMatrix());
        for(var scc: sccs){
            if(scc.getNodes().size() > 1){
                var backEdges = getBackEdges(scc, dominatorsMap);
                //System.out.println("Back edges: " + backEdges);
                loops.addAll(new ArrayList<>(backEdges));
                scc.removeEdges(backEdges);
                discoverBackEdges(scc, dominatorsMap, loops);
            }
        }
        return loops;
    }

    private List<Edge> getBackEdges(DirectlyFollowsGraph scc, HashMap<Node, List<Node>> dominatorsMap){
        List<Edge> backEdges = new ArrayList<>();

        for(var header: getHeaders(scc, dominatorsMap))
            for(var edge: scc.getEdges())
                if(edge.getTarget().equals(header))
                    backEdges.add(edge);
                return backEdges;
    }
    */

    private List<Edge> discoverBackEdges(DirectlyFollowsGraph dfg, HashMap<Node, List<Node>> dominatorsMap, List<Edge> loops, int i){
        var k = i+1;
        List<DirectlyFollowsGraph> sccs = dfg.getSCComponents(dfg.getAdjacencyMatrix());
        for(var scc: sccs){
            if(scc.getNodes().size() > 1){
                var backEdges = getBackEdges(scc, dominatorsMap);

                if(backEdges == null){
                    List<Edge> loopCandidates = rankByGraphDistance(scc.identifyBackEdges(scc.getAdjacencyMatrix(), 0), scc);
                    scc.removeEdges(Collections.singletonList(loopCandidates.get(0)));
                    discoverBackEdges(scc, dominatorsMap, loops, i);
                }
                else{
                    System.out.println(backEdges + " (level = " + k + ")");
                    loops.addAll(new ArrayList<>(backEdges));
                    scc.removeEdges(backEdges);
                    discoverBackEdges(scc, dominatorsMap, loops, k);
                }
            }
        }
        return loops;
    }

    private List<Edge> getBackEdges(DirectlyFollowsGraph scc, HashMap<Node, List<Node>> dominatorsMap){
        List<Edge> backEdges = new ArrayList<>();

        var header = getHeader(scc, dominatorsMap);

        if(header == null){
            return null;
        }
        else{
            for(var edge: scc.getEdges())
                if(edge.getTarget().equals(header))
                    backEdges.add(edge);
            return Collections.singletonList(rankByGraphDistance(backEdges, scc).get(0));
        }
    }

    private Node getHeader(DirectlyFollowsGraph scc, HashMap<Node, List<Node>> dominatorsMaps){
        Node header = null;
        for(var key: dominatorsMaps.keySet()){
            if(scc.getNodes().contains(key) && dominatorsMaps.get(key).containsAll(scc.getNodes().stream().filter(el -> !el.equals(key)).collect(Collectors.toList()))){
                header = new Node(key);
                break;
            }
        }
        // testing
        //if(header == null)
        //    header = new Node(scc.getNodes().get(0));
        return header;
    }
}