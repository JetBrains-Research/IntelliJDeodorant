import java.util.Iterator;

/**
 * @see GraphPanel#DRAG_LASSO **/
public class DragLasso extends DragMode {
    public int getDragMode() {
        return GraphPanel.DRAG_LASSO;
    }

    public void mouseDragged(GraphPanel.Point2D mousePoint, boolean isCtrl, GraphPanel graphPanel) {
        double x1 = graphPanel.getMouseDownPoint().getX();
        double y1 = graphPanel.getMouseDownPoint().getY();
        double x2 = mousePoint.getX();
        double y2 = mousePoint.getY();
        GraphPanel.Rectangle2D lasso = new GraphPanel.Rectangle2D();
        Iterator iter = graphPanel.getGraph().iterator();
        while (iter.hasNext()) {
            GraphPanel.Node n = (GraphPanel.Node) iter.next();
            GraphPanel.Rectangle2D bounds = n.getBounds();
            if (!isCtrl && !lasso.contains(bounds)) {
                graphPanel.removeSelectedItem(n);
                n.setColor(GraphPanel.PURPLE);
            } else if (lasso.contains(bounds)) {
                graphPanel.addSelectedItem(n);
            }
        }
    }
}