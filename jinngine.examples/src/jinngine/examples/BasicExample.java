/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
package jinngine.examples;

import jinngine.geometry.Box;
import jinngine.math.Vector3;
import jinngine.physics.*;
import jinngine.physics.force.GravityForce;
import jinngine.rendering.Rendering;

public class BasicExample implements Rendering.Callback {	
	private final Scene scene;
	
	public BasicExample() {
		
		// start jinngine 
		scene = new DefaultScene();
		
		// add boxes to bound the world
		Body floor = new Body("floor", new Box(1500,20,1500));
		floor.setPosition(new Vector3(0,-30,0));
		floor.setFixed(true);
		
		Body back = new Body( "back", new Box(200,200,20));		
		back.setPosition(new Vector3(0,0,-55));
		back.setFixed(true);

		Body front = new Body( "front", new Box(200,200,20));		
		front.setPosition(new Vector3(0,0,-7));
		front.setFixed(true);

		Body left = new Body( "left", new Box(20,200,200));		
		left.setPosition(new Vector3(-35,0,0));
		left.setFixed(true);

		Body right = new Body( "right", new Box(20,200,200));		
		right.setPosition(new Vector3(10,0,0));
		right.setFixed(true);
		
		// create a box
		Box boxgeometry = new Box(2,2,2);
		Body box = new Body( "box", boxgeometry );
		box.setPosition(new Vector3(-10,-11,-25));
		
		// add all to scene
		scene.addBody(floor);
		scene.addBody(back);
		scene.addBody(front);		
		scene.addBody(left);
		scene.addBody(right);
		scene.addBody(box);

		// put gravity on box
		scene.addForce( new GravityForce(box));		
		
		// handle drawing
		Rendering rendering = new jinngine.rendering.jogl.JoglRendering(this);
		rendering.drawMe(boxgeometry);
		rendering.start();
	}

	@Override
	public void callback() {
		// each frame, to a time step on the Scene
		scene.tick();
	}
	
	public static void main( String[] args) {
		new BasicExample();
	}

}
