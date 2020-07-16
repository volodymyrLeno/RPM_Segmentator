import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.unimelb.processmining.optimization.IMdProxy;
import au.edu.unimelb.processmining.optimization.SimpleDirectlyFollowGraph;
import data.Event;
import data.Pattern;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {
    public static List<String> parsePatterns(List<Pattern> patterns){
        List<String> parsedPatterns = new ArrayList<>();

        for(var pattern: patterns){
            String parsed = "::1::";
            for(var item: pattern.getPattern())
                parsed += item + "::";
            parsedPatterns.add(parsed);
        }

        writePatterns(parsedPatterns);

        return parsedPatterns;
    }

    public static void writePatterns(List<String> patterns){
        try {
            FileWriter writer = new FileWriter("temp.txt", false);
            for(var pattern: patterns)
                writer.write(pattern + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("WRONG!!!");
        }
    }

    public static void discoverModel(List<Pattern> patterns){
        parsePatterns(patterns);

        try {
            double eta = 0.0;
            double epsilon = 1.0;
            boolean parallelismFirst =  true;
            boolean replaceIORs = true;
            boolean removeLoopActivities = true;
            String logpath = "temp.txt";

            /* Split Miner */
            SplitMiner yam = new SplitMiner();
            SimpleLog sLog = LogParser.getSimpleLog(logpath);
            //BPMNDiagram output = yam.mineBPMNModel(sLog, new XEventNameClassifier(), eta, epsilon, DFGPUIResult.FilterType.FWG, parallelismFirst, replaceIORs, removeLoopActivities, SplitMinerUIResult.StructuringTime.NONE);

            /* Inductive Miner */
            IMdProxy iMdProxy = new IMdProxy();
            DirectlyFollowGraphPlus dfgp = new DirectlyFollowGraphPlus(sLog,0.0,0.0, DFGPUIResult.FilterType.NOF,true);
            dfgp.buildDirectlyFollowsGraph();
            SimpleDirectlyFollowGraph sdfg = new SimpleDirectlyFollowGraph(dfgp, false);
            BPMNDiagram output = iMdProxy.discoverFromSDFG(sdfg);

            var gateways = output.getGateways();
            var splits = new ArrayList<Gateway>();

            for(var gateway: gateways)
                if(output.getOutEdges(gateway).size() > 1)
                    splits.add(gateway);

                for(int i = 0; i < splits.size(); i++){
                    System.out.println("Gateway type: " + splits.get(i).getGatewayType());
                    for( BPMNEdge<? extends BPMNNode, ? extends BPMNNode> edge : output.getOutEdges(splits.get(i)) ) {
                        BPMNNode node = edge.getTarget();
                        System.out.println(i + ") " + node);
                    }
                }

            BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
            UIContext context = new UIContext();
            UIPluginContext uiPluginContext = context.getMainPluginContext();
            bpmnExportPlugin.export(uiPluginContext, output, new File("test.bpmn"));
            return;
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
    }

    public static List<List<Event>> getCases(List<Event> events){
        List<List<Event>> output = new ArrayList<>();
        HashMap<Integer, List<Event>> cases = new HashMap<>();

        for(var event: events){
            Integer caseID = Integer.valueOf(event.getCaseID());
            if(!cases.keySet().contains(caseID))
                cases.put(caseID, Collections.singletonList(event));
            else
                cases.put(caseID, Stream.concat(cases.get(caseID).stream(), Stream.of(event)).collect(Collectors.toList()));
        }

        for(var caseID: cases.keySet())
            output.add(new ArrayList<>(cases.get(caseID)));

        return output;
    }

    public static List<Event> getEvents(HashMap<Integer, List<Event>> cases){
        List<List<Event>> temp = new ArrayList<>();
        for(var caseID: cases.keySet())
            temp.add(cases.get(caseID));

        List<Event> events = new ArrayList<>();
        temp.forEach(events::addAll);

        events.forEach((event) -> event.setCaseID(""));

        for(int i = 0; i < events.size(); i++)
            events.get(i).setEid(i);

        return events;
    }

    public static HashMap<Integer, List<Event>> shuffleCases(List<List<Event>> cases1, List<List<Event>> cases2){
        HashMap<Integer, List<Event>> newCases = new HashMap<>();
        int j = 0;
        int k = 0;
        int n = 0;
        for(int i = 0; i < cases1.size() + cases2.size(); i++){
            if(i % 2 == 0){
                for(int l = 0; l < cases1.get(j).size(); l++){
                    cases1.get(j).get(l).setEid(n);
                    n++;
                }
                newCases.put(i, new ArrayList<>(cases1.get(j)));
                j++;
            }
            else{
                for(int l = 0; l < cases1.get(k).size(); l++) {
                    cases1.get(k).get(l).setEid(n);
                    n++;
                }
                newCases.put(i, new ArrayList<>(cases2.get(k)));
                k++;
            }
        }

        return newCases;
    }
}
