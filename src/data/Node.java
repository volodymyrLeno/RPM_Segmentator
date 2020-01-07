package data;

import java.util.HashMap;
import java.util.Objects;

public class Node {
    private String eventType;
    private HashMap<String, String> context;
    private Integer frequency;

    public Node(String eventType, HashMap<String, String> context, Integer frequency){
        this.eventType = eventType;
        this.context = new HashMap<>(context);
        this.frequency = frequency;
    }

    public Node(Node node){
        this.eventType = node.getEventType();
        this.context = new HashMap<>(node.getContext());
        this.frequency = node.getFrequency();
    }

    public String getEventType(){ return this.eventType; }

    public Integer getFrequency(){ return this.frequency; }

    public HashMap<String, String> getContext(){ return this.context; }

    public void increaseFrequency() { this.frequency += 1; }

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Node node = (Node) obj;
            if(!this.eventType.equals(node.eventType) || !this.context.equals(node.context))
                return false;
            else
                return true;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(eventType, context);
    }
}