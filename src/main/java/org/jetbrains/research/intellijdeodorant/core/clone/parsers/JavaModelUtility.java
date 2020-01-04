package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.impl.ProjectRootUtil;
import com.intellij.psi.PsiDirectory;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class JavaModelUtility {
    public static Set<String> getAllSourceDirectories(Project jProject) {
        /*
         * We sort the src paths by their character lengths in a non-increasing
         * order, because we are going to see whether a Java file's path starts
         * with a specific source path For example, if the Java file's path is
         * "src/main/org/blah/blah", the "src/main" is considered the source
         * path not "src/"
         */
        Set<String> allSrcDirectories = new TreeSet<String>((o1, o2) -> {
            if (o1.equals(o2))
                return 0;

            if (o1.length() == o2.length())
                return 1;

            return -Integer.compare(o1.length(), o2.length());
        });

        PsiDirectory[] directories = ProjectRootUtil.getAllContentRoots(jProject);
        for (PsiDirectory dir : directories) {
            if (dir.getName().equals("/" + jProject.getName()))
                allSrcDirectories.add(dir.getName());
            else if (dir.getName().length() > jProject.getName().length() + 2) {
                String srcDirectory = dir.getName().substring(jProject.getName().length() + 2);
                allSrcDirectories.add(srcDirectory);
            }
        }
        return allSrcDirectories;
    }

}
