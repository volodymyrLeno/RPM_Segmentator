import com.opencsv.CSVWriter;
import data.DirectlyFollowsGraph;
import data.Event;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        String filePath = args[0];
        Double threshold = Double.parseDouble(args[1]);
        Boolean preprocessing = Boolean.parseBoolean(args[2]);
        Boolean considerMissingValues = Boolean.parseBoolean(args[3]);

        List<Event> events = logReader.readCSV(filePath);

        if(preprocessing)
            events = Preprocessor.applyPreprocessing(filePath, Utils.eventListToString(events));

        HashMap<String, List<Event>> groupedEvents = groupByEventType(events);
        for(var group: groupedEvents.keySet())
            setContextAttributes(groupedEvents.get(group), threshold, considerMissingValues);

        DirectlyFollowsGraph dfg = new DirectlyFollowsGraph(events);
        dfg.buildGraph();
        dfg.convertIntoDOT();
        dfg.getAdjacencyMatrix();

        SegmentsDiscoverer disco = new SegmentsDiscoverer();
        Map<Integer, List<Event>> cases = disco.extractSegmentsFromDFG(dfg);
        writeSegments(filePath.substring(0, filePath.lastIndexOf(".")) + "_segmented.csv", cases);
    }

    static void writeSegments(String filePath, Map<Integer, List<Event>> segments){
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
    }

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
                Double variance = (double)uniqueValues.size()/events.size();

                if(considerMissingValues){
                    if(!(uniqueValues.size() == 1 && uniqueValues.get(0) == null) && variance <= threshold)
                        context.add(attribute);
                }
                else{
                    if(!uniqueValues.contains(null) && variance <= threshold)
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
}