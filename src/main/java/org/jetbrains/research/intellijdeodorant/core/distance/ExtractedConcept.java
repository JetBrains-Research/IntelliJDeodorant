package org.jetbrains.research.intellijdeodorant.core.distance;

import org.jetbrains.research.intellijdeodorant.utils.TopicFinder;

import java.util.*;

public class ExtractedConcept implements Comparable<ExtractedConcept> {

    private final Set<ExtractClassCandidateRefactoring> conceptClusters;
    private final Set<Entity> conceptEntities;
    private final String sourceClass;
    private List<String> topics;

    public ExtractedConcept(Set<Entity> conceptEntities, String sourceClass) {
        this.conceptEntities = conceptEntities;
        this.conceptClusters = new HashSet<>();
        this.sourceClass = sourceClass;
        this.topics = new ArrayList<>();
    }

    public List<String> getTopics() {
        return topics;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public Set<Entity> getConceptEntities() {
        return conceptEntities;
    }

    public Set<ExtractClassCandidateRefactoring> getConceptClusters() {
        return conceptClusters;
    }

    public void addConceptCluster(ExtractClassCandidateRefactoring candidate) {
        this.conceptClusters.add(candidate);
    }

    public void findTopics() {
        List<String> codeElements = new ArrayList<>();
        for (Entity entity : this.getConceptEntities()) {
            if (entity instanceof MyAttribute) {
                MyAttribute attribute = (MyAttribute) entity;
                codeElements.add(attribute.getName());
            } else if (entity instanceof MyMethod) {
                MyMethod method = (MyMethod) entity;
                codeElements.add(method.getMethodName());
            }
        }
        this.topics = TopicFinder.findTopics(codeElements);
    }

    public int compareTo(ExtractedConcept other) {
        TreeSet<ExtractClassCandidateRefactoring> thisSet = new TreeSet<>(this.conceptClusters);
        TreeSet<ExtractClassCandidateRefactoring> otherSet = new TreeSet<>(other.conceptClusters);
        ExtractClassCandidateRefactoring thisFirst = thisSet.first();
        ExtractClassCandidateRefactoring otherFirst = otherSet.first();
        return thisFirst.compareTo(otherFirst);
    }

    @Override
    public String toString() {
        return topics.toString();
    }
}
