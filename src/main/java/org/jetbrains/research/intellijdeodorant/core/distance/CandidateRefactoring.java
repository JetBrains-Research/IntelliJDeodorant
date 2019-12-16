package org.jetbrains.research.intellijdeodorant.core.distance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

abstract class CandidateRefactoring {
    public abstract String getSourceEntity();

    public abstract String getSource();

    public abstract String getTarget();

    protected abstract Set<String> getEntitySet();

    public abstract int getDistinctSourceDependencies();
    
    public abstract int getDistinctTargetDependencies();

    public String getAnnotationText() {
        Map<String, ArrayList<String>> accessMap = new LinkedHashMap<>();
        for (String entity : getEntitySet()) {
            String[] tokens = entity.split("::");
            String classOrigin = tokens[0];
            String entityName = tokens[1];
            if (accessMap.containsKey(classOrigin)) {
                ArrayList<String> list = accessMap.get(classOrigin);
                list.add(entityName);
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add(entityName);
                accessMap.put(classOrigin, list);
            }
        }

        StringBuilder sb = new StringBuilder();
        Set<String> keySet = accessMap.keySet();
        int i = 0;
        for (String key : keySet) {
            ArrayList<String> entities = accessMap.get(key);
            sb.append(key).append(": ").append(entities.size());
            if (i < keySet.size() - 1)
                sb.append(" | ");
            i++;
        }
        return sb.toString();
    }
}
