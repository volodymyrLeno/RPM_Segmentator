import data.DirectlyFollowsGraph;
import data.Event;
import data.Pattern;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        String filePath = args[0];
        Double threshold = Double.parseDouble(args[1]);
        Boolean preprocessing = Boolean.parseBoolean(args[2]);
        Boolean considerMissingValues = Boolean.parseBoolean(args[3]);
        String approach = args[4];

        List<Event> events = logReader.readCSV(filePath);

        if(preprocessing)
            events = Preprocessor.applyPreprocessing(filePath, Utils.eventListToString(events));

        HashMap<String, List<Event>> groupedEvents = Utils.groupByEventType(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), threshold, considerMissingValues);

        /********** Graph-based approach **********/

        if(approach.equals("-1")){

            DirectlyFollowsGraph dfg = new DirectlyFollowsGraph(events);
            dfg.buildGraph();
            dfg.convertIntoDOT();

            SegmentsDiscoverer disco = new SegmentsDiscoverer();
            HashMap<Integer, List<Event>> cases = disco.extractSegmentsFromDFG(dfg);
            Utils.writeSegments(filePath.substring(0, filePath.lastIndexOf(".")) + "_segmented.csv", cases);
            System.out.println("Discovering frequent patterns...");

            var patterns = patternsMiner.discoverPatterns(cases, patternsMiner.SPMFAlgorithmName.BIDE, 50);

            List<List<String>> groundTruth = new ArrayList<>();
            for(var path: patternsMiner.parseSequences("ground truth.txt"))
                groundTruth.add(Arrays.asList(path.split(",")));

            int i = 1;
            for(var pattern: patterns){
                pattern.assignClosestMatch(groundTruth);
                pattern.computeConfusionMatrix(dfg);
                System.out.println("\nPattern " + i + ":\n" + pattern + "\n" + pattern.getClosestMatch());
                System.out.println("Length = " + pattern.getLength());
                System.out.printf("Sup = %.2f\n", pattern.getRelativeSupport());
                System.out.printf("Coverage = %.2f\n", (double)pattern.getLength()*pattern.getAbsoluteSupport()/events.size());
                System.out.printf("Precision = %.3f\n", pattern.calculatePrecision());
                System.out.printf("Recall = %.3f\n", pattern.calculateRecall());
                System.out.printf("Accuracy = %.3f\n", pattern.calculateAccuracy());
                System.out.printf("F-score = %.3f\n", pattern.calculateFScore());
                i++;
            }
            System.out.println("\nOverall results:\n");
            System.out.printf("Average length = %.2f\n", patterns.stream().mapToInt(Pattern::getLength).average().orElse(0.0));
            System.out.printf("Average support = %.2f\n", patterns.stream().mapToDouble(Pattern::getRelativeSupport).average().orElse(0.0));
            System.out.printf("Average precision = %.3f\n", patterns.stream().mapToDouble(Pattern::getPrecision).average().orElse(0.0));
            System.out.printf("Average recall = %.3f\n", patterns.stream().mapToDouble(Pattern::getRecall).average().orElse(0.0));
            System.out.printf("Average accuracy = %.3f\n", patterns.stream().mapToDouble(Pattern::getAccuracy).average().orElse(0.0));
            System.out.printf("Average f-score = %.3f\n", patterns.stream().mapToDouble(Pattern::getFscore).average().orElse(0.0));
        }

        /********** General repeats mining **********/

        else if(approach.equals("-2")){
            var patterns = repeatsMiner.discoverRepeats(Utils.toSequence(events, threshold, considerMissingValues), 5, 1, 1);
            List<List<String>> groundTruth = new ArrayList<>();
            for(var path: patternsMiner.parseSequences("ground truth.txt"))
                groundTruth.add(Arrays.asList(path.split(",")));

            int i = 1;
            for(var pattern: patterns){
                pattern.assignClosestMatch(groundTruth);
                pattern.computeConfusionMatrix(events);
                System.out.println("\nPattern " + i + ":\n" + pattern);
                System.out.println("Length = " + pattern.getLength());
                System.out.println("Absolute support = " + pattern.getAbsoluteSupport());
                System.out.printf("Precision = %.3f\n", pattern.calculatePrecision());
                System.out.printf("Recall = %.3f\n", pattern.calculateRecall());
                System.out.printf("Accuracy = %.3f\n", pattern.calculateAccuracy());
                System.out.printf("F-score = %.3f\n", pattern.calculateFScore());
                i++;
            }
            System.out.println("\nOverall results:\n");
            System.out.printf("Average length = %.2f\n", patterns.stream().mapToInt(Pattern::getLength).average().orElse(0.0));
            System.out.printf("Average support = %.2f\n", patterns.stream().mapToInt(Pattern::getAbsoluteSupport).average().orElse(0.0));
            System.out.printf("Average precision = %.3f\n", patterns.stream().mapToDouble(Pattern::getPrecision).average().orElse(0.0));
            System.out.printf("Average recall = %.3f\n", patterns.stream().mapToDouble(Pattern::getRecall).average().orElse(0.0));
            System.out.printf("Average accuracy = %.3f\n", patterns.stream().mapToDouble(Pattern::getAccuracy).average().orElse(0.0));
            System.out.printf("Average f-score = %.3f\n", patterns.stream().mapToDouble(Pattern::getFscore).average().orElse(0.0));
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
        //repeatsDiscoverer.m("ABCDABABCDAB", "ABCDA", 1);
        //repeatsDiscoverer.m("ABCABCABCA", "ABCA", 1);
        //repeatsDiscoverer.m("GCGAGAGACGCC", "GAGA", 1);
    }
}