package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

import com.intellij.openapi.project.Project;

public class CloneDetectorOutputParserFactory {

    public static CloneDetectorOutputParser getCloneToolParser(
            CloneDetectorType tool,
            Project jProject,
            String mainFile,
            String... otherArgs
    ) throws InvalidInputFileException {
        switch (tool) {
            case CCFINDER:
                return new CCFinderOutputParser(jProject, mainFile, otherArgs[0]);
            case CONQAT:
                return new ConQATOutputParser(jProject, mainFile);
            case DECKARD:
                return new DeckardOutputParser(jProject, mainFile);
            case NICAD:
                return new NiCadOutputParser(jProject, mainFile);
            case CLONEDR:
                return new CloneDROutputParser(jProject, mainFile);
            default:
                throw new IllegalArgumentException("Not yet implemented.");
        }
    }
}
