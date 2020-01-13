package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Edge {
    private Node from;
    private Node to;
    private Integer frequency;
    private List<Event> startEvents;
    private List<Event> endEvents;

    Edge(Node from, Node to, Integer frequency){
        this.from = from;
        this.to = to;
        this.frequency = frequency;
        this.startEvents = new ArrayList<>();
        this.endEvents = new ArrayList<>();
    }

    Edge(Node from, Node to){
        this.from = new Node(from);
        this.to = new Node(to);
        this.frequency = 0;
        this.startEvents = new ArrayList<>();
        this.endEvents = new ArrayList<>();
    }

    void increaseFrequency(){
        this.frequency += 1;
    }

    Node getFromNode(){ return this.from; }

    Node getToNode(){ return this.to; }

    Integer getFrequency(){ return this.frequency; }

    List<Event> getStartEvents(){ return this.startEvents; }

    List<Event> getEndEvents(){ return this.endEvents; }

    void addEventPair(Event from, Event to){
        startEvents.add(from);
        endEvents.add(to);
    }

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Edge edge = (Edge) obj;
            return this.from.equals(edge.from) && this.to.equals(edge.to);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(from, to);
    }
}