package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DirectlyFollowsGraph {
    List<Node> nodes;
    List<Edge> edges;

    public void buildGraph(List<Event> events){
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        Event previousEvent = null;

        for(var event: events){
            Node node = new Node(event.getEventType(), event.context);
            if(!nodes.contains(node))
                nodes.add(node);
            if(previousEvent != null){
                Node from = new Node(previousEvent.getEventType(), previousEvent.context);
                Node to = new Node(event.getEventType(), event.context);
                Edge edge = new Edge(from, to);
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
}