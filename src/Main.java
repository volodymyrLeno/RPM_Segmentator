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

        /********** Graph-based approach **********/

        List<Event> events = logReader.readCSV(filePath);

        if(preprocessing)
            events = Preprocessor.applyPreprocessing(filePath, Utils.eventListToString(events));

        HashMap<String, List<Event>> groupedEvents = Utils.groupByEventType(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), threshold, considerMissingValues);

        DirectlyFollowsGraph dfg = new DirectlyFollowsGraph(events);
        dfg.buildGraph();
        dfg.convertIntoDOT();

        SegmentsDiscoverer disco = new SegmentsDiscoverer();
        Map<Integer, List<Event>> cases = disco.extractSegmentsFromDFG(dfg);
        Utils.writeSegments(filePath.substring(0, filePath.lastIndexOf(".")) + "_segmented.csv", cases);
        System.out.println("Done!");


        /********** General repeats mining **********/

        /*
        List<Event> events = logReader.readCSV(filePath);
        if(preprocessing)
            events = Preprocessor.applyPreprocessing(filePath, Utils.eventListToString(events));
        repeatsMiner.m(Utils.toSequence(events, threshold, considerMissingValues), 1);
        */

        /********** Some testing *********/

        // Dynamic programming approach \\

        //repeatsDiscoverer.discoverRepeats("GCGAGAGACGCC", 1);
        //repeatsDiscoverer.discoverRepeats("AXBCDABCD", 1);
        //repeatsDiscoverer.discoverRepeats("ABCDAXBCD", 1);
        //repeatsDiscoverer.discoverRepeats("ABDBDABCADC", 1);
        //repeatsDiscoverer.discoverRepeats("ABCDABCDABCDABCD", 1); // searching for maximal repeat
        //repeatsDiscoverer.discoverRepeats("ABCABCAB", 1); // overlapping repeats

        //repeatsDiscoverer.discoverRepeats("abcdxaybwabcdybxawabcd", 1);
        //repeatsDiscoverer.discoverRepeats("abcdxadbcx", 1);
        //repeatsDiscoverer.discoverRepeats("abcdbcabcbcd", 1);

        // Shift-based approach \\

        //repeatsDiscoverer.discoverRepeats("GCGAGAGACGCC", 3, 1);
        //repeatsDiscoverer.discoverRepeats("AXBCDABCD", 3, 1); // removal
        //repeatsDiscoverer.discoverRepeats("ABCDAXBCD", 4, 1); // insertion
        //repeatsDiscoverer.discoverRepeats("ABDBDABCADC", 3, 1);
        //repeatsDiscoverer.discoverRepeats("ABCDABCDABCDABCD", 3, 1); // some post processing is required
        //repeatsDiscoverer.discoverRepeats("ABCABCAB", 3, 1); // dealing with overlapping repeats

        // Find all approximate repeats of a pattern \\

        //repeatsDiscoverer.m("ABDBDABCADC", "ABC", 1);
        //repeatsDiscoverer.m("GCGAGAGACGCC", "GAGA", 1);
    }
}