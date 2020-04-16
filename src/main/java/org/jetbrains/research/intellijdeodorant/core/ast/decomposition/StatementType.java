package org.jetbrains.research.intellijdeodorant.core.ast.decomposition;

public enum StatementType {

    ASSERT,
    BLOCK {
        public String toString() {
            return "{";
        }
    },
    BREAK,
    CONTINUE,
    DO,
    EMPTY,
    ENHANCED_FOR {
        public String toString() {
            return "for";
        }
    },
    EXPRESSION,
    FOR,
    IF,
    LABELED,
    RETURN,
    SWITCH_CASE,
    SWITCH,
    SYNCHRONIZED,
    THROW,
    TRY,
    VARIABLE_DECLARATION,
    WHILE,
    EXPRESSION_LIST;

    public String toString() {
        return name().toLowerCase();
    }
}
