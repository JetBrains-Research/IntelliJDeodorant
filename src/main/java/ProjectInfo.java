import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import utils.PsiUtils;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectInfo {

    private final List<PsiJavaFile> psiFiles;
    private final List<PsiClass> psiClasses;
    private final List<PsiMethod> psiMethods;

    public ProjectInfo(Project project) {
        this.psiFiles = PsiUtils.extractFiles(project);
        this.psiClasses = psiFiles.stream()
                .flatMap(x -> PsiUtils.extractClasses(x).stream())
                .collect(Collectors.toList());

        this.psiMethods = psiClasses.stream()
                .flatMap(x -> PsiUtils.extractMethods(x).stream())
                .collect(Collectors.toList());
    }


    public List<PsiClass> getClasses() {
        return psiClasses;
    }

    public List<PsiMethod> getMethods() {
        return psiMethods;
    }

    public List<PsiJavaFile> getPsiFiles() {
        return psiFiles;
    }

}
