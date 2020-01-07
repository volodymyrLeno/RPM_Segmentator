package data;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DirectlyFollowsGraph {
    List<Event> events;
    List<Node> nodes;
    List<Edge> edges;

    public DirectlyFollowsGraph(List<Event> events){
        this.events = new ArrayList<>(events);
    }

    public void buildGraph(){
        System.out.println("Building DFG...\n");
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        Event previousEvent = null;

        for(var event: this.events){
            Node node = new Node(event.getEventType(), event.context, 1);
            if(!nodes.contains(node))
                nodes.add(node);
            else{
                for(int i = 0; i < nodes.size(); i++)
                    if(node.equals(nodes.get(i))){
                        nodes.get(i).increaseFrequency();
                        break;
                    }
            }
            if(previousEvent != null) {
                Node from = null;
                Node to = null;
                for (int i = 0; i < nodes.size(); i++){
                    if (previousEvent.getEventType().equals(nodes.get(i).getEventType()) && previousEvent.context.equals(nodes.get(i).getContext()))
                        from = nodes.get(i);
                    if (event.getEventType().equals(nodes.get(i).getEventType()) && event.context.equals(nodes.get(i).getContext()))
                        to = nodes.get(i);
                    if(from != null && to != null)
                        break;
                }
                Edge edge = new Edge(from, to, 1);
                if(!edges.contains(edge))
                    edges.add(edge);
                else{
                    for(int i = 0; i < edges.size(); i++)
                        if(edges.get(i).getFromNode().equals(from) && edges.get(i).getToNode().equals(to)){
                            edges.get(i).increaseFrequency();
                            break;
                        }
                }
            }
            previousEvent = event;
        }
        this.nodes = new ArrayList<>(nodes);
        this.edges = new ArrayList<>(edges);
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


            DOT = DOT + "   " + edge.getFromNode().getEventType() + "_" + contextFrom.replaceAll(" ", "_").replaceAll("\\.", "") + " -> " +
                    edge.getToNode().getEventType() + "_" + contextTo.replaceAll(" ", "_").replaceAll("\\.","") + " [label=" + edge.getFrequency() + "];" + "\n";
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
}