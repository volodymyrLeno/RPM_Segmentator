import data.DirectlyFollowsGraph;
import data.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        String filePath = args[0];
        Double threshold = Double.parseDouble(args[1]);

        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        //List<Event> events = logReader.readCSV(filePath.substring(0, filePath.lastIndexOf(".")) + "_filtered.csv");
        List<Event> events = logReader.readCSV(filePath);

        HashMap<String, List<Event>> groupedEvents = groupByEventType(events);
        for(var group: groupedEvents.keySet())
            setContextAttributes(groupedEvents.get(group), threshold);

        DirectlyFollowsGraph dfg = new DirectlyFollowsGraph();
        dfg.buildGraph(events);
        dfg.convertIntoDOT();
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

    public static List<String> getContextAttributes(List<Event> events, Double threshold){
        List<String> context = new ArrayList<>();

        for(String attribute: events.get(0).payload.keySet()){
            Double variance = (double)events.stream().map(el -> el.payload.get(attribute)).distinct().collect(Collectors.toList()).size()/events.size();
            if(variance <= threshold)
                context.add(attribute);
        }

        return context;
    }

    public static void setContextAttributes(List<Event> events, Double threshold){
        List<String> contextAttributes = getContextAttributes(events, threshold);
        for(var event: events){
            HashMap<String, String> context = new HashMap<>();
            for(var attribute: event.payload.keySet())
                if(contextAttributes.contains(attribute))
                    context.put(attribute, event.payload.get(attribute));
                event.context = new HashMap<>(context);
        }
    }
}