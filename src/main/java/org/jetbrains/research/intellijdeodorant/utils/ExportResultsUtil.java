package org.jetbrains.research.intellijdeodorant.utils;

import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.Refactoring;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.List;

/**
 * Exports refactoring suggestions to the file.
 */
public class ExportResultsUtil {

    /**
     * @param refactorings list of refactoring suggestions.
     * @param panel        panel of current project.
     */
    public static void export(List<? extends Refactoring> refactorings, JPanel panel) {
        if (refactorings.isEmpty()) return;
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel);
        FileDialog fileDialog = new FileDialog(frame, IntelliJDeodorantBundle.message("export"), FileDialog.SAVE);
        fileDialog.setFile(refactorings.get(0).getExportDefaultFilename() + ".csv");
        fileDialog.setFilenameFilter((dir, name) -> name.endsWith(".csv"));
        fileDialog.setVisible(true);
        if (fileDialog.getDirectory() == null || fileDialog.getFile() == null) {
            return;
        }
        exportToFile(refactorings, fileDialog.getFiles()[0].toURI());
    }

    /**
     * @param refactorings list of refactoring suggestions.
     * @param pathUri      uri that representing the path to the file where the results will be written.
     */
    private static void exportToFile(List<? extends Refactoring> refactorings, URI pathUri) {
        try (PrintWriter writer = new PrintWriter(new File(pathUri))) {
            for (Refactoring refactoring : refactorings) {
                writer.write(refactoring.getDescription() + '\n');
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
