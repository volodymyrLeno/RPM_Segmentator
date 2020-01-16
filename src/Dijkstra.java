import data.DirectlyFollowsGraph;
import data.Edge;
import data.Node;

import java.util.HashMap;
import java.util.List;

public class Dijkstra {

    public static Integer getLongestPath(Node source, Node target, DirectlyFollowsGraph dfg){

        Integer[][] adjacencyMatrix = dfg.getAdjacencyMatrix();
        for(int i = 0; i < adjacencyMatrix.length; i++){
            for(int j = 0; j < adjacencyMatrix[i].length; j++){
                if(adjacencyMatrix[i][j] > 0)
                    adjacencyMatrix[i][j] = -1;
                //System.out.print(adjacencyMatrix[i][j] + " ");
            }
            //System.out.println();
        }

        Integer distance = dijkstra(adjacencyMatrix, dfg.getNodes().indexOf(source), dfg.getNodes().indexOf(target));

        System.out.println(source + " -> " + target + " - " + distance);

        return distance;
    }


    static Integer dijkstra(Integer graph[][], int src, int tgt)
    {
        int V = graph.length;

        int dist[] = new int[V];

        // sptSet[i] will true if vertex i is included in shortest
        // path tree or shortest distance from src to i is finalized
        Boolean sptSet[] = new Boolean[V];

        // Initialize all distances as INFINITE and stpSet[] as false
        for (int i = 0; i < V; i++) {
            dist[i] = Integer.MAX_VALUE;
            sptSet[i] = false;
        }

        // Distance of source vertex from itself is always 0
        dist[src] = 0;

        // Find shortest path for all vertices
        for (int count = 0; count < V - 1; count++) {
            // Pick the minimum distance vertex from the set of vertices
            // not yet processed. u is always equal to src in first
            // iteration.
            int u = minDistance(dist, sptSet, V);

            // Mark the picked vertex as processed
            sptSet[u] = true;

            // Update dist value of the adjacent vertices of the
            // picked vertex.
            for (int v = 0; v < V; v++)

                // Update dist[v] only if is not in sptSet, there is an
                // edge from u to v, and total weight of path from src to
                // v through u is smaller than current value of dist[v]
                if (!sptSet[v] && graph[u][v] != 0 &&
                        dist[u] != Integer.MAX_VALUE && dist[u] + graph[u][v] < dist[v])
                    dist[v] = dist[u] + graph[u][v];
        }

        return Math.abs(dist[tgt]);
    }

    static int minDistance(int dist[], Boolean sptSet[], Integer V)
    {
        // Initialize min value
        int min = Integer.MAX_VALUE, min_index = -1;

        for (int v = 0; v < V; v++)
            if (sptSet[v] == false && dist[v] <= min) {
                min = dist[v];
                min_index = v;
            }

        return min_index;
    }


}
