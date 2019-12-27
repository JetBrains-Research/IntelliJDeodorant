package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.*;
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodDialog;
import com.intellij.refactoring.move.moveMembers.MoveMembersDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.*;
import org.jetbrains.research.intellijdeodorant.utils.PsiUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class RefactoringsApplier {
    private RefactoringsApplier() {
    }

    public static @NotNull
    Set<MoveMethodRefactoring> moveRefactoring(
            final @NotNull List<MoveMethodRefactoring> refactorings,
            final @NotNull AnalysisScope scope) {
        if (!checkValid(refactorings)) {
            throw new IllegalArgumentException("Methods in refactorings list must be unique!");
        }

        final Map<PsiClass, List<MoveMethodRefactoring>> groupedRefactorings = prepareRefactorings(refactorings);
        final Set<MoveMethodRefactoring> accepted = new HashSet<>();

        ApplicationManager.getApplication().runReadAction(() -> {
            for (Map.Entry<PsiClass, List<MoveMethodRefactoring>> refactoring : groupedRefactorings.entrySet()) {
                final PsiClass target = refactoring.getKey();
                final List<MoveMethodRefactoring> filteredRefactorings = refactoring.getValue().stream()
                        .sequential()
                        .filter(r -> r.accept(refactoring1 -> {
                            if (canMoveInstanceMethod(refactoring1.getMethod(), target)) {
                                if (moveInstanceMethod(refactoring1.getMethod(), target)) {
                                    accepted.add(refactoring1);
                                }
                                return false;
                            } else {
                                return true;
                            }
                        }))
                        .collect(Collectors.toList());

                accepted.addAll(moveMembersRefactoring(filteredRefactorings, target, scope));
            }
        });

        return accepted;
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

    private static boolean moveInstanceMethod(@NotNull PsiMethod method, PsiClass target) {
        PsiVariable[] available = getAvailableVariables(method, target);
        if (available.length == 0) {
            throw new IllegalStateException("Cannot move instance method");
        }
        MoveInstanceMethodDialog dialog = new MoveInstanceMethodDialog(method, available);
        dialog.setTitle("Move Instance Method " + PsiUtils.calculateSignature(method));
        TransactionGuard.getInstance().submitTransactionAndWait(dialog::show);
        return dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE;
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

    private static Set<MoveMethodRefactoring> moveMembersRefactoring(Collection<MoveMethodRefactoring> elements, PsiClass targetClass,
                                                                     AnalysisScope scope) {
        final Map<PsiClass, Set<MoveMethodRefactoring>> groupByCurrentClass = elements.stream()
                .collect(groupingBy((MoveMethodRefactoring it) -> it.getMethod().getContainingClass(), Collectors.toSet()));

        final Set<MoveMethodRefactoring> accepted = new HashSet<>();
        for (Entry<PsiClass, Set<MoveMethodRefactoring>> movement : groupByCurrentClass.entrySet()) {
            final Set<PsiMember> members = movement.getValue().stream().map(MoveMethodRefactoring::getMethod).collect(Collectors.toSet());
            MoveMembersDialog dialog = new MoveMembersDialog(scope.getProject(), movement.getKey(), targetClass,
                    members, null);
            TransactionGuard.getInstance().submitTransactionAndWait(dialog::show);
            if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                accepted.addAll(movement.getValue());
            }
        }
        return accepted;
    }

    private static boolean canMoveInstanceMethod(@NotNull PsiMethod method, PsiClass target) {
        PsiVariable[] available = getAvailableVariables(method, target);
        return available.length != 0;
    }
}