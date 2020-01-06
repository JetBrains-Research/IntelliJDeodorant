package org.jetbrains.research.intellijdeodorant.util.math;

import org.jetbrains.research.intellijdeodorant.core.distance.Entity;

import java.util.ArrayList;

public class Cluster {

    private ArrayList<Entity> entities;
    private int hashCode;

    public Cluster() {
        entities = new ArrayList<Entity>();
    }

    public Cluster(ArrayList<Entity> entities) {
        this.entities = new ArrayList<Entity>(entities);
    }

    public void addEntity(Entity entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public ArrayList<Entity> getEntities() {
        return entities;
    }

    public void addEntities(ArrayList<Entity> entities) {
        if (!this.entities.containsAll(entities)) {
            this.entities.addAll(entities);
        }
    }

    public boolean equals(Object o) {
        Cluster c = (Cluster) o;
        return this.entities.equals(c.entities);
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            for (Entity entity : entities) {
                result = 37 * result + entity.hashCode();
            }
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        String s = "{";

        for (Entity entity : entities) {
            s += entity + ", ";
        }
        return s += "}";
    }
}
