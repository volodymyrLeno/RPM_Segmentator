package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Event {
    private int eid;
    private String caseID;
    private String eventType;
    private String timestamp;
    public HashMap<String, String> payload;
    private List<String> attributes;
    public HashMap<String, String> context;

    private boolean start;
    private boolean end;

    public Event(List<String> attributes, String[] values, int eid){
        this.start = false;
        this.end = false;
        this.eid = eid;
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
        this.start = false;
        this.end = false;
    }

    public Event(Event event){
        this.caseID = event.caseID;
        this.eventType = event.eventType;
        this.timestamp = event.timestamp;
        this.payload = new HashMap<>(event.payload);
        this.start = false;
        this.end = false;
    }

    public String getEventType() { return this.eventType; }

    public String getTimestamp(){
        return this.timestamp;
    }

    public String getCaseID() { return this.caseID; }

    public void setCaseID(String caseID){
        this.caseID = caseID;
    }

    public List<String> getAttributes(){ return this.attributes; }

    public int getID() { return eid; }

    public boolean isStart() { return start; }

    public void setStart(boolean start) { this.start = start; }

    public boolean isEnd() { return end; }

    public void setEnd(boolean end) { this.end = end; }

    public String toString() {
        return "(" + this.caseID + ", " + this.eventType + ", " + this.timestamp + ", " + payload + ")";
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof Event && obj != null) return this.eid == ((Event) obj).eid;
        return false;
    }

}
