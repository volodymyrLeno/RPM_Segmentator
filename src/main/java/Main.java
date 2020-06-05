import data.DirectlyFollowsGraph;
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

        /*
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

        long t1 = System.currentTimeMillis();
        SegmentsDiscoverer disco = new SegmentsDiscoverer();
        HashMap<Integer, List<Event>> cases = disco.extractSegments(events);
        long t2 = System.currentTimeMillis();
        System.out.println("Segmentation time - " + (t2 - t1) / 1000.0 + " sec");

        List<List<String>> groundTruth = new ArrayList<>();
        for(var path: PatternsMiner.parseSequences(groundTruthFile))
            groundTruth.add(Arrays.asList(path.split(",")));

        //List<List<String>> groundTruth = new ArrayList<>(sequences);

        var patterns = PatternsMiner.discoverPatterns(cases, algorithm, minSupport, minCoverage, metric);
        Utils.getSummary(patterns, groundTruth, events);
        */

        List<String> files = new ArrayList<>() {{
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log1.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log2.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log3.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log4.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log5.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log6.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log7.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log8.xes");
            //add("src\\main\\java\\logs\\Evaluation\\XES\\log9.xes");
            add("src\\main\\java\\logs\\Evaluation\\CSV\\Reimbursement.csv");
        }};

        for (var filePath : files) {
            System.out.println("----- " + filePath + " -----");
            long startTime = System.currentTimeMillis();

            List<Event> events = new ArrayList<>();
            String fileType = filePath.substring(filePath.lastIndexOf('.') + 1);
            List<List<String>> sequences = new ArrayList<>();
            HashMap<Integer, List<Event>> originalCases = new HashMap<>();

            if (fileType.equals("xes")) {
                originalCases = LogReader.readXES(filePath);
                originalCases.values().forEach(events::addAll);
                events.forEach(event -> event.setCaseID(""));
                events.forEach(event -> event.removeAttribute("caseID"));
                System.out.println("Original segments - " + originalCases.size());
                System.out.println("Average segment length - " + originalCases.values().stream().mapToInt(el -> el.size()).average());

                var trueLengths = originalCases.values().stream().map(val -> val.size()).collect(Collectors.toList());
                var trueMean = trueLengths.stream().mapToDouble(el -> el).average().orElse(0.0);
                var trueSd = 0.0;
                for (var length : trueLengths)
                    trueSd += Math.pow((length - trueMean), 2);

                trueSd = Math.sqrt(trueSd / trueLengths.size());
                System.out.println("True mean segment size - " + trueMean + ", True standard deviation - " + trueSd);

                sequences = Utils.toSequences(originalCases, contextAttributes).stream().distinct().collect(Collectors.toList());
            } else if (fileType.equals("csv")) {
                events = LogReader.readCSV(filePath);
                events = Preprocessor.applyPreprocessing(filePath, Utils.eventListToString(events), preprocessing);
            } else {
                System.out.println("The tool only supports XES and CSV formats!");
                return;
            }

            //originalCases = Utils.segmentByActivity(events, "clickButton");

            HashMap<String, List<Event>> groupedEvents = Utils.groupEvents(events);
            for (var group : groupedEvents.keySet())
                //Utils.setContextAttributes(groupedEvents.get(group), 0.05, false);
                Utils.setContextAttributes(groupedEvents.get(group), contextAttributes);

            List<List<String>> groundTruth = new ArrayList<>();
            for (var path : PatternsMiner.parseSequences(groundTruthFile))
                groundTruth.add(Arrays.asList(path.split(",")));

            //List<List<String>> groundTruth = new ArrayList<>(sequences);

            long t1 = System.currentTimeMillis();
            SegmentsDiscoverer disco = new SegmentsDiscoverer();
            HashMap<Integer, List<Event>> cases = disco.extractSegments(events);
            long t2 = System.currentTimeMillis();
            System.out.println("Segmentation time - " + (t2 - t1) / 1000.0 + " sec");
            var patterns = PatternsMiner.discoverPatterns(cases, algorithm, minSupport, minCoverage, metric);
            Utils.getSummary(patterns, groundTruth, events);
        }
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