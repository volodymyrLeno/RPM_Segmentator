package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Event {
    public String caseID;
    private String eventType;
    private String timestamp;
    public HashMap<String, String> payload;
    public List<String> attributes;
    public HashMap<String, String> context;

    public Event(List<String> attributes, String[] values){
        String temp;
        this.attributes = new ArrayList<>(attributes);
        this.caseID = attributes.contains("caseID") ? values[attributes.indexOf("caseID")] : "";
        this.eventType = values[attributes.indexOf("eventType")];
        this.timestamp = values[attributes.indexOf("timeStamp")];
        payload = new HashMap<>();
        for(int i = 0; i < values.length; i++){
            if(values[i].matches("\"+"))
                temp = "";
            else
                temp = values[i].replaceAll("\"{4}","\"\"").replaceAll("\"([^;\"\\[\\]]+)\"","$1").
                        replaceAll("^\"(.*)\"$", "$1");

            if(i != attributes.indexOf("eventType") && i != attributes.indexOf("timeStamp") && i != attributes.indexOf("caseID"))
            {
                if((!temp.equals("\"\"") && !temp.equals("")) || (i == attributes.indexOf("target.value")
                        && (this.eventType.equals("clickTextField") ||
                        this.eventType.equals("editField") || this.eventType.equals("getCell") || this.eventType.equals("editRange"))))
                    payload.put(attributes.get(i), temp);
            }
        }

        /* Handling Excel events */

        if(payload.containsKey("targetApp") && payload.get("targetApp").equals("Excel")){
            if(payload.containsKey("target.id")){
                payload.put("target.row", payload.get("target.id").replaceAll("[A-Za-z]+",""));
                payload.put("target.column", payload.get("target.id").replaceAll("\\d+", ""));
            }
        }
    }

    public Event(String activityName, String timestamp) {
        this.caseID = "";
        this.eventType = activityName;
        this.timestamp = timestamp;
        payload = new HashMap<>();
    }

    public Event(Event event){
        this.caseID = event.caseID;
        this.eventType = event.eventType;
        this.timestamp = event.timestamp;
        this.payload = new HashMap<>(event.payload);
    }

    public String getEventType() { return this.eventType; }

    public String getTimestamp(){
        return this.timestamp;
    }

    public void setCaseID(String caseID){
        this.caseID = caseID;
    }

    public String toString() {
        return "(" + this.caseID + ", " + this.eventType + ", " + this.timestamp + ", " + payload + ")";
    }
}
