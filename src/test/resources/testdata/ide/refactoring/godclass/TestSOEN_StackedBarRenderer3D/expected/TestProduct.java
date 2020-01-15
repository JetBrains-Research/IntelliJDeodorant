package TestSOEN_StackedBarRenderer3D.actual;

import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.data.DataUtilities;
import org.jfree.data.category.CategoryDataset;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TestProduct implements Serializable {
    private boolean ignoreZeroValues;

    public boolean getIgnoreZeroValues() {
        return ignoreZeroValues;
    }


    public void setIgnoreZeroValues(boolean ignore, Test test) {
        this.ignoreZeroValues = ignore;
        //TEST GIVES A WRONG RESULT: should be `test.notifyListeners()`. That's just a result of unresolved reference to the super class, in real project it won't happen.
        notifyListeners(new RendererChangeEvent(test));
    }


    public List createStackedValueList(CategoryDataset dataset, Comparable category, int[] includedRows, double base,
                                       boolean asPercentages) {
        List result = new ArrayList();
        double posBase = base;
        double negBase = base;
        double total = 0.0;
        if (asPercentages) {
            total = DataUtilities.calculateColumnTotal(dataset, dataset.getColumnIndex(category), includedRows);
        }
        int baseIndex = -1;
        int rowCount = includedRows.length;
        for (int i = 0; i < rowCount; i++) {
            int r = includedRows[i];
            Number n = dataset.getValue(dataset.getRowKey(r), category);
            if (n == null) {
                continue;
            }
            double v = n.doubleValue();
            if (asPercentages) {
                v = v / total;
            }
            if ((v > 0.0) || (!this.ignoreZeroValues && v >= 0.0)) {
                if (baseIndex < 0) {
                    result.add(new Object[]{null, new Double(base)});
                    baseIndex = 0;
                }
                posBase = posBase + v;
                result.add(new Object[]{new Integer(r), new Double(posBase)});
            } else if (v < 0.0) {
                if (baseIndex < 0) {
                    result.add(new Object[]{null, new Double(base)});
                    baseIndex = 0;
                }
                negBase = negBase + v;
                result.add(0, new Object[]{new Integer(-r - 1), new Double(negBase)});
                baseIndex++;
            }
        }
        return result;
    }


    public List createStackedValueList(CategoryDataset dataset, Comparable category, double base,
                                       boolean asPercentages) {
        int[] rows = new int[dataset.getRowCount()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i;
        }
        return createStackedValueList(dataset, category, rows, base, asPercentages);
    }
}