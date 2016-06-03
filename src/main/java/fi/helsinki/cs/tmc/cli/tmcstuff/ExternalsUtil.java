package fi.helsinki.cs.tmc.cli.tmcstuff;

import fi.helsinki.cs.tmc.cli.command.SubmitCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Utility class for using external programs.
 */
public class ExternalsUtil {

    /**
     * Create a temp file with a template and open an editor for the user.
     * Editor can be specified with the EDITOR env var.
     * The default editor is nano for Linux/Unix and notepad for Windows.
     * @param template Line(s) to add to the end of the file
     * @param filename Descriptive name for the temp file - not the full path
     * @param escapeComments True if lines beginning with # should be ignored
     * @return: User created message
     */
    public static String getUserEditedMessage(
            String template, String filename, boolean escapeComments) {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"))
                .resolve(filename);
        try {
            // Write the template to file
            PrintWriter writer = new PrintWriter(tempFile.toFile());
            writer.print(template);
            writer.close();
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(SubmitCommand.class);
            logger.error("Couldn't write to file", e);
            return null;
        }
        String editor = System.getenv("EDITOR");
        if (editor == null) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                editor = "notepad";
            } else {
                editor = "nano";
            }
        }
        List<String> messageLines;
        // User writes to file
        execExternal(editor, tempFile);
        try {
            // Read from file
            messageLines = Files.readAllLines(tempFile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(SubmitCommand.class);
            logger.error("Couldn't open file in editor", e);
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String messageLine : messageLines) {
            // remove comments and empty lines
            if (messageLine.length() > 0
                    && (messageLine.charAt(0) != '#' || escapeComments == false)) {
                sb.append(messageLine + "\n");
            }
        }
        return sb.toString();
    }

    /**
     * Show a file in a pager for the user.
     * Use less in Linux/Unix and more in Windows.
     * @param file File to be shown to the user (eg. a log file)
     */
    public static void showFileInPager(Path file) {
        String pager = System.getenv("PAGER");
        if (pager == null) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                pager = "more";
            } else {
                pager = "less";
            }
        }
        execExternal(pager, file);
    }

    private static void execExternal(String program, Path file) {
        String[] exec;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            exec = new String[]{program + " \'" + file.toString() + "\'"};
        } else {
            exec = new String[]{
                    "sh", "-c", program + " \'" + file.toString() + "\'"
                    + " </dev/tty >/dev/tty"};
        }
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(exec);
            proc.waitFor();
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(SubmitCommand.class);
            logger.error("Exception when running external program", e);
        }
    }
}
