/**
 * Copyright (c) 2010-2011 Morten Silcowitz
 *
 * This file is part of jinngine.
 *
 * jinngine is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://code.google.com/p/jinngine/>.
 */

package jinngine.test.unit;

import jinngine.collision.RayCast;
import jinngine.geometry.Box;
import jinngine.geometry.Sphere;
import jinngine.math.Matrix3;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import junit.framework.TestCase;

@SuppressWarnings("unused")
public class RayCastTest extends TestCase {

    // epsilon is the allowed error in the 
    // computed distances. envelope is the shell 
    // around objects that is considered as contact area
    double epsilon = 1e-7;
    double envelope = 1e-5;

    /**
     * create a sphere and cast a ray against it
     */
    public void testRay1() {
        final RayCast raycast = new RayCast();

        // setup cube geometry
        final Sphere s1 = new Sphere(1);
        final Body b1 = new Body("default");
        b1.addGeometry(Matrix3.identity(), new Vector3(), s1);

        // pick a point outside the sphere, and let the direction point towards 
        // the centre of the sphere.
        final Vector3 point = new Vector3(-20, 5, 6);
        final Vector3 direction = point.multiply(-1);

        // do the raycast
        final double lambda = raycast.run(s1, null, point, direction, new Vector3(), new Vector3(), 0, envelope,
                epsilon, true);

        // we know the exact intersection point
        final Vector3 expected = point.normalize();

        // calculate the deviation of the returned point and the reference point (account for sphere sweeping)
        final double error = point.add(direction.multiply(lambda)).sub(expected).norm();

        System.out.println("lambda=" + lambda + " expected " + expected + " point "
                + point.add(direction.multiply(lambda)));

        // deviation from expected hitpoint should be lower than envelope+epsilon
        assertTrue(error < envelope + epsilon);
    }

    /**
     * A similar ray test where the sphere is moved off the origo
     */
    public void testRay2() {
        final RayCast raycast = new RayCast();

        // setup cube geometry
        final Sphere s1 = new Sphere(1);
        final Body b1 = new Body("b1");
        b1.addGeometry(Matrix3.identity(), new Vector3(), s1);
        b1.setPosition(0, -0, -0);
        b1.update();

        // pick a point outside the sphere, and let the direction point towards 
        // the centre of the sphere.
        final Vector3 point = new Vector3(0, 5, 0);
        final Vector3 direction = b1.getPosition().sub(point);

        final Vector3 hit = new Vector3();

        // do the raycast
        final double lambda = raycast.run(s1, null, point, direction, new Vector3(), hit, 0, envelope, epsilon, true);

        // we know the exact intersection point ( go from the centre of the sphere
        // to the boundary along the oposite ray direction )
        final Vector3 expected = b1.getPosition().sub(direction.normalize());

        // calculate the deviation of the returned point and the reference point
        final double error = point.add(direction.multiply(lambda)).sub(expected).norm();

        System.out.println("lambda=" + lambda + " expected " + expected + " point "
                + point.add(direction.multiply(lambda)) + " hit " + hit);

        System.out.println("error" + error);

        // deviation from expected hitpoint should be lower than envelope+epsilon
        assertTrue(error < envelope + epsilon);

    }

    /*
     * Something bad about this test, that needs to be fixed TODO
     */
    //	/**
    //	 * A sphere is placed at the origo, and a ray is shot in such a way, that it 
    //	 * exactly misses the sphere. Due to the envelope, we still expect a hit, 
    //	 * and this hitpoint should be within the envelope around the sphere. 
    //	 * Next, we move the sphere a bit, such that we expect a miss.
    //	 */
    //	public void testRay3() {
    //		RayCast raycast = new RayCast();
    //		
    //		// setup sphere geometry
    //		Sphere s1 = new Sphere(1);
    //		Body b1 = new Body("default");
    //		b1.addGeometry(Matrix3.identity(), new Vector3(), s1);
    //
    //
    //		// select a point (5,1,0) and the raydirection (-1,0,0)
    //		Vector3 point = new Vector3(5, 1, 0);
    //		Vector3 direction = new Vector3(-1,0,0);
    //		
    //		System.out.println("*********************************************************************");
    //		
    //		// do the raycast
    //		double lambda = raycast.run(s1, null, point, direction, new Vector3(), new Vector3(), 0, envelope, epsilon, false );
    //		
    //		// calculate the  point 
    //		Vector3 p = point.add(direction.multiply(lambda));
    //		
    //		System.out.println("p norm="+p.norm());
    //		
    //		// the hitpoint must be within the envelope
    //		assertTrue( Math.abs(p.norm()-1) < envelope+epsilon);
    //		
    //		// move the sphere a bit downwards
    //		b1.setPosition(0,-envelope-2*epsilon, 0);
    //		
    //		// do the raycast
    //		lambda = raycast.run(s1, null, point, direction, new Vector3(), new Vector3(), 0, envelope, epsilon, false );
    //		
    //		System.out.println("returned lambda="+lambda);
    //		
    //		assertTrue( lambda == Double.POSITIVE_INFINITY);
    //		
    //	}

    /**
     * A ray against box test
     */
    public void testRay4() {
        final RayCast raycast = new RayCast();

        // setup cube geometry
        final Box box = new Box("box", 1, 1, 1);
        final Body b1 = new Body("default");
        b1.addGeometry(Matrix3.identity(), new Vector3(), box);

        // select a point (5,1,0) and the raydirection (-1,0,0)
        final Vector3 point = new Vector3(0, 5, 0);
        final Vector3 direction = new Vector3(0, -1, 0);

        // do the raycast
        final double lambda = raycast.run(box, null, point, direction, new Vector3(), new Vector3(), 0, envelope,
                epsilon, false);

        // calculate the  point 
        final Vector3 p = point.add(direction.multiply(lambda));

        // expected point
        final Vector3 e = new Vector3(0, 0.5, 0);

        // the hitpoint must be within the envelope
        assertTrue(p.sub(e).norm() < envelope + epsilon);
    }

    /**
     * A ray against box corner test
     */
    public void testRay5() {
        final RayCast raycast = new RayCast();

        // setup cube geometry
        final Box box = new Box("box", 1, 1, 1);
        final Body b1 = new Body("default");
        b1.addGeometry(Matrix3.identity(), new Vector3(), box);

        // select a point (5,1,0) and the raydirection (-1,0,0)
        final Vector3 point = new Vector3(2, 5, 9);
        final Vector3 direction = new Vector3(0.5, 0.5, 0.5).sub(point);

        // do the raycast
        final double lambda = raycast.run(box, null, point, direction, new Vector3(), new Vector3(), 0, envelope,
                epsilon, false);

        // calculate the  point 
        final Vector3 p = point.add(direction.multiply(lambda));

        // expected point
        final Vector3 e = new Vector3(0.5, 0.5, 0.5);

        // the hitpoint must be within the envelope
        assertTrue(p.sub(e).norm() < envelope + epsilon);
    }

}
