import com.opencsv.CSVWriter;
import data.Event;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class preprocessor {
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
        String cellRegex = "(.*\"copyCell\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\"),.*\\n)" +
                "((.*\\n)*)" +
                "((.*)\"editCell\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){7}\\4.*)\\n*)";

        String rangeRegex = "(.*\"copyRange\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",){7}(\"([^\"]|\"\")*\",).*\\n)" +
                "((.*\\n)*)" +
                "((.*)\"editRange\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){7}\\8.*)\\n*)";

        String chromeRegex = "(.*\"Chrome\",\"copy\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",).*\\n)" +
                "((.*\\n)*)" +
                "((.*)\"editCell\",(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){7}\\4.*\\n*))";


        if (Pattern.compile(cellRegex).matcher(log).find()) {
            log = log.replaceAll(cellRegex, "$1$6$9\"pasteIntoCell\",$10$4,$14\n");
            return identifyPasteAction(log);
        }

        if (Pattern.compile(rangeRegex).matcher(log).find()) {
            log = log.replaceAll(rangeRegex, "$1$10$13\"pasteIntoRange\",$14$4$18\n");
            return identifyPasteAction(log);
        }

        if (Pattern.compile(chromeRegex).matcher(log).find()) {
            log = log.replaceAll(chromeRegex, "$1$6$9\"pasteIntoCell\",$10$4$14\n");
            return identifyPasteAction(log);
        }

        return log;
    }

    private static String mergeNavigationCellCopy(String log) {
        String getCellRegex = "((\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){2})\"getCell\",(\"([^\"]|\"\")*\",){2}(.*)\\n" +
                "(((?!(\"([^\"]|\"\")*\",){3}(\"editCell\"|\"getRange\"|\"getCell\"),(\"([^\"]|\"\")*\",){9}).)*\\n)*)" +
                "(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)\"OS-Clipboard\",\"copy\",((\"([^\"]|\"\")*\",){2}).*\\n*";

        String getRangeRegex = "((\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){2})\"getRange\",(\"([^\"]|\"\")*\",){2}(.*)\\n" +
                "(((?!(\"([^\"]|\"\")*\",){3}(\"editCell\"|\"getRange\"|\"getCell\"),(\"([^\"]|\"\")*\",){9}).)*\\n)*)" +
                "(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)\"OS-Clipboard\",\"copy\",((((?!,).)*,){2}).*\\n*";

        String editCellRegex = "((\"([^\"]|\"\")*\",)((\"([^\"]|\"\")*\",){2})\"editCell\",(\"([^\"]|\"\")*\",){2}(.*)\\n" +
                "(((?!(\"([^\"]|\"\")*\",){3}(\"editCell\"|\"getRange\"|\"getCell\"),(\"([^\"]|\"\")*\",){9}).)*\\n)*)" +
                "(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",)\"OS-Clipboard\",\"copy\",((\"([^\"]|\"\")*\",){2}).*\\n*";

        if (Pattern.compile(getCellRegex).matcher(log).find()) {
            log = log.replaceAll(getCellRegex, "$1$17$4\"copyCell\",$21$9\n");
            return mergeNavigationCellCopy(log);
        }

        if (Pattern.compile(getRangeRegex).matcher(log).find()) {
            log = log.replaceAll(getRangeRegex, "$1$17$4\"copyRange\",$21$9\n");
            return mergeNavigationCellCopy(log);
        }

        if (Pattern.compile(editCellRegex).matcher(log).find()) {
            log = log.replaceAll(editCellRegex, "$1$17$4\"copyCell\",$21$9\n");
            return mergeNavigationCellCopy(log);
        }

        log = log.replaceAll("((\"([^\"]|\"\")*\",){3}\"getCell\",.*\\n*)|" +
                "((\"([^\"]|\"\")*\",){3}\"getRange\",.*\\n*)", "");

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

    static String eventListToString(List<Event> events){
        String[] header = {"caseID", "timeStamp", "userID", "targetApp", "eventType", "url", "content", "target.workbookName",
                "target.sheetName", "target.id", "target.class", "target.tagName", "target.type", "target.name",
                "target.value", "target.innerText", "target.checked", "target.href", "target.option", "target.title", "target.innerHTML"
        };
        String str = "";
        for(var event: events){
            str += "\"" + event.getTimestamp() + "\",";
            str += event.payload.containsKey("userID") ? "\"" + event.payload.get("userID") + "\"," : "\"\",";
            str += event.payload.containsKey("targetApp") ? "\"" + event.payload.get("targetApp") + "\"," : "\"\",";
            str += "\"" + event.getEventType() + "\",";

            for(int i = 5; i < header.length; i++)
                if(event.payload.containsKey(header[i]) && !event.payload.get(header[i]).equals("\"\""))
                    str += "\"" + event.payload.get(header[i]) + "\",";
                else
                    str += "\"\",";

                str = str.substring(0, str.lastIndexOf(",")) + "\n";
        }
        return str;
    }

    static List<Event> applyPreprocessing(String filePath, String events){
        System.out.println("Preprocessing...");
        String sortedEvents = sortLog(events);

        System.out.print("\tRemoving Clipboard copy actions");
        long startTime = System.currentTimeMillis();
        sortedEvents = deleteChromeClipboardCopy(sortedEvents);
        long stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

        System.out.print("\tIdentifying Excel copy actions");
        startTime = System.currentTimeMillis();
        sortedEvents = mergeNavigationCellCopy(sortedEvents);
        stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

        System.out.print("\tIdentifying Excel paste actions");
        startTime = System.currentTimeMillis();
        sortedEvents = identifyPasteAction(sortedEvents);
        stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

        System.out.print("\tRemoving click text field actions");
        startTime = System.currentTimeMillis();
        while(containsRedundantClickTextField(sortedEvents))
            sortedEvents = removeRedundantClickTextField(sortedEvents);
        stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");


        System.out.print("\tRemoving redundant copy actions");
        startTime = System.currentTimeMillis();
        while(containsSingleCopy(sortedEvents))
            sortedEvents = removeSingleCopy(sortedEvents);
        while(containsRedundantCopy(sortedEvents))
            sortedEvents = removeRedundantCopy(sortedEvents);
        stopTime = System.currentTimeMillis();
        System.out.println(" (" + (stopTime - startTime) / 1000.0 + " sec)");

        String preprocessedLog = filePath.substring(0, filePath.lastIndexOf(".")) + "_preprocessed.csv";
        writeDataLineByLine(preprocessedLog, sortedEvents);
        return logReader.readCSV(preprocessedLog);
    }

    private static void writeDataLineByLine(String filePath, String data) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filePath),
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.NO_ESCAPE_CHARACTER,
                    CSVWriter.RFC4180_LINE_END);

            String[] headers = {"\"timeStamp\"", "\"userID\"", "\"targetApp\"", "\"eventType\"", "\"url\"",
                    "\"content\"", "\"target.workbookName\"", "\"target.sheetName\"", "\"target.id\"", "\"target.class\"",
                    "\"target.tagName\"", "\"target.type\"", "\"target.name\"", "\"target.value\"", "\"target.innerText\"",
                    "\"target.checked\"", "\"target.href\"", "\"target.option\"", "\"target.title\"", "\"target.innerHTML\""
            };

            writer.writeNext(headers);
            writeActionsValues(writer, data);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeActionsValues(CSVWriter writer, String data){
        String[] actions = data.split("\n");

        for (String action : actions) {
            String[] actionValues = action.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            actionValues = Arrays.stream(actionValues)
                    .map(e -> e.replaceAll("\"{2}(([^\"]|\"\")*)\"{2}", "\"\"\"$1\"\"\""))
                    .toArray(String[]::new);
            writer.writeNext(actionValues);
        }
    }

    /* Read actions filter*/

    private static String redundantFirstCopyRegex = "((\"([^\"]|\"\")*\",){3}\"copy.*\\n)" +
            "((((?!(\"([^\"]|\"\")*\",){3}\"paste).)*\",.*\\n)*" +
            "(\"([^\"]|\"\")*\",){3}\"copy.*\\n*)";

    /*
    private static String singleCopyRegex = "((.*\\n)*)" +
            "((\"([^\"]|\"\")*\",){3}(\"copy[a-zA-Z]*\",)(\"([^\"]|\"\")*\",)(\"([^\"]|\"\")*\",).*\\n*)" +
            "(((\"([^\"]|\"\")*\",){3}(?!((\"paste[a-zA-Z]*\",(\"([^\"]|\"\")*\",)\\9)|\"copy[a-zA-Z]*\")).*\\n*)*)";
            */

    private static String singleCopyRegex = "((.*\\n)*)" +
            "((\"([^\"]|\"\")*\",){3}(\"copy).*\\n*)" +
            "(((\"([^\"]|\"\")*\",){3}((?!\"paste|\"copy).)*\",.*\\n*)*)";


    public static boolean containsRedundantCopy(String log) {
        Pattern p = Pattern.compile(redundantFirstCopyRegex);
        Matcher matcher = p.matcher(log);

        return matcher.find();
    }

    public static boolean containsSingleCopy(String log) {
        Pattern p = Pattern.compile(singleCopyRegex);
        Matcher matcher = p.matcher(log);

        return matcher.matches();
    }

    public static String removeRedundantCopy(String log) {
        if (containsRedundantCopy(log)) {
            log = log.replaceAll(redundantFirstCopyRegex, "$4");
            return removeRedundantCopy(log);
        }

        return log;
    }

    public static String removeSingleCopy(String log) {
        if(containsSingleCopy(log)){
            log = log.replaceAll(singleCopyRegex, "$1$7");
            // log = log.replaceAll(singleCopyRegex, "$1$11");
            return removeSingleCopy(log);
        }

        return log;
    }

    /* Navigation actions filter */

    private static String redundantClickTextFieldRegex = "((\"([^\"]|\"\")*\",){3}\"clickTextField\",.*\\n*)";

    public static boolean containsRedundantClickTextField(String log) {
        Pattern pattern = Pattern.compile(redundantClickTextFieldRegex);
        Matcher matcher = pattern.matcher(log);

        return matcher.find();
    }

    public static String removeRedundantClickTextField(String log) {
        if (containsRedundantClickTextField(log)) {
            log = log.replaceAll(redundantClickTextFieldRegex, "");
            return removeRedundantClickTextField(log);
        }

        return log;
    }
}
