package data;

import java.util.*;
import java.util.stream.Collectors;

public class Pattern implements Comparable<Pattern> {
    List<String> pattern;
    List<String> closestMatch;
    int length;
    double relSup;
    int absSup;
    double coverage;

    int tp = 0;
    int tn = 0;
    int fp = 0;
    int fn = 0;

    double jaccard;
    double precision;
    double recall;
    double accuracy;
    double fscore;

    public Pattern(List<String> pattern, Double relSup, Integer absSup){
        this.pattern = new ArrayList<>(pattern);
        this.closestMatch = new ArrayList<>();
        this.length = pattern.size();
        this.relSup = relSup;
        this.absSup = absSup;
    }

    public Pattern(List<String> pattern, Integer absSup){
        this.pattern = new ArrayList<>(pattern);
        this.closestMatch = new ArrayList<>();
        this.length = pattern.size();
        this.relSup = 0.0; // general repeats work with unsegmented log, thus we don't have a total amount of cases
        this.absSup = absSup;
    }

    public Pattern(List<String> pattern){
        this.pattern = new ArrayList<>(pattern);
        this.closestMatch = new ArrayList<>();
        this.length = pattern.size();
        this.relSup = 0.0;
        this.absSup = 0;
    }

    public List<String> getPattern() { return this.pattern; }

    public List<String> getClosestMatch(){ return this.closestMatch; }

    public Double getRelativeSupport(){ return this.relSup; }

    public Integer getAbsoluteSupport(){ return this.absSup; }

    public int getLength(){ return this.length; }

    public void setRelativeSupport(double support){ this.relSup = support; }

    public void setAbsoluteSupport(Integer support){ this.absSup = support; }

    public void setCoverage(double coverage){ this.coverage = coverage; }

    public double getCoverage(){ return this.coverage; }

    public void assignClosestMatch(List<List<String>> groundTruth){
        List<String> closestMatch = new ArrayList<>(groundTruth.get(0));
        Integer bestMatchScore = LevenshteinDistance(pattern, groundTruth.get(0));
        for(int i = 1; i < groundTruth.size(); i++) {
            Integer matchScore = LevenshteinDistance(pattern, groundTruth.get(i));
            if (bestMatchScore >= matchScore) {
                closestMatch = new ArrayList<>(groundTruth.get(i));
                bestMatchScore = matchScore;
            }
        }
        this.closestMatch = new ArrayList<>(closestMatch);
    }

    public Integer LevenshteinDistance(List<String> pattern, List<String> groundTruth){
        int rows = groundTruth.size() + 1;
        int cols = pattern.size() + 1;
        int editMatrix[][] = new int[rows][cols];
        for(int i = 0; i < rows; i++)
            editMatrix[i][0] = i;
        for(int j = 1; j < cols; j++)
            editMatrix[0][j] = j;
        for(int i = 1; i < rows; i++){
            for(int j = 1; j < cols; j++){
                editMatrix[i][j] = Math.min(
                        editMatrix[i-1][j] + 1,
                        Math.min(
                                editMatrix[i][j-1] + 1,
                                editMatrix[i-1][j-1] + (groundTruth.get(i-1).equals(pattern.get(j-1)) ? 0 : 1)
                        )
                );
            }
        }
        var editDistance = editMatrix[rows - 1][cols - 1];
        return editDistance;
    }

    public void computeConfusionMatrix(DirectlyFollowsGraph dfg){
        List<String> elements = new ArrayList<>(closestMatch);
        for(var element: pattern){
            if(elements.contains(element)){
                tp++;
                elements.remove(element);
            }
            else
                fp++;
        }
        for(var element: closestMatch){
            if(!pattern.contains(element))
                fn++;
        }
        elements = dfg.getNodes().stream().map(Node::toString).collect(Collectors.toList());
        for(var element: elements)
            if(!pattern.contains(element) && !closestMatch.contains(element))
                tn++;
    }


    public void computeConfusionMatrix(List<Event> events){
        tp = fn = fp = tn = 0;
        List<String> elements = new ArrayList<>(closestMatch);
        for(var element: pattern){
            if(elements.contains(element)){
                tp++;
                elements.remove(element);
            }
            else
                fp++;
        }
        for(var element: closestMatch){
            if(!pattern.contains(element))
                fn++;
        }
        elements = events.stream().map(event -> new Node(event).toString()).distinct().collect(Collectors.toList());
        for(var element: elements)
            if(!pattern.contains(element) && !closestMatch.contains(element))
                tn++;
    }

    public double calculateJaccard(){
        jaccard = (double)tp/(tp + fp + fn);
        return jaccard;
    }

    public double calculateJaccard(List<List<String>> groundTruths, List<Event> events){
        List<String> bestMatch = new ArrayList<>();
        Double bestJaccard = 0.0;
        HashMap<List<String>, Double> jaccards = new HashMap<>();

        for(var gt: groundTruths){
            closestMatch = new ArrayList<>(gt);
            computeConfusionMatrix(events);
            var jaccard = calculateJaccard();
            jaccards.put(gt, jaccard);
            if(jaccard > bestJaccard){
                bestJaccard = jaccard;
                bestMatch = new ArrayList<>(gt);
            }
        }

        closestMatch = new ArrayList<>(bestMatch);
        jaccard = bestJaccard;
        return jaccard;
    }

    public double calculatePrecision(){
        precision = (double)tp/(tp+fp);
        return precision;
    }

    public double calculateRecall(){
        recall = (double)tp/(tp+fn);
        return recall;
    }

    public double calculateAccuracy(){
        accuracy = (double)(tp+tn)/(tp+tn+fp+fn);
        return accuracy;
    }

    public double calculateFScore(){
        double precision = calculatePrecision();
        double recall = calculateRecall();
        fscore = precision == 0 && recall == 0 ? 0.0 : (2 * precision * recall)/(precision + recall);
        return fscore;
    }

    public double getPrecision(){ return precision; }

    public double getRecall(){ return recall; }

    public double getAccuracy(){ return accuracy; }

    public double getFscore(){ return fscore; }

    public double getJaccard(){ return jaccard; }

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Pattern pattern = (Pattern) obj;
            return this.pattern.equals(pattern.getPattern());
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(pattern);
    }

    @Override
    public String toString(){
        return this.pattern.toString();
    }

    @Override
    public int compareTo(Pattern e){
        if(e.getLength() == this.length)
            return e.getAbsoluteSupport() - this.getAbsoluteSupport();
        else
            return e.getLength() - this.getLength();
    }
}
