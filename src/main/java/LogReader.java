import data.Event;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

final class LogReader {

    static List<Event> readCSV(String path){
        int eid = 0;
        List<Event> events = new ArrayList<>();
        List<String> attributes = new ArrayList();
        int counter = 0;
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((line = br.readLine()) != null) {
                if(Character.codePointAt(line, 0) == 0xFEFF)
                    line = line.substring(1);
                String[] row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if(row.length != 20)
                    System.out.print("");

                if(counter == 0) {
                    counter++;
                    for(int i = 0; i < row.length; i++)
                        row[i] = row[i].replaceAll("^\"(.*)\"$","$1");
                    Collections.addAll(attributes, row);
                }
                else {
                    for(int i = 0; i < row.length; i++){
                        if(row[i].matches("\"+"))
                            row[i] = "\"\"";
                        else
                            row[i] = row[i].replaceAll("^\"(.*)\"$","$1");
                    }
                    //System.out.print(eid);
                    events.add(new Event(attributes, row, eid));
                    //System.out.println(events.get(eid));
                    eid++;
                    counter++;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    public static HashMap<Integer, List<Event>> readXES(String path){
        HashMap<Integer, List<Event>> cases = new HashMap<>();
        final String conceptname = "concept:name";
        final String timestamp = "time:timestamp";

        try {
            File xesFile = new File(path);
            XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
            if (!parser.canParse(xesFile)) {
                parser = new XesXmlGZIPParser();
                if (!parser.canParse(xesFile)) {
                    System.out.println("Unparsable log file: " + xesFile.getAbsolutePath());
                }
            }
            List<XLog> xLogs = parser.parse(xesFile);
            XLog xLog = xLogs.remove(0);

            int eid = 0;

            for(int i = 0; i < xLog.size(); i++){
                XTrace trace = xLog.get(i);
                String traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
                List<Event> events = new ArrayList();
                for(int j = 0; j < trace.size(); j++){
                    List<String> attributes = new ArrayList<>();
                    String[] values = new String[trace.get(j).getAttributes().size() + 1];
                    values[0] = traceID;
                    attributes.add("caseID");
                    values[1] = trace.get(j).getAttributes().get(conceptname).toString();
                    attributes.add("eventType");
                    values[2] = trace.get(j).getAttributes().get(timestamp).toString();
                    if(values[2].contains("2019-02-19T04:36:04"))
                        System.out.println();
                    values[2] = values[2].substring(0, values[2].indexOf('+'));
                    if(values[2].length() == 19)
                        values[2] = values[2] + ".000";
                    values[2] = values[2] + "Z";
                    attributes.add("timeStamp");

                    int k = 3;

                    for(String s: trace.get(j).getAttributes().keySet()){
                        switch(s){
                            case timestamp:
                                break;
                            case conceptname:
                                break;
                            default: {
                                attributes.add(s);
                                values[k] = trace.get(j).getAttributes().get(s).toString();
                                k++;
                            }
                        }
                    }
                    events.add(new Event(attributes, values, eid));
                    eid++;
                }
                cases.put(Integer.valueOf(traceID), events);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return cases;
    }

    static List<Event> readClickStream(String path){
        int eid = 0;
        List<Event> events = new ArrayList<>();
        List<String> attributes = new ArrayList();
        int counter = 0;
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((line = br.readLine()) != null) {
                if(Character.codePointAt(line, 0) == 0xFEFF)
                    line = line.substring(1);
                String[] row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if(counter == 0) {
                    counter++;
                    for(int i = 0; i < row.length; i++)
                        row[i] = row[i].replaceAll("^\"(.*)\"$","$1");
                    Collections.addAll(attributes, row);
                }
                else {
                    for(int i = 0; i < row.length; i++){
                        if(row[i].matches("\"+"))
                            row[i] = "\"\"";
                        else
                            row[i] = row[i].replaceAll("^\"(.*)\"$","$1");
                    }
                    var timestamp = row[0];
                    var eventType = row[1];
                    HashMap<String, String> payload = new HashMap<>();
                    if(eventType.equals("M")){
                        var attrList = row[2].replaceAll("\\[", "").replaceAll("\\]","").split(",");
                        payload.put("X_pos", attrList[0]);
                        payload.put("Y_pos", attrList[1]);
                    }
                    else if(eventType.startsWith("A")){
                        if(eventType.equals("A7")){
                            var attrList = row[2].replaceAll("\\[", "").replaceAll("\\]", "").split(";");
                            int i = 0;
                            for(var attr: attrList){
                                payload.put("Application" + i, attr);
                                i++;
                            }
                        }
                        else{
                            var attrList = row[2].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
                            payload.put("N1", attrList[0]);
                            payload.put("Application", attrList[1]);

                            if(eventType.equals("A1") || eventType.equals("A2") || eventType.equals("A5")){
                                payload.put("Text", attrList[2]);
                                if(attrList.length > 3){
                                    var coordinates = "";
                                    for(int i = 3; i < attrList.length; i++)
                                        coordinates += attrList[i] + ",";
                                    payload.put("Coordinates", coordinates.substring(0, coordinates.length() - 1));
                                }
                            }
                            else if(eventType.equals("A6")){
                                var coordinates = "";
                                for(int i = 2; i < attrList.length; i++)
                                    coordinates += attrList[i] + ",";
                                payload.put("Coordinates", coordinates.substring(0, coordinates.length() - 1));
                            }
                            else if(eventType.equals("A8"))
                                if(attrList.length > 2)
                                    payload.put("Location", attrList[2]);
                                else
                                    payload.put("Location", "");
                        }
                    }
                    else if(eventType.startsWith("K")){
                        var attrList = row[2].replaceAll("[|]", "").split(",");
                        if(eventType.equals("K1") || eventType.equals("K2") || eventType.equals("K5")){
                            payload.put("Type", attrList[0]);
                        }
                        else{
                            payload.put("Type", attrList[0]);
                            payload.put("X_pos", attrList[1]);
                            payload.put("Y_pos", attrList[2]);
                        }
                    }

                    events.add(new Event(timestamp, eventType, payload, eid));
                    eid++;
                    counter++;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }
}