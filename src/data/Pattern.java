package data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Pattern {
    List<String> pattern;
    List<String> closestMatch;
    int length;
    double relSupport;
    int absSup;

    int tp = 0;
    int tn = 0;
    int fp = 0;
    int fn = 0;

    double precision;
    double recall;
    double accuracy;
    double fscore;

    public Pattern(List<String> pattern, Double relSup, Integer absSup){
        this.pattern = new ArrayList<>(pattern);
        this.closestMatch = new ArrayList<>();
        this.length = pattern.size();
        this.relSupport = relSup;
        this.absSup = absSup;
    }

    public Pattern(List<String> pattern, Integer absSup){
        this.pattern = new ArrayList<>(pattern);
        this.closestMatch = new ArrayList<>();
        this.length = pattern.size();
        this.relSupport = 0.0; // general repeats work with unsegmented log, thus we don't have a total amount of cases
        this.absSup = absSup;
    }

    public List<String> getPattern() { return this.pattern; }

    public List<String> getClosestMatch(){ return this.closestMatch; }

    public Double getRelativeSupport(){ return this.relSupport; }

    public Integer getAbsoluteSupport(){ return this.absSup; }

    public int getLength(){ return this.length; }

    public void assignClosestMatch(List<List<String>> groundTruth){
        List<String> closestMatch = new ArrayList<>(groundTruth.get(0));
        Integer bestMatchScore = LevenshteinDistance(pattern, groundTruth.get(0));
        for(int i = 1; i < groundTruth.size(); i++) {
            Integer matchScore = LevenshteinDistance(pattern, groundTruth.get(i));
            if (bestMatchScore > matchScore) {
                closestMatch = new ArrayList<>(groundTruth.get(i));
                bestMatchScore = matchScore;
            }
        }
        this.closestMatch = new ArrayList<>(closestMatch);
    }

    private Integer LevenshteinDistance(List<String> pattern, List<String> groundTruth){
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
        return editMatrix[rows - 1][cols - 1];
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
        fscore = (2 * precision * recall)/(precision + recall);
        return fscore;
    }

    public double getPrecision(){ return precision; }

    public double getRecall(){ return recall; }

    public double getAccuracy(){ return accuracy; }

    public double getFscore(){ return fscore; }

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
}
