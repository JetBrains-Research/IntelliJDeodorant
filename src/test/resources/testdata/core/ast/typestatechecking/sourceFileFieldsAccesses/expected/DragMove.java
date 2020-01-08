import java.util.Iterator;

/**
 * @see GraphPanel#DRAG_MOVE **/
public class DragMove extends DragMode {
    public int getDragMode() {
        return GraphPanel.DRAG_MOVE;
    }

    public void mouseDragged(GraphPanel.Point2D mousePoint, boolean isCtrl, GraphPanel graphPanel) {
        if (graphPanel.getLastSelected() instanceof GraphPanel.Node) {
            GraphPanel.Node lastNode = (GraphPanel.Node) graphPanel.getLastSelected();
            GraphPanel.Rectangle2D bounds = lastNode.getBounds();
            double dx = mousePoint.getX() - graphPanel.getLastMousePoint().getX();
            double dy = mousePoint.getY() - graphPanel.getLastMousePoint().getY();
            Iterator iter = graphPanel.getSelectedItems().iterator();
            while (iter.hasNext()) {
                Object selected = iter.next();
                if (selected instanceof GraphPanel.Node) {
                    GraphPanel.Node n = (GraphPanel.Node) selected;
                    bounds.add(n.getBounds());
                }
            }
            dx = Math.max(dx, -bounds.getX());
            dy = Math.max(dy, -bounds.getY());
            iter = graphPanel.getSelectedItems().iterator();
            while (iter.hasNext()) {
                Object selected = iter.next();
                if (selected instanceof GraphPanel.Node) {
                    GraphPanel.Node n = (GraphPanel.Node) selected;
                    // If the father is selected, don't move the children
                    if (!graphPanel.getSelectedItems().contains(n.getParent())) {
                        n.translate(dx, dy);
                    }
                }
            }
        }
    }
}