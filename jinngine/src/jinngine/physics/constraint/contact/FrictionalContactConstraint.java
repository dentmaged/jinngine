/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
package jinngine.physics.constraint.contact;

import java.util.*;

import jinngine.geometry.contact.*;
import jinngine.geometry.contact.ContactGenerator.ContactPoint;
import jinngine.math.Matrix3;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.solver.Solver.NCPConstraint;
import jinngine.util.GramSchmidt;
import jinngine.util.Pair;


/**
 * A constraint the models a contact point between two bodies. A ContactConstraint acts 
 * like any other constraint/joint, for instance {@link BallInSocketJoint}. ContactConstraint uses one ore more
 * {@link ContactGenerator} instances to supply contact points and contact normals of the involved geometries. 
 * When two bodies are subject to a contact constraint, a ContactGenerator for each interacting geometry pair is required. 
 * Determining and instantiating these ContactGenerators should be handled by the simulator itself, however, one can create new 
 * and possibly optimised ContactGenerators for certain geometry pairs. A trivial example would be a ContactGenerator
 * for the Sphere-Sphere case, which is already implemented in Jinngine.
 *
 * @author mo
 *
 */
public final class FrictionalContactConstraint implements ContactConstraint {	
	private final Body b1, b2;                  //bodies in constraint
	private final List<ContactGenerator> generators = new ArrayList<ContactGenerator>();
	private final List<NCPConstraint>       ncpconstraints = new ArrayList<NCPConstraint>();
	private final List<ContactPoint>       contacts = new ArrayList<ContactPoint>();
	
	private double frictionBoundMagnitude = Double.POSITIVE_INFINITY;
	
	private boolean enableCoupling = true;
	
	public static double frictionStabilisation = 1.0;
	public static double normalStabilisation = 1.0;
	
	/**
	 * Create a new ContactConstraint, using one initial ContactGenerator
	 * @param b1
	 * @param b2
	 * @param generator
	 */
	public FrictionalContactConstraint(Body b1, Body b2, ContactGenerator generator) {
		super();
		this.b1 = b1;
		this.b2 = b2;
		this.generators.add(generator);
	}
	
	/**
	 * Add a new ContactGenerator for generating contact points and normal vectors
	 * @param g a new ContactGenerator
	 */
	public void addGenerator(ContactGenerator g) {
		this.generators.add(g);
	}
	
	/**
	 * Remove a contact generator
	 * @param g Previously added contact generator to be removed from this contact constraint
	 */
	public void removeGenerator(ContactGenerator g) {
		this.generators.remove(g);
	}
	
	/**
	 * Return the number of contact point generators
	 * @return
	 */
	public double getNumberOfGenerators() {
		return generators.size();
	}
	
	@Override
	public final void applyConstraints(ListIterator<NCPConstraint> constraintIterator, double dt) {
		// clear list of ncp constraints
//		ncpconstraints.clear();
		contacts.clear();

		// use ContactGenerators to create new contactpoints
		for ( ContactGenerator cg: generators) {
			//run contact generator
			cg.run();

			// generate contacts
			Iterator<ContactGenerator.ContactPoint> i = cg.getContacts();
			while (i.hasNext()) {
				ContactGenerator.ContactPoint cp = i.next();

				contacts.add(cp);
			}
		}



		ListIterator<NCPConstraint> ncpiter = ncpconstraints.listIterator();

		for (ContactPoint cp: contacts) {
			createFrictionalContactConstraint(cp, b1, b2, cp.point, cp.normal, cp.depth, dt, ncpiter);				
		}

		// kill remaining ncps
		while (ncpiter.hasNext()) {
			ncpiter.next(); 
			ncpiter.remove();
		}

		// add all ncps to output
		ncpiter = ncpconstraints.listIterator();
		while (ncpiter.hasNext()) {
			constraintIterator.add(ncpiter.next());
		}

	}


	// create a regular contact constraint including tangential friction
	public final void createFrictionalContactConstraint( 
			ContactGenerator.ContactPoint cp,
			Body b1, Body b2, Vector3 p, Vector3 n, double depth, double dt,
			ListIterator<NCPConstraint> outConstraints 
	) {
		

		// use a gram-schmidt process to create a orthonormal basis for the contact point ( normal and tangential directions)
		final Vector3 t1 = new Vector3(), t2 = new Vector3(), t3 = new Vector3();
		final Matrix3 B  = GramSchmidt.run(n);
		B.getColumnVectors(t1, t2, t3);

		// interaction points and jacobian for normal constraint
		final Vector3 r1 = p.sub(b1.state.position);
		final Vector3 r2 = p.sub(b2.state.position);

		// jacobians for normal direction
		final Vector3 nJ1 = n;
		final Vector3 nJ2 = r1.cross(n);
		final Vector3 nJ3 = n.negate();
		final Vector3 nJ4 = r2.cross(n).negate();

		// first off, create the constraint in the normal direction
		final double e = cp.restitution; // coefficient of restitution
		final double uni = nJ1.dot(b1.state.velocity) + nJ2.dot(b1.state.omega) + nJ3.dot(b2.state.velocity) + nJ4.dot(b2.state.omega);
		final double unf = uni<0 ? -e*uni: 0;		
		
		// compute B vector
		final Matrix3 I1 = b1.state.inverseinertia;
		final Matrix3 M1 = b1.state.inverseanisotropicmass;
		final Matrix3 I2 = b2.state.inverseinertia;
		final Matrix3 M2 = b2.state.inverseanisotropicmass;
		final Vector3 nB1 = M1.multiply(nJ1);
		final Vector3 nB2 = I1.multiply(nJ2);
		final Vector3 nB3 = M2.multiply(nJ3);
		final Vector3 nB4 = I2.multiply(nJ4);

		// clear out B's if mass is "infinity"
		if (b1.isFixed()) { nB1.assignZero(); nB2.assignZero(); }
		if (b2.isFixed()) { nB3.assignZero(); nB4.assignZero(); }

		// external forces acing at contact (obsolete, external forces are modelled using the delta velocities)
		// double Fext = B1.dot(b1.state.force) + B2.dot(b1.state.torque) + B3.dot(b2.state.force) + B4.dot(b2.state.torque);
		double correction = depth*(1/dt); // the true correction velocity. This velocity corrects the contact in the next timestep.
		final double escape = (cp.envelope-cp.distance)*(1/dt);
		final double lowerNormalLimit = 0;
		final double limit = 4*dt;
		
		
		// if the unf velocity will make the contact leave the envelope in the next timestep, 
		// we ignore corrections
		if (unf > escape) {
			//System.out.println("escape");
			correction = 0;
		} else {
			//even with unf, we stay inside the envelope
			//truncate correction velocity if already covered by repulsive velocity
			if (correction > 0) {
				if (unf > correction ) {
					correction = 0;
				} else {
					correction = correction - unf; // not sure this is smart TODO
				}
			}
		}
	
		// limit the correction velocity
		correction = correction< -limit? -limit:correction;  
		correction = correction>  limit?  limit:correction;
		
		// take a factor of real correction velocity
		correction = correction * 0.9 * normalStabilisation;
		
		//correction=correction>0?0:correction;

		final NCPConstraint c;

		if (outConstraints.hasNext()) {
			c = outConstraints.next();
		} else {
			c = new NCPConstraint();
			outConstraints.add(c);
		}

		double currentNormal = c.lambda;
		
		// the normal constraint
		c.assign(b1,b2,
				nB1, nB2, nB3, nB4,
				nJ1, nJ2, nJ3, nJ4,
				lowerNormalLimit, Double.POSITIVE_INFINITY,
				null,
			     -(unf-uni)-correction, -correction) ;
		
		// set distance (unused in simulator)
		c.distance = cp.distance;
		
		// normal-friction coupling 
		final NCPConstraint coupling = enableCoupling?c:null;
		
		// set the correct friction setting for this contact
		c.mu = cp.friction;
						
		// first tangent
		final Vector3 t2J1 = t2;
		final Vector3 t2J2 = r1.cross(t2);
		final Vector3 t2J3 = t2.negate();
		final Vector3 t2J4 = r2.cross(t2).negate();
		final Vector3 t2B1 = b1.isFixed()? new Vector3(): M1.multiply(t2J1);
		final Vector3 t2B2 = b1.isFixed()? new Vector3(): I1.multiply(t2J2);
		final Vector3 t2B3 = b2.isFixed()? new Vector3(): M2.multiply(t2J3);
		final Vector3 t2B4 = b2.isFixed()? new Vector3(): I2.multiply(t2J4);

		// then the tangential friction constraints 
		final double ut1i = t2J1.dot(b1.state.velocity) + t2J2.dot(b1.state.omega) + t2J3.dot(b2.state.velocity) + t2J4.dot(b2.state.omega);
	    double ut1f = 0;
		
		// double t2Fext = t2B1.dot(b1.state.FCm) + t2B2.dot(b1.state.tauCm) + t2B3.dot(b2.state.FCm) + t2B4.dot(b2.state.tauCm);
		final NCPConstraint c2;
		if (outConstraints.hasNext()) {
			c2 = outConstraints.next();
		} else {
			c2 = new NCPConstraint();
			outConstraints.add(c2);
		}

//		best
//		final double releaseLimit = 1e-4;
//		final double kickinFactor = 0.5;

		final double releaseLimit = 1e-5;
		final double kickinFactor = 0.6;
		final double kcor = 1;
		
		if (c2.sticking) {
			Vector3 cor = new Vector3();
			// correction vector
			cor.assign( b1.toWorld(c2.stickA).sub(b2.toWorld(c2.stickB)) );  
//			System.out.println("cor="+cor);

			ut1f = -t2J1.dot(cor)*kcor*frictionStabilisation;
		} else {
			ut1f=0;
		}
		
		// determine sticking
		if (c2.sticking) {
			
			if (currentNormal*c.mu -Math.abs(c2.lambda) < releaseLimit ) {
				c2.sticking = false;
			}			
		} else {	
			if (Math.abs(c2.lambda) < currentNormal*c.mu*kickinFactor) {
				// sticking
				c2.sticking = true;
				c2.stickA.assign(b1.toModel(cp.point));
				c2.stickB.assign(b2.toModel(cp.point));
			} 
		}
		


		
		
		c2.assign(b1,b2,
				t2B1, t2B2,	t2B3, t2B4,				
				t2J1, t2J2, t2J3, t2J4,
				-frictionBoundMagnitude, frictionBoundMagnitude,
				coupling,
				-(ut1f-ut1i),
				0
		);
		
		// second tangent
		final Vector3 t3J1 = t3;
		final Vector3 t3J2 = r1.cross(t3);
		final Vector3 t3J3 = t3.negate();
		final Vector3 t3J4 = r2.cross(t3).negate();
		final Vector3 t3B1 = b1.isFixed()? new Vector3(): M1.multiply(t3J1);
		final Vector3 t3B2 = b1.isFixed()? new Vector3(): I1.multiply(t3J2);
		final Vector3 t3B3 = b2.isFixed()? new Vector3(): M2.multiply(t3J3);
		final Vector3 t3B4 = b2.isFixed()? new Vector3(): I2.multiply(t3J4);

		final double ut2i = t3J1.dot(b1.state.velocity) + t3J2.dot(b1.state.omega) + t3J3.dot(b2.state.velocity) + t3J4.dot(b2.state.omega); 
	    double ut2f = 0;

		
		final NCPConstraint c3;
		if (outConstraints.hasNext()) {
			c3 = outConstraints.next();
		} else {
			c3 = new NCPConstraint();
			outConstraints.add(c3);
		}

		
		
		if (c3.sticking) {
			Vector3 cor = new Vector3();
			// correction vector
			cor.assign( b1.toWorld(c3.stickA).sub(b2.toWorld(c3.stickB)) );  
			ut2f = -t3J1.dot(cor)*kcor*frictionStabilisation;
		} else {
			ut2f=0;
		}
		
		// determine sticking
		if (c3.sticking) {
			if (currentNormal*coupling.mu - Math.abs(c3.lambda) < releaseLimit ) {
//				System.out.println("release");
				c3.sticking = false;
			} 			
		} else {	
			if (Math.abs(c3.lambda) < currentNormal*coupling.mu*kickinFactor) {
				// sticking
//				System.out.println("stick!");

				c3.sticking = true;
				c3.stickA.assign(b1.toModel(cp.point));
				c3.stickB.assign(b2.toModel(cp.point));
			} 
		}


		
		c3.assign(b1,b2,
				t3B1, t3B2,	t3B3, t3B4,
				t3J1, t3J2, t3J3, t3J4,
				-frictionBoundMagnitude, frictionBoundMagnitude,
				coupling,
				-(ut2f-ut2i), 0  
		);

//		outConstraints.add(c);
//		outConstraints.add(c2);
//		outConstraints.add(c3);
		
		// add to list
//		ncpconstraints.add(c);
//		ncpconstraints.add(c2);
//		ncpconstraints.add(c3);

	}

	@Override
	public final Pair<Body> getBodies() {
		return new Pair<Body>(b1,b2);
	}

	/**
	 * Specify whether a normal force magnitude coupling should be used on the friction force bounds.
	 * If not enabled, the bounds will be fixed.
	 * @param coupling
	 */
	public final void setCouplingEnabled( boolean coupling ) {
		this.enableCoupling = coupling;
	}
	
	/**
	 * Set the limits for fixed bound friction
	 * @param magnitude
	 */
	public final void setFixedFrictionBoundsMagnitude( double magnitude) {
		this.frictionBoundMagnitude  = magnitude;
	}

	@Override
	public final Iterator<NCPConstraint> getNcpConstraints() {
		return ncpconstraints.iterator();
	}

	@Override
	public final Iterator<ContactGenerator> getGenerators() {
		return generators.iterator();
	}
	
}
