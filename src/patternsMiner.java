import data.Edge;
import data.Event;
import data.Node;
import data.Pattern;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class patternsMiner {

    public static List<Pattern> discoverPatterns(HashMap<Integer, List<Event>> cases, SPMFAlgorithmName algorithm, Integer support){
        writeFile(convertToSPMF(cases), "input.txt");
        runSFPM(algorithm, support);
        return extractPatterns(parseSequences("output.txt"), cases);
    }

    private static List<Pattern> extractPatterns(List<String> sequences, HashMap<Integer, List<Event>> cases){
        List<Pattern> patterns = new ArrayList<>();
        for(var sequence: sequences){
            List<String> elements = Arrays.asList(sequence.split(","));
            Integer absSupport = Integer.valueOf(elements.get(elements.size() - 1).replace("#SUP: ",""));
            patterns.add(new Pattern(elements.subList(0, elements.size() - 1),
                    (double)absSupport/cases.size(), absSupport));
        }
        return patterns;
    }

    private static void runSFPM(SPMFAlgorithmName algorithm, int minSupp){
        try{
            Process p = Runtime.getRuntime().exec("java -jar spmf.jar run " + algorithm.value + " input.txt output.txt " + minSupp + "%");
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private static StringBuilder convertToSPMF(HashMap<Integer, List<Event>> cases){
        StringBuilder result = new StringBuilder();
        for(var caseID: cases.keySet()){
            for(Event event: cases.get(caseID)){
                assembleEvent(result, event);
            }
            result.append("-2\n");
        }
        return formatData(result);
    }

    private static void assembleEvent(StringBuilder result, Event event) {
        Node node = new Node(event);
        result.append(node.toString()).append(" -1 ");
    }

    private static void writeFile(StringBuilder data, String fileName) {
        try{
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(fw);
            var traces = data.toString().split("\\n");
            int i = 0;
            for(var trace: traces){
                bufferedWriter.append(trace);
                i++;
                if(i < traces.length)
                    bufferedWriter.append("\n");
            }
            bufferedWriter.close();
            fw.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private static StringBuilder formatData(StringBuilder data) {
        Map<String, String> actionCode = new HashMap<>();
        Integer[] code = {1};

        String res = Arrays.stream(data.toString().split("\n")).map(itemset -> {
            String[] split = itemset.split(" -1 ");
            List<String> actions = new ArrayList<>(Arrays.asList(split));
            actions.removeIf(el -> el.equals("-2"));
            actions.forEach(action -> actionCode.computeIfAbsent(action, k -> String.valueOf(code[0]++)));
            return actions.stream().map(actionCode::get).collect(Collectors.joining(" -1 "));
        }).collect(Collectors.joining(" -1 -2\n", "", " -1 -2"));

        StringBuilder stringBuilder = new StringBuilder(res);
        Map<String, String> sorted = actionCode.entrySet()
                .stream()
                .sorted(comparingByValue(Comparator.comparingInt(Integer::parseInt)))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        sorted.forEach((key, value) ->
                stringBuilder.insert(0, String.format("@ITEM=%s=%s\n", value, key))
        );
        stringBuilder.insert(0, "@CONVERTED_FROM_TEXT\n");

        return stringBuilder;
    }

    public static List<String> parseSequences(String fileName) {
        List<String> sequences = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(sequences::add);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sequences = sequences.stream()
                .map(sequence -> sequence
                        .replace(" -1 -2", "")
                        .replace(" -1 ", ","))
                .collect(Collectors.toList());

        return sequences;
    }

    public static List<Pattern> rankBySupport(List<Pattern> patterns){
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getRelativeSupport).reversed());
        return rankedPatterns;
    }

    public static List<Pattern> rankByLength(List<Pattern> patterns){
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getLength));
        return rankedPatterns;
    }

    public enum SPMFAlgorithmName {
        PrefixSpan("PrefixSpan"),
        CloSpan("CloSpan"),
        BIDE("BIDE+"),
        MaxSP("MaxSP");

        public final String value;

        SPMFAlgorithmName(String value) {
            this.value = value;
        }
    }
}
