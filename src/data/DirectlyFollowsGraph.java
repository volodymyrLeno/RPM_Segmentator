package data;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class DirectlyFollowsGraph {
    private List<Event> events;
    private List<Node> nodes;
    private List<Edge> edges;
    private HashMap<Node, List<Edge>> incoming;
    private HashMap<Node, List<Edge>> outgoing;
    private List<Edge> loops;

    public DirectlyFollowsGraph(List<Event> events){
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.incoming = new HashMap<>();
        this.outgoing = new HashMap<>();
        this.events = new ArrayList<>(events);
        this.loops = new ArrayList<>();
    }

    public DirectlyFollowsGraph(DirectlyFollowsGraph dfg){
        this.nodes = new ArrayList<>(dfg.getNodes());
        this.edges = new ArrayList<>(dfg.getEdges());
        this.incoming = new HashMap<>(dfg.getIncomingEdges());
        this.outgoing = new HashMap<>(dfg.getOutgoingEdges());
        this.events = new ArrayList<>(dfg.getEvents());
        this.loops = new ArrayList<>(dfg.getLoops());
    }

    public DirectlyFollowsGraph(List<Node> nodes, List<Edge> edges, List<Edge> loops){
        this.nodes = new ArrayList<>(nodes);
        this.edges = new ArrayList<>(edges);
        this.incoming = getIncomingEdges(this.edges);
        this.outgoing = getOutgoingEdges(this.edges);
        this.loops = loops.stream().filter(edge -> this.edges.contains(edge)).collect(Collectors.toList());

        List<Event> eventsList = new ArrayList<>();

        for(var edge: this.edges){
            eventsList.addAll(edge.getSourceEvents());
            eventsList.addAll(edge.getTargetEvents());
        }
        eventsList = eventsList.stream().distinct().collect(Collectors.toList());
        Collections.sort(eventsList, comparing(Event::getID));
        this.events = new ArrayList<>(eventsList);
    }

    public List<Node> getNodes(){ return nodes; }

    public List<Edge> getEdges() { return edges; }

    public List<Event> getEvents() { return events; }

    public HashMap<Node, List<Edge>> getIncomingEdges() { return incoming; }

    public HashMap<Node, List<Edge>> getIncomingEdges(List<Edge> edges){
        HashMap<Node, List<Edge>> incomingEdges = new HashMap<>();
        for(var edge: edges){
            if(!incomingEdges.containsKey(edge.getTarget()))
                incomingEdges.put(edge.getTarget(), Collections.singletonList(edge));
            else
                incomingEdges.put(edge.getTarget(), Stream.concat(incomingEdges.get(edge.getTarget()).stream(),
                        Stream.of(edge)).collect(Collectors.toList()));
        }
        return incomingEdges;
    }

    public HashMap<Node, List<Edge>> getOutgoingEdges() { return outgoing; }

    public HashMap<Node, List<Edge>> getOutgoingEdges(List<Edge> edges){
        HashMap<Node, List<Edge>> outgoingEdges = new HashMap<>();
        for(var edge: edges){
            if(!outgoingEdges.containsKey(edge.getSource()))
                outgoingEdges.put(edge.getSource(), Collections.singletonList(edge));
            else
                outgoingEdges.put(edge.getTarget(), Stream.concat(outgoingEdges.get(edge.getSource()).stream(),
                        Stream.of(edge)).collect(Collectors.toList()));
        }
        return outgoingEdges;
    }

    public List<Edge> getLoops(){ return this.loops; }

    /*
    public void buildGraph(){
        System.out.println("Building DFG...\n");
        Event previousEvent = null;

        for(var event: events){
            Node node = new Node(event.getEventType(), event.context, 1);
            if(!nodes.contains(node))
                nodes.add(node);
            else
                nodes.get(nodes.indexOf(node)).increaseFrequency();

            if(previousEvent != null) {
                Node src = null;
                Node tgt = null;
                for (Node n : nodes) {
                    if (previousEvent.getEventType().equals(n.getEventType()) && previousEvent.context.equals(n.getContext()))
                        src = n;
                    if (event.getEventType().equals(n.getEventType()) && event.context.equals(n.getContext()))
                        tgt = n;
                    if (src != null && tgt != null)
                        break;
                }
                Edge edge = new Edge(src, tgt, 1);

                if(!edges.contains(edge)){
                    edge.addEventPair(previousEvent, event);
                    edges.add(edge);
                    updateIncomingEdges(src, tgt);
                    updateOutgoingEdges(src, tgt);
                }
                else{
                    updateIncomingEdges(src, tgt);
                    updateOutgoingEdges(src, tgt);
                    edges.get(edges.indexOf(edge)).addEventPair(previousEvent, event);
                    edges.get(edges.indexOf(edge)).increaseFrequency();
                }
            }
            previousEvent = event;
        }
    }
    */

    public void buildGraph(){
        System.out.println("Building DFG...\n");
        Event previousEvent = null;
        Node previousNode = null;

        List<Edge> loops = new ArrayList<>();
        HashMap<Node, Set<Node>> reachability = new HashMap<>();
        Set<Node> comingFrom = new HashSet<>();

        for(var event: events) {
            Node node = new Node(event.getEventType(), event.context, 1);
            if (!nodes.contains(node)){
                nodes.add(node);
                addEdge(previousEvent, event);
                comingFrom.add(node);
                reachability.put(node, new HashSet<>(comingFrom));
            }
            else{
                nodes.get(nodes.indexOf(node)).increaseFrequency();
                addEdge(previousEvent, event);
                if(reachability.get(previousNode) != null && reachability.get(previousNode).contains(node)){
                    var loopEdge = new Edge(previousNode, node, 1);
                    if(!loops.contains(loopEdge))
                        loops.add(edges.get(edges.indexOf(loopEdge)));
                    comingFrom = new HashSet<>(reachability.get(loopEdge.getTarget()));
                }
                else{
                    comingFrom.add(node);
                }
                if(reachability.get(node) != null)
                    reachability.put(node, new HashSet<>(Stream.concat(reachability.get(node).stream(), comingFrom.stream()).collect(Collectors.toSet())));
            }
            previousEvent = event;
            previousNode = node;
        }
        this.loops = new ArrayList<>(loops);
    }

    public void addEdge(Event sourceEvent, Event targetEvent){
        if(sourceEvent != null) {
            Node src = null;
            Node tgt = null;
            for (Node n : nodes) {
                if (sourceEvent.getEventType().equals(n.getEventType()) && sourceEvent.context.equals(n.getContext()))
                    src = n;
                if (targetEvent.getEventType().equals(n.getEventType()) && targetEvent.context.equals(n.getContext()))
                    tgt = n;
                if (src != null && tgt != null)
                    break;
            }
            Edge edge = new Edge(src, tgt, 1);

            if(!this.edges.contains(edge)){
                edge.addEventPair(sourceEvent, targetEvent);
                edges.add(edge);
                updateIncomingEdges(src, tgt);
                updateOutgoingEdges(src, tgt);
            }
            else{
                updateIncomingEdges(src, tgt);
                updateOutgoingEdges(src, tgt);
                edges.get(edges.indexOf(edge)).addEventPair(sourceEvent, targetEvent);
                edges.get(edges.indexOf(edge)).increaseFrequency();
            }
        }
    }

    public void convertIntoDOT(){
        String DOT = "digraph g {\n";
        for(Edge edge: this.edges){
            String contextFrom = "";
            String contextTo = "";
            if(edge.getSource().getContext().containsKey("target.row"))
                contextFrom = edge.getSource().getContext().get("target.row");
            else if(edge.getSource().getContext().containsKey("target.column"))
                contextFrom = edge.getSource().getContext().get("target.column");
            else if(edge.getSource().getContext().containsKey("target.id"))
                contextFrom = edge.getSource().getContext().get("target.id");
            else if(edge.getSource().getContext().containsKey("target.name"))
                contextFrom = edge.getSource().getContext().get("target.name");
            else if(edge.getSource().getContext().containsKey("target.innerText"))
                contextFrom = edge.getSource().getContext().get("target.innerText");
            else if(edge.getSource().getContext().containsKey("url"))
                contextFrom = edge.getSource().getContext().get("url");

            if(edge.getTarget().getContext().containsKey("target.row"))
                contextTo = edge.getTarget().getContext().get("target.row");
            else if(edge.getTarget().getContext().containsKey("target.column"))
                contextTo = edge.getTarget().getContext().get("target.column");
            else if(edge.getTarget().getContext().containsKey("target.id"))
                contextTo = edge.getTarget().getContext().get("target.id");
            else if(edge.getTarget().getContext().containsKey("target.name"))
                contextTo = edge.getTarget().getContext().get("target.name");
            else if(edge.getTarget().getContext().containsKey("target.innerText"))
                contextTo = edge.getTarget().getContext().get("target.innerText");
            else if(edge.getTarget().getContext().containsKey("url"))
                contextTo = edge.getTarget().getContext().get("url");


            DOT = DOT + "   " + edge.getSource().getEventType() + "_" + contextFrom.replaceAll("[^a-zA-Z0-9]+", "_") + " -> " +
                    edge.getTarget().getEventType() + "_" + contextTo.replaceAll("[^a-zA-Z0-9]+", "_") + " [label=" + edge.getFrequency() + "];" + "\n";
        }
        DOT = DOT + "}";
        try{
            PrintWriter writer = new PrintWriter("TEMP.dot");
            writer.print(DOT);
            writer.close();
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public Integer[][] getAdjacencyMatrix(){
        Integer[][] adjacencyMatrix = new Integer[nodes.size()][nodes.size()];
        for(int i = 0; i < nodes.size(); i++){
            for(int j = 0; j < nodes.size(); j++){
                Edge edge = new Edge(nodes.get(i), nodes.get(j));
                if(edges.contains(edge))
                    adjacencyMatrix[i][j] = edges.get(edges.indexOf(edge)).getFrequency();
                else
                    adjacencyMatrix[i][j] = 0;
                //System.out.print(adjacencyMatrix[i][j] + " ");
            }
            //System.out.println();
        }

        return adjacencyMatrix;
    }

    public Integer[][] transposeAdjacencyMatrix(Integer[][] adj){
        Integer[][] transposedMatrix = new Integer[adj.length][adj[0].length];
        for(int i = 0; i < adj.length; i++){
            for(int j = 0; j < adj[i].length; j++){
                transposedMatrix[i][j] = adj[j][i];
            }
        }
        return transposedMatrix;
    }

    private void updateIncomingEdges(Node src, Node tgt){
        Edge incomingEdge = edges.stream().filter(el -> el.getSource().equals(src) && el.getTarget().equals(tgt)).findFirst().orElse(null);
        if(incomingEdge != null) {
            if (!incoming.containsKey(tgt))
                incoming.put(tgt, Collections.singletonList(incomingEdge));
            else if (!incoming.get(tgt).contains(incomingEdge))
                incoming.put(tgt, Stream.concat(incoming.get(tgt).stream(), Stream.of(incomingEdge)).collect(Collectors.toList()));
        }
    }

    private void updateOutgoingEdges(Node src, Node to){
        Edge outgoingEdge = edges.stream().filter(el -> el.getSource().equals(src) && el.getTarget().equals(to)).findFirst().orElse(null);
        if(outgoingEdge != null) {
            if (!outgoing.containsKey(src))
                outgoing.put(src, Collections.singletonList(outgoingEdge));
            else if (!outgoing.get(src).contains(outgoingEdge))
                outgoing.put(src, Stream.concat(outgoing.get(src).stream(), Stream.of(outgoingEdge)).collect(Collectors.toList()));
        }
    }

    public List<Node> getAdjacent(Node source){
        return incoming.get(source).stream().map(el -> el.getTarget()).collect(Collectors.toList());
    }

    public void removeLoops(){
        for(var loop: loops)
            edges.remove(edges.indexOf(loop));

        HashMap<Node, List<Edge>> out = new HashMap<>();
        HashMap<Node, List<Edge>> in = new HashMap<>();

        for(Node node: nodes){
            List<Edge> incomingEdges = edges.stream().filter(edge -> edge.getTarget().equals(node)).collect(Collectors.toList());
            List<Edge> outgoingEdges = edges.stream().filter(edge -> edge.getSource().equals(node)).collect(Collectors.toList());

            in.put(node, incomingEdges);
            out.put(node, outgoingEdges);
        }

        incoming = new HashMap<>(in);
        outgoing = new HashMap<>(out);
    }

    /*
    public List<Edge> discoverLoops() {
        List<Edge> loops = new ArrayList<>();

        Map<Node, Integer> depths = new HashMap<>();
        Node source = nodes.get(0);
        Node target;
        Edge next;

        int depth = 0;
        depths.put(source, depth);
        System.out.println("DEBUG - ("+ depth +") node: " + source.toString());

        ArrayList<Edge> unexplored = new ArrayList<>();
        for(Edge e : outgoing.get(source))
            unexplored.add(e);

        while(!unexplored.isEmpty()) {
            depth++;
            unexplored.add(null);
            while((next = unexplored.remove(0)) != null) {
                target = next.getTarget();
                if( depths.get(target) == null ) {
                    depths.put(target, depth);
                    System.out.println("DEBUG - ("+ depth +") node: " + target.toString());
                    for(Edge e : outgoing.get(target)) unexplored.add(e);
                } else {
                    if( depth < depths.get(target) ) {
                        System.out.println("ERROR 0001 - this should not happen.");
                    } else {
                        System.out.println("DEBUG - found a loop edge ("+ depths.get(target) + ","+ depth +"): " + next.toString() + " - " + next.getFrequency() +
                                " logLength = " + next.getAvgLogLength() + ", topologicalDepth = " + Math.abs(depth - depths.get(target)));
                        loops.add(next);
                    }
                }
            }
            System.out.println("DEBUG - null (" + depth + ")");
        }

        int f = 0;
        for(Edge e : loops) f+= e.getFrequency();
        System.out.println("DEBUG - total loops discovered ("+ f +"): " + loops.size());

        return loops;
    }
    */

    public List<Edge> identifyBackEdges(Integer[][] adjacencyMatrix, int source){
        Stack<Integer> stack = new Stack<>();
        boolean[] visited = new boolean[adjacencyMatrix.length];
        List<Edge> backEdges = new ArrayList<>();

        visited[source] = true;
        int element = source;
        int destination = source;
        stack.push(source);

        while(!stack.isEmpty()){
            element = stack.peek();
            destination = element;
            while(destination <= adjacencyMatrix.length - 1){
                if(adjacencyMatrix[element][destination] > 0 && visited[destination]){
                    if(stack.contains(destination)){
                        Edge backEdge = new Edge(this.nodes.get(element), this.nodes.get(destination));
                        backEdges.add(edges.get(edges.indexOf(backEdge)));
                        adjacencyMatrix[element][destination] = 0;
                    }
                }

                if(adjacencyMatrix[element][destination] > 0 && !visited[destination]){
                    stack.push(destination);
                    visited[destination] = true;
                    adjacencyMatrix[element][destination] = 0;
                    element = destination;
                    destination = 0;
                    continue;
                }
                destination++;
            }
            stack.pop();
        }
        return backEdges;
    }

    /* SCC */

    public void DFS(Integer[][] adjacencyMatrix, int v, boolean[] visited, List<Integer> comp){
        visited[v] = true;
        for(int i = 0; i < adjacencyMatrix[v].length; i++)
            if(adjacencyMatrix[v][i] > 0 && !visited[i])
                DFS(adjacencyMatrix, i, visited, comp);
            comp.add(v);
    }

    public List<Integer> fillOrder(Integer[][] adjacencyMatrix, boolean[] visited)
    {
        int V = adjacencyMatrix.length;
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < V; i++)
            if (!visited[i])
                DFS(adjacencyMatrix, i, visited, order);
        return order;
    }

    public List<DirectlyFollowsGraph> getSCComponents(Integer[][] adjacencyMatrix)
    {
        int V = adjacencyMatrix.length;
        boolean[] visited = new boolean[V];
        List<Integer> order = fillOrder(adjacencyMatrix, visited);
        Integer[][] transposedAdjacencyMatrix = transposeAdjacencyMatrix(adjacencyMatrix);
        visited = new boolean[V];
        Collections.reverse(order);

        List<List<Integer>> SCComp = new ArrayList<>();
        for (int i = 0; i < order.size(); i++)
        {
            int v = order.get(i);
            if (!visited[v])
            {
                List<Integer> comp = new ArrayList<>();
                DFS(transposedAdjacencyMatrix, v, visited, comp);
                SCComp.add(comp);
            }
        }

        var sccs = new ArrayList<DirectlyFollowsGraph>();

        for(var scomp: SCComp){
            Collections.sort(scomp);

            List<Node> nodes = new ArrayList<>();
            List<Edge> edges = new ArrayList<>();
            for (Integer aScomp : scomp) nodes.add(this.nodes.get(aScomp));

            for(int i = 0; i < this.edges.size(); i++)
                if(nodes.contains(this.edges.get(i).getSource()) && nodes.contains(this.edges.get(i).getTarget()))
                    edges.add(this.edges.get(i));
                sccs.add(new DirectlyFollowsGraph(nodes, edges, loops));
            }

        return sccs;
    }
}