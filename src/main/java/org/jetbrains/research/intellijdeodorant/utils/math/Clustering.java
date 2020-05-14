package org.jetbrains.research.intellijdeodorant.utils.math;

import org.jetbrains.research.intellijdeodorant.core.distance.Entity;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class Clustering {
    protected double[][] distanceMatrix;

    public static Clustering getInstance(double[][] distanceMatrix) {
        return new Hierarchical(distanceMatrix);
    }

    public abstract HashSet<Cluster> clustering(ArrayList<Entity> entities);
}
