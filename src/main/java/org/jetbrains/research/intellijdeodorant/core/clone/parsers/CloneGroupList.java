package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

import com.intellij.openapi.project.Project;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class CloneGroupList implements Iterable<CloneGroup> {
    private Project javaProject;
    private final Map<Integer, CloneGroup> cloneGroupMap = new LinkedHashMap<>();

    public CloneGroupList(Project javaProject) {
        this.javaProject = javaProject;
    }

    public Project getJavaProject() {
        return javaProject;
    }

    public void add(CloneGroup cloneGroup) {
        if (!cloneGroupMap.containsKey(cloneGroup.hashCode())) {
            for (CloneGroup otherCloneGroup : cloneGroupMap.values()) {
                if (cloneGroup.isSubCloneOf(otherCloneGroup)) {
                    cloneGroup.setSubCloneOf(otherCloneGroup);
                    break;
                } else if (otherCloneGroup.isSubCloneOf(cloneGroup)) {
                    otherCloneGroup.setSubCloneOf(cloneGroup);
                    break;
                }
            }
            cloneGroupMap.put(cloneGroup.hashCode(), cloneGroup);
        }
    }

    public Iterator<CloneGroup> iterator() {
        return cloneGroupMap.values().iterator();
    }

    public CloneGroup[] getCloneGroups() {
        return cloneGroupMap.values().toArray(new CloneGroup[0]);
    }

    public int getCloneGroupsCount() {
        return cloneGroupMap.size();
    }

    public boolean containsCloneGroup(CloneGroup cloneGroup) {
        return cloneGroupMap.containsKey(cloneGroup.hashCode());
    }

    public boolean removeClonesExistingInFile(String filePath) {
        boolean changed = false;
        for (CloneGroup cloneGroup : cloneGroupMap.values()) {
            changed |= cloneGroup.removeClonesExistingInFile(filePath);
        }
        return changed;
    }

    public boolean updateClonesExistingInFile(String filePath, String newSourceCode) {
        boolean changed = false;
        for (CloneGroup cloneGroup : cloneGroupMap.values()) {
            changed |= cloneGroup.updateClonesExistingInFile(filePath, newSourceCode);
        }
        return changed;
    }
}
