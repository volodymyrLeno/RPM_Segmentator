package data;

import java.util.HashMap;
import java.util.Objects;

public class Node {
    private String eventType;
    private HashMap<String, String> context;
    private Integer frequency;

    Node(String eventType, HashMap<String, String> context, Integer frequency){
        this.eventType = eventType;
        this.context = new HashMap<>(context);
        this.frequency = frequency;
    }

    Node(Node node){
        this.eventType = node.getEventType();
        this.context = new HashMap<>(node.getContext());
        this.frequency = node.getFrequency();
    }

    public String getEventType(){ return this.eventType; }

    public Integer getFrequency(){ return this.frequency; }

    HashMap<String, String> getContext(){ return this.context; }

    void increaseFrequency() { this.frequency += 1; }

    @Override
    public String toString() { return eventType;}

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Node node = (Node) obj;
            return this.eventType.equals(node.eventType) && this.context.equals(node.context);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(eventType, context);
    }
}