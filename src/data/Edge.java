package data;

public class Edge {
    private Node from;
    private Node to;
    private Integer frequency;

    public Edge(Node from, Node to, Integer frequency){
        this.from = new Node(from);
        this.to = new Node(to);
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
}