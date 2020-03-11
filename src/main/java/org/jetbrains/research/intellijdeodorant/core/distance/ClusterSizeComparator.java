package org.jetbrains.research.intellijdeodorant.core.distance;

import java.util.Comparator;

public class ClusterSizeComparator implements Comparator<ExtractClassCandidateRefactoring> {

    public int compare(ExtractClassCandidateRefactoring o1,
                       ExtractClassCandidateRefactoring o2) {
        return Integer.compare(o2.getExtractedEntities().size(), o1.getExtractedEntities().size());
    }

}
