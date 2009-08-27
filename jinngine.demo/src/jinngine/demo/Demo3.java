package jinngine.demo;

import java.util.LinkedList;
import java.util.List;

import jinngine.demo.graphics.Entity;
import jinngine.demo.graphics.FlatShade;
import jinngine.demo.graphics.Graphics;
import jinngine.demo.graphics.Hull;
import jinngine.demo.graphics.Render;
import jinngine.geometry.Box;
import jinngine.math.Matrix4;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.Model;
import jinngine.physics.constraint.HingeJoint;
import jinngine.physics.force.GravityForce;
import jinngine.util.Pair;

public class Demo3 {
	public Demo3() {
		Graphics g = new Graphics();
		Model model = g.getModel();
		
		//Setup a shape (a box) for drawing
		Vector3 size = new Vector3(3,2,2);
		List<Vector3> points = new LinkedList<Vector3>();
		points.add( new Vector3( 1, 1, 1 ).multiply(0.5));
		points.add( new Vector3( -1, 1, 1 ).multiply(0.5));
		points.add( new Vector3( 1, -1, 1 ).multiply(0.5));
		points.add( new Vector3( -1, -1, 1 ).multiply(0.5));
		points.add( new Vector3( 1, 1, -1 ).multiply(0.5));
		points.add( new Vector3( -1, 1, -1 ).multiply(0.5));
		points.add( new Vector3( 1, -1, -1 ).multiply(0.5));
		points.add( new Vector3( -1, -1, -1 ).multiply(0.5));

		//resize the drawing shape to the right dimensions
		Matrix4 transform = jinngine.math.Transforms.scale(size);
		for (Vector3 p: points)
			p.assign( transform.multiply(p));
		
		//create drawing shape
		Hull shape = new Hull(points.iterator());
			
		
		//create a world that contains walls and a floor (not drawn)
		Body floor = new Body(new Box(1500,10,1500));
		floor.setPosition(new Vector3(0,-25,0));
		floor.setFixed(true);
		
		Body back = new Body( new Box(200,200,2));		
		back.setPosition(new Vector3(0,0,-45));
		back.setFixed(true);

		Body front = new Body( new Box(200,200,2));		
		front.setPosition(new Vector3(0,0,-15));
		front.setFixed(true);

		Body left = new Body( new Box(2,200,200));		
		left.setPosition(new Vector3(-25,0,0));
		left.setFixed(true);

		Body right = new Body( new Box(2,200,200));		
		right.setPosition(new Vector3(0,0,0));
		right.setFixed(true);

		model.addBody(left);
		model.addBody(right);
		model.addBody(front);
		model.addBody(floor);
		model.addBody(back);

		//we need some power for this
		model.getSolver().setMaximumIterations(35);

		//get renderer from graphics
		Render render = g.getRender();
		
		//build a chain of joined boxes
		Body prevBody = null;
		for (int i=0; i<5; i++) {
			final Body body;

			//Setup the physics
			Box box = new Box(size.a1, size.a2, size.a3);

			//set material properties
			box.setFrictionCoefficient(0.7);
			box.setRestitution(0.4);
			
			//create a body, and add the geometry to it
			body = new Body();		
			body.addGeometry(box);		
			body.finalize();                 // called when all geometry is added
			body.setPosition(new Vector3(-17+i*4-5,-20,-25));
			body.sleepKinetic = 0.0;

			//Tell the model about our new box and attach a gravity force to it
			model.addForce(new GravityForce(body,1));
			model.addBody(body);

			//create chain joints
			if (prevBody !=null) {
				HingeJoint joint = new HingeJoint(prevBody,body,new Vector3(-17+i*4-5-2.7,-20,-25), new Vector3(0,0,1));
				joint.getHingeControler().setLimits(-Math.PI/4, Math.PI/4);
				joint.getHingeControler().setFrictionMagnitude(0);
				model.addConstraint(new Pair<Body>(prevBody,body), joint);
				prevBody = body;
			} else {
				prevBody = body;		
			}
			
			//tell the renderer about all this (not directly related to jinngine physics)
			//looks weird, but just a simple class to make the graphics work
			Entity e = new Entity() {
				private boolean alarmed = false;
				@Override
				public boolean getAlarmed() { return alarmed; }
				@Override
				public Vector3 getPosition() { return body.state.rCm.copy();}
				@Override
				public Body getPrimaryBody() {return body;}
				@Override
				public void setAlarmed(boolean alarmed) {this.alarmed = alarmed;}
				@Override
				public void setPosition(Vector3 p) {body.setPosition(p);}
				@Override
				public void setSelected(boolean selected) {}
			};

			//bind the box geometry to the entity
			box.setAuxiliary(e); 
			
			//finally, ask render to draw this shape
			render.addShape( new FlatShade(), shape, body.state.transform, e);
			
		}

		//start animation
		g.start();
	}
	
	public static void main( String args[]) {
		new Demo3();
	}
}
