package org.jetbrains.research.intellijdeodorant.utils;

import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.Refactoring;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static java.nio.file.Files.createFile;
import static java.nio.file.Files.write;

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
        fileDialog.setFile(refactorings.get(0).getExportDefaultFilename() + ".txt");
        fileDialog.setFilenameFilter((dir, name) -> name.endsWith(".txt"));
        fileDialog.setVisible(true);
        if (fileDialog.getDirectory() == null || fileDialog.getFile() == null) {
            return;
        }
        exportToFile(refactorings, fileDialog.getFiles()[0].toPath());
    }

    /**
     * @param refactorings list of refactoring suggestions.
     * @param path         path to the file where the results will be written.
     */
    private static void exportToFile(List<? extends Refactoring> refactorings, Path path) {
        try {
            StringBuilder results = new StringBuilder();
            for (Refactoring refactoring : refactorings) {
                results.append(refactoring.getDescription()).append(System.lineSeparator());
            }
            Files.deleteIfExists(path);
            createFile(path);
            write(path, results.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
