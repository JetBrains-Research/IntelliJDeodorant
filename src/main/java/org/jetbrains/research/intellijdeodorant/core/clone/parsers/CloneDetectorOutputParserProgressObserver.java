package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

public interface CloneDetectorOutputParserProgressObserver {
    public void notify(int cloneGroupIndex);
}
