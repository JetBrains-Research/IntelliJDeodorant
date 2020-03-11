package org.jetbrains.research.intellijdeodorant.utils.math;


import org.jetbrains.research.intellijdeodorant.core.distance.Entity;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class Clustering {

    protected ArrayList<ArrayList<Double>> distanceList;
    protected double[][] distanceMatrix;

    public static Clustering getInstance(int type, double[][] distanceMatrix) {
        if (type == 0) {
            return new Hierarchical(distanceMatrix);
        }
        return null;
    }

    public abstract HashSet<Cluster> clustering(ArrayList<Entity> entities);
}
