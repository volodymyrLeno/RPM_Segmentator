import com.opencsv.CSVWriter;
import data.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    static String eventListToString(List<Event> events){
        String[] header = {"caseID", "timeStamp", "userID", "targetApp", "eventType", "url", "content", "target.workbookName",
                "target.sheetName", "target.id", "target.class", "target.tagName", "target.type", "target.name",
                "target.value", "target.innerText", "target.checked", "target.href", "target.option", "target.title", "target.innerHTML"
        };
        String str = "";
        for(var event: events){
            str += "\"" + event.getTimestamp() + "\",";
            str += event.payload.containsKey("userID") ? "\"" + event.payload.get("userID") + "\"," : "\"\",";
            str += event.payload.containsKey("targetApp") ? "\"" + event.payload.get("targetApp") + "\"," : "\"\",";
            str += "\"" + event.getEventType() + "\",";

            for(int i = 5; i < header.length; i++)
                if(event.payload.containsKey(header[i]) && !event.payload.get(header[i]).equals("\"\""))
                    str += "\"" + event.payload.get(header[i]) + "\",";
                else
                    str += "\"\",";

            str = str.substring(0, str.lastIndexOf(",")) + "\n";
        }
        return str;
    }

    static void writeDataLineByLine(String filePath, String data) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filePath),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER,
                    CSVWriter.RFC4180_LINE_END);

            String[] headers = {"\"timeStamp\"", "\"userID\"", "\"targetApp\"", "\"eventType\"", "\"url\"",
                    "\"content\"", "\"target.workbookName\"", "\"target.sheetName\"", "\"target.id\"", "\"target.class\"",
                    "\"target.tagName\"", "\"target.type\"", "\"target.name\"", "\"target.value\"", "\"target.innerText\"",
                    "\"target.checked\"", "\"target.href\"", "\"target.option\"", "\"target.title\"", "\"target.innerHTML\""
            };

            writer.writeNext(headers);
            writeActionsValues(writer, data);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeActionsValues(CSVWriter writer, String data){
        String[] actions = data.split("\n");

        for (String action : actions) {
            String[] actionValues = action.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            actionValues = Arrays.stream(actionValues)
                    .map(e -> e.replaceAll("\"{2}(([^\"]|\"\")*)\"{2}", "\"\"\"$1\"\"\""))
                    .toArray(String[]::new);
            writer.writeNext(actionValues);
        }
    }

    static void writeSegments(String filePath, Map<Integer, List<Event>> segments){
        System.out.print("\nSaving segmented log... ");
        long startTime = System.currentTimeMillis();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filePath),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER,
                    CSVWriter.RFC4180_LINE_END);

            //Map.Entry<Integer,List<Event>> entry = segments.entrySet().iterator().next();
            //var value = entry.getValue();

            List<Event> events = new ArrayList<>();
            segments.values().forEach(events::addAll);

            String[] headers = Stream.concat(Stream.of("\"caseID\""),
                    extractAttributes(events).stream().map(el -> "\"" + el + "\"")).toArray(String[]::new);
                    //value.get(0).getAttributes().stream().map(el -> "\"" + el + "\"")).toArray(String[]::new);
            writer.writeNext(headers);

            StringBuilder row = new StringBuilder();
            System.out.println(row.toString());
            for(var caseID: segments.keySet())
                for(var event: segments.get(caseID)){
                    for (String header : headers) {
                        switch (header) {
                            case "\"caseID\"":
                                row.append("\"").append(caseID).append("\",");
                                break;
                            case "\"timeStamp\"":
                                row.append("\"").append(event.getTimestamp()).append("\",");
                                break;
                            case "\"eventType\"":
                                row.append("\"").append(event.getEventType()).append("\",");
                                break;
                            default:
                                String attribute = header.replaceAll("^\"(.*)\"$", "$1");
                                row.append(event.payload.containsKey(attribute) ? "\"" + event.payload.get(attribute) + "\"," : ",");
                                break;
                        }
                    }
                    row = new StringBuilder(row.substring(0, row.lastIndexOf(",")) + "\n");
                }
            Utils.writeActionsValues(writer, row.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");
    }

    /* Context attributes analysis */

    public static HashMap<String, List<Event>> groupByEventType(List<Event> events){
        HashMap<String, List<Event>> groupedEvents = new HashMap<>();
        for(var event: events){
            if(!groupedEvents.containsKey(event.getEventType()))
                groupedEvents.put(event.getEventType(), Collections.singletonList(event));
            else
                groupedEvents.put(event.getEventType(), Stream.concat(groupedEvents.get(event.getEventType()).stream(),
                        Stream.of(event)).collect(Collectors.toList()));
        }
        return groupedEvents;
    }

    public static HashMap<String, List<Event>> groupEvents(List<Event> events){
        HashMap<String, List<Event>> groupedEvents = new HashMap<>();
        for(var event: events){
            var key = event.getEventType() + "_" + event.getApplication();
            if(!groupedEvents.containsKey(key))
                groupedEvents.put(key, Collections.singletonList(event));
            else
                groupedEvents.put(key, Stream.concat(groupedEvents.get(key).stream(),
                        Stream.of(event)).collect(Collectors.toList()));
        }

        return groupedEvents;
    }

    public static List<String> getContextAttributes(List<Event> events, Double threshold, Boolean considerMissingValues){
        List<String> context = new ArrayList<>();
        List<String> attributes = new ArrayList<>(extractAttributes(events));
        if(events.get(0).payload.containsKey("target.row"))
            attributes.add("target.row");
        if(events.get(0).payload.containsKey("target.column"))
            attributes.add("target.column");

        for(String attribute: attributes){
            if(!attribute.equals("ref") && !attribute.equals("Value") && !attribute.equals("timeStamp") && !attribute.equals("eventType")){
                var uniqueValues = events.stream().map(el -> el.payload.get(attribute)).distinct().collect(Collectors.toList());
                Double variance = (double)(uniqueValues.size() - 1)/events.size();

                if((attribute.equals("target.innerText") || attribute.equals("target.name") ||
                        attribute.equals("target.row") || attribute.equals("target.column") || variance > 0.0) && variance <= threshold){
                    if(!considerMissingValues && uniqueValues.contains(null)){
                        if(uniqueValues.size() - 1 > 1 || attribute.equals("target.innerText") || attribute.equals("target.name"))
                            context.add(attribute);
                    }
                    else
                        context.add(attribute);
                }
            }
        }

        if((attributes.contains("target.column") || attributes.contains("target.row")) &&
                (!context.contains("target.column") && !context.contains("target.row"))){
                var uniqueColumns = events.stream().map(el -> el.payload.get("target.column")).distinct().collect(Collectors.toList());
                var uniqueRows = events.stream().map(el -> el.payload.get("target.row")).distinct().collect(Collectors.toList());

                if(uniqueColumns.size() <= uniqueRows.size())
                    context.add("target.column");
                else
                    context.add("target.row");
        }


        if(events.get(0).getEventType().equals("selectWorksheet") && !context.contains("target.sheetName"))
            context.add("target.sheetName");

        if(events.get(0).getEventType().equals("selectTab") && !context.contains("target.title"))
            context.add("target.title");

        if(events.get(0).getEventType().equals("navigate_to") && !context.contains("url"))
            context.add("url");

        if(events.get(0).getEventType().equals("mouseClick") && !context.contains("target.innerText"))
            context.add("target.innerText");

        if(events.get(0).getEventType().equals("clickButton") && !context.contains("target.innerText"))
            context.add("target.innerText");

        return context;
    }

    public static void setContextAttributes(List<Event> events, Double threshold, Boolean considerMissingValues){
        List<String> contextAttributes = getContextAttributes(events, threshold, considerMissingValues);
        for(var event: events){
            HashMap<String, String> context = new HashMap<>();
            for(var attribute: event.payload.keySet())
                if(contextAttributes.contains(attribute))
                    context.put(attribute, event.payload.get(attribute));
                //context.put("targetApp", event.getApplication());
            event.context = new HashMap<>(context);
        }
    }

    public static void setContextAttributes(List<Event> events, List<String> contextAttr){
        var contextAttributes = new ArrayList<>(contextAttr);

        if(contextAttributes.contains("Row/Column")) {
            var uniqueColumns = events.stream().map(el -> el.payload.get("Column")).distinct().collect(Collectors.toList());
            var uniqueRows = events.stream().map(el -> el.payload.get("Row")).distinct().collect(Collectors.toList());
            if (uniqueColumns.size() < uniqueRows.size())
                contextAttributes.add("Column");
                //contextAttributes.put("Column", event.payload.get("Column"));
            else if (uniqueRows.size() < uniqueColumns.size())
                contextAttributes.add("Row");
                //context.put("Row", event.payload.get("Row"));
            else {
                contextAttributes.add("Row");
                contextAttributes.add("Column");
                //context.put("Row", event.payload.get("Row"));
                //context.put("Column", event.payload.get("Column"));
            }
        }

        for(var event: events){
            HashMap<String, String> context = new HashMap<>();
            for(var attribute: event.payload.keySet())
                if(contextAttributes.contains(attribute)) {
                    if (attribute.equals("target.id") && event.getApplication().equals("Excel")) {
                        var uniqueColumns = events.stream().map(el -> el.payload.get("target.column")).distinct().collect(Collectors.toList());
                        var uniqueRows = events.stream().map(el -> el.payload.get("target.row")).distinct().collect(Collectors.toList());
                        if (uniqueColumns.size() < uniqueRows.size())
                            attribute = "target.column";
                        else
                            attribute = "target.row";
                    }
                    context.put(attribute, event.payload.get(attribute));
                }
            event.context = new HashMap<>(context);
        }
    }

    public static List<String> toSequence(List<Event> events, Double threshold, Boolean considerMissingValues){
        List<String> sequence = new ArrayList<>();
        HashMap<String, List<Event>> groupedEvents = groupByEventType(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), threshold, considerMissingValues);
        for(var event: events)
            sequence.add(new Node(event.getEventType(), event.context, 1).toString());
        return sequence;
    }


    public static List<List<String>> toSequences(HashMap<Integer, List<Event>> cases, Double threshold, Boolean considerMissingValues){
        List<List<String>> sequences = new ArrayList<>();

        List<Event> events = new ArrayList<>();
        cases.values().forEach(events::addAll);

        HashMap<String, List<Event>> groupedEvents = groupEvents(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), threshold, considerMissingValues);
        for(var caseID: cases.keySet()){
            List<String> sequence = new ArrayList<>();
            for(var event: cases.get(caseID))
                sequence.add(new Node(event.getEventType(), event.context, 1).toString());
            sequences.add(sequence);
        }
        return sequences;
    }

    public static List<String> extractContextAttributes(String filePath){
        List<String> contextAttributes = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(filePath));
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray context = (JSONArray) jsonObject.get("context");

            if (context != null) {
                for (int i = 0; i < context.size(); i++){
                    contextAttributes.add(context.get(i).toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contextAttributes;
    }

    public static List<List<String>> toSequences(HashMap<Integer, List<Event>> cases, List<String> contextAttributes){
        List<List<String>> sequences = new ArrayList<>();

        List<Event> events = new ArrayList<>();
        cases.values().forEach(events::addAll);

        HashMap<String, List<Event>> groupedEvents = groupEvents(events);
        for(var group: groupedEvents.keySet())
            Utils.setContextAttributes(groupedEvents.get(group), contextAttributes);
        for(var caseID: cases.keySet()){
            List<String> sequence = new ArrayList<>();
            for(var event: cases.get(caseID))
                sequence.add(new Node(event.getEventType(), event.context, 1).toString());
            sequences.add(sequence);
        }
        return sequences;
    }

    public static List<String> toSequence(List<Event> events){
        List<String> sequence = new ArrayList<>();
        for(var event: events)
            sequence.add(new Node(event.getEventType(), event.context, 1).toString());
        return sequence;
    }

    public static HashMap<Integer, List<Event>> segmentByActivity(List<Event> events, String endAction){
        HashMap<Integer, List<Event>> cases = new HashMap<>();
        Integer id = 0;
        List<Event> caseEvents = new ArrayList<>();
        for(Event ev: events) {
            caseEvents.add(ev);
            if (ev.getEventType().equals(endAction)) {
                cases.put(id, new ArrayList<>(caseEvents));
                caseEvents.clear();
                id++;
                //caseEvents.add(ev);
            }
            //else
            //    caseEvents.add(ev);
        }
        //cases.put(id, caseEvents);
        return cases;
    }

    /* Summary */

    public static double getEditDistance(HashMap<Integer, List<Event>> discoveredSegments,  HashMap<Integer, List<Event>> originalTraces){
        List<Double> editDistances = new ArrayList<>();
        for(var caseID: discoveredSegments.keySet()){
            Pattern pattern = new Pattern(toSequence(discoveredSegments.get(caseID)));
            List<List<Event>> coveredTraces = new ArrayList<>();
            int startIdx = discoveredSegments.get(caseID).get(0).getID();
            int endIdx = discoveredSegments.get(caseID).get(discoveredSegments.get(caseID).size()-1).getID();
            for(var trace: originalTraces.keySet()){
                var ids = originalTraces.get(trace).stream().map(el -> el.getID()).filter(el -> el >= startIdx &&
                        el <= endIdx).collect(Collectors.toList());
                if(ids.size() > 0){
                    if(ids.contains(endIdx)){
                        coveredTraces.add(originalTraces.get(trace));
                        break;
                    }
                    else
                        coveredTraces.add(originalTraces.get(trace));
                }
            }
            double editDistance = Double.MAX_VALUE;
            for(int i = 0; i < coveredTraces.size(); i++){
                var trace = toSequence(coveredTraces.get(i));
                var dist = (double)pattern.LevenshteinDistance(pattern.getPattern(), trace)/Math.max(discoveredSegments.get(caseID).size(),
                        trace.size());
                if(dist < editDistance)
                    editDistance = dist;
            }
            editDistances.add(editDistance);
        }
        var meanEditDistance = editDistances.stream().mapToDouble(d -> d).average().orElse(0.0);
        return meanEditDistance;
    }

    public static void getSummary(List<Pattern> patterns, List<List<String>> groundTruth, List<Event> events){
        int i = 1;
        for (var pattern : patterns) {
            System.out.println("\nRoutine " + i + "\nPattern:  " + pattern);
            if(groundTruth.size() > 0){
                pattern.assignClosestMatch(groundTruth);
                pattern.computeConfusionMatrix(events);
                System.out.println("The closest match:  " + pattern.getClosestMatch());
            }
            System.out.println("Length = " + pattern.getLength());
            System.out.printf("Sup = %.2f\n", pattern.getRelativeSupport());
            System.out.printf("Coverage = %.2f\n", pattern.getCoverage());

            if(groundTruth.size() > 0){
                System.out.printf("Precision = %.3f\n", pattern.calculatePrecision());
                System.out.printf("Recall = %.3f\n", pattern.calculateRecall());
                System.out.printf("Accuracy = %.3f\n", pattern.calculateAccuracy());
                System.out.printf("F-score = %.3f\n", pattern.calculateFScore());
                System.out.printf("Jaccard = %.3f\n", pattern.calculateJaccard(groundTruth, events));
            }
            i++;
        }
        System.out.println("\nOverall results:\n");
        System.out.printf("Average length = %.2f\n", patterns.stream().mapToInt(Pattern::getLength).average().orElse(0.0));
        System.out.printf("Average support = %.2f\n", patterns.stream().mapToDouble(Pattern::getRelativeSupport).average().orElse(0.0));
        System.out.printf("Total coverage = %.2f\n", patterns.stream().mapToDouble(Pattern::getCoverage).sum());
        System.out.printf("Average coverage = %.2f\n", patterns.stream().mapToDouble(Pattern::getCoverage).average().orElse(0.0));

        if(groundTruth.size() > 0){
            System.out.printf("Average precision = %.3f\n", patterns.stream().mapToDouble(Pattern::getPrecision).average().orElse(0.0));
            System.out.printf("Average recall = %.3f\n", patterns.stream().mapToDouble(Pattern::getRecall).average().orElse(0.0));
            System.out.printf("Average accuracy = %.3f\n", patterns.stream().mapToDouble(Pattern::getAccuracy).average().orElse(0.0));
            System.out.printf("Average f-score = %.3f\n", patterns.stream().mapToDouble(Pattern::getFscore).average().orElse(0.0));
            System.out.printf("Average Jaccard = %.3f\n", patterns.stream().mapToDouble(Pattern::getJaccard).average().orElse(0.0));
        }
    }

    public static List<String>  extractAttributes(List<Event> events){
        List<String> attributes = new ArrayList<>();
        for(int i = 0; i < events.size(); i++){
            for(String attr: events.get(i).getAttributes())
                if(!attributes.contains(attr))
                    attributes.add(attr);
        }
        return attributes;
    }

    public static HashMap<Integer, List<Event>> mergeHashMaps(HashMap<Integer, List<Event>> cases1, HashMap<Integer, List<Event>> cases2){
        HashMap<Integer, List<Event>> mergedCases = new HashMap<>(cases1);
        for(var caseID: cases2.keySet())
            mergedCases.put(caseID + cases1.size(), cases2.get(caseID));
        return mergedCases;
    }
}