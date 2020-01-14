package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Edge implements Comparable<Edge> {
    private Node src;
    private Node tgt;
    private Integer frequency;
    private List<Event> sourceEvents;
    private List<Event> targetEvents;
    private int avgLogLength;

    Edge(Node src, Node tgt, Integer frequency){
        this.src = src;
        this.tgt = tgt;
        this.frequency = frequency;
        this.sourceEvents = new ArrayList<>();
        this.targetEvents = new ArrayList<>();
    }

    Edge(Node src, Node tgt){
        this.src = new Node(src);
        this.tgt = new Node(tgt);
        this.frequency = 0;
        this.sourceEvents = new ArrayList<>();
        this.targetEvents = new ArrayList<>();
    }

    void increaseFrequency(){
        this.frequency += 1;
    }

    public Node getSource(){ return this.src; }

    public Node getTarget(){ return this.tgt; }

    public Integer getFrequency(){ return this.frequency; }

    public List<Event> getSourceEvents(){ return this.sourceEvents; }

    public List<Event> getTargetEvents(){ return this.targetEvents; }

    void addEventPair(Event from, Event to){
        sourceEvents.add(from);
        targetEvents.add(to);

    }

    @Override
    public String toString() {
        return src.getEventType() + ">>" + tgt.getEventType();
    }

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Edge edge = (Edge) obj;
            return this.src.equals(edge.src) && this.tgt.equals(edge.tgt);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(src, tgt);
    }

    @Override
    public int compareTo(Edge e){
        return e.frequency - this.frequency;
    }
}