import data.Event;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Preprocessor {
    private static String sortLog(String log) {
        System.out.print("\tSorting the log");
        long startTime = System.currentTimeMillis();
        List<String> actions = Arrays.asList(log.split("\n"));
        Collections.sort(actions);
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");
        return actions.stream().map(el -> el + "\n").collect(Collectors.joining());
    }

    private static String identifyPasteAction(String log) {
        log = transformCopyCellEditToPaste(log);
        log = transformCopyRangeEditToPaste(log);
        log = transformChromeCopyEditToPaste(log);

        return log;
    }

    private static String transformCopyCellEditToPaste(String log) {
        String cellRegex = "(.*\"copyCell\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\"),.*\\n)" +
                "((.*\\n)*)" +
                "((.*)\"editCell\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){7}\\4.*)\\n*)";

        if (log.contains("editCell") && Pattern.compile(cellRegex).matcher(log).find()) {
            log = log.replaceAll(cellRegex, "$1$6$9\"pasteIntoCell\",$10$4,$14\n");

            return transformCopyCellEditToPaste(log);
        }

        return log;
    }

    private static String transformCopyRangeEditToPaste(String log) {
        String rangeRegex = "(.*\"copyRange\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",){7}(\"([^\"]|\"\")*\",).*\\n)" +
                "((.*\\n)*)" +
                "((.*)\"editRange\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){7}\\8.*)\\n*)";

        if (log.contains("editRange") && Pattern.compile(rangeRegex).matcher(log).find()) {
            log = log.replaceAll(rangeRegex, "$1$10$13\"pasteIntoRange\",$14$4$18\n");

            return transformCopyRangeEditToPaste(log);
        }

        return log;
    }

    private static String transformChromeCopyEditToPaste(String log) {
        String chromeRegex = "(.*\"Chrome\",\"copy\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",).*\\n)" +
                "((.*\\n)*)" +
                "((.*)\"editCell\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){7}\\4.*\\n*))";

        if (log.contains("editCell") && Pattern.compile(chromeRegex).matcher(log).find()) {
            log = log.replaceAll(chromeRegex, "$1$6$9\"pasteIntoCell\",$10$4$14\n");

            return transformChromeCopyEditToPaste(log);
        }

        return log;
    }


    private static String mergeNavigationCellCopy(String log) {
        log = mergeGetCellCopy(log);
        log = mergeGetRangeCopy(log);
        log = mergeEditCellCopy(log);

        log = log.replaceAll("((\"([^\"]|\"\")*\",){3}\"getCell\",.*\\n*)|" +
                "((\"([^\"]|\"\")*\",){3}\"getRange\",.*\\n*)", "");

        return log;
    }

    private static String mergeGetCellCopy(String log) {
        String getCellRegex = "((\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){2})\"getCell\",(\"([^\"]|\"\")*\",){2}(.*)\\n" +
                "(((?!(\"([^\"]|\"\")*\",){3}(\"editCell\"|\"getRange\"|\"getCell\"),(\"([^\"]|\"\")*\",){9}).)*\\n)*)" +
                "(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)\"OS-Clipboard\",\"copy\",((\"([^\"]|\"\")*\",){2}).*\\n*";

        if (log.contains("getCell") && Pattern.compile(getCellRegex).matcher(log).find()) {
            log = log.replaceAll(getCellRegex, "$1$17$4\"copyCell\",$21$9\n");

            return mergeGetCellCopy(log);
        }

        return log;
    }

    private static String mergeGetRangeCopy(String log) {
        String getRangeRegex = "((\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){2})\"getRange\",(\"([^\"]|\"\")*\",){2}(.*)\\n" +
                "(((?!(\"([^\"]|\"\")*\",){3}(\"editCell\"|\"getRange\"|\"getCell\"),(\"([^\"]|\"\")*\",){9}).)*\\n)*)" +
                "(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)\"OS-Clipboard\",\"copy\",((((?!,).)*,){2}).*\\n*";

        if (log.contains("getRange") && Pattern.compile(getRangeRegex).matcher(log).find()) {
            log = log.replaceAll(getRangeRegex, "$1$17$4\"copyRange\",$21$9\n");

            return mergeGetRangeCopy(log);
        }

        return log;
    }

    private static String mergeEditCellCopy(String log) {
        String editCellRegex = "((\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){2})\"editCell\",(\"([^\"]|\"\")*\",){2}(.*)\\n" +
                "(((?!(\"([^\"]|\"\")*\",){3}(\"editCell\"|\"getRange\"|\"getCell\"),(\"([^\"]|\"\")*\",){9}).)*\\n)*)" +
                "(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)\"OS-Clipboard\",\"copy\",((\"([^\"]|\"\")*\",){2}).*\\n*";

        if (log.contains("editCell") && Pattern.compile(editCellRegex).matcher(log).find()) {
            log = log.replaceAll(editCellRegex, "$1$17$4\"copyCell\",$21$9\n");

            return mergeEditCellCopy(log);
        }

        return log;
    }

    private static String deleteChromeClipboardCopy(String log) {
        String regex = "((\"([^\"]|\"\")*\",){2}\"Chrome\",\"copy\",.*\\n)" +
                "((\"([^\"]|\"\")*\",){2}\"OS-Clipboard\",\"copy\",.*\\n*)";

        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(log);

        if (matcher.find()) {
            log = log.replaceAll(regex, "$1");
            return deleteChromeClipboardCopy(log);
        }
        return log;
    }

    /* Read actions filter*/

    private static String redundantFirstCopyRegex = "((\"([^\"]|\"\")*\",){3}\"copy.*\\n)" +
            "((((?!(\"([^\"]|\"\")*\",){3}\"paste).)*\",.*\\n)*" +
            "(\"([^\"]|\"\")*\",){3}\"copy.*\\n*)";

    private static String singleCopyRegex = "((\"([^\"]|\"\")*\",){3}(\"copy[a-zA-Z]*\",)(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",).*\\n)" +
            "(((\"([^\"]|\"\")*\",){3}(?!(\"paste[a-zA-Z]*\"|\"copy[a-zA-Z]*\")).*\\n)*" +
            "((\"([^\"]|\"\")*\",){3}(\"paste[a-zA-Z]*\",(\"([^\"]|\"\")*\",)(?!\\7)).*\\n*))";

    private static boolean containsRedundantCopy(String log) {
        Pattern p = Pattern.compile(redundantFirstCopyRegex);
        Matcher matcher = p.matcher(log);

        return matcher.find();
    }

    private static boolean containsSingleCopy(String log) {
        Pattern p = Pattern.compile(singleCopyRegex);
        Matcher matcher = p.matcher(log);

        return matcher.find();
    }

    private static String removeRedundantCopy(String log) {
        if (containsRedundantCopy(log)) {
            log = log.replaceAll(redundantFirstCopyRegex, "$4");
            return removeRedundantCopy(log);
        }

        return log;
    }

    private static String removeSingleCopy(String log) {
        if (containsSingleCopy(log)) {
            log = log.replaceAll(singleCopyRegex, "$9");
            return removeSingleCopy(log);
        }

        return log;
    }

    /* Navigation actions filter */

    private static String redundantClickTextFieldRegex = "((\"([^\"]|\"\")*\",){3}\"clickTextField\",.*\\n*)";

    private static boolean containsRedundantClickTextField(String log) {
        Pattern pattern = Pattern.compile(redundantClickTextFieldRegex);
        Matcher matcher = pattern.matcher(log);

        return matcher.find();
    }

    private static String removeRedundantClickTextField(String log) {
        if (containsRedundantClickTextField(log)) {
            log = log.replaceAll(redundantClickTextFieldRegex, "");
            return removeRedundantClickTextField(log);
        }

        return log;
    }

    /* Preprocessing */

    static List<Event> applyPreprocessing(String filePath, String events, Boolean applyPreprocessing){
        System.out.println("Preprocessing...");
        events = sortLog(events);

        if(applyPreprocessing){
            System.out.print("\tRemoving Clipboard copy actions");
            long startTime = System.currentTimeMillis();
            events = deleteChromeClipboardCopy(events);
            long stopTime = System.currentTimeMillis();
            System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

            System.out.print("\tIdentifying Excel copy actions");
            startTime = System.currentTimeMillis();
            events = mergeNavigationCellCopy(events);
            stopTime = System.currentTimeMillis();
            System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

            System.out.print("\tIdentifying Excel paste actions");
            startTime = System.currentTimeMillis();
            events = identifyPasteAction(events);
            stopTime = System.currentTimeMillis();
            System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

            System.out.print("\tRemoving click text field actions");
            startTime = System.currentTimeMillis();
            while(containsRedundantClickTextField(events))
                events= removeRedundantClickTextField(events);
            stopTime = System.currentTimeMillis();
            System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

            System.out.print("\tRemoving redundant copy actions");
            startTime = System.currentTimeMillis();
            while(containsSingleCopy(events))
                events = removeSingleCopy(events);
            while(containsRedundantCopy(events))
                events = removeRedundantCopy(events);
            stopTime = System.currentTimeMillis();
            System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");
        }

        String preprocessedLog = filePath.substring(0, filePath.lastIndexOf(".")) + "_preprocessed.csv";
        Utils.writeDataLineByLine(preprocessedLog, events);
        return LogReader.readCSV(preprocessedLog);
    }
}