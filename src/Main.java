import data.DirectlyFollowsGraph;
import data.Event;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        String filePath = args[0];
        Double threshold = Double.parseDouble(args[1]);
        Boolean preprocessing = Boolean.parseBoolean(args[2]);
        Boolean considerMissingValues = Boolean.parseBoolean(args[3]);
        String approach = args[4];

        long startTime = System.currentTimeMillis();

        List<Event> events = logReader.readCSV(filePath);

        if(preprocessing)
            events = Preprocessor.applyPreprocessing(filePath, Utils.eventListToString(events));

        HashMap<String, List<Event>> groupedEvents = Utils.groupByEventType(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), threshold, considerMissingValues);

        List<List<String>> groundTruth = new ArrayList<>();
        for(var path: patternsMiner.parseSequences("ground truth.txt"))
            groundTruth.add(Arrays.asList(path.split(",")));

        /********** Graph-based approach **********/

        if(approach.equals("-1")){

            DirectlyFollowsGraph dfg = new DirectlyFollowsGraph(events);
            dfg.buildGraph();
            dfg.convertIntoDOT();

            SegmentsDiscoverer disco = new SegmentsDiscoverer();
            HashMap<Integer, List<Event>> cases = disco.extractSegmentsFromDFG(dfg);
            Utils.writeSegments(filePath.substring(0, filePath.lastIndexOf(".")) + "_segmented.csv", cases);
            System.out.println("\nDiscovering frequent patterns...\n");

            //var patterns = patternsMiner.discoverPatterns(cases, patternsMiner.SPMFAlgorithmName.BIDE, 0.2, 0.0);
            var patterns = patternsMiner.discoverPatterns2(cases, patternsMiner.SPMFAlgorithmName.BIDE, 0.2, 0.05);
            Utils.getSummary(patterns, groundTruth, events);
        }

        /********** General repeats mining **********/

        else if(approach.equals("-2")){
            var patterns = repeatsMiner.discoverRepeats(Utils.toSequence(events, threshold, considerMissingValues), 5, 1, 1);
            Utils.getSummary(patterns, groundTruth, events);
        }

        /**********  Some testing  *********/

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

        //repeatsDiscoverer.m("ABDBDABCADC", "ABC", 1);
        //repeatsDiscoverer.m("ABCDABABCDAB", "ABCDA", 1);
        //repeatsDiscoverer.m("ABCABCABCA", "ABCA", 1);
        //repeatsDiscoverer.m("GCGAGAGACGCC", "GAGA", 1);

        long stopTime = System.currentTimeMillis();
        System.out.println("\nTotal time - " + (stopTime - startTime) / 1000.0 + " sec");
    }
}