import data.DirectlyFollowsGraph;
import data.Event;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String filePath = args[0];
        Double threshold = Double.parseDouble(args[1]);
        Boolean preprocessing = Boolean.parseBoolean(args[2]);
        Boolean considerMissingValues = Boolean.parseBoolean(args[3]);

        List<Event> events = logReader.readCSV(filePath);

        if(preprocessing)
            events = Preprocessor.applyPreprocessing(filePath, Utils.eventListToString(events));

        HashMap<String, List<Event>> groupedEvents = Utils.groupByEventType(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), threshold, considerMissingValues);

        DirectlyFollowsGraph dfg = new DirectlyFollowsGraph(events);
        dfg.buildGraph();
        dfg.convertIntoDOT();
        dfg.getAdjacencyMatrix();

        SegmentsDiscoverer disco = new SegmentsDiscoverer();
        Map<Integer, List<Event>> cases = disco.extractSegmentsFromDFG(dfg);
        Utils.writeSegments(filePath.substring(0, filePath.lastIndexOf(".")) + "_segmented.csv", cases);
    }
}