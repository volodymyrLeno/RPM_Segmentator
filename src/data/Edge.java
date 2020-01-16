package data;

import java.util.*;

public class Edge implements Comparable<Edge> {
    private Node src;
    private Node tgt;
    private Integer frequency;
    private List<Event> sourceEvents;
    private List<Event> targetEvents;
    private Map<Integer, Integer> pairs;
    private int avgLogLength;

    Edge(Node src, Node tgt, Integer frequency){
        this.src = src;
        this.tgt = tgt;
        this.frequency = frequency;
        this.sourceEvents = new ArrayList<>();
        this.targetEvents = new ArrayList<>();
        this.pairs = new HashMap<>();
        avgLogLength = 0;
    }

    Edge(Node src, Node tgt){
        this.src = new Node(src);
        this.tgt = new Node(tgt);
        this.frequency = 0;
        this.sourceEvents = new ArrayList<>();
        this.targetEvents = new ArrayList<>();
        this.pairs = new HashMap<>();
        avgLogLength = 0;
    }

    void increaseFrequency(){
        this.frequency += 1;
    }

    public Node getSource(){ return this.src; }

    public Node getTarget(){ return this.tgt; }

    public Integer getFrequency(){ return this.frequency; }

    public List<Event> getSourceEvents(){ return this.sourceEvents; }

    public List<Event> getTargetEvents(){ return this.targetEvents; }

    public Map<Integer, Integer> getEventPairs() { return this.pairs; }

    public void addEventPair(Event src, Event tgt){
        if(!pairs.isEmpty())
            avgLogLength += (src.getID() - targetEvents.get(targetEvents.size()-1).getID());
        sourceEvents.add(src);
        targetEvents.add(tgt);
        pairs.put(src.getID(), tgt.getID());
    }

    public int getAvgLogLength() { return pairs.size() != 1 ? avgLogLength/(pairs.size()-1) : 0; }

    @Override
    public String toString() {
        return src.toString() + ">>" + tgt.toString();
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