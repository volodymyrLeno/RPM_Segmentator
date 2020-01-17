package data;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectlyFollowsGraph {
    private List<Event> events;
    private List<Node> nodes;
    private List<Edge> edges;
    private HashMap<Node, List<Edge>> incoming;
    private HashMap<Node, List<Edge>> outgoing;

    public DirectlyFollowsGraph(List<Event> events){
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.incoming = new HashMap<>();
        this.outgoing = new HashMap<>();
        this.events = new ArrayList<>(events);
    }

    public DirectlyFollowsGraph(DirectlyFollowsGraph dfg){
        this.nodes = new ArrayList<>(dfg.getNodes());
        this.edges = new ArrayList<>(dfg.getEdges());
        this.incoming = new HashMap<>(dfg.getIncomingEdges());
        this.outgoing = new HashMap<>(dfg.getOutgoingEdges());
        this.events = new ArrayList<>(dfg.getEvents());
    }

    public List<Node> getNodes(){ return nodes; }

    public List<Edge> getEdges() { return edges; }

    public List<Event> getEvents() { return events; }

    public HashMap<Node, List<Edge>> getIncomingEdges() { return incoming; }

    public HashMap<Node, List<Edge>> getOutgoingEdges() { return outgoing; }

    public void buildGraph(){
        System.out.println("Building DFG...\n");
        Event previousEvent = null;

        for(var event: events){
            Node node = new Node(event.getEventType(), event.context, 1);
            if(!nodes.contains(node))
                nodes.add(node);
            else
                nodes.get(nodes.indexOf(node)).increaseFrequency();

            if(previousEvent != null) {
                Node src = null;
                Node tgt = null;
                for (Node n : nodes) {
                    if (previousEvent.getEventType().equals(n.getEventType()) && previousEvent.context.equals(n.getContext()))
                        src = n;
                    if (event.getEventType().equals(n.getEventType()) && event.context.equals(n.getContext()))
                        tgt = n;
                    if (src != null && tgt != null)
                        break;
                }
                Edge edge = new Edge(src, tgt, 1);

                if(!edges.contains(edge)){
                    edge.addEventPair(previousEvent, event);
                    edges.add(edge);
                    updateIncomingEdges(src, tgt);
                    updateOutgoingEdges(src, tgt);
                }
                else{
                    updateIncomingEdges(src, tgt);
                    updateOutgoingEdges(src, tgt);
                    edges.get(edges.indexOf(edge)).addEventPair(previousEvent, event);
                    edges.get(edges.indexOf(edge)).increaseFrequency();
                }
            }
            previousEvent = event;
        }
    }

    public void convertIntoDOT(){
        String DOT = "digraph g {\n";
        for(Edge edge: this.edges){
            String contextFrom = "";
            String contextTo = "";
            if(edge.getSource().getContext().containsKey("target.row"))
                contextFrom = edge.getSource().getContext().get("target.row");
            else if(edge.getSource().getContext().containsKey("target.column"))
                contextFrom = edge.getSource().getContext().get("target.column");
            else if(edge.getSource().getContext().containsKey("target.id"))
                contextFrom = edge.getSource().getContext().get("target.id");
            else if(edge.getSource().getContext().containsKey("target.name"))
                contextFrom = edge.getSource().getContext().get("target.name");
            else if(edge.getSource().getContext().containsKey("target.innerText"))
                contextFrom = edge.getSource().getContext().get("target.innerText");
            else if(edge.getSource().getContext().containsKey("url"))
                contextFrom = edge.getSource().getContext().get("url");

            if(edge.getTarget().getContext().containsKey("target.row"))
                contextTo = edge.getTarget().getContext().get("target.row");
            else if(edge.getTarget().getContext().containsKey("target.column"))
                contextTo = edge.getTarget().getContext().get("target.column");
            else if(edge.getTarget().getContext().containsKey("target.id"))
                contextTo = edge.getTarget().getContext().get("target.id");
            else if(edge.getTarget().getContext().containsKey("target.name"))
                contextTo = edge.getTarget().getContext().get("target.name");
            else if(edge.getTarget().getContext().containsKey("target.innerText"))
                contextTo = edge.getTarget().getContext().get("target.innerText");
            else if(edge.getTarget().getContext().containsKey("url"))
                contextTo = edge.getTarget().getContext().get("url");


            DOT = DOT + "   " + edge.getSource().getEventType() + "_" + contextFrom.replaceAll("[^a-zA-Z0-9]+", "_") + " -> " +
                    edge.getTarget().getEventType() + "_" + contextTo.replaceAll("[^a-zA-Z0-9]+", "_") + " [label=" + edge.getFrequency() + "];" + "\n";
        }
        DOT = DOT + "}";
        try{
            PrintWriter writer = new PrintWriter("TEMP.dot");
            writer.print(DOT);
            writer.close();
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public Integer[][] getAdjacencyMatrix(){
        Integer[][] adjacencyMatrix = new Integer[nodes.size()][nodes.size()];
        for(int i = 0; i < nodes.size(); i++){
            for(int j = 0; j < nodes.size(); j++){
                Edge edge = new Edge(nodes.get(i), nodes.get(j));
                if(edges.contains(edge))
                    adjacencyMatrix[i][j] = edges.get(edges.indexOf(edge)).getFrequency();
                else
                    adjacencyMatrix[i][j] = 0;
                //System.out.print(adjacencyMatrix[i][j] + " ");
            }
            //System.out.println();
        }

        return adjacencyMatrix;
    }

    public Integer[][] transposeAdjacencyMatrix(Integer[][] adj){
        Integer[][] transposedMatrix = new Integer[adj.length][adj[0].length];
        for(int i = 0; i < adj.length; i++){
            for(int j = 0; j < adj[i].length; j++){
                transposedMatrix[i][j] = adj[j][i];
            }
        }
        return transposedMatrix;
    }

    private void updateIncomingEdges(Node src, Node tgt){
        Edge incomingEdge = edges.stream().filter(el -> el.getSource().equals(src) && el.getTarget().equals(tgt)).findFirst().orElse(null);
        if(incomingEdge != null) {
            if (!incoming.containsKey(tgt))
                incoming.put(tgt, Collections.singletonList(incomingEdge));
            else if (!incoming.get(tgt).contains(incomingEdge))
                incoming.put(tgt, Stream.concat(incoming.get(tgt).stream(), Stream.of(incomingEdge)).collect(Collectors.toList()));
        }
    }

    private void updateOutgoingEdges(Node src, Node to){
        Edge outgoingEdge = edges.stream().filter(el -> el.getSource().equals(src) && el.getTarget().equals(to)).findFirst().orElse(null);
        if(outgoingEdge != null) {
            if (!outgoing.containsKey(src))
                outgoing.put(src, Collections.singletonList(outgoingEdge));
            else if (!outgoing.get(src).contains(outgoingEdge))
                outgoing.put(src, Stream.concat(outgoing.get(src).stream(), Stream.of(outgoingEdge)).collect(Collectors.toList()));
        }
    }

    public List<Node> getAdjacent(Node source){
        return incoming.get(source).stream().map(el -> el.getTarget()).collect(Collectors.toList());
    }

    public void removeLoops(List<Edge> loops){
        for(var loop: loops)
            edges.remove(edges.indexOf(loop));

        HashMap<Node, List<Edge>> out = new HashMap<>();
        HashMap<Node, List<Edge>> in = new HashMap<>();

        for(Node node: nodes){
            List<Edge> incomingEdges = edges.stream().filter(edge -> edge.getTarget().equals(node)).collect(Collectors.toList());
            List<Edge> outgoingEdges = edges.stream().filter(edge -> edge.getSource().equals(node)).collect(Collectors.toList());

            in.put(node, incomingEdges);
            out.put(node, outgoingEdges);
        }

        incoming = new HashMap<>(in);
        outgoing = new HashMap<>(out);
    }

    public List<Edge> discoverLoops() {
        List<Edge> loops = new ArrayList<>();

        Map<Node, Integer> depths = new HashMap<>();
        Node source = nodes.get(0);
        Node target;
        Edge next;

        int depth = 0;
        depths.put(source, depth);
        System.out.println("DEBUG - ("+ depth +") node: " + source.toString());

        ArrayList<Edge> unexplored = new ArrayList<>();
        for(Edge e : outgoing.get(source))
            unexplored.add(e);

        while(!unexplored.isEmpty()) {
            depth++;
            unexplored.add(null);
            while((next = unexplored.remove(0)) != null) {
                target = next.getTarget();
                if( depths.get(target) == null ) {
                    depths.put(target, depth);
                    System.out.println("DEBUG - ("+ depth +") node: " + target.toString());
                    for(Edge e : outgoing.get(target)) unexplored.add(e);
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
}