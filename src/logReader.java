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
}