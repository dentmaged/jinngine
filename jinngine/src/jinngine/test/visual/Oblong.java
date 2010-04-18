/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
package jinngine.test.visual;

import java.util.ArrayList;
import java.util.List;

import jinngine.geometry.Box;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.DefaultScene;
import jinngine.physics.Scene;
import jinngine.physics.force.GravityForce;


public class Oblong implements Testcase {
	
	private double dt;
	
	public Oblong(double dt) {
		super();
		this.dt = dt;
	}

	// Use the visualiser to run the configuration
	List<Body> boxes = new ArrayList<Body>();
	
	
	@Override
	public void deleteScene(DefaultScene model) {
		for (Body b:boxes) {
			model.removeBody(b);
		}
		
		boxes.clear();
	}

	@Override
	public void initScene(DefaultScene model) {
		model.setTimestep(dt);

		
		Body seesaw =  new Body( "default", new Box(30,2,8) );
//		seesaw.getBoxGeometry().setEnvelope(1);
//		seesaw.setMass(10);
		seesaw.setPosition(new Vector3(0,-2,0));
		model.addBody(seesaw);
		model.addForce( new GravityForce(seesaw));
		
		Body table = new Body("default", new Box(50,1+40,50));
//		table.getBoxGeometry().setEnvelope(2);
		table.setPosition( new Vector3(0,-13-20,0));
//		table.setMass(9e9);
		table.setFixed(true);
		model.addBody(table);
		
		// Use the visualiser to run the configuration
		boxes.add(table);
		boxes.add(seesaw);
				
	}

	public static void main(String arg[]) {
		DefaultScene model = new DefaultScene();
		Seesaw test = new Seesaw(0.02);
		test.initScene(model);		
		new BoxVisualisor(model, test.boxes).start();
		
	}

}