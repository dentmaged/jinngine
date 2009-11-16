package jinngine.physics;

import java.util.Iterator;

import jinngine.collision.BroadfaseCollisionDetection;
import jinngine.geometry.contact.*;
import jinngine.util.*;
import jinngine.physics.force.*;
import jinngine.physics.constraint.*;
import jinngine.physics.solver.*;

/**
 * An interface to a physical simulation. Use methods to insert bodies, forces and constraints. The method tick() should be 
 * invoked to perform a single time step in the simulation. In every such time step, geometric and physical quantities are
 * updated in each body present in the simulation. This information can be read at any time, in order to visualise the configuration etc.  
 * @author mo
 *
 */
public interface Model {
	/**
	 * Insert a rigid body into the simulation
	 * @param body
	 */
	public void addBody(Body body);
	
	/**
	 * Remove an existing body from simulation
	 * @param body 
	 */
	public void removeBody(Body body);
	
	/**
	 * Get an iterator for all bodies in the model
	 * @return
	 */
	public Iterator<Body> getBodies(); 
	
	/**
	 * Add an acting force. 
	 * @param force
	 */
	public void addForce(Force force);
		
	/**
	 * Remove existing force
	 * @param force
	 */
	public void removeForce(Force force);
	
	/**
	 * Add a velocity constraint. 
	 * @param joint
	 */
	public void addConstraint(Pair<Body> pair, Constraint joint);

	/**
	 * Remove an existing velocity constraint
	 * @param joint
	 */
	public void removeConstraint(Pair<Body> pair);
	
	/**
	 * Add a new contact generator classifier to this model. This method should be used when the user 
	 * has created new geometry types, as well as new {@link ContactGenerator} types that can handle 
	 * pairs of this new geometry, and/or pairs of this new geometry and some more general existing geometry type. 
	 * <p>
	 * When the model encounters pairs geometries that are near each other, it uses its {@link ContactGeneratorClassifier} instances
	 * to create new ContactGenerators 
	 * @param classifier 
	 */
	public void addContactGeneratorClasifier( ContactGeneratorClassifier classifier );

	/**
	 * Return the broad-fase collision detection instance
	 * @return
	 */
	public BroadfaseCollisionDetection getBroadfase();
	
	/**
	 * Return the NCP solver
	 * @return
	 */
	public Solver getSolver();
	
	/**
	 * Set another NCP solver
	 * @return
	 */
	public void setSolver(Solver s);
	
	
	
	/**
	 * Set the time step size
	 * @param dt
	 */
	public void setDt( double dt );
	
	/**
	 * Get time-step size
	 * @return
	 */
	public double getDt();
	
	
	/**
	 * Perform a time step on this model
	 */
	public void tick();
}
