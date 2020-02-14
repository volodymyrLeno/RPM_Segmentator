import data.Edge;
import data.Node;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

public class repeatsMiner {
    public static final int MATCHSCORE = 1;
    public static final int MISMATCHSCORE = -1;
    public static final int GAPSCORE = -1;

    int threshold = 0;

    int[][] matrix;

    //static HashMap<List<Node>, List<Repeat>> patterns = new HashMap<>();

    static List<List<Node>> ptrns = new ArrayList<>();
    static HashMap<List<Node>, Integer> patterns = new HashMap<>();

    public static void m(List<Node> sequence, int threshold){
        int[][] matrix = buildMatrix(sequence);

        //System.out.println("Matrix built");
        //printMatrix(matrix, str);
        findRepeats(matrix, sequence, threshold);

        //ptrns = ptrns.stream().filter(el -> el.size() == 1).collect(Collectors.toList());

        String str = sequence.toString().replaceAll("\\[","").replaceAll("\\]", "");

        for(var pattern: ptrns)
            patterns.put(pattern, getExactMatches(str, pattern));

        Map<List<Node>, Integer> sorted = patterns.entrySet().stream().sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        return;
    }

    public static Integer getExactMatches(String str, List<Node> ptrn){
        Pattern pattern;
        String targetString = ptrn.toString().replaceAll("\\[","").
                replaceAll("\\]", "");
        if(ptrn.size() == 1)
            pattern = Pattern.compile(targetString + "($|,)");
        else
            pattern = Pattern.compile(targetString);

        Matcher matcher = pattern.matcher(str);

        Integer count = 0;
        while (matcher.find())
            count++;
        return count;
    }

    /*
    public static Integer getApproximateMatches(List<Node> sequence, List<Node> ptrn, Integer threshold){

    }
    */

