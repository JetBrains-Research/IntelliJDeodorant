package ui;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import refactoring.MoveMethodRefactoring;
import utils.IntelliJDeodorantBundle;
import utils.PsiUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MoveMethodTableModel extends AbstractTableModel {
    private static final String METHOD_COLUMN_TITLE_KEY = "method.column.title";
    private static final String MOVE_TO_COLUMN_TITLE_KEY = "move.to.column.title";

    static final int SELECTION_COLUMN_INDEX = 0;
    private static final int ENTITY_COLUMN_INDEX = 1;
    private static final int MOVE_TO_COLUMN_INDEX = 2;
    private static final int COLUMNS_COUNT = 3;

    private final List<MoveMethodRefactoring> refactorings = new ArrayList<>();
    private final List<Integer> virtualRows = new ArrayList<>();
    private boolean[] isSelected;
    private boolean[] isActive;

    MoveMethodTableModel(List<MoveMethodRefactoring> refactorings) {
        updateTable(refactorings);
    }

    void updateTable(List<MoveMethodRefactoring> refactorings) {
        this.refactorings.clear();
        this.refactorings.addAll(refactorings);
        isSelected = new boolean[refactorings.size()];
        isActive = new boolean[refactorings.size()];
        Arrays.fill(isActive, true);
        IntStream.range(0, refactorings.size())
                .forEachOrdered(virtualRows::add);
        fireTableDataChanged();
    }

    void clearTable() {
        this.refactorings.clear();
        this.virtualRows.clear();
        fireTableDataChanged();
    }

    void selectAll() {
        virtualRows.forEach(i -> isSelected[i] = true);
        fireTableDataChanged();
    }

    void deselectAll() {
        Arrays.fill(isSelected, false);
        fireTableDataChanged();
    }


    void setAppliedRefactorings(@NotNull Set<MoveMethodRefactoring> accepted) {
        virtualRows.forEach(i -> {
            if (accepted.contains(refactorings.get(i))) {
                isActive[i] = false;
                isSelected[i] = false;
            }
        });
        fireTableDataChanged();
    }

    List<MoveMethodRefactoring> pullSelectable() {
        return virtualRows.stream()
                .filter(i -> isActive[i])
                .map(refactorings::get)
                .collect(Collectors.toList());
    }

    List<MoveMethodRefactoring> pullSelected() {
        return virtualRows.stream()
                .filter(i -> isSelected[i] && isActive[i])
                .map(refactorings::get)
                .collect(Collectors.toList());
    }

    @Override
    public int getColumnCount() {
        return COLUMNS_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case SELECTION_COLUMN_INDEX:
                return "";
            case ENTITY_COLUMN_INDEX:
                return IntelliJDeodorantBundle.message(METHOD_COLUMN_TITLE_KEY);
            case MOVE_TO_COLUMN_INDEX:
                return IntelliJDeodorantBundle.message(MOVE_TO_COLUMN_TITLE_KEY);
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + column);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == SELECTION_COLUMN_INDEX && isActive[rowIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == SELECTION_COLUMN_INDEX ? Boolean.class : String.class;
    }

    @Override
    public int getRowCount() {
        return virtualRows.size();
    }


    @Override
    public void setValueAt(Object value, int virtualRow, int columnIndex) {
        isSelected[virtualRows.get(virtualRow)] = (Boolean) value;
        fireTableCellUpdated(virtualRow, columnIndex);
    }

    boolean isAnySelected() {
        for (boolean isSelectedItem : isSelected) {
            if (isSelectedItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public Object getValueAt(int virtualRow, int columnIndex) {
        final int rowIndex = virtualRows.get(virtualRow);
        switch (columnIndex) {
            case SELECTION_COLUMN_INDEX:
                return isSelected[rowIndex];
            case ENTITY_COLUMN_INDEX:
                Optional<PsiMethod> method = refactorings.get(rowIndex).getOptionalMethod();
                return method.map(PsiUtils::getHumanReadableName).orElseGet(() -> IntelliJDeodorantBundle.message("java.member.is.not.valid"));
            case MOVE_TO_COLUMN_INDEX:
                Optional<PsiClass> targetClass = refactorings.get(rowIndex).getOptionalTargetClass();
                return targetClass.map(PsiUtils::getHumanReadableName).orElseGet(() -> IntelliJDeodorantBundle.message("target.class.is.not.valid"));
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + columnIndex);
    }

    Optional<? extends PsiMember> getUnitAt(int virtualRow, int column) {
        final int row = virtualRows.get(virtualRow);
        switch (column) {
            case ENTITY_COLUMN_INDEX:
                return refactorings.get(row).getOptionalMethod();
            case MOVE_TO_COLUMN_INDEX:
                return refactorings.get(row).getOptionalTargetClass();
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + column);
    }

    Set<MoveMethodRefactoring> getRefactorings() {
        return new HashSet<>(refactorings);
    }

    MoveMethodRefactoring getRefactoring(int virtualRow) {
        return refactorings.get(virtualRows.get(virtualRow));
    }

    void setupRenderer(JTable table) {
        table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer() {
            private final JLabel EMPTY_LABEL = new JLabel();

            {
                EMPTY_LABEL.setBackground(JBColor.LIGHT_GRAY);
                EMPTY_LABEL.setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus,
                                                           int row, int column) {
                final int realRow = virtualRows.get(table.convertRowIndexToModel(row));
                if (isActive[realRow]) {
                    return super.getTableCellRendererComponent(table, value, isSel, hasFocus, row, column);
                } else {
                    return EMPTY_LABEL;
                }
            }
        });
        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int virtualRow, int column) {
                final int row = virtualRows.get(table.convertRowIndexToModel(virtualRow));
                if (!isActive[row]) {
                    setBackground(JBColor.LIGHT_GRAY);
                } else if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(table.getBackground());
                }
                setEnabled(isActive[row]);
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, virtualRow, column);
            }
        });
    }
}