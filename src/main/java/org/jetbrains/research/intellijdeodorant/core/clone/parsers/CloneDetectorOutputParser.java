package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.utils.PsiUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class CloneDetectorOutputParser {

    private final String toolOutputFilePath;
    private final Project iJavaProject;
    private List<CloneDetectorOutputParserProgressObserver> cloneDetectorOutputParserProgressObservers =
            new ArrayList<>();
    private boolean operationCanceled;
    private List<Throwable> exceptions = new ArrayList<>();
    private int cloneGroupCount;

    public CloneDetectorOutputParser(Project iJavaProject, String cloneOutputFilePath) {
        this.toolOutputFilePath = cloneOutputFilePath;
        this.iJavaProject = iJavaProject;
    }

    public String getToolOutputFilePath() {
        return toolOutputFilePath;
    }

    public Project getIJavaProject() {
        return this.iJavaProject;
    }

    public int getCloneGroupCount() {
        return this.cloneGroupCount;
    }

    protected void setCloneGroupCount(int cloneGroupCount) {
        this.cloneGroupCount = cloneGroupCount;
    }

    public abstract CloneGroupList readInputFile() throws InvalidInputFileException;

    public String readFileContents(String filePath) {
        try {
            StringBuffer fileData;
            char[] buffer;
            int numRead = 0;
            String readData;

            BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));

            fileData = new StringBuffer(1000);
            buffer = new char[1024];

            while ((numRead = bufferedReader.read(buffer)) != -1) {
                readData = String.valueOf(buffer, 0, numRead);
                fileData.append(readData);
                buffer = new char[1024];
            }

            bufferedReader.close();

            return fileData.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String getResultsFileContents() {
        return readFileContents(getToolOutputFilePath());
    }

    protected void progress(int cloneGroupIndex) {
        for (CloneDetectorOutputParserProgressObserver observer : cloneDetectorOutputParserProgressObservers)
            observer.notify(cloneGroupIndex);
    }

    public void addParserProgressObserver(CloneDetectorOutputParserProgressObserver observer) {
        cloneDetectorOutputParserProgressObservers.add(observer);
    }

    public static PsiMethod getIMethod(PsiFile iCompilationUnit, PsiFile cunit, int begin, int length) {

        PsiMethod iMethod = null;
        PsiElement method = iCompilationUnit.findElementAt(begin);
        if (method instanceof PsiMethod) {
            iMethod = (PsiMethod) method;
        }
        return iMethod;

    }

    public void cancelOperation() {
        this.operationCanceled = true;
    }

    public boolean isOperationCanceled() {
        return operationCanceled;
    }

    protected void addExceptionHappenedDuringParsing(Throwable ex) {
        exceptions.add(new CloneDetectorOutputParseException(ex));
    }

    public List<Throwable> getWarningExceptions() {
        return new ArrayList<>(this.exceptions);
    }

    protected CloneInstance getCloneInstance(String filePath, int cloneInstanceID, boolean isAbsoluteFilePath,
                                             int startLine, int endLine, int endColumn)
            throws ResourceInfo.ICompilationUnitNotFoundException {
        ResourceInfo resourceInfo = ResourceInfo.getResourceInfo(this.getIJavaProject(), filePath, isAbsoluteFilePath);
        CloneInstanceLocationInfo locationInfo = new CloneInstanceLocationInfo(resourceInfo.getFullPath(), startLine, 0, endLine, endColumn);
        return getCloneInstance(cloneInstanceID, resourceInfo, locationInfo);
    }

    protected CloneInstance getCloneInstance(String filePath, int cloneInstanceIndex,
                                             int startOffset, int endOffset)
            throws ResourceInfo.ICompilationUnitNotFoundException {
        ResourceInfo resourceInfo = ResourceInfo.getResourceInfo(this.getIJavaProject(), filePath, true);
        CloneInstanceLocationInfo locationInfo = new CloneInstanceLocationInfo(resourceInfo.getFullPath(), startOffset, endOffset);
        return getCloneInstance(cloneInstanceIndex, resourceInfo, locationInfo);
    }

    private CloneInstance getCloneInstance(int cloneInstanceIndex, ResourceInfo resourceInfo, CloneInstanceLocationInfo locationInfo) {
        CloneInstance cloneInstance = new CloneInstance(locationInfo, cloneInstanceIndex);
        cloneInstance.setSourceFolder(resourceInfo.getSourceFolder());
        cloneInstance.setPackageName(resourceInfo.getPackageName());
        cloneInstance.setClassName(resourceInfo.getClassName());
        PsiMethod iMethod = getIMethod(resourceInfo.getICompilationUnit(), resourceInfo.getCompilationUnit(),
                locationInfo.getStartOffset(), locationInfo.getLength());
        if (iMethod != null) {
            cloneInstance.setMethodName(iMethod.getName());
            try {
                cloneInstance.setIMethodSignature(PsiUtils.calculateSignature(iMethod));
            } catch (Exception e) {
                e.printStackTrace();
            }
            cloneInstance.setMethodSignature(PsiUtils.calculateSignature(iMethod));
            PsiElement parent = iMethod.getParent();
            if (parent instanceof PsiType) {
                cloneInstance.setContainingClassFullyQualifiedName(((PsiClass) parent).getQualifiedName());
            }
        }
        return cloneInstance;
    }

    protected static String formatPath(String cloneDROutputFilePath) {
        cloneDROutputFilePath = cloneDROutputFilePath.replace("\\", "/");
        if (!cloneDROutputFilePath.endsWith("/"))
            cloneDROutputFilePath += "/";
        return cloneDROutputFilePath;
    }

}
