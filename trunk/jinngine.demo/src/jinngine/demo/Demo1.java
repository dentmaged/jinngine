package jinngine.demo;

import jinngine.demo.graphics.Graphics;
import jinngine.geometry.Box;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.Model;


public class Demo1 {
	
	public Demo1() {
		Graphics g = new Graphics();
		Model model = g.getModel();
		
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

		
		//create some cubes and a gear
		new Cube(g, new Vector3(2.5,2.5,2.5), new Vector3(-10,-10,-20), 10 );
		new Cube(g, new Vector3(3,3,3), new Vector3(-15,-10,-20), 10 );
		new Cube(g, new Vector3(2,2,2), new Vector3(-5,-10,-20), 10 );
//		new Cube(g, new Vector3(30,10,10), 10, 15);
//		new Cube(g, new Vector3(40,10,10), 10, 15);
		
		//new Gear(g, new Vector3(0,50,0), 1, 20);
		new Gear(g, new Vector3(-10,-10,-20), 25, 3);

		g.start();
	}
	
	public static void main( String args[]) {
		new Demo1();
	}
}
