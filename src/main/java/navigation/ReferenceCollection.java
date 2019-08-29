package navigation;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.ArrayListSet;

import java.util.*;

public class ReferenceCollection extends AbstractCollection<Reference> {
    private Set<Reference> references = new ArrayListSet<Reference>();
    public static ReferenceCollection EMPTY = new ReferenceCollection(new ArrayList<>());

    public ReferenceCollection(Collection<PsiReference> psiReferences) {
        for (PsiReference reference : psiReferences) {
            references.add(new Reference((PsiMethod) reference.resolve(), ""));
        }
    }

    public ReferenceCollection(ReferenceCollection references) {
        this.references.addAll(references);
    }

    public Iterator<Reference> iterator() {
        return references.iterator();
    }

    public boolean add(Reference reference) {
        return references.add(reference);
    }

    public int size() {
        return references.size();
    }
}