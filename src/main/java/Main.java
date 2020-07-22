import data.Event;

import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main {

    static Boolean preprocessing = null;
    static PatternsMiner.SPMFAlgorithmName algorithm = null;
    static String metric = null;
    static List<String> contextAttributes = null;
    static Double minSupport = 0.0;
    static Double minCoverage = 0.0;

    public static void main(String[] args) {
        String log = args[0];
        String config = args[1];
        String groundTruthFile = args[2];

        readConfiguration(config);

        List<Event> events = new ArrayList<>();
        String fileType = log.substring(log.lastIndexOf('.') + 1);
        List<List<String>> sequences = new ArrayList<>();
        HashMap<Integer, List<Event>> originalCases = new HashMap<>();

        if (fileType.equals("xes")) {
            originalCases = LogReader.readXES(log);
            originalCases.values().forEach(events::addAll);
            events.forEach(event -> event.setCaseID(""));
            events.forEach(event -> event.removeAttribute("caseID"));
            sequences = Utils.toSequences(originalCases, contextAttributes).stream().distinct().collect(Collectors.toList());
        } else if (fileType.equals("csv")) {
            events = LogReader.readCSV(log);
            events = Preprocessor.applyPreprocessing(log, Utils.eventListToString(events), preprocessing);
        } else {
            System.out.println("The tool only supports XES and CSV formats!");
            return;
        }

        var groupedEvents = Utils.groupEvents(events);

        for(var key: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(key), contextAttributes);
            //Utils.setContextAttributes(groupedEvents.get(key), 0.2, true);

        System.out.println("\nSegmentation...");
        long t1 = System.currentTimeMillis();
        SegmentsDiscoverer disco = new SegmentsDiscoverer();
        HashMap<Integer, List<Event>> cases = disco.extractSegments(events);
        long t2 = System.currentTimeMillis();
        System.out.println("Segmentation time - " + (t2 - t1) / 1000.0 + " sec");
        Utils.writeSegments(log.substring(0, log.lastIndexOf(".")) + "_segmented.csv", cases);

        List<List<String>> groundTruth = new ArrayList<>();

        if(!groundTruthFile.equals("null")){
            for(var path: PatternsMiner.parseSequences(groundTruthFile))
                groundTruth.add(Arrays.asList(path.split(",")));
        }

        //List<List<String>> groundTruth = new ArrayList<>(sequences);

        var patterns = PatternsMiner.discoverPatterns(cases, algorithm, minSupport, minCoverage, metric);
        Utils.getSummary(patterns, groundTruth, events);


        /*
            var events1 = LogReader.readCSV("src\\main\\java\\logs\\Evaluation\\CSV\\StudentRecord_segmented.csv");
            var events2 = LogReader.readCSV("src\\main\\java\\logs\\Evaluation\\CSV\\Reimbursement_segmented.csv");

            var originalCases1 = Parser.getCases(events1);
            var originalCases2 = Parser.getCases(events2);
            var originalCases = Parser.shuffleCases(originalCases2, originalCases1);

            HashMap<String, List<Event>> groupedEvents = Utils.groupEvents(events1);
            for (var group : groupedEvents.keySet())
                Utils.setContextAttributes(groupedEvents.get(group), contextAttributes);

            groupedEvents = Utils.groupEvents(events2);
            for (var group : groupedEvents.keySet())
                Utils.setContextAttributes(groupedEvents.get(group), contextAttributes);

            HashMap<Integer, List<Event>> dict = Parser.shuffleCases(Parser.getCases(events2), Parser.getCases(events1));
            List<Event> events = Parser.getEvents(dict);
            */
    }

    static void readConfiguration(String config){
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(config));
            JSONObject jsonObject = (JSONObject) obj;

            preprocessing = (Boolean) jsonObject.get("preprocessing");
            algorithm = PatternsMiner.SPMFAlgorithmName.valueOf(jsonObject.get("algorithm").toString());
            minSupport = (Double) jsonObject.get("minSupport");
            minCoverage = (Double) jsonObject.get("minCoverage");
            metric = jsonObject.get("metric").toString();

            JSONArray context = (JSONArray) jsonObject.get("context");
            List<String> temp = new ArrayList<>();
            if (context != null) {
                for (int i = 0; i < context.size(); i++){
                    temp.add(context.get(i).toString());
                }
                contextAttributes = new ArrayList<>(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}