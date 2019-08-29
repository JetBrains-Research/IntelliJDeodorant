package navigation;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import utils.PsiUtils;

import javax.swing.*;

public class Reference {
    private PsiMethod psiElement;
    private String targetClassName;

    public Reference(PsiMethod reference, String targetClassName) {
        this.psiElement = reference;
        this.targetClassName = targetClassName;
    }

    Navigatable location() {
        return new OpenFileDescriptor(psiElement.getProject(), getVirtualFile(), psiElement.getTextOffset());
    }

    VirtualFile getVirtualFile() {
        return containingFile().getVirtualFile();
    }

    private PsiMethod getPsiMethod() {
        return psiElement;
    }

    private PsiClass getParentClass() {
        PsiElement parent;
        PsiElement current = psiElement;
        while (true) {
            parent = current.getParent();
            if (parent instanceof PsiFile) return null;
            if (parent instanceof PsiClass) return (PsiClass) parent;
            current = parent;
        }
    }

    private PsiFile containingFile() {
        return psiElement.getContainingFile();
    }

    private String containingPackage() {
        String fullPackageName = "default";
        try {
            PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiElement.getContainingFile().getContainingDirectory());
            if (!psiPackage.getQualifiedName().trim().equals(""))
                fullPackageName = psiPackage.getQualifiedName();
        } catch (NullPointerException e) {
            fullPackageName = "default";
        }
        return fullPackageName;
    }

    public Icon icon() {
        if (getPsiMethod() != null)
            return getPsiMethod().getIcon(0);
        if (getParentClass() != null)
            return getParentClass().getIcon(0);
        if (containingPackage() != null)
            return containingFile().getIcon(0);
        return psiElement.getIcon(0);
    }

    public boolean equals(Object reference) {
        return reference instanceof Reference
                && psiElement.equals(((Reference) reference).psiElement);
    }

    public String description() {
        StringBuilder description = new StringBuilder();
        PsiClass containingClass = getParentClass();
        PsiMethod containingMethod = getPsiMethod();
        PsiFile containingFile = containingFile();

        if (getPsiMethod() != null)
            description.append(PsiUtils.calculateSignature(getPsiMethod()));
        if ((containingClass != null && !"".equals(containingClass.getName())) || !"".equals(containingFile.getName()) 
                || (containingMethod != null && !containingMethod.getName().equals("")))
            description.append("    Move to    ");
        description.append(targetClassName);
        return description.toString();
    }
    
}