import com.opencsv.CSVWriter;
import data.*;

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

            Map.Entry<Integer,List<Event>> entry = segments.entrySet().iterator().next();
            var value = entry.getValue();

            String[] headers = Stream.concat(Stream.of("\"caseID\""),
                    value.get(0).getAttributes().stream().map(el -> "\"" + el + "\"")).toArray(String[]::new);
            writer.writeNext(headers);

            StringBuilder row = new StringBuilder();
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

    public static List<String> getContextAttributes(List<Event> events, Double threshold, Boolean considerMissingValues){
        List<String> context = new ArrayList<>();
        List<String> attributes = new ArrayList<>(events.get(0).getAttributes());
        if(events.get(0).payload.containsKey("target.row"))
            attributes.add("target.row");
        if(events.get(0).payload.containsKey("target.column"))
            attributes.add("target.column");

        for(String attribute: attributes){
            if(!attribute.equals("timeStamp") && !attribute.equals("eventType")){
                var uniqueValues = events.stream().map(el -> el.payload.get(attribute)).distinct().collect(Collectors.toList());
                Double variance = (double)(uniqueValues.size() - 1)/events.size();

                if((attribute.equals("target.innerText") || attribute.equals("target.name") || variance > 0.0) && variance < threshold){
                    if(!considerMissingValues){
                        if(uniqueValues.size() - 1 > 1 || attribute.equals("target.innerText") || attribute.equals("target.name"))
                            context.add(attribute);
                    }
                    else
                        context.add(attribute);
                }
            }
        }

        return context;
    }

    public static void setContextAttributes(List<Event> events, Double threshold, Boolean considerMissingValues){
        List<String> contextAttributes = getContextAttributes(events, threshold, considerMissingValues);
        for(var event: events){
            HashMap<String, String> context = new HashMap<>();
            for(var attribute: event.payload.keySet())
                if(contextAttributes.contains(attribute))
                    context.put(attribute, event.payload.get(attribute));
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

    /* Summary */

    public static void getSummary(List<Pattern> patterns, List<List<String>> groundTruth, List<Event> events){
        int i = 1;
        for(var pattern: patterns){
            pattern.assignClosestMatch(groundTruth);
            pattern.computeConfusionMatrix(events);
            System.out.println("\nPattern " + i + ":\n" + pattern + "\n" + pattern.getClosestMatch());
            System.out.println("Length = " + pattern.getLength());
            System.out.printf("Sup = %.2f\n", pattern.getRelativeSupport());
            System.out.printf("Coverage = %.2f\n", pattern.getCoverage());
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
}