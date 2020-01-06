import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GraphPanel
{

    private double zoom;
    private double gridSize;
    private boolean hideGrid;

    private Object lastSelected;
    private Set selectedItems;

    private Point2D lastMousePoint;
    private Point2D mouseDownPoint;
    private int dragMode;

    private List<Node> graph;
    private static final int DRAG_NONE = 0;
    private static final int DRAG_MOVE = 1;
    private static final int DRAG_RUBBERBAND = 2;
    private static final int DRAG_LASSO = 3;

    private static final int GRID = 10;

    public static final int CONNECT_THRESHOLD = 8;

    public static final Color PURPLE = new Color(0.7f, 0.4f, 0.7f);

    private static class Color {
        Color(float a, float b, float c) {
        }
    }

    public static class Point2D {
        private Point2D() {

        }

        private Point2D(int event) {
            this();
        }

        protected double getX() {
            return 0;
        }

        protected double getY() {
            return 0;
        }
    }

    public static class Node {
        public Rectangle2D getBounds() {
            return new Rectangle2D();
        }

        Node getParent() {
            return new Node();
        }

        void translate(double a, double c) {

        }

        public void setColor(Color color) {
        }
    }

    public static class Rectangle2D extends Point2D {
        public void add(Rectangle2D bounds) {
        }

        public boolean contains(Rectangle2D bounds) {
            return true;
        }
    }

    public void mouseDragged(int event)
    {
        Point2D mousePoint = new Point2D(event);
        boolean isCtrl = event != 0;

        if (dragMode == DRAG_MOVE && lastSelected instanceof Node)
        {
            Node lastNode = (Node) lastSelected;
            Rectangle2D bounds = lastNode.getBounds();
            double dx = mousePoint.getX() - lastMousePoint.getX();
            double dy = mousePoint.getY() - lastMousePoint.getY();

            // we don't want to drag nodes into negative coordinates
            // particularly with multiple selection, we might never be
            // able to get them back.
            Iterator iter = selectedItems.iterator();
            while (iter.hasNext())
            {
                Object selected = iter.next();
                if (selected instanceof Node)
                {
                    Node n = (Node) selected;
                    bounds.add(n.getBounds());
                }
            }
            dx = Math.max(dx, -bounds.getX());
            dy = Math.max(dy, -bounds.getY());

            iter = selectedItems.iterator();
            while (iter.hasNext())
            {
                Object selected = iter.next();
                if (selected instanceof Node)
                {
                    Node n = (Node) selected;
                    // If the father is selected, don't move the children
                    if (!selectedItems.contains(n.getParent()))
                    {
                        n.translate(dx, dy);
                    }
                }
            }
            // we don't want continuous layout any more because of multiple selection
            // graph.layout();
        }
        else if (dragMode == DRAG_LASSO)
        {
            double x1 = mouseDownPoint.getX();
            double y1 = mouseDownPoint.getY();
            double x2 = mousePoint.getX();
            double y2 = mousePoint.getY();
            Rectangle2D lasso = new Rectangle2D();
            Iterator iter = graph.iterator();
            while (iter.hasNext())
            {
                Node n = (Node) iter.next();
                Rectangle2D bounds = n.getBounds();
                if (!isCtrl && !lasso.contains(bounds))
                {
                    removeSelectedItem(n);
                    n.setColor(PURPLE);
                }
                else if (lasso.contains(bounds))
                {
                    addSelectedItem(n);
                }
            }
        }

        lastMousePoint = mousePoint;
        repaint();
    }

    private void repaint() {
    }

    private void removeSelectedItem(Node n) {
    }

    private void addSelectedItem(Node n) {
    }
}