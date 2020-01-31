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

    void increaseFrequency() { this.frequency += 1; }

    @Override
    public String toString() {
        String context = "";

        if(this.context.containsKey("target.row"))
            context = this.context.get("target.row");
        else if(this.context.containsKey("target.column"))
            context = this.context.get("target.column");
        else if(this.context.containsKey("target.id"))
            context = this.context.get("target.id");
        else if(this.context.containsKey("target.name"))
            context = this.context.get("target.name");
        else if(this.context.containsKey("target.innerText"))
            context = this.context.get("target.innerText");
        else if(this.context.containsKey("url"))
            context = this.context.get("url");

        return eventType + "_" + context.replaceAll("[^a-zA-Z0-9]+", "_");
    }

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