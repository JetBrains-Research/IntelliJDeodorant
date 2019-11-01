package core.ast.decomposition.cfg;

import com.intellij.psi.PsiVariable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class ReachingAliasSet {

    private List<LinkedHashSet<PsiVariable>> aliasSets;

    ReachingAliasSet() {
        this.aliasSets = new ArrayList<>();
    }

    private ReachingAliasSet(List<LinkedHashSet<PsiVariable>> aliasSets) {
        this.aliasSets = aliasSets;
    }

    void insertAlias(PsiVariable leftHandSideReference, PsiVariable rightHandSideReference) {
        boolean rightHandSideReferenceFound = false;
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            if (aliasSet.contains(rightHandSideReference)) {
                rightHandSideReferenceFound = true;
                aliasSet.add(leftHandSideReference);
            }
        }
        if (!rightHandSideReferenceFound) {
            LinkedHashSet<PsiVariable> aliasSet = new LinkedHashSet<>();
            aliasSet.add(leftHandSideReference);
            aliasSet.add(rightHandSideReference);
            aliasSets.add(aliasSet);
        }
        List<LinkedHashSet<PsiVariable>> aliasSetsToBeRemoved = new ArrayList<>();
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            if (aliasSet.contains(leftHandSideReference)) {
                if (!aliasSet.contains(rightHandSideReference))
                    aliasSet.remove(leftHandSideReference);
                if (aliasSet.size() == 1)
                    aliasSetsToBeRemoved.add(aliasSet);
            }
        }
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSetsToBeRemoved) {
            aliasSets.remove(aliasSet);
        }
    }

    void removeAlias(PsiVariable leftHandSideReference) {
        List<LinkedHashSet<PsiVariable>> aliasSetsToBeRemoved = new ArrayList<>();
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            if (aliasSet.contains(leftHandSideReference)) {
                aliasSet.remove(leftHandSideReference);
                if (aliasSet.size() == 1)
                    aliasSetsToBeRemoved.add(aliasSet);
            }
        }
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSetsToBeRemoved) {
            aliasSets.remove(aliasSet);
        }
    }

    public boolean containsAlias(PsiVariable variableDeclaration) {
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            if (aliasSet.contains(variableDeclaration))
                return true;
        }
        return false;
    }

    boolean containsAlias(AbstractVariable variable) {
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            for (PsiVariable alias : aliasSet) {
                if (alias.equals(variable.getOrigin()))
                    return true;
            }
        }
        return false;
    }

    public Set<PsiVariable> getAliases(PsiVariable variable) {
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            if (aliasSet.contains(variable)) {
                Set<PsiVariable> aliases = new LinkedHashSet<>();
                for (PsiVariable alias : aliasSet) {
                    if (!alias.equals(variable))
                        aliases.add(alias);
                }
                return aliases;
            }
        }
        return null;
    }

    Set<PsiVariable> getAliases(AbstractVariable variable) {
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            boolean containsVariable = false;
            for (PsiVariable alias : aliasSet) {
                if (alias.equals(variable.getOrigin())) {
                    containsVariable = true;
                    break;
                }
            }
            if (containsVariable) {
                Set<PsiVariable> aliases = new LinkedHashSet<>();
                for (PsiVariable alias : aliasSet) {
                    if (!alias.equals(variable.getOrigin()))
                        aliases.add(alias);
                }
                return aliases;
            }
        }
        return null;
    }

    ReachingAliasSet copy() {
        List<LinkedHashSet<PsiVariable>> aliasSetsCopy = new ArrayList<>();
        for (LinkedHashSet<PsiVariable> aliasSet : aliasSets) {
            LinkedHashSet<PsiVariable> aliasSetCopy = new LinkedHashSet<>(aliasSet);
            aliasSetsCopy.add(aliasSetCopy);
        }
        return new ReachingAliasSet(aliasSetsCopy);
    }

    public String toString() {
        return aliasSets.toString();
    }
}
