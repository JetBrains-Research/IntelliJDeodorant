package org.jetbrains.research.intellijdeodorant.core.distance;

import java.util.Set;

abstract class Entity {

    public abstract Set<String> getEntitySet();

    public abstract Set<String> getFullEntitySet();

    public abstract String getClassOrigin();
}
