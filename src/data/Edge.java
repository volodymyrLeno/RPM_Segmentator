package data;

import java.util.Objects;

public class Edge {
    private Node from;
    private Node to;
    private Integer frequency;

    public Edge(Node from, Node to, Integer frequency){
        this.from = from;
        this.to = to;
        this.frequency = frequency;
    }

    public Edge(Node from, Node to){
        this.from = new Node(from);
        this.to = new Node(to);
        this.frequency = 0;
    }

    public void increaseFrequency(){
        this.frequency += 1;
    }

    public Node getFromNode(){ return this.from; }

    public Node getToNode(){ return this.to; }

    public Integer getFrequency(){ return this.frequency; }

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Edge edge = (Edge) obj;
            if(!this.from.equals(edge.from) || !this.to.equals(edge.to))
                return false;
            else
                return true;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(from, to);
    }
}