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
    private HashMap<Node, List<Node>> incoming;
    private HashMap<Node, List<Node>> outgoing;

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

    public HashMap<Node, List<Node>> getIncomingEdges() { return incoming; }

    public HashMap<Node, List<Node>> getOutgoingEdges() { return outgoing; }

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
                Node from = null;
                Node to = null;
                for (Node n : nodes) {
                    if (previousEvent.getEventType().equals(n.getEventType()) && previousEvent.context.equals(n.getContext()))
                        from = n;
                    if (event.getEventType().equals(n.getEventType()) && event.context.equals(n.getContext()))
                        to = n;
                    if (from != null && to != null)
                        break;
                }
                Edge edge = new Edge(from, to, 1);
                if(!edges.contains(edge)){
                    edges.add(edge);
                    updateIncomingEdges(from, to);
                    updateOutgoingEdges(from, to);
                }
                else{
                    updateIncomingEdges(from, to);
                    updateOutgoingEdges(from, to);
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
            if(edge.getFromNode().getContext().containsKey("target.row"))
                contextFrom = edge.getFromNode().getContext().get("target.row");
            else if(edge.getFromNode().getContext().containsKey("target.column"))
                contextFrom = edge.getFromNode().getContext().get("target.column");
            else if(edge.getFromNode().getContext().containsKey("target.id"))
                contextFrom = edge.getFromNode().getContext().get("target.id");
            else if(edge.getFromNode().getContext().containsKey("target.name"))
                contextFrom = edge.getFromNode().getContext().get("target.name");
            else if(edge.getFromNode().getContext().containsKey("target.innerText"))
                contextFrom = edge.getFromNode().getContext().get("target.innerText");

            if(edge.getToNode().getContext().containsKey("target.row"))
                contextTo = edge.getToNode().getContext().get("target.row");
            else if(edge.getToNode().getContext().containsKey("target.column"))
                contextTo = edge.getToNode().getContext().get("target.column");
            else if(edge.getToNode().getContext().containsKey("target.id"))
                contextTo = edge.getToNode().getContext().get("target.id");
            else if(edge.getToNode().getContext().containsKey("target.name"))
                contextTo = edge.getToNode().getContext().get("target.name");
            else if(edge.getToNode().getContext().containsKey("target.innerText"))
                contextTo = edge.getToNode().getContext().get("target.innerText");


            DOT = DOT + "   " + edge.getFromNode().getEventType() + "_" + contextFrom.replaceAll("[^a-zA-Z0-9]+", "_") + " -> " +
                    edge.getToNode().getEventType() + "_" + contextTo.replaceAll("[^a-zA-Z0-9]+", "_") + " [label=" + edge.getFrequency() + "];" + "\n";
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

    private void updateIncomingEdges(Node from, Node to){
        if(!incoming.containsKey(to))
            incoming.put(to, Collections.singletonList(from));
        else if(!incoming.get(to).contains(from))
            incoming.put(to, Stream.concat(incoming.get(to).stream(), Stream.of(from)).collect(Collectors.toList()));
    }

    private void updateOutgoingEdges(Node from, Node to){
        if(!outgoing.containsKey(from))
            outgoing.put(from, Collections.singletonList(to));
        else if(!outgoing.get(from).contains(to))
            outgoing.put(from, Stream.concat(outgoing.get(from).stream(), Stream.of(to)).collect(Collectors.toList()));
    }
}