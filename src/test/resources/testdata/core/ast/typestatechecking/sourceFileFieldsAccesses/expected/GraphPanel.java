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
	DragMode dragMode;

	private List<Node> graph;
	private static final int DRAG_NONE = 0;
	public static final int DRAG_MOVE = 1;
	private static final int DRAG_RUBBERBAND = 2;
	public static final int DRAG_LASSO = 3;

	private static final int GRID = 10;

	public static final int CONNECT_THRESHOLD = 8;

	public static final Color PURPLE = new Color(0.7f, 0.4f, 0.7f);

	public void setDragMode(int dragMode) {
		switch (dragMode) {
			case DRAG_LASSO:
				this.dragMode = new DragLasso();
				break;
			case DRAG_MOVE:
				this.dragMode = new DragMove();
				break;
			default:
				this.dragMode = null;
				break;
		}
	}

	public int getDragMode() {
		return dragMode.getDragMode();
	}

	public Point2D getMouseDownPoint() {
		return mouseDownPoint;
	}

	public List<Node> getGraph() {
		return graph;
	}

	public Object getLastSelected() {
		return lastSelected;
	}

	public Point2D getLastMousePoint() {
		return lastMousePoint;
	}

	public Set getSelectedItems() {
		return selectedItems;
	}

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

		dragMode.mouseDragged(mousePoint, isCtrl, this);

		lastMousePoint = mousePoint;
		repaint();
	}

	private void repaint() {
	}

	public void removeSelectedItem(Node n) {
	}

	public void addSelectedItem(Node n) {
	}
}