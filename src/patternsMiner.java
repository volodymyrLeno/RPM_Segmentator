import data.Event;
import data.Node;
import data.Pattern;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class patternsMiner {

    static List<Pattern> discoverPatterns(HashMap<Integer, List<Event>> cases, SPMFAlgorithmName algorithm, Double support, Double minCoverage){
        writeFile(convertToSPMF(cases), "input.txt");

        runSFPM(algorithm, support);
        var patterns = extractPatterns(parseSequences("output.txt"), cases);

        List<Event> events = new ArrayList<>();
        cases.values().forEach(events::addAll);

        var coverages = computeCoverages(patterns, cases, events);
        for(var pattern: patterns)
            pattern.setCoverage(coverages.get(pattern));

        if(minCoverage == 0.0)
            patterns = new ArrayList<>(patterns.stream().filter(pattern -> pattern.getCoverage() > 0.0).collect(Collectors.toList()));
        else
            patterns = new ArrayList<>(patterns.stream().filter(pattern -> pattern.getCoverage() >= minCoverage).collect(Collectors.toList()));

        return rankByCoverage(patterns);
    }

    private static List<Pattern> patterns = new ArrayList<>();

    static List<Pattern> discoverPatterns2(HashMap<Integer, List<Event>> cases, SPMFAlgorithmName algorithm, Double support, Double minCoverage){
        int minFrequency = (int)Math.round(cases.size() * support);

        getPattern(toSequences(cases), algorithm, support, minFrequency);

        List<Event> events = new ArrayList<>();
        cases.values().forEach(events::addAll);

        var coverages = computeCoverages(patterns, cases, events);
        for(var pattern: patterns){
            pattern.setCoverage(coverages.get(pattern));
            pattern.setRelativeSupport((double) pattern.getAbsoluteSupport()/cases.size());
        }

        patterns = new ArrayList<>(patterns.stream().filter(pattern -> pattern.getCoverage() >= minCoverage).collect(Collectors.toList()));

        return rankByCoverage(patterns);
    }

    private static void getPattern(List<String>[] cases, SPMFAlgorithmName algorithm, Double support, Integer minFrequency){
        List<List<String>> temp = new ArrayList<>();
        for (List<String> aCase : cases) {
            if (aCase.size() != 0)
                temp.add(aCase);
        }
        if(temp.size() != 0){
            cases = (ArrayList<String>[])new ArrayList[temp.size()];
            for(int i = 0; i < cases.length; i++)
                cases[i] = new ArrayList<>(temp.get(i));
            writeFile(convertToSPMF(cases), "input.txt");
            runSFPM(algorithm, support);
            var ptrns = rankByLength(extractPatterns(parseSequences("output.txt"))).stream().filter(pattern ->
                    pattern.getAbsoluteSupport() >= minFrequency).collect(Collectors.toList());
            if(ptrns.size() > 0){
                patterns.add(ptrns.get(0));
                var updatedCases = removePattern(cases, ptrns.get(0));
                getPattern(updatedCases, algorithm, support, minFrequency);
            }
        }
    }

    private static List<String>[] removePattern(List<String>[] cases, Pattern pattern){
        HashMap<String, List<Integer>> pos = new HashMap<>();
        for(int c = 0; c < cases.length; c++){
            pos.clear();
            for(var element: pattern.getPattern()){
                if(cases[c].contains(element)){
                    for(int i = 0; i < cases[c].size(); i++)
                        if(cases[c].get(i).equals(element)){
                            if(pos.containsKey(element) && !pos.get(element).contains(i))
                                pos.put(element, Stream.concat(pos.get(element).stream(),
                                        Stream.of(i)).collect(Collectors.toList()));
                            else{
                                int finalI = i;
                                pos.put(element, new ArrayList<>(){{
                                    add(finalI);
                                }});
                            }
                        }
                }
                else
                    break;
            }
            if(pos.size() == pattern.getPattern().stream().distinct().collect(Collectors.toList()).size()){
                List<Integer> positions = new ArrayList<>();
                for(var element: pattern.getPattern()){
                    if(positions.size() == 0)
                        positions.add(pos.get(element).get(0));
                    else{
                        var t = pos.get(element).stream().filter(el -> el > positions.get(positions.size()-1)).collect(Collectors.toList());
                        if(t.size() > 0)
                            positions.add(t.get(0));
                        else
                            break;
                    }
                }
                if(positions.size() == pattern.getLength()){
                    cases[c] = new ArrayList<>(IntStream.range(0, cases[c].size())
                            .filter(i -> !positions.contains(i))
                            .mapToObj(cases[c]::get)
                            .collect(Collectors.toList()));
                }
            }
        }
        return cases;
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

    private static List<Pattern> extractPatterns(List<String> sequences){
        List<Pattern> patterns = new ArrayList<>();
        for(var sequence: sequences){
            List<String> elements = Arrays.asList(sequence.split(","));
            Integer absSupport = Integer.valueOf(elements.get(elements.size() - 1).replace("#SUP: ",""));
            patterns.add(new Pattern(elements.subList(0, elements.size() - 1), absSupport));
        }
        return patterns;
    }


    private static void runSFPM(SPMFAlgorithmName algorithm, Double minSupp){
        Process p;

        try{
            ProcessBuilder pb = new ProcessBuilder();
            List<String> commands = new ArrayList<>(){{
                add("java");
                add("-jar");
                add("spmf.jar");
                add("run");
                add(algorithm.value);
                add("input.txt");
                add("output.txt");
                add(minSupp.toString());
            }};
            pb.command(commands);
            p = pb.start();
            p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
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

    private static StringBuilder convertToSPMF(List<String>[] cases){
        StringBuilder result = new StringBuilder();
        for (List<String> aCase : cases) {
            for (String element : aCase) {
                result.append(element).append(" -1 ");
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

    static List<String> parseSequences(String fileName) {
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

    private static List<Pattern> rankByLength(List<Pattern> patterns){
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getLength).reversed());
        return rankedPatterns;
    }

    private static List<Pattern> rankByCoverage(List<Pattern> patterns){
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getCoverage).reversed());
        return rankedPatterns;
    }

    private static HashMap<Pattern, Double> computeCoverages(List<Pattern> patterns, HashMap<Integer, List<Event>> cases, List<Event> events){
        var sequences = toSequences(cases);

        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        Collections.sort(rankedPatterns);

        HashMap<Pattern, Double> coverages = new HashMap<>();
        HashMap<String, List<Integer>> pos = new HashMap<>();

        for(Pattern pattern: rankedPatterns){

            int sum = 0;
            int absSupport = 0;
            for(int c = 0; c < sequences.length; c++){
                int counter = 0;
                pos.clear();
                for(var element: pattern.getPattern()){
                    if(sequences[c].contains(element)){
                        counter++;
                        for(int i = 0; i < sequences[c].size(); i++)
                            if(sequences[c].get(i).equals(element)){
                                if(pos.containsKey(element) && !pos.get(element).contains(i))
                                    pos.put(element, Stream.concat(pos.get(element).stream(),
                                            Stream.of(i)).collect(Collectors.toList()));
                                else{
                                    int finalI = i;
                                    pos.put(element, new ArrayList<>(){{
                                        add(finalI);
                                    }});
                                }
                            }
                    }
                    else
                        break;
                }
                if(pos.size() == pattern.getPattern().stream().distinct().collect(Collectors.toList()).size()){
                    List<Integer> positions = new ArrayList<>();
                    for(var element: pattern.getPattern()){
                        if(positions.size() == 0)
                            positions.add(pos.get(element).get(0));
                        else{
                            var t = pos.get(element).stream().filter(el -> el > positions.get(positions.size()-1)).collect(Collectors.toList());
                            if(t.size() > 0)
                                positions.add(t.get(0));
                            else
                                break;
                        }
                    }
                    if(positions.size() == pattern.getLength()){
                        sum += pattern.getLength();
                        absSupport++;
                        sequences[c] = new ArrayList<>(IntStream.range(0, sequences[c].size())
                                .filter(i -> !positions.contains(i))
                                .mapToObj(sequences[c]::get)
                                .collect(Collectors.toList()));
                    }
                }
            }
            coverages.put(pattern, (double)sum/events.size());
            pattern.setAbsoluteSupport(absSupport);
            pattern.setRelativeSupport((double)absSupport/cases.size());
        }
        return coverages;
    }

    private static List<String>[] toSequences(HashMap<Integer, List<Event>> cases){
        ArrayList<String>[] sequences = (ArrayList<String>[])new ArrayList[cases.size()];
        int i = 0;
        for(Integer key: cases.keySet()){
            List<String> sequence = new ArrayList<>(cases.get(key).stream().map(el -> new Node(el).toString()).collect(Collectors.toList()));
            sequences[i] = new ArrayList<>(sequence);
            i++;
        }
        return sequences;
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