    public static int[][] buildMatrix(List<Node> sequence, List<Node> ptrn){
        int[][] matrix = new int[ptrn.size() + 1][sequence.size() + 1];
        int score;
        for(int i = 0; i <= ptrn.size(); i++)
            matrix[i][0] = 0;
        for(int j = 0; j < sequence.size(); j++)
            matrix[0][j] = 0;
        for(int i = 1; i < ptrn.size(); i++)
            for(int j = 1; j < sequence.size(); j++){
                if(sequence.get(j-1).equals(ptrn.get(i-1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                matrix[i][j] = max(matrix[i-1][j-1] + score, matrix[i-1][j] + GAPSCORE, matrix[i][j-1] + GAPSCORE, 0);
            }
            return matrix;
    }

    public static int[][] buildMatrix(List<Node> sequence){
        int[][] matrix = new int[sequence.size() + 1][sequence.size() + 1];
        int score;
        for(int i = 0; i <= sequence.size(); i++){
            matrix[0][i] = 0;
            matrix[i][i] = 0;
        }
        for(int i = 1; i <= sequence.size(); i++)
            for(int j = i + 1; j <= sequence.size(); j++){
                if(sequence.get(j-1).equals(sequence.get(i-1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                matrix[i][j] = max(matrix[i-1][j-1] + score, matrix[i-1][j] + GAPSCORE, matrix[i][j-1] + GAPSCORE, 0);
            }
        return matrix;
    }

    public static void findRepeats(int[][] matrix, List<Node> sequence, int threshold){
        Position[] path;
        Position end_path = findMax(matrix);
        while(matrix[end_path.getRow()][end_path.getCol()] >= threshold) {
            path = traceBack(matrix, sequence, end_path);
            matrix = adjustMatrix(matrix, path, sequence);

            //matrix[end_path.getRow()][end_path.getCol()] = 0;

            //System.out.println("Adjusted Matrix: ");
            //printMatrix(matrix, str);
            end_path = findMax(matrix);
        }
    }

    public static Position findMax(int[][] matrix) {
        int max = 0;
        Position maxPos = new Position();
        for(int i = 1; i < matrix.length;i++) {
            for(int j = i; j < matrix.length; j++) {
                if(matrix[i][j] >= max) {
                    max = matrix[i][j];
                    maxPos = new Position(i,j);
                }
            }
        }
        return maxPos;
    }

    public static Position[] traceBack(int[][] matrix, List<Node> sequence, Position end_path) {
        int i = end_path.getRow();
        int j = end_path.getCol();
        Position[] tmppath = new Position[2 * i];
        int path_i = 0;

        List<Node> topSeq = new ArrayList<>();
        List<Node> leftSeq = new ArrayList<>();

        while (matrix[i][j] != 0) {
            tmppath[path_i] = new Position(i, j);
            path_i++;
            if (matrix[i][j] == matrix[i][j - 1] + GAPSCORE) {
                topSeq.add(sequence.get(j - 1));
                leftSeq.add(null);
                j--;
            } else if (matrix[i][j] == matrix[i - 1][j] + GAPSCORE) {
                topSeq.add(null);
                leftSeq.add(sequence.get(i - 1));
                i--;
            } else {
                topSeq.add(sequence.get(j - 1));
                leftSeq.add(sequence.get(i - 1));
                i--;
                j--;
            }
        }

        tmppath[path_i] = new Position(i, j);
        Position[] path = new Position[path_i + 1];
        for (i = 0; i < path.length; i++) {
            path[i] = tmppath[path_i - i];
        }

        Collections.reverse(topSeq);
        Collections.reverse(leftSeq);

        topSeq = topSeq.stream().filter(Objects::nonNull).collect(Collectors.toList());
        leftSeq = leftSeq.stream().filter(Objects::nonNull).collect(Collectors.toList());

        /*
        System.out.println();
        System.out.print("Top sequence: ");
        System.out.printf("%3d", (path[0].getCol() + 1));
        System.out.print(" " + topSeq + " ");
        System.out.printf("%3d", (path[path.length - 1].getCol()));
        System.out.println();
        System.out.print("Left sequence: ");
        System.out.printf("%3d", (path[0].getRow() + 1));
        System.out.print(" " + leftSeq + " ");
        System.out.printf("%3d", (path[path.length - 1].getRow()));
        System.out.println("\n");
        */

        Repeat r1 = new Repeat(topSeq,  path[0].getCol() + 1, path[path.length - 1].getCol());
        Repeat r2 = new Repeat(leftSeq, path[0].getRow(), path[path.length - 1].getRow());

        var pattern1 = new ArrayList<>(r1.getPattern());
        var pattern2 = new ArrayList<>(r2.getPattern());

        /*
        if(!patterns.containsKey(pattern1))
            patterns.put(pattern1, Collections.singletonList(r1));
        else if(!patterns.get(pattern1).contains(r1))
            patterns.put(pattern1, Stream.concat(patterns.get(pattern1).stream(), Stream.of(r1)).collect(Collectors.toList()));

        if(!patterns.containsKey(pattern2))
            patterns.put(pattern2, Collections.singletonList(r2));
        else if(patterns.get(pattern2).contains(r2))
            patterns.put(pattern2, Stream.concat(patterns.get(pattern2).stream(), Stream.of(r2)).collect(Collectors.toList()));
            */

        if(!ptrns.contains(pattern1))
            ptrns.add(pattern1);
        if(!ptrns.contains(pattern2))
            ptrns.add(pattern2);

        return path;

        /*
        String pattern = topStr.replaceAll("-", "");
        Repeat r1 = new Repeat(topStr, path[0].getCol() + 1, path[path.length - 1].getCol());
        Repeat r2 = new Repeat(leftStr, path[0].getRow(), path[path.length - 1].getRow());

        if(!patterns.containsKey(pattern))
            patterns.put(pattern, Stream.concat(Stream.of(r1), Stream.of(r2)).collect(Collectors.toList()));
        else{
            if(!patterns.get(pattern).contains(r1))
                patterns.put(pattern, Stream.concat(patterns.get(pattern).stream(),
                        Stream.of(r1)).collect(Collectors.toList()));
            if(!patterns.get(pattern).contains(r2))
                patterns.put(pattern, Stream.concat(patterns.get(pattern).stream(),
                        Stream.of(r2)).collect(Collectors.toList()));
        }
        return path;
        */
    }

    public static int max(int diag, int top, int left, int zero) {
        int max = 0;
        max = diag;
        if(max < top)
            max = top;
        if(max < left)
            max = left;
        if(max < zero)
            max = zero;
        return max;
    }

    public static int[][] adjustMatrix(int[][] matrix, Position[] path, List<Node> sequence) {
        Position pathfirstpos = path[0];
        int pathfirstrow = pathfirstpos.getRow();
        int[][] shaded = new int[matrix.length][2];
        shaded = shade(matrix, path, sequence);
        //printMatrix(matrix, str, path, shaded);
        int top, diag, left, score;

        for(int i = pathfirstrow+1; i < matrix.length &&
                shaded[i][0] != -1; i++) {
            for(int j = shaded[i][0]; j <= shaded[i][1]; j++) {
                if(sequence.get(i-1).equals(sequence.get(j-1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                if(isPath(path, i-1, j-1, i, j))
                    diag = 0;
                else
                    diag = matrix[i-1][j-1] + score;
                if(isPath(path, i-1, j, i, j))
                    top = 0;
                else
                    top = matrix[i-1][j] + GAPSCORE;
                if(isPath(path, i, j-1, i, j))
                    left = 0; //set the
                else
                    left= matrix[i][j-1] + GAPSCORE;
                matrix[i][j] = max(diag, top, left, 0);
            }
        }
        return matrix;
    }

    public static int[][] shade(int[][] matrix, Position[] path, List<Node> sequence) {
        int score, max;
        int[][] shaded = new int[matrix.length][2];
        for(int i = 0; i < shaded.length; i++) {
            shaded[i][0] = -1;
        }
        for(int i = 0; i < path.length; i++) {
            int row = path[i].getRow();
            if(shaded[row][0] == -1)
                shaded[row][0] = shaded[row][1] =
                        path[i].getCol();
            else
                shaded[row][1] = path[i].getCol();
        }
        for(int i = (path[0].getRow())+1; i < matrix.length && shaded[i-1][0] != -1; i++) {
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
                    if(shaded[i][0]== -1)
                        shaded[i][0] = shaded[i][1] = j;
                    else if (shaded[i][0] != -1 && j < shaded[i][0] )
                        shaded[i][0] = j;
                    else if (j > shaded[i][1])
                        shaded[i][1] = j;
                }
                if (shaded[i][0] != -1 && !(isShaded(shaded, i - 1, j) || isShaded(shaded, i - 1, j + 1) || isShaded(shaded, i, j)))
                    foundEnd = true;
                if (shaded[i][0] == -1 && j > shaded[i-1][1]+1)
                    foundEnd = true;
            }
        }
        return shaded;
    }

    public static boolean isShaded(int[][] shaded, int row, int col)
    {
        if(shaded[row][0] != -1 && col >= shaded[row][0] && col <= shaded[row][1]) {
            return true;
        }
        return false;
    }

    public static boolean isPath(Position[] path, int prevrow, int prevcol, int row, int col) {
        Position pathpos = path[0];

        for(int i = 0; i < path.length; i++) {
            pathpos = path[i];
            if(pathpos.getRow() == row && pathpos.getCol() == col) {
                if(path[i-1].getRow() == prevrow && path[i- 1].getCol() == prevcol)
                    return true;
                else
                    return false;
            }
        }
        return false;
    }

    public static boolean isPath(Position[] path, int row, int col) {
        Position pathpos = path[0];

        for(int i = 0; i < path.length; i++) {
            pathpos = path[i];
            if(pathpos.getRow() == row && pathpos.getCol() == col)
                return true;
        }
        return false;
    }
}

class Repeat{
    private int start, end;
    private List<Node> pattern;

    Repeat(List<Node> pattern, int start, int end){
        this.pattern = new ArrayList<>(pattern);
        this.start = start;
        this.end = end;
    }

    public List<Node> getPattern(){ return pattern; }

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