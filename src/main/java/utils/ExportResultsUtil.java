package utils;

import refactoring.MoveMethodRefactoring;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiClass;
import refactoring.Refactoring;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Function;
import java.util.Optional;

import static java.nio.file.Files.createFile;
import static java.nio.file.Files.write;
import static utils.PsiUtils.getHumanReadableName;

/**
 * Class exports (MoveMethod) refactoring results to the file
 */
public class ExportResultsUtil {

    /**
     * Exports refactoring results to the file.
     */
    public static void export(List<? extends Refactoring> refactorings) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.CANCEL_OPTION) {
            return;
        }
        try {
            ExportResultsUtil.exportToFile(refactorings, fileChooser.getSelectedFile().getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param refactorings List with offerings to refactoring
     * @param directory of the file
     */
    private static void exportToFile(List<? extends Refactoring> refactorings, String directory) {
        try {
            StringBuilder results = new StringBuilder();
            for (Refactoring refactoring: refactorings) {
                results.append(refactoring.getDescription()).append(System.lineSeparator());
            }
            Path path = Paths.get(directory + File.separator + refactorings.get(0).getClass().getSimpleName());
            Files.deleteIfExists(path);
            createFile(path);
            write(path, results.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
