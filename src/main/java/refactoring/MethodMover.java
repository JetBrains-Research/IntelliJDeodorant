package refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MethodMover {

    public static void moveMethod(Project project, PsiMethod method, PsiClass target) {
        if (method == null) {
            throw new IllegalStateException("Failed to move method cause method is null");
        }

        if (target == null) {
            throw new IllegalStateException("Failed to move method cause target class is null");
        }

        List<PsiVariable> possibleTargetVariables =
                Arrays.stream(method.getParameterList().getParameters())
                        .filter(it -> {
                            PsiType type = it.getType();
                            return type instanceof PsiClassType && target.equals(((PsiClassType) type).resolve());

                        })
                        .collect(Collectors.toList());

        if (possibleTargetVariables.isEmpty()) {
            possibleTargetVariables =
                    Arrays.stream(method.getContainingClass().getFields())
                            .filter(it -> {
                                PsiType type = it.getType();
                                return type instanceof PsiClassType && target.equals(((PsiClassType) type).resolve());

                            })
                            .collect(Collectors.toList());
        }

        PsiVariable targetVariable = possibleTargetVariables.get(0);
        Map<PsiClass, String> parameterNames = new HashMap<>();
        parameterNames.put(method.getContainingClass(), method.getName());

        MoveInstanceMethodProcessor moveMethodProcessor =
                new MoveInstanceMethodProcessor(
                        project,
                        method,
                        targetVariable,
                        PsiModifier.PUBLIC,
                        parameterNames
                );

        moveMethodProcessor.run();

        List<PsiMethod> candidates = new ArrayList<>();
        new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethod(final @NotNull PsiMethod aMethod) {
                super.visitMethod(aMethod);

                if (aMethod.getName().equals(method.getName())) {
                    candidates.add(aMethod);
                }
            }
        }.visitElement(target);

        if (candidates.size() != 1) {
            throw new IllegalStateException("Failed to find moved method: " + method.getName() + "; Method was moved to " + target.getQualifiedName());
        }
    }
}
