package utils;

import refactoring.MoveMethodRefactoring;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiClass;
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

    //private static final Logger LOGGER = Logging.getLogger(ExportResultsUtil.class);


    /**
     * Method exports results of refactoring to the file (Now support only MoveMethodRefactoring)
     * @param refactorings List with offerings to refactoring
     * @param directory of the file
     */
    public static void exportToFile(List<MoveMethodRefactoring> refactorings, String directory) {
        exportToFile(refactorings, ExportResultsUtil::defaultRefactoringView, directory);
    }

    /**
     * Method exports results of refactoring to the file (Now support only MoveMethodRefactoring)
     * @param refactorings List with offerings to refactoring
     * @param show Function returns readable view for refactoring method
     * @param directory of the file
     */
    private static void exportToFile(List<MoveMethodRefactoring> refactorings, Function<MoveMethodRefactoring, String> show, String directory) {
        try {
            StringBuilder results = new StringBuilder();
            for (MoveMethodRefactoring refactoring: refactorings) {
                results.append(show.apply(refactoring)).append(System.lineSeparator());
            }
            Path path = Paths.get(directory + File.separator + refactorings.get(1).getClass().getSimpleName());
            Files.deleteIfExists(path);
            createFile(path);
            write(path, results.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param r Refactoring method
     * @return readable view for refactoring method
     */
    private static String defaultRefactoringView(MoveMethodRefactoring r) {
        Optional<PsiMethod> member = r.getOptionalMethod();
        Optional<PsiClass> target = r.getOptionalTargetClass();
        return String.format("%s --> %s",
                member.isPresent() ?
                        getHumanReadableName(member.get()) :
                        IntelliJDeodorantBundle.message("java.member.is.not.valid"),
                target.isPresent() ?
                        getHumanReadableName(target.get()) :
                        IntelliJDeodorantBundle.message("target.class.is.not.valid"));
    }
}
