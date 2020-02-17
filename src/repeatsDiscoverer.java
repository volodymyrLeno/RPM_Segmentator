import data.Node;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class repeatsDiscoverer {
    public static final int MATCHSCORE = 1;
    public static final int MISMATCHSCORE = -1;
    public static final int GAPSCORE = -1;

    public static void discoverRepeats(String str, int threshold) {
        System.out.println(str);

        int[][] matrix = buildMatrix(str);

        System.out.println("Matrix built");
        printMatrix(matrix, str);
        findRepeats(matrix, str, threshold);
        return;
    }

    public static int[][] buildMatrix(String str) {
        int[][] matrix = new int[str.length() + 1][str.length() + 1];
        int score;
        for (int i = 0; i <= str.length(); i++) {
            matrix[0][i] = 0;
            matrix[i][i] = 0;
        }
        for (int i = 1; i <= str.length(); i++)
            for (int j = i + 1; j <= str.length(); j++) {
                if (str.charAt(j - 1) == str.charAt(i - 1))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                matrix[i][j] = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);
            }
        return matrix;
    }

    public static void findRepeats(int[][] matrix, String str, int threshold) {
        Position[] path;
        Position end_path = findMax(matrix);
        while (matrix[end_path.getRow()][end_path.getCol()] >= threshold) {
            path = traceBack(matrix, str, end_path);
            matrix = adjustMatrix(matrix, path, str);

            //matrix[end_path.getRow()][end_path.getCol()] = 0;

            System.out.println("Adjusted Matrix: ");
            printMatrix(matrix, str);
            end_path = findMax(matrix);
        }
    }

    public static Position findMax(int[][] matrix) {
        int max = 0;
        Position maxPos = new Position();
        for (int i = 1; i < matrix.length; i++) {
            for (int j = i; j < matrix[i].length; j++) {
                //if(matrix[i][j] > max){
                if (matrix[i][j] >= max) {
                    max = matrix[i][j];
                    maxPos = new Position(i, j);
                }
            }
        }
        return maxPos;
    }

    public static Position[] traceBack(int[][] matrix, String str, Position end_path) {
        int i = end_path.getRow();
        int j = end_path.getCol();
        Position[] tmppath = new Position[2 * i];
        int path_i = 0;

        StringBuffer topStrbf = new StringBuffer();
        StringBuffer leftStrbf = new StringBuffer();
        while (matrix[i][j] != 0) {
            tmppath[path_i] = new Position(i, j);
            path_i++;
            if (matrix[i][j] == matrix[i][j - 1] + GAPSCORE) {
                topStrbf.insert(0, str.charAt(j - 1));
                leftStrbf.insert(0, "-");
                j--;
            } else if (matrix[i][j] == matrix[i - 1][j] + GAPSCORE) {
                topStrbf.insert(0, "-");
                leftStrbf.insert(0, str.charAt(i - 1));
                i--;
            } else {
                topStrbf.insert(0, str.charAt(j - 1));
                leftStrbf.insert(0, str.charAt(i - 1));
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


        /*
        if(endLeft > startTop){
            int overlapSize = (endLeft - startTop) + 1;

            for(int l = 0; l < leftStrbf.length(); l++)
                if(leftStrbf.charAt(l) == '-')
                    overlapSize++;

            topStrbf.delete(topStrbf.length() - overlapSize, topStrbf.length());
            leftStrbf.delete(leftStrbf.length() - overlapSize, leftStrbf.length());

            endTop = endTop - overlapSize;
            endLeft = endLeft - overlapSize;
        }
        */

        String topStr = topStrbf.toString();
        String leftStr = leftStrbf.toString();
        System.out.println();
        System.out.print("Top string: ");
        System.out.printf("%3d", startTop);
        System.out.print(" " + topStr + " ");
        System.out.printf("%3d", endTop);
        System.out.println();
        System.out.print("Left string: ");
        System.out.printf("%3d", startLeft);
        System.out.print(" " + leftStr + " ");
        System.out.printf("%3d", endLeft);
        System.out.println("\n");

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
        */

        printMatrix(matrix, str, path);
        return path;
    }


    public static int[][] adjustMatrix(int[][] matrix, Position[] path, String str) {
        Position pathfirstpos = path[0];
        int pathfirstrow = pathfirstpos.getRow();
        int[][] shaded = new int[matrix.length][2];
        shaded = shade(matrix, path, str);
        //printMatrix(matrix, str, path, shaded);
        int top, diag, left, score;

        for (int i = pathfirstrow + 1; i < matrix.length &&
                shaded[i][0] != -1; i++) {
            for (int j = shaded[i][0]; j <= shaded[i][1]; j++) {
                if (str.charAt(i - 1) == str.charAt(j - 1))
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
                    left = 0; //set the
                else
                    left = matrix[i][j - 1] + GAPSCORE;
                matrix[i][j] = max(diag, top, left, 0);
            }
        }
        return matrix;
    }

    public static int[][] shade(int[][] matrix, Position[] path, String str) {
        int score, max;
        int[][] shaded = new int[matrix.length][2];
        for (int i = 0; i < shaded.length; i++) {
            shaded[i][0] = -1;
        }
        for (int i = 0; i < path.length; i++) {
            int row = path[i].getRow();
            if (shaded[row][0] == -1)
                shaded[row][0] = shaded[row][1] =
                        path[i].getCol();
            else
                shaded[row][1] = path[i].getCol();
        }
        for (int i = (path[0].getRow()) + 1; i < matrix.length && shaded[i - 1][0] != -1; i++) {
            boolean foundEnd = false;

            for (int j = (shaded[i - 1][0] > i ? shaded[i - 1][0] : i + 1); j < matrix[0].length && !foundEnd; j++) {
                if (str.charAt(i - 1) == str.charAt(j - 1))
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

    public static boolean isShaded(int[][] shaded, int row, int col) {
        if (shaded[row][0] != -1 && col >= shaded[row][0] && col <= shaded[row][1]) {
            return true;
        }
        return false;
    }

    public static boolean isPath(Position[] path, int prevrow, int prevcol, int row, int col) {
        Position pathpos = path[0];

        for (int i = 0; i < path.length; i++) {
            pathpos = path[i];
            if (pathpos.getRow() == row && pathpos.getCol() == col) {
                if (path[i - 1].getRow() == prevrow && path[i - 1].getCol() == prevcol)
                    return true;
                else
                    return false;
            }
        }
        return false;
    }

    public static boolean isPath(Position[] path, int row, int col) {
        Position pathpos = path[0];

        for (int i = 0; i < path.length; i++) {
            pathpos = path[i];
            if (pathpos.getRow() == row && pathpos.getCol() == col)
                return true;
        }
        return false;
    }

    public static void printMatrix(int[][] matrix, String str) {
        System.out.print("  |   | ");

        for (int i = 0; i < str.length(); i++) {
            System.out.print(str.charAt(i) + " | ");
        }
        System.out.println();

        for (int i = 0; i < matrix.length; i++) {
            if (i == 0)
                System.out.print("  | ");
            else
                System.out.print(str.charAt(i - 1) + " | ");

            for (int j = 0; j < matrix.length; j++)
                System.out.print(matrix[i][j] + " | ");
            System.out.println();
        }
    }

    public static void printMatrix(int[][] matrix, String str, Position[] path) {
        System.out.println("Matrix with path: ");
        System.out.print(" | | ");

        for (int i = 0; i < str.length(); i++) {
            System.out.print(str.charAt(i) + " | ");
        }
        System.out.println();

        for (int i = 0; i < matrix.length; i++) {
            System.out.print(i == 0 ? " | " : str.charAt(i - 1) + " | ");

            for (int j = 0; j < matrix.length; j++) {
                if (isPath(path, i, j))
                    System.out.print(matrix[i][j] + "*| ");
                else
                    System.out.print(matrix[i][j] + " | ");
            }
            System.out.println();
        }
    }

    public static void printMatrix(int[][] matrix, String str, Position[] path, int[][] shaded) {
        System.out.println("Matrix with shading: ");
        System.out.print(" | | ");

        for (int i = 0; i < str.length(); i++) {
            System.out.print(str.charAt(i) + " | ");
        }
        System.out.println();

        for (int i = 0; i < matrix.length; i++) {
            System.out.print(i == 0 ? " | " : str.charAt(i - 1) + " | ");
            for (int j = 0; j < matrix.length; j++) {
                if (isPath(path, i, j))
                    System.out.print(matrix[i][j] + "*| ");
                else if (isShaded(shaded, i, j))
                    System.out.print(matrix[i][j] + "$| ");
                else
                    System.out.print(matrix[i][j] + " | ");
            }
            System.out.println();
        }
    }

    /********************** Approximate match ***********************/

    private static List<Integer> coveredColumns = new ArrayList<>();

    public static void m(String sequence, String pattern, int maxDiff) {
        coveredColumns.clear();
        int threshold = pattern.length() - 2 * maxDiff;
        if(threshold > 0){
            int[][] matrix = buildMatrix(sequence, pattern);
            printMatrix(matrix, sequence, pattern);

            findRepeats(matrix, sequence, pattern, threshold, maxDiff);
        }
    }

    public static int[][] buildMatrix(String sequence, String ptrn) {
        int[][] matrix = new int[ptrn.length() + 1][sequence.length() + 1];
        int score;
        for (int i = 0; i <= ptrn.length(); i++)
            matrix[i][0] = 0;
        for (int j = 0; j <= sequence.length(); j++)
            matrix[0][j] = 0;
        for (int i = 1; i <= ptrn.length(); i++)
            for (int j = 1; j <= sequence.length(); j++) {
                if (sequence.charAt(j - 1) == (ptrn.charAt(i - 1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;
                matrix[i][j] = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);
            }
        return matrix;
    }

    public static int max(int diag, int top, int left, int zero) {
        int max = 0;
        max = diag;
        if (max < top)
            max = top;
        if (max < left)
            max = left;
        if (max < zero)
            max = zero;
        return max;
    }

    public static void printMatrix(int[][] matrix, String sequence, String pattern) {
        System.out.print("  |   | ");

        for (int i = 0; i < sequence.length(); i++) {
            System.out.print(sequence.charAt(i) + " | ");
        }
        System.out.println();

        for (int i = 0; i < matrix.length; i++) {
            if (i == 0)
                System.out.print("  | ");
            else
                System.out.print(pattern.charAt(i - 1) + " | ");

            for (int j = 0; j <= sequence.length(); j++)
                System.out.print(matrix[i][j] + " | ");
            System.out.println();
        }
    }

    public static void findRepeats(int[][] matrix, String sequence, String pattern, int threshold, int maxDiff) {
        Position[] path;
        Position end_path = findMax1(matrix);
        while (matrix[end_path.getRow()][end_path.getCol()] >= threshold && end_path.getCol() >= pattern.length()) {
            path = traceBack(matrix, sequence, pattern, end_path, maxDiff);

            matrix = adjustMatrix(matrix, path, sequence, pattern);

            System.out.println("Adjusted Matrix: ");
            printMatrix(matrix, sequence, pattern);
            end_path = findMax1(matrix);
        }
    }

    public static Position findMax1(int[][] matrix) {
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

    public static Position[] traceBack(int[][] matrix, String sequence, String pattern, Position end_path, int maxDiff){
        int i = end_path.getRow();
        int j = end_path.getCol();
        Position[] tmppath = new Position[2 * i];
        int path_i = 0;

        StringBuffer appMatch = new StringBuffer();
        while(i != 0){
            tmppath[path_i] = new Position(i, j);
            path_i++;
            if (matrix[i][j] == matrix[i][j - 1] + GAPSCORE) {
                appMatch.insert(0, sequence.charAt(j - 1));
                j--;
            } else if (matrix[i][j] == matrix[i - 1][j] + GAPSCORE) {
                appMatch.insert(0, "-");
                i--;
            } else {
                appMatch.insert(0, sequence.charAt(j-1));
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
            System.out.println("\nFound match - " + appMatch.toString() + " (startIndex = " + start + ", endIndex = " + end + ")\n");

        return path;
    }


    public static int[][] adjustMatrix(int[][] matrix, Position[] path, String sequence, String pattern){
        List<Position> shaded = shade(matrix, path, sequence, pattern);
        int score;

        List<Integer> coveredCol = Arrays.asList(path).subList(1, path.length).stream().
                map(Position::getCol).distinct().collect(Collectors.toList());

        coveredColumns.addAll(coveredCol);

        for(var column: coveredCol)
            for(int i = 0; i <= pattern.length(); i++)
                matrix[i][column] = 0;

        /*
        for(int k = 0; k < path.length; k++){
            int row = path[k].getRow();
            int col = path[k].getCol();
            matrix[row][col] = 0;
        }
        */

        /*
        List<Integer> coveredColumns = new ArrayList<>(shaded.stream().map(position -> position.getCol()).collect(Collectors.toList()));
        for(var col: coveredColumns)
            for(int i = 1; i < pattern.length(); i++){
                matrix[i][col] = 0;
                shaded.add(new Position(i, col));
            }
        */
        for(var pos: shaded){
            int i = pos.getRow();
            int j = pos.getCol();

            if(matrix[i][j] != 0){
                if (sequence.charAt(j - 1) == (pattern.charAt(i - 1)))
                    score = MATCHSCORE;
                else
                    score = MISMATCHSCORE;

                matrix[i][j] = max(matrix[i - 1][j - 1] + score, matrix[i - 1][j] + GAPSCORE, matrix[i][j - 1] + GAPSCORE, 0);
            }
        }

        return matrix;
    }

    public static List<Position> shade(int[][] matrix, Position[] path, String sequence, String pattern){
        List<Position> shaded = new ArrayList<>(List.of(path));
        //List<Integer> coveredCols = shaded.stream().map(Position::getCol).distinct().collect(Collectors.toList());

        for(int i = 1; i <= pattern.length(); i++)
            for(int j = 1; j <= sequence.length(); j++){
                Position tempPos = new Position(i, j);
                if(coveredColumns.contains(tempPos.getCol()) || isShaded(matrix, tempPos, sequence, pattern, shaded));
                    shaded.add(tempPos);
            }
            return shaded;
    }

    public static List<Position> comingFrom(int[][] matrix, Position pos, String sequence, String pattern){
        List<Position> positions = new ArrayList<>();
        int score;
        int i = pos.getRow();
        int j = pos.getCol();

        if (sequence.charAt(j - 1) == (pattern.charAt(i - 1)))
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

    public static boolean isShaded(int[][] matrix, Position pos, String sequence, String pattern, List<Position> shaded){
        var positions = new ArrayList<>(comingFrom(matrix, pos, sequence, pattern));
        if(positions.size() == 0)
            return false;
        for(var position: positions){
            if(!shaded.contains(position))
                return false;
        }
        return true;
    }

    /******************** Alternative approach ********************/

    public static void discoverRepeats(String str, int minSize, int mismatches) {
        int offset = minSize;

        System.out.println("Original string - " + str);
        System.out.println("\nRepeats discovered: \n");

        while(offset <= str.length() - minSize){
            //getRepeatsWithRemovals(str, minSize, offset, mismatches);
            getRepeatsMismatches(str, minSize, offset, mismatches);
            offset++;
        }
    }

    public static void getRepeatsMismatches(String str, int minSize, int offset, int mismatches){
        int mis = 0;
        String pattern1 = "";
        String pattern2 = "";

        int start1 = 0;
        int end1 = 0;
        int start2 = offset;
        int end2 = 0;

        for (int i = 0; i < str.length() - offset; i++) {
            if (str.charAt(i) != str.charAt(i + offset)) {
                mis++;
                if (mis <= mismatches) {
                    pattern1 += str.charAt(i);
                    pattern2 += str.charAt(i + offset);
                } else {
                    if (pattern1.length() <= offset && pattern1.length() >= minSize){
                        end1 = i - 1;
                        end2 = i + offset - 1;
                        System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                    }

                    pattern1 = "";
                    pattern2 = "";
                    mis = 0;
                }
            }
            else {
                if(pattern1.length() == 0){
                    start1 = i;
                    start2 = i + offset;
                }
                if (pattern1.length() < offset) {
                    pattern1 += str.charAt(i);
                    pattern2 += str.charAt(i + offset);
                } else {
                    if (pattern1.length() >= minSize) {
                        end1 = i - 1;
                        end2 = i + offset - 1;
                        System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }
            }
        }
        if(pattern1.length() >= minSize && pattern1.length() <= offset){
            end1 = str.length() - offset - 1;
            end2 = str.length() - 1;
            start1 = end1 - pattern1.length() + 1;
            start2 = end2 - pattern2.length() + 1;
            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
        }
    }

    /* Consider removals */

    public static void getRepeatsWithRemovals(String str, int minSize, int offset, int mismatches){
        int mis = 0;
        String pattern1 = "";
        String pattern2 = "";

        int start1 = 0;
        int end1 = 0;
        int start2 = offset;
        int end2 = 0;

        System.out.println("Offset - " + offset);

        int j = 0;

        for (int i = 0; i < str.length() - offset - j; i++) {
            if (str.charAt(i) != str.charAt(i + offset + j)) {
                int k = i + offset + j + 1;
                if(k < str.length() && str.charAt(i) == str.charAt(k)){
                    mis++;
                    if(mis <= mismatches){
                        pattern1 += "-" + str.charAt(i);
                        pattern2 += str.charAt(i + offset + j);
                        pattern2 += str.charAt(i + offset + j + 1);
                        j++;
                    }
                    else {
                        if (pattern1.length() - j <= offset && pattern1.length() >= minSize) {
                            end1 = i - 1;
                            end2 = i + offset + j - 1;
                            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        }

                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }

                else{
                    mis++;
                    if (mis <= mismatches) {
                        pattern1 += str.charAt(i);
                        pattern2 += str.charAt(i + offset + j);
                    } else {
                        if (pattern1.length() - j <= offset && pattern1.length() >= minSize){
                            end1 = i - 1;
                            end2 = i + offset + j - 1;
                            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        }

                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }
            } else {
                if(pattern1.length() == 0){
                    start1 = i;
                    start2 = i + j + offset;
                }
                if (pattern1.length() - j < offset) {
                    pattern1 += str.charAt(i);
                    pattern2 += str.charAt(i + offset + j);
                } else {
                    if (pattern1.length() - j >= minSize) {
                        end1 = i - 1;
                        end2 = i + offset - 1 - j;
                        System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }
            }
        }
        if(pattern1.length() >= minSize && pattern1.length() - j <= offset){
            end1 = str.length() - offset - j - 1;
            end2 = str.length() - 1;
            start1 = end1 - pattern1.length() + 1 + j;
            start2 = end2 - pattern2.length() + 1;
            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
        }
    }

    /* Consider insertions */

    public static void getRepeats(String str, int minSize, int offset, int mismatches){
        int mis = 0;
        String pattern1 = "";
        String pattern2 = "";

        int start1 = 0;
        int end1 = 0;
        int start2 = offset;
        int end2 = 0;

        System.out.println("Offset - " + offset);

        int i = 0;
        int r = 0;

        for (int j = 0; j < str.length() - offset - i + r; j++) {
            if (str.charAt(j) != str.charAt(j + offset + i)) {
                int k = j + offset + i + 1;

                /* insertion */

                if(k < str.length() && str.charAt(j) == str.charAt(k)){
                    mis++;
                    if(mis <= mismatches){
                        pattern1 += "-" + str.charAt(k);
                        pattern2 += str.charAt(j + offset + i);
                        pattern2 += str.charAt(j + offset + i + 1);
                        i++;
                    }
                    else {
                        if (pattern1.length() - i <= offset && pattern1.length() >= minSize) {
                            end1 = j - 1;
                            end2 = j + offset + i - 1;
                            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        }

                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }

                /* removal */

                //else if(j != 0 && str.charAt(j+1) == str.charAt(j+offset)){
                else if(str.charAt(j+1) == str.charAt(j+offset)){
                    mis++;
                    if(mis <= mismatches){
                        pattern1 += str.charAt(j);
                        pattern1 += str.charAt(j+1);
                        pattern2 += "-" + str.charAt(j + offset);
                        r++;
                    }
                    else {
                        if (pattern1.length() - i <= offset && pattern1.length() >= minSize) {
                            end1 = j - 1;
                            end2 = j + offset + i - 1;
                            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        }

                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }

                else{
                    mis++;
                    if (mis <= mismatches) {
                        pattern1 += str.charAt(j);
                        pattern2 += str.charAt(j + offset + i);
                    } else {
                        if (pattern1.length() - i <= offset && pattern1.length() >= minSize){
                            end1 = j - 1;
                            end2 = j + offset + i - 1;
                            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        }

                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }
            } else {
                if(pattern1.length() == 0){
                    start1 = j;
                    start2 = j + i + offset;
                }
                if (pattern1.length() - i < offset) {
                    pattern1 += str.charAt(j);
                    pattern2 += str.charAt(j + offset + i);
                } else {
                    if (pattern1.length() - i >= minSize) {
                        end1 = j - 1;
                        end2 = j + offset - 1 - i;
                        System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                                " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
                        pattern1 = "";
                        pattern2 = "";
                        mis = 0;
                    }
                }
            }
        }
        if(pattern1.length() >= minSize && pattern1.length() - i <= offset){
            end1 = str.length() - offset - i - 1;
            end2 = str.length() - 1;
            start1 = end1 - pattern1.length() + 1;
            start2 = end2 - pattern2.length() + 1 + i;
            System.out.println(pattern1 + "(start = " + start1 + ", end = " + end1 + ")" +
                    " - " + pattern2 + " (start = " + start2 + ", end = " + end2 + ")");
        }
    }
}