package it.unibo.cs.gauss.istar2SGO;

import java.util.ArrayList;

public class Actor {
	
	String id,text,type;
	double x,y;
	ArrayList<Node> nodes;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public ArrayList<Node> getNodes() {
		return nodes;
	}

//	public void setNodes(ArrayList<Node> nodes) {
//		this.nodes = nodes;
//	}
	public void addNode(Node n) {
		nodes.add(n);
	}

	public Actor() {
		nodes = new ArrayList<Node>();
		return ;
	}
}
