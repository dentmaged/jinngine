package jinngine.physics.constraint;

import java.util.*;

import jinngine.geometry.contact.*;
import jinngine.math.Matrix3;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.physics.solver.*;
import jinngine.util.GramSchmidt;


/**
 * A constraint the models contact region forces, acting between two bodies. A ContactConstraint acts 
 * like any other constraint/joint, for instance {@link BallInSocketJoint}. ContactConstraint uses one ore more
 * {@link ContactGenerator} instances to supply contact points and contact normals of the involved geometries. 
 * When two bodies are subject to a contact constraint, a ContactGenerator for each interacting geometry pair is required. 
 * Determining and instantiating these ContactGenerators should be handled by the simulator itself, however, one can create new 
 * and possibly optimised ContactGenerators for certain geometry pairs. A trivial example would be a ContactGenerator
 * for the Sphere-Sphere case, which is already implemented in Jinngine, see ({@link SphereContactGenerator}).
 *
 * @author mo
 *
 */
public interface ContactConstraint extends Constraint {	

	/**
	 * Add a new ContactGenerator for generating contact points and normal vectors
	 * @param g a new ContactGenerator
	 */
	public void addGenerator(ContactGenerator g);
	
	/**
	 * Remove a contact generator
	 * @param g Previously added contact generator to be removed from this contact constraint
	 */
	public void removeGenerator(ContactGenerator g);
	
	/**
	 * Return the number of contact point generators
	 * @return
	 */
	public double getNumberOfGenerators();	

}
