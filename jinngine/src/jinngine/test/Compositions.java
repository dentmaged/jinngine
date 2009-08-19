package jinngine.test;

import java.util.ArrayList;
import java.util.List;

import jinngine.geometry.*;
import jinngine.math.*;
import jinngine.physics.*;
import jinngine.physics.force.*;

public class Compositions implements Testcase {
	// Use the visualiser to run the configuration
	List<Body> bodies = new ArrayList<Body>();
	private int dimention = 7;
	private double dt;
	
	public Compositions(int dimention, double dt) {
		this.dimention = dimention;
		this.dt = dt;
	}

	@Override
	public void deleteScene(Model model) {
		for (Body b:bodies) {
			model.removeBody(b);
		}
		
		bodies.clear();
	}

	@Override
	public void initScene(Model model) {
		//parameters
		model.setDt(dt);

		Body table = new Body( new Box(220,1,120));
		table.setPosition( new Vector3(0,-13,0));
		table.setFixed(true);
		table.advancePositions(1);
		model.addBody(table);
		bodies.add(table);	

		//build a stack
		for (int i=0; i<dimention; i++) {
			for (int j=0; j<dimention; j++) {
				for ( int k=0; k<dimention; k++) {
					Body b = new Body( new Sphere(4) );
					b.setPosition(new Vector3(-40+i*9,j*9 ,k*9));
					//b.setMass(5);	
					//b.getGeometries().next().setEnvelope(1);
					model.addBody(b);
					model.addForce( new GravityForce(b,1.0));
					//model.addForce( new LinearDragForce(b,5.5));
					bodies.add(b);
				}
			}
		}
		
	}

	public static void main(String arg[]) {
		Model model = new Engine();
		ThinWall test = new ThinWall(7, 0.05);
		test.initScene(model);			
		new BoxVisualisor(model, test.boxes, 1).start();
		
	}

}
