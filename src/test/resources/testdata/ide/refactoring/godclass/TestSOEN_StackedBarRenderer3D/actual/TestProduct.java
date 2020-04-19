package TestSOEN_StackedBarRenderer3D.actual;

import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;

import java.awt.*;
import java.io.Serializable;

public class TestProduct implements Serializable {
    private boolean renderAsPercentages;

    public boolean getRenderAsPercentages() {
        return renderAsPercentages;
    }

    public void setRenderAsPercentages2(boolean renderAsPercentages) {
        this.renderAsPercentages = renderAsPercentages;
    }

    public void setRenderAsPercentages(boolean asPercentages) {
        this.renderAsPercentages = asPercentages;
        fireChangeEvent();
    }

    public Range findRangeBounds(CategoryDataset dataset) {
        if (dataset == null) {
            return null;
        }
        if (this.renderAsPercentages) {
            return new Range(0.0, 1.0);
        } else {
            return DatasetUtilities.findStackedRangeBounds(dataset);
        }
    }
}