import data.Event;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class logReader {

    /*
    public static List<Event> readLog(String path){
        String type = path.substring(path.lastIndexOf("."));
        if(type.equals("csv"))
            return readCSV(path);
        else if(type.equals("xes"))
            return readXES(path);
        else
            return null;
    }
    */

    public static List<Event> readCSV(String path){
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
                    events.add(new Event(attributes, row));
                    counter++;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    /*
    public static HashMap<String, List<Event>> readXES(String path){
        HashMap<String, List<Event>> cases = new HashMap<>();

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

            for(int i = 0; i < xLog.size(); i++){
                XTrace trace = xLog.get(i);
                String traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
                List<Event> events = new ArrayList();
                for(int j = 0; j < trace.size(); j++){
                    List<String> attributes = new ArrayList<>();
                    String[] values = new String[trace.get(j).getAttributes().size() + 1];
                    values[0] = traceID;
                    attributes.add("CaseID");
                    values[1] = trace.get(j).getAttributes().get(conceptname).toString();
                    attributes.add("Activity");
                    values[2] = trace.get(j).getAttributes().get(timestamp).toString();
                    attributes.add("Timestamp");

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
                    events.add(new Event(attributes, values));
                }
                cases.put(traceID, events);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return cases;
    }
    */
}
