import data.Event;
import data.Node;
import data.Pattern;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class PatternsMiner {
    private static List<Pattern> patterns = new ArrayList<>();

    public static List<Pattern> discoverPatterns(HashMap<Integer, List<Event>> cases, SPMFAlgorithmName algorithm, Double minSupport, Double minCoverage, String metric){
        List<String> supportedMetrics = new ArrayList<>(){{
            add("frequency");
            add("coverage");
            add("length");
            add("cohesion");
        }};


        if(algorithm == null){
            System.out.println("The selected algorithm is not supported!");
            return null;
        }
        else if(!supportedMetrics.contains(metric)){
            System.out.println("The tool only supports the following metrics: 1) frequency; 2) coverage; 3) length; 4) cohesion");
            return null;
        }
        else{
            int minFrequency = (int)Math.round(cases.size() * minSupport);

            List<Event> events = new ArrayList<>();
            cases.values().forEach(events::addAll);

            System.out.print("\nDiscovering routines...");
            long s1 = System.currentTimeMillis();
            getPattern(toSequences(cases), algorithm, minSupport, minFrequency, cases, metric);
            long s2 = System.currentTimeMillis();
            System.out.println(" (" + (s2 - s1) / 1000.0 + " sec)");
            //System.out.println("\nDiscovery time - " + (s2 - s1) / 1000.0 + " sec");

            var coverages = computeCoverages(patterns, cases, events);
            for(var pattern: patterns){
                pattern.setCoverage(coverages.get(pattern));
                pattern.setRelativeSupport((double) pattern.getAbsoluteSupport()/cases.size());
            }

            patterns = new ArrayList<>(patterns.stream().filter(pattern -> pattern.getCoverage() >= minCoverage).collect(Collectors.toList()));

            return rankByCoverage(patterns);
        }
    }

    private static void getPattern(List<String>[] cases, SPMFAlgorithmName algorithm, Double support, Integer minFrequency, HashMap<Integer, List<Event>> originalCases, String metric){
        List<List<String>> temp = new ArrayList<>();
        for (List<String> aCase : cases) {
            temp.add(aCase);
            //if (aCase.size() != 0)
            //    temp.add(aCase);
        }
        if(temp.size() != 0){
            cases = (ArrayList<String>[])new ArrayList[temp.size()];
            for(int i = 0; i < cases.length; i++)
                cases[i] = new ArrayList<>(temp.get(i));
            writeFile(convertToSPMF(cases), "input.txt");
            runSFPM(algorithm, support);

            List<Pattern> ptrns;

            switch(metric){
                case "frequency":{
                    ptrns = rankBySupport(extractPatterns(parseSequences("output.txt", algorithm))).stream().filter(pattern ->
                            pattern.getAbsoluteSupport() >= minFrequency).collect(Collectors.toList());
                    break;
                }
                case "coverage":{
                    ptrns = rankByCoverage(extractPatterns(parseSequences("output.txt", algorithm)), originalCases).stream().filter(pattern ->
                            pattern.getAbsoluteSupport() >= minFrequency).collect(Collectors.toList());
                    break;
                }
                case "length":{
                    ptrns = rankByLength(extractPatterns(parseSequences("output.txt", algorithm))).stream().filter(pattern ->
                            pattern.getAbsoluteSupport() >= minFrequency).collect(Collectors.toList());
                    break;
                }
                case "cohesion":{
                    ptrns = rankByCohesion(extractPatterns(parseSequences("output.txt", algorithm)), originalCases).
                            stream().filter(pattern -> pattern.getAbsoluteSupport() >= minFrequency).collect(Collectors.toList());
                    break;
                }
                default:{
                    ptrns = new ArrayList<>();
                    break;
                }
            }

            if(ptrns.size() > 0){
                patterns.add(ptrns.get(0));
                var updatedCases = removePattern(cases, ptrns.get(0));
                getPattern(updatedCases, algorithm, support, minFrequency, originalCases, metric);
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
            List<String> patternItems = new ArrayList<>();
            for(var element: elements.subList(0, elements.size() - 1))
                patternItems.add(element);
            patterns.add(new Pattern(patternItems, (double)absSupport/cases.size(), absSupport));
        }
        return patterns;
    }

    private static List<Pattern> extractPatterns(List<String> sequences){
        List<Pattern> patterns = new ArrayList<>();
        for(var sequence: sequences){
            List<String> elements = Arrays.asList(sequence.split(","));
            Integer absSupport = Integer.valueOf(elements.get(elements.size() - 1).replace("#SUP: ",""));
            int index = 0;
            List<String> patternItems = new ArrayList<>();
            for(var element: elements.subList(0, elements.size() - 1)){
                patternItems.add(element);
                index++;
            }
            patterns.add(new Pattern(patternItems, absSupport));
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
                //System.out.println(line);
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

    static List<String> parseSequences(String fileName, SPMFAlgorithmName algorithm) {
        List<String> sequences = new ArrayList<>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(sequences::add);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(algorithm.value.equals("CloFast")){
            sequences = sequences.stream()
                    .map(sequence -> sequence
                            .replace(" -1 -2 ", ",")
                            .replace(" -1 ", ","))
                    .collect(Collectors.toList());
        }

        else{
            sequences = sequences.stream()
                    .map(sequence -> sequence
                            .replace(" -1 -2", "")
                            .replace(" -1 ", ","))
                    .collect(Collectors.toList());
        }

        return sequences;
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


    /*public static List<Pattern> rankBySupport(List<Pattern> patterns){
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getRelativeSupport).reversed());
        return rankedPatterns;
    }*/

    private static List<Pattern> rankBySupport(List<Pattern> patterns){
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getAbsoluteSupport).reversed());
        return rankedPatterns;
    }

    private static List<Pattern> rankByLength(List<Pattern> patterns){
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getLength).reversed());
        return rankedPatterns;
    }

    private static List<Pattern> rankByCoverage(List<Pattern> patterns) {
        List<Pattern> rankedPatterns = new ArrayList<>(patterns);
        rankedPatterns.sort(comparing(Pattern::getCoverage).reversed());
        return rankedPatterns;
    }

    private static List<Pattern> rankByCoverage(List<Pattern> patterns, HashMap<Integer, List<Event>> cases){
        List<Event> events = new ArrayList<>();
        cases.values().forEach(events::addAll);
        for(var pattern: patterns)
            pattern.setCoverage((double)(pattern.getAbsoluteSupport() * pattern.getLength())/events.size());
        return rankByCoverage(patterns);
    }

    private static List<Pattern> rankByCohesion(List<Pattern> patterns, HashMap<Integer, List<Event>> cases){
        HashMap<Pattern, Integer> cohesions = new HashMap<>();
        for(var pattern: patterns)
            cohesions.put(pattern, computeCohesion(pattern.getPattern(), cases));

        Map<Pattern, Integer> sorted = cohesions.entrySet().stream().sorted(comparingByValue(Comparator.reverseOrder()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        return new ArrayList<>(sorted.keySet());
    }

    private static int computeCohesion(List<String> pattern, HashMap<Integer, List<Event>> cases) {
        int medianOutlierCount = getMedianOutlierCount(pattern, cases);
        return pattern.size() - medianOutlierCount;
    }

    private static int getMedianOutlierCount(List<String> pattern, HashMap<Integer, List<Event>> cases) {
        List<Integer> outliers = new ArrayList<>();
        var occurences = getOccurences(toSequences(cases), pattern);

        for(var segment: occurences)
            outliers.add(getOutliers(segment, pattern).size());

        Collections.sort(outliers);
        return outliers.size() % 2 == 0 ? (outliers.get(outliers.size()/2) + outliers.get(outliers.size()/2-1)) / 2 :
                outliers.get(outliers.size() / 2);
    }

    private static List<String> getOutliers(List<String> segment, List<String> pattern){
        List<String> outliers = new ArrayList<>();

        int startIdx = 0;
        for(int i = 0; i < segment.size(); i++)
            if(segment.get(i).equals(pattern.get(0))){
                startIdx = i;
                break;
            }

        for(var p: pattern){
            for(int i = startIdx; i < segment.size(); i++)
                if(segment.get(i).equals(p)){
                    startIdx = i+1;
                    break;
                }
                else
                    outliers.add(segment.get(i));
        }

        return outliers;
    }

    private static List<List<String>> getOccurences(List<String>[] cases, List<String> pattern){
        HashMap<String, List<Integer>> positions = new HashMap<>();
        List<List<String>> occurences = new ArrayList<>();
        for(int i = 0; i < cases.length; i++){
            positions.clear();
            for(var p: pattern){
                for(int j = 0; j < cases[i].size(); j++){
                    if(cases[i].get(j).equals(p))
                        if(positions.containsKey(p))
                            positions.put(p, Stream.concat(positions.get(p).stream(),
                                    Stream.of(j)).collect(Collectors.toList()));
                        else{
                            int finalI = j;
                            positions.put(p, new ArrayList<>(){{add(finalI);}});
                        }
                }
            }
            if(positions.size() == pattern.stream().distinct().collect(Collectors.toList()).size()){
                List<Integer> pos = new ArrayList<>();
                for(var p: pattern){
                    boolean flag = false;
                    if(pos.size() == 0 || pos.get(pos.size() - 1) < positions.get(p).get(0))
                        pos.add(positions.get(p).get(0));
                    else{
                        for(var idx: positions.get(p)){
                            if(idx > pos.get(pos.size() - 1)){
                                pos.add(idx);
                                flag = true;
                                break;
                            }
                        }
                        if(flag == false)
                            break;
                    }
                }
                if(pos.size() == pattern.size())
                    occurences.add(cases[i]);
            }
        }
        return occurences;
    }

    /*
    private List<String> getOutliers(String sequence, List<String> pattern) {
        List<String> outliers = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        Matcher m = getPattern(pattern).matcher(sequence);

        for (int i = 2; i < 2 * pattern.size() - 1; i += 2) {
            indexes.add(i);
        }

        if (m.find()) {
            indexes.forEach(index -> outliers.addAll(Arrays.asList(m.group(index).split(","))));
            StringJoiner anyElement = new StringJoiner("|", "[", "]");
            itemset.forEach(anyElement::add);
            outliers.removeIf(item -> item == null || "".equals(item) || item.matches(anyElement.toString()));
        }

        return outliers;
    }
    */

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
        MaxSP("MaxSP"),
        CloFast("CloFast");

        public final String value;

        SPMFAlgorithmName(String value) {
            this.value = value;
        }
    }
}
