package data;

import java.util.HashMap;
import java.util.Objects;

public class Node {
    private String eventType;
    private HashMap<String, String> context;

    public Node(String eventType, HashMap<String, String> context){
        this.eventType = eventType;
        this.context = new HashMap<>(context);
    }

    public Node(Node node){
        this.eventType = node.getEventType();
        this.context = new HashMap<>(node.getContext());
    }

    public String getEventType(){ return this.eventType; }

    public HashMap<String, String> getContext(){ return this.context; }

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

    public int hashCode(){
        return Objects.hash(eventType, context);
    }
}