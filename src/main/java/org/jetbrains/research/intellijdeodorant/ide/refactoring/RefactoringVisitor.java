package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.movemethod.MoveMethodRefactoring;

public interface RefactoringVisitor<R> {
    @NotNull
    R visit(final @NotNull MoveMethodRefactoring refactoring);
}