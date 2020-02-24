import data.Event;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class logReader {

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
                    events.add(new Event(attributes, row, eid));
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