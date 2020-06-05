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
        System.out.print("\nBuilding DFG... ");
        long startTime = System.currentTimeMillis();

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
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

        Node entry = new Node();
        this.nodes.add(0, entry);

        this.edges.add(0, new Edge(entry, getNodes().get(1), 1));

        this.outgoing.put(entry, new ArrayList<>(){{
            add(new Edge(entry, getNodes().get(1), 1));
        }});
    }

    private void addEdge(Event sourceEvent, Event targetEvent){
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
        System.out.print("\nCreating DOT file from a DFG... ");

        String DOT = "digraph g {\n";
        long startTime = System.currentTimeMillis();
        DOT = DOT + "\tENTRY [shape=box];\n";
        for(Edge edge: this.edges){
            DOT = DOT + "\t" + edge.getSource().toString() + " -> " + edge.getTarget().toString();
            if(edge.getSource().toString().equals("ENTRY"))
                DOT = DOT + " [style=dotted];\n";
            else
                DOT = DOT + " [label=" + edge.getFrequency() + "];\n";
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
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");
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
        return incoming.get(source).stream().map(Edge::getTarget).collect(Collectors.toList());
    }

    public void removeLoops(){
        for(var loop: loops)
            edges.remove(loop);

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

    public void removeEdges(List<Edge> edges){
        for(var edge: edges)
            this.edges.remove(edge);

        HashMap<Node, List<Edge>> out = new HashMap<>();
        HashMap<Node, List<Edge>> in = new HashMap<>();

        for(Node node: nodes){
            List<Edge> incomingEdges = this.edges.stream().filter(edge -> edge.getTarget().equals(node)).collect(Collectors.toList());
            List<Edge> outgoingEdges = this.edges.stream().filter(edge -> edge.getSource().equals(node)).collect(Collectors.toList());

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

    public List<Edge> identifyLoops(Integer[][] adjacencyMatrix, int source){
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

    /* Longest path */

    public Integer getLongestPath(Node source, Node target)
    {
        Integer[][] adj = getAdjacencyMatrix();
        int s = nodes.indexOf(source);
        int t = nodes.indexOf(target);

        for(int i = 0; i < adj.length; i++)
            for(int j = 0; j < adj[i].length; j++){
                Edge e = new Edge(nodes.get(i), nodes.get(j));
                if(loops.contains(e))
                    adj[i][j] = 0;
            }

        Stack<Integer> stack = new Stack<>();
        int[] dist = new int[adj.length];

        boolean[] visited = new boolean[adj.length];
        for (int i = 0; i < adj.length; i++)
            visited[i] = false;

        for (int i = 0; i < adj.length; i++)
            if (!visited[i])
                topologicalSortUtil(adj, i, visited, stack);

        for (int i = 0; i < adj.length; i++)
            dist[i] = Integer.MIN_VALUE;
        dist[s] = 0;

        while (!stack.empty()) {
            int u = stack.peek();
            stack.pop();

            if (dist[u] != Integer.MIN_VALUE) {
                for(int i = 0; i < adj.length; i++)
                    if(adj[u][i] > 0)
                        if(dist[i] < dist[u] + 1)
                            dist[i] = dist[u] + 1;
            }
        }

        Integer distance = dist[t];
        return distance;
    }

    public List<Node> getLongestPathNodes(Node source, Node target)
    {
        HashMap<Integer, List<Node>> container = new HashMap<>();

        //List<Node> pathNodes = new ArrayList<>();
        Integer[][] adj = getAdjacencyMatrix();
        int s = nodes.indexOf(source);
        int t = nodes.indexOf(target);

        for(int i = 0; i < adj.length; i++)
            for(int j = 0; j < adj[i].length; j++){
                Edge e = new Edge(nodes.get(i), nodes.get(j));
                if(loops.contains(e))
                    adj[i][j] = 0;
            }

        Stack<Integer> stack = new Stack<>();
        int[] dist = new int[adj.length];

        boolean[] visited = new boolean[adj.length];
        for (int i = 0; i < adj.length; i++)
            visited[i] = false;

        for (int i = 0; i < adj.length; i++)
            if (!visited[i])
                topologicalSortUtil(adj, i, visited, stack);

        for (int i = 0; i < adj.length; i++){
            dist[i] = Integer.MIN_VALUE;
            container.put(i, new ArrayList<>(Collections.singletonList(nodes.get(s))));
        }

        dist[s] = 0;

        while (!stack.empty()) {
            int u = stack.peek();
            stack.pop();

            if (dist[u] != Integer.MIN_VALUE) {
                for(int i = 0; i < adj.length; i++)
                    if(adj[u][i] > 0)
                        if(dist[i] < dist[u] + 1){
                            dist[i] = dist[u] + 1;
                            container.put(i, Stream.concat(container.get(u).stream(), Stream.of(nodes.get(i))).collect(Collectors.toList()));
                        }
            }
        }

        return container.get(t);
    }

    private void topologicalSortUtil(Integer[][] adj, int v, boolean visited[], Stack<Integer> stack)
    {
        visited[v] = true;

        for(int i = 0; i < adj.length; i++)
            if(adj[v][i] > 0 && !visited[i])
                topologicalSortUtil(adj, i, visited, stack);
        stack.push(v);
    }

    /* Shortest path */

    public Integer getShortestPath(Node source, Node target){
        Integer[][] adjacencyMatrix = getAdjacencyMatrix();

        for(int i = 0; i < adjacencyMatrix.length; i++){
            for(int j = 0; j < adjacencyMatrix[i].length; j++){
                if(adjacencyMatrix[i][j] > 0)
                    adjacencyMatrix[i][j] = 1;
            }
        }
        Integer distance = dijkstra(adjacencyMatrix, nodes.indexOf(source), nodes.indexOf(target));
        System.out.println(source + " -> " + target + " - " + distance + "\n\n");
        return distance;
    }

    private Integer dijkstra(Integer graph[][], int src, int tgt)
    {
        int V = graph.length;
        int dist[] = new int[V];
        Boolean sptSet[] = new Boolean[V];

        for (int i = 0; i < V; i++) {
            dist[i] = Integer.MAX_VALUE;
            sptSet[i] = false;
        }

        dist[src] = 0;

        for (int count = 0; count < V - 1; count++) {
            int u = minDistance(dist, sptSet, V);
            sptSet[u] = true;
            for (int v = 0; v < V; v++)
                if (!sptSet[v] && graph[u][v] != 0 &&
                        dist[u] != Integer.MAX_VALUE && dist[u] + graph[u][v] < dist[v])
                    dist[v] = dist[u] + graph[u][v];
        }

        return dist[tgt];
    }

    private int minDistance(int dist[], Boolean sptSet[], Integer V)
    {
        int min = Integer.MAX_VALUE;
        int min_index = -1;

        for (int v = 0; v < V; v++)
            if (!sptSet[v] && dist[v] <= min) {
                min = dist[v];
                min_index = v;
            }

        return min_index;
    }

    /* SCC */

    private void DFS(Integer[][] adjacencyMatrix, int v, boolean[] visited, List<Integer> comp){
        visited[v] = true;
        for(int i = 0; i < adjacencyMatrix[v].length; i++)
            if(adjacencyMatrix[v][i] > 0 && !visited[i])
                DFS(adjacencyMatrix, i, visited, comp);
            comp.add(v);
    }

    private List<Integer> fillOrder(Integer[][] adjacencyMatrix, boolean[] visited)
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
        for (Integer anOrder : order) {
            int v = anOrder;
            if (!visited[v]) {
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

            for (Edge edge : this.edges)
                if (nodes.contains(edge.getSource()) && nodes.contains(edge.getTarget()))
                    edges.add(edge);
                sccs.add(new DirectlyFollowsGraph(nodes, edges, loops));
            }

        return sccs;
    }

    /* Control-flow graph */

    List<List<Node>> paths = new ArrayList<>();

    public HashMap<Node, List<Node>> getDominatorsMap(){
        HashMap<Node, List<List<Node>>> allPaths = getAllPaths();
        HashMap<Node, List<Node>> dominatorsMap = new HashMap<>();
        for(int i = 0; i < nodes.size(); i++){
            for(int j = 0; j < nodes.size(); j++){
                if(isDominator(i, j, allPaths) && i != j){
                    if(!dominatorsMap.containsKey(nodes.get(i)))
                        dominatorsMap.put(nodes.get(i), new ArrayList<>(Collections.singletonList(nodes.get(j))));
                    else
                        dominatorsMap.put(nodes.get(i), Stream.concat(dominatorsMap.get(nodes.get(i)).stream(),
                                Stream.of(nodes.get(j))).collect(Collectors.toList()));
                }
            }
        }
        return dominatorsMap;
    }

    public boolean isDominator(int node1, int node2, HashMap<Node, List<List<Node>>> allPaths)
    {
        for(var path: allPaths.get(nodes.get(node2))){
            if(!path.contains(nodes.get(node1)))
                return false;
        }
        return true;
    }

    private HashMap<Node, List<List<Node>>> getAllPaths(){
        HashMap<Node, List<List<Node>>> allPaths = new HashMap<>();

        Integer[][] adj = getAdjacencyMatrix();
        boolean[] isVisited = new boolean[adj.length];
        List<Node> pathList = new ArrayList<>();
        pathList.add(nodes.get(0));

        for(int i = 0; i < nodes.size(); i++){
            paths.clear();
            getPath(0, i, isVisited, pathList);
            allPaths.put(nodes.get(i), new ArrayList<>(paths));
            System.out.println(i + ") " + nodes.get(i) + ": " + allPaths.get(nodes.get(i)));
        }

        return allPaths;
    }

    public List<List<Node>> getAllPaths(Node source, Node target){
        paths.clear();
        Integer[][] adj = getAdjacencyMatrix();
        boolean[] isVisited = new boolean[adj.length];
        List<Node> pathList = new ArrayList<>();
        pathList.add(source);
        getPath(nodes.indexOf(source), nodes.indexOf(target), isVisited, pathList);

        return paths;
    }

    private void getPath(int source, int target, boolean[] visited, List<Node> localPathList){
        Integer[][] adj = getAdjacencyMatrix();
        visited[source] = true;

        if (source == target)
        {
            visited[source] = false;
            paths.add(new ArrayList<>(localPathList));
            return ;
        }

        for(int i = 0; i < adj.length; i++)
            if(adj[source][i] > 0 && !visited[i]){
                localPathList.add(nodes.get(i));
                getPath(i, target, visited, localPathList);

                localPathList.remove(nodes.get(i));
            }

        visited[source] = false;
    }
}