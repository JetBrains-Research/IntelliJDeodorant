package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloneDROutputParser extends CloneDetectorOutputParser {

    private final Set<Integer> allCloneGroupIDs;

    public CloneDROutputParser(Project iJavaProject, String cloneDROutputFilePath) throws InvalidInputFileException {
        super(iJavaProject, formatPath(cloneDROutputFilePath));
        this.allCloneGroupIDs = getAllCloneGroups(this.getToolOutputFilePath());
        if (this.allCloneGroupIDs.size() == 0)
            throw new InvalidInputFileException();
        this.setCloneGroupCount(this.allCloneGroupIDs.size());
    }

    @Override
    public CloneGroupList readInputFile() throws InvalidInputFileException {

        CloneGroupList cloneGroups = new CloneGroupList(getIJavaProject());

        String commonPathPrefix = getCommonPathPrefix(getToolOutputFilePath());

        for (Integer cloneGroupID : this.allCloneGroupIDs) {

            if (isOperationCanceled())
                break;

            String filePath = this.getToolOutputFilePath() + "xCloneSet" + cloneGroupID + ".html";

            // There will be one clone group for each file
            CloneGroup cloneGroup = new CloneGroup(cloneGroupID);

            String fileContents = readFileContents(filePath);
            Pattern pattern = Pattern.compile("<a id=\"CloneInstance\\d+\">.*<br/>(\\d+)</a><td>Line Count<br/>(\\d+)</td><td>Source Line<br/>(\\d+).*Source File</div><pre>(.*)</pre>");
            Matcher cloneMatcher = pattern.matcher(fileContents);
            int cloneCount = 0;
            while (cloneMatcher.find()) {
                try {
                    int cloneLineCount = Integer.parseInt(cloneMatcher.group(2));
                    int startLine = Integer.parseInt(cloneMatcher.group(3));
                    int endLine = startLine + cloneLineCount - 1;
                    String cloneFilePath = cloneMatcher.group(4);
                    if (!"".equals(commonPathPrefix)) {
                        cloneFilePath = commonPathPrefix + "/" + cloneFilePath;
                    }
                    cloneCount++;
                    CloneInstance cloneInstance = getCloneInstance(cloneFilePath, cloneCount, true, startLine, endLine, 0);
                    cloneGroup.addClone(cloneInstance);
                } catch (NullPointerException | StringIndexOutOfBoundsException | NumberFormatException | ResourceInfo.ICompilationUnitNotFoundException e) {
                    addExceptionHappenedDuringParsing(e);
                }
            }
            if (cloneGroup.getCloneGroupSize() > 1)
                cloneGroups.add(cloneGroup);
            progress(cloneGroupID);
        }

        if (cloneGroups.getCloneGroupsCount() == 0)
            throw new InvalidInputFileException();

        return cloneGroups;
    }

    private Set<Integer> getAllCloneGroups(String pathToFiles) {

        File f = new File(pathToFiles);
        File[] files = f.listFiles();

        Set<Integer> toReturn = new TreeSet<>(Integer::compare);
        if (files != null) {
            for (File file : files) {
                Pattern pattern = Pattern.compile("xCloneSet(\\d+).html");
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.find()) {
                    toReturn.add(Integer.parseInt(matcher.group(1)));
                }
            }
        }

        return toReturn;
    }

    private String getCommonPathPrefix(String pathToFiles) {
        String readFileContents = readFileContents(pathToFiles + "jFilesAnalyzed.html");
        Pattern pattern = Pattern.compile("<h2>Common Path Prefix: <tt>(.+)</tt></h2>");
        Matcher matcher = pattern.matcher(readFileContents);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

}
