package data;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
}