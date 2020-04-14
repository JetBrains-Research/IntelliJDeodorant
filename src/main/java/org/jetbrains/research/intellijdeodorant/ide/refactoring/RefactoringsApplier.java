package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.ide.fus.collectors.IntelliJDeodorantCounterCollector;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.moveMethod.MoveMethodRefactoring;
import org.jetbrains.research.intellijdeodorant.utils.PsiUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefactoringsApplier {
    private RefactoringsApplier() {
    }

    public static void moveRefactoring(final @NotNull List<MoveMethodRefactoring> refactorings) {
        if (!checkValid(refactorings)) {
            throw new IllegalArgumentException("Methods in refactorings list must be unique!");
        }

        final Map<PsiClass, List<MoveMethodRefactoring>> groupedRefactorings = prepareRefactorings(refactorings);

        ApplicationManager.getApplication().runReadAction(() -> {
            for (Map.Entry<PsiClass, List<MoveMethodRefactoring>> refactoring : groupedRefactorings.entrySet()) {
                final PsiClass target = refactoring.getKey();
                refactoring.getValue().forEach(r -> {
                    if (canMoveInstanceMethod(r.getMethod(), target)) {
                        moveInstanceMethod(r, target);
                        if (!r.getOptionalMethod().isPresent()) {
                            IntelliJDeodorantCounterCollector.getInstance().moveMethodRefactoringApplied(target.getProject(),
                                    r.getSourceAccessedMembers(), r.getTargetAccessedMembers(),
                                    r.getMethodLength(), r.getMethodParametersCount());
                        }
                    }
                });
            }
        });
    }

    private static boolean checkValid(Collection<MoveMethodRefactoring> refactorings) {
        final long uniqueUnits = refactorings.stream()
                .map(MoveMethodRefactoring::getMethod)
                .distinct()
                .count();
        return uniqueUnits == refactorings.size();
    }

    private static @NotNull
    Map<PsiClass, List<MoveMethodRefactoring>> prepareRefactorings(
            final @NotNull List<MoveMethodRefactoring> refactorings
    ) {
        return refactorings.stream().collect(
                Collectors.groupingBy(MoveMethodRefactoring::getTargetClass, Collectors.toList())
        );
    }

    private static void moveInstanceMethod(@NotNull MoveMethodRefactoring refactoring, PsiClass target) {
        PsiMethod methodToMove = refactoring.getMethod();
        PsiVariable[] available = getAvailableVariables(methodToMove, target);
        if (available.length == 0) {
            throw new IllegalStateException("Cannot move instance method");
        }
        MoveInstanceMethodDialog dialog = new MoveInstanceMethodDialog(methodToMove, available);
        dialog.setTitle("Move Instance Method " + PsiUtils.calculateSignature(methodToMove));
        ApplicationManager.getApplication().invokeAndWait(dialog::show);
    }

    private static PsiVariable[] getAvailableVariables(@NotNull PsiMethod method, @NotNull PsiClass target) {
        final PsiClass psiClass = method.getContainingClass();
        Stream<PsiVariable> parameters = Arrays.stream(method.getParameterList().getParameters());
        Stream<PsiVariable> fields = psiClass == null ? Stream.empty() : Arrays.stream(psiClass.getFields());
        return Stream.concat(parameters, fields)
                .filter(Objects::nonNull)
                .filter(p -> p.getType() instanceof PsiClassType && target.equals(((PsiClassType) p.getType()).resolve()))
                .toArray(PsiVariable[]::new);
    }

    private static boolean canMoveInstanceMethod(@NotNull PsiMethod method, PsiClass target) {
        PsiVariable[] available = getAvailableVariables(method, target);
        return available.length != 0;
    }
}
