package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import org.jetbrains.annotations.NotNull;

public interface RefactoringVisitor<R> {
    @NotNull
    R visit(final @NotNull MoveMethodRefactoring refactoring);
}