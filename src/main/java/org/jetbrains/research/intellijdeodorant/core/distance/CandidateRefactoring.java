package org.jetbrains.research.intellijdeodorant.core.distance;

import java.util.Set;

public abstract class CandidateRefactoring {
    public abstract String getSourceEntity();

    public abstract String getSource();

    public abstract String getTarget();

    protected abstract Set<String> getEntitySet();

    public abstract int getDistinctSourceDependencies();

    public abstract int getDistinctTargetDependencies();

}
