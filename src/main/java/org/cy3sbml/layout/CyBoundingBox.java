package org.cy3sbml.layout;

public class CyBoundingBox {
	public static final double STANDARD_HEIGHT = 30.0;
	public static final double STANDARD_WIDTH = 30.0;

	private String nodeId;
	private double xpos;
	private double ypos;
	private double height;
	private double width;
	
	
	public CyBoundingBox(String id, double x, double y){
		nodeId = id;
		xpos = x;
		ypos = y;
		height = STANDARD_HEIGHT;
		width = STANDARD_WIDTH;
	}
	
	public CyBoundingBox(String id, double x, double y, double h, double w){
		nodeId = id;
		xpos = x;
		ypos = y;
		height = h;
		width = w;
	}
	
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public double getXpos() {
		return xpos;
	}
	public void setXpos(double xpos) {
		this.xpos = xpos;
	}
	public double getYpos() {
		return ypos;
	}
	public void setYpos(double ypos) {
		this.ypos = ypos;
	}
	public double getHeight() {
		return height;
	}
	public void setHeight(double height) {
		this.height = height;
	}
	public double getWidth() {
		return width;
	}
	public void setWidth(double width) {
		this.width = width;
	}

	public String toString(){
		return String.format("id=%s, x=%s, y=%s, h=%s, w=%s", nodeId, xpos, ypos, height, width);
	}
	
}
