public abstract class DragMode {
    public abstract int getDragMode();

    public abstract void mouseDragged(GraphPanel.Point2D mousePoint, boolean isCtrl, GraphPanel graphPanel);
}