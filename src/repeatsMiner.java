import data.Edge;
import data.Node;
import data.Pattern;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import static java.util.Map.Entry.comparingByValue;

public class repeatsMiner {
    private static final int MATCHSCORE = 1;
    private static final int MISMATCHSCORE = -1;
    private static final int GAPSCORE = -1;

    private static List<List<String>> ptrns = new ArrayList<>();
    private static HashMap<List<String>, Integer> patterns = new HashMap<>();

    public static List<data.Pattern> discoverRepeats(List<String> sequence, int threshold, int minSup, int maxDiff) {
        int[][] matrix = buildMatrix(sequence);
        System.out.print("Searching for repeats...\n");
        long startTime = System.currentTimeMillis();
        findRepeats(matrix, sequence, threshold);
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

        for(var pattern: ptrns)
            getApproximateMatches(sequence, pattern, maxDiff);

        var result = patterns.entrySet().stream().sorted(Collections.reverseOrder(comparingByValue())).filter(map -> map.getValue() > minSup).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        List<Pattern> patterns = new ArrayList<>();
        for(var key: result.keySet())
            patterns.add(new Pattern(key, result.get(key)));

        return patterns;
    }

    private static int[][] buildMatrix(List<String> sequence) {
        int[][] matrix = new int[sequence.size() + 1][sequence.size() + 1];
        int score;
        for (int i = 0; i <= sequence.size(); i++) {
            matrix[0][i] = 0;
            matrix[i][i] = 0;
        }
        for (int i = 1; i <= sequence.size(); i++)
            for (int j = i + 1; j <= sequence.size(); j++) {
                if (sequence.get(j - 1).equals(sequence.get(i-1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                matrix[i][j] = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);
            }
        return matrix;
    }

    private static int max(int diag, int top, int left, int zero) {
        int max = diag;
        if (max < top)
            max = top;
        if (max < left)
            max = left;
        if (max < zero)
            max = zero;
        return max;
    }

    private static void findRepeats(int[][] matrix, List<String> sequence, int threshold) {
        Position[] path;
        Position end_path = findMax(matrix);
        while (matrix[end_path.getRow()][end_path.getCol()] >= threshold) {
            path = traceBack(matrix, sequence, end_path);
            matrix = adjustMatrix(matrix, path, sequence);
            end_path = findMax(matrix);
        }
    }

    private static Position findMax(int[][] matrix) {
        int max = 0;
        Position maxPos = new Position();
        for (int i = 1; i < matrix.length; i++) {
            for (int j = i; j < matrix[i].length; j++) {
                if (matrix[i][j] >= max) {
                    max = matrix[i][j];
                    maxPos = new Position(i, j);
                }
            }
        }
        return maxPos;
    }

    private static Position[] traceBack(int[][] matrix, List<String> sequence, Position end_path) {
        int i = end_path.getRow();
        int j = end_path.getCol();
        Position[] tmppath = new Position[2 * i];
        int path_i = 0;

        List<String> topSeq = new ArrayList<>();
        List<String> leftSeq = new ArrayList<>();

        while (matrix[i][j] != 0) {
            tmppath[path_i] = new Position(i, j);
            path_i++;
            if (matrix[i][j] == matrix[i][j - 1] + GAPSCORE) {
                topSeq.add(0, sequence.get(j - 1));
                leftSeq.add(0, null);
                j--;
            } else if (matrix[i][j] == matrix[i - 1][j] + GAPSCORE) {
                topSeq.add(0, null);
                leftSeq.add(0, sequence.get(i - 1));
                i--;
            } else {
                topSeq.add(0, sequence.get(j - 1));
                leftSeq.add(0, sequence.get(i - 1));
                i--;
                j--;
            }
        }

        tmppath[path_i] = new Position(i, j);
        Position[] path = new Position[path_i + 1];
        for (i = 0; i < path.length; i++) {
            path[i] = tmppath[path_i - i];
        }

        int startTop = path[0].getCol() + 1;
        int endTop = path[path.length - 1].getCol();
        int startLeft = path[0].getRow() + 1;
        int endLeft = path[path.length - 1].getRow();

        topSeq = topSeq.stream().filter(Objects::nonNull).collect(Collectors.toList());
        leftSeq = leftSeq.stream().filter(Objects::nonNull).collect(Collectors.toList());

        Repeat r1 = new Repeat(topSeq,  startTop, endTop);
        Repeat r2 = new Repeat(leftSeq, startLeft, endLeft);

        var pattern1 = new ArrayList<>(r1.getPattern());
        var pattern2 = new ArrayList<>(r2.getPattern());

        if(!ptrns.contains(pattern1))
            ptrns.add(pattern1);
        if(!ptrns.contains(pattern2))
            ptrns.add(pattern2);

        return path;
    }

    private static int[][] adjustMatrix(int[][] matrix, Position[] path, List<String> sequence) {
        Position pathfirstpos = path[0];
        int pathfirstrow = pathfirstpos.getRow();
        int[][] shaded = shade(matrix, path, sequence);

        int top, diag, left, score;

        for (int i = pathfirstrow + 1; i < matrix.length &&
                shaded[i][0] != -1; i++) {
            for (int j = shaded[i][0]; j <= shaded[i][1]; j++) {
                if (sequence.get(i - 1).equals(sequence.get(j - 1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                if (isPath(path, i - 1, j - 1, i, j))
                    diag = 0;
                else
                    diag = matrix[i - 1][j - 1] + score;
                if (isPath(path, i - 1, j, i, j))
                    top = 0;
                else
                    top = matrix[i - 1][j] + GAPSCORE;
                if (isPath(path, i, j - 1, i, j))
                    left = 0;
                else
                    left = matrix[i][j - 1] + GAPSCORE;
                matrix[i][j] = max(diag, top, left, 0);
            }
        }
        return matrix;
    }

    private static int[][] shade(int[][] matrix, Position[] path, List<String> sequence) {
        int score, max;
        int[][] shaded = new int[matrix.length][2];
        for (int i = 0; i < shaded.length; i++) {
            shaded[i][0] = -1;
        }
        for (Position position : path) {
            int row = position.getRow();
            if (shaded[row][0] == -1)
                shaded[row][0] = shaded[row][1] = position.getCol();
            else
                shaded[row][1] = position.getCol();
        }
        for (int i = (path[0].getRow()) + 1; i < matrix.length && shaded[i - 1][0] != -1; i++) {
            boolean foundEnd = false;

            for (int j = (shaded[i - 1][0] > i ? shaded[i - 1][0] : i + 1); j < matrix[0].length && !foundEnd; j++) {
                if (sequence.get(i - 1).equals(sequence.get(j - 1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                max = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);
                boolean comesFromShaded = !((max == matrix[i - 1][j - 1] + score) && !isShaded(shaded, i - 1, j - 1) ||
                        (max == matrix[i - 1][j] + GAPSCORE) && !isShaded(shaded, i - 1, j) ||
                        (max == matrix[i][j - 1] + GAPSCORE) && !isShaded(shaded, i, j - 1));
                if (max != 0 && comesFromShaded) {
                    if (shaded[i][0] == -1)
                        shaded[i][0] = shaded[i][1] = j;
                    else if (shaded[i][0] != -1 && j < shaded[i][0])
                        shaded[i][0] = j;
                    else if (j > shaded[i][1])
                        shaded[i][1] = j;
                }
                if (shaded[i][0] != -1 && !(isShaded(shaded, i - 1, j) || isShaded(shaded, i - 1, j + 1) || isShaded(shaded, i, j)))
                    foundEnd = true;
                if (shaded[i][0] == -1 && j > shaded[i - 1][1] + 1)
                    foundEnd = true;
            }
        }
        return shaded;
    }

    private static boolean isShaded(int[][] shaded, int row, int col) {
        return shaded[row][0] != -1 && col >= shaded[row][0] && col <= shaded[row][1];
    }

    private static boolean isPath(Position[] path, int prevrow, int prevcol, int row, int col) {
        Position pathpos;

        for (int i = 0; i < path.length; i++) {
            pathpos = path[i];
            if (pathpos.getRow() == row && pathpos.getCol() == col) {
                return path[i - 1].getRow() == prevrow && path[i - 1].getCol() == prevcol;
            }
        }
        return false;
    }

    public static boolean isPath(Position[] path, int row, int col) {
        Position pathpos;

        for (Position position : path) {
            pathpos = position;
            if (pathpos.getRow() == row && pathpos.getCol() == col)
                return true;
        }
        return false;
    }


    /********* Approximate matches *********/

    public static int support;
    private static Set<Integer> coveredColumns;

    private static void getApproximateMatches(List<String> sequence, List<String> pattern, int maxDiff) {
        coveredColumns = new HashSet<>();
        java.util.regex.Pattern ptrn;

        //int threshold = pattern.size() - 2 * maxDiff;
        int threshold = 1; // to find all potential matches

        if(threshold > 0){
            support = 0;
            int[][] matrix = buildMatrix(sequence, pattern);
            System.out.print("\tIdentifying approximate matches...");
            long startTime = System.currentTimeMillis();
            findRepeats(matrix, sequence, pattern, threshold, maxDiff);
            patterns.put(new ArrayList<>(pattern), support);
            long stopTime = System.currentTimeMillis();
            System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");
        }
        else{
            String str = sequence.toString().replaceAll("\\[","").replaceAll("\\]", "");
            String targetString = pattern.toString().replaceAll("\\[","").replaceAll("\\]", "");
            ptrn = java.util.regex.Pattern.compile(targetString + "($|,)");
            Matcher matcher = ptrn.matcher(str);
            Integer count = 0;
            while (matcher.find())
                count++;
            patterns.put(pattern, count);
        }
    }

    private static int[][] buildMatrix(List<String> sequence, List<String> pattern) {
        int[][] matrix = new int[pattern.size() + 1][sequence.size() + 1];
        int score;
        for (int i = 0; i <= pattern.size(); i++)
            matrix[i][0] = 0;
        for (int j = 0; j <= sequence.size(); j++)
            matrix[0][j] = 0;
        for (int i = 1; i <= pattern.size(); i++)
            for (int j = 1; j <= sequence.size(); j++) {
                if (sequence.get(j - 1).equals(pattern.get(i - 1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                matrix[i][j] = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);
            }
        return matrix;
    }

    private static void findRepeats(int[][] matrix, List<String> sequence, List<String> pattern, int threshold, int maxDiff) {
        Position[] path;
        Position end_path = findMax1(matrix);
        while (matrix[end_path.getRow()][end_path.getCol()] >= threshold && end_path.getCol() >= pattern.size()) {
            path = traceBack(matrix, sequence, pattern, end_path, maxDiff);
            matrix = adjustMatrix(matrix, path, sequence, pattern);
            end_path = findMax1(matrix);
        }
    }

    private static Position findMax1(int[][] matrix) {
        int max = 0;
        int i = matrix.length - 1;
        Position maxPos = new Position();
        for(int j = 1; j < matrix[i].length; j++){
            if(matrix[i][j] >= max){
                max = matrix[i][j];
                maxPos = new Position(i, j);
            }
        }
        return maxPos;
    }

    private static Position[] traceBack(int[][] matrix, List<String> sequence, List<String> pattern, Position end_path, int maxDiff){
        int i = end_path.getRow();
        int j = end_path.getCol();
        Position[] tmppath = new Position[2 * i];
        int path_i = 0;

        List<String> appMatch = new ArrayList<>();
        while(i != 0){
            tmppath[path_i] = new Position(i, j);
            path_i++;
            if (matrix[i][j] == matrix[i][j - 1] + GAPSCORE) {
                appMatch.add(0, sequence.get(j - 1));
                j--;
            } else if (matrix[i][j] == matrix[i - 1][j] + GAPSCORE) {
                appMatch.add(0, null);
                i--;
            } else {
                appMatch.add(0, sequence.get(j-1));
                i--;
                j--;
            }
        }

        tmppath[path_i] = new Position(i, j);
        Position[] path = new Position[path_i + 1];
        for (i = 0; i < path.length; i++) {
            path[i] = tmppath[path_i - i];
        }
        int start = path[1].getCol();
        int end = path[path.length-1].getCol();

        boolean overlap = false;
        int zeroes = 0;
        for(int k = 1; k < path.length; k++){
            if(matrix[path[k].getRow()][path[k].getCol()] == 0)
                zeroes++;
            if(coveredColumns.contains(path[k].getCol()))
                overlap = true;
        }

        if(zeroes <= maxDiff && !overlap)
            support++;

        return path;
    }


    private static int[][] adjustMatrix(int[][] matrix, Position[] path, List<String> sequence, List<String> pattern){
        List<Integer> coveredCol = Arrays.asList(path).subList(1, path.length).stream().
                map(Position::getCol).distinct().collect(Collectors.toList());

        coveredColumns.addAll(coveredCol);

        System.out.print("\tShading the matrix...");
        long startTime = System.currentTimeMillis();
        List<Position> shaded = shade(matrix, path, sequence, pattern);
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");
        int score;

        for (Position position : path) {
            int row = position.getRow();
            int col = position.getCol();
            matrix[row][col] = 0;
        }

        System.out.print("\tRecalculating affected cells...");
        startTime = System.currentTimeMillis();
        for(var column: coveredCol)
            for(int i = 0; i <= pattern.size(); i++)
                matrix[i][column] = 0;

        for(var pos: shaded){
            int i = pos.getRow();
            int j = pos.getCol();

            if(matrix[i][j] != 0){
                if (sequence.get(j - 1).equals(pattern.get(i - 1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;

                matrix[i][j] = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);
            }
        }
        stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

        return matrix;
    }

    private static List<Position> shade(int[][] matrix, Position[] path, List<String> sequence, List<String> pattern){
        List<Position> shaded = new ArrayList<>(List.of(path));

        Set<Integer> nonCoveredCols = new HashSet<>();
        for(int j = 1; j <= sequence.size(); j++)
            if(!coveredColumns.contains(j))
                nonCoveredCols.add(j);

        for(int i = 1; i <= pattern.size(); i++)
            for(var j: coveredColumns)
                shaded.add(new Position(i, j));

            for(int i = 1; i <= pattern.size(); i++)
                for(var j: nonCoveredCols){
                    Position tempPos = new Position(i, j);
                    if(isShaded(matrix, tempPos, sequence, pattern, shaded))
                        shaded.add(tempPos);
                }
                /*
        for(int i = 1; i <= pattern.size(); i++)
            for(int j = 1; j <= sequence.size(); j++){
                Position tempPos = new Position(i, j);
                if(coveredColumns.contains(tempPos.getCol()) || isShaded(matrix, tempPos, sequence, pattern, shaded))
                    shaded.add(tempPos);
            }
            */
        return shaded;
    }

    private static List<Position> comingFrom(int[][] matrix, Position pos, List<String> sequence, List<String> pattern){
        List<Position> positions = new ArrayList<>();
        int score;
        int i = pos.getRow();
        int j = pos.getCol();

        if (sequence.get(j - 1).equals(pattern.get(i - 1)))
            score = MATCHSCORE;
        else
            score = MISMATCHSCORE;

        int max = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);

        if(max == matrix[i - 1][j - 1] + score)
            positions.add(new Position(i-1, j-1));
        if(max == matrix[i - 1][j] + GAPSCORE)
            positions.add(new Position(i-1, j));
        if(max == matrix[i][j - 1] + GAPSCORE)
            positions.add(new Position(i, j-1));
        return positions;
    }

    private static boolean isShaded(int[][] matrix, Position pos, List<String> sequence, List<String> pattern, List<Position> shaded){
        var positions = new ArrayList<>(comingFrom(matrix, pos, sequence, pattern));
        if(positions.size() == 0)
            return false;
        for(var position: positions){
            if(!shaded.contains(position))
                return false;
        }
        return true;
    }
}

class Repeat{
    private int start, end;
    private List<String> pattern;

    Repeat(List<String> pattern, int start, int end){
        this.pattern = new ArrayList<>(pattern);
        this.start = start;
        this.end = end;
    }

    public List<String> getPattern(){ return pattern; }

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Repeat repeat = (Repeat) obj;
            return this.pattern.equals(repeat.pattern) && this.start == repeat.start && this.end == repeat.end;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(pattern, start, end);
    }
}

class Position {
    private int row, col;

    Position(){ ; }

    Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        return "(" + getRow() + ", " + getCol() + ")";
    }

    @Override
    public boolean equals(Object obj){
        if(obj != null && getClass() == obj.getClass()){
            Position position = (Position) obj;
            return this.row == position.getRow() && this.col == position.getCol();
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(row, col);
    }
}