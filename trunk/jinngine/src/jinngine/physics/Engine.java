/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
package jinngine.physics;
import java.util.*;
import java.util.Map.Entry;

import jinngine.physics.constraint.*;
import jinngine.physics.constraint.contact.*;	
import jinngine.physics.solver.*;
import jinngine.physics.solver.Solver.constraint;
import jinngine.physics.solver.experimental.NonsmoothNonlinearConjugateGradient;
import jinngine.geometry.contact.*;
import jinngine.collision.*;
import jinngine.geometry.*;
import jinngine.math.*;
import jinngine.physics.force.*;
import jinngine.util.*;
import jinngine.util.ComponentGraph.Component;

/**
 * A fixed time stepping rigid body simulator. It uses a contact graph to organise constraints, and generates
 * contact constraints upon intersecting/touching geometries. The engine is limited to having one constraint per. 
 * body pair. This means that if a joint constraint is present, there can be no contact constraints simultaneously. 
 * Such behaviour should be modelled using joint limits. 
 */
public final class Engine implements Model, Scene {
	// Bodies in model
	public final List<Body> bodies = new ArrayList<Body>();
	
	// List of geometry classifiers
	private final List<ContactGeneratorClassifier> geometryClassifiers = new ArrayList<ContactGeneratorClassifier>();
	
	// list of contact constraint creators
	private final List<ContactConstraintCreator> contactConstraintCreators = new ArrayList<ContactConstraintCreator>();

	// the default contact constraint creator
	private final ContactConstraintCreator defaultcreator = new ContactConstraintCreator() {
		public final ContactConstraint createContactConstraint(Body b1, Body b2, ContactGenerator g) {
			return new FrictionalContactConstraint(b1,b2,g,this);
//			return new SimplifiedContactConstraint(b1,b2,g,this);
			
		}
		@Override
		public void removeContactConstraint(ContactConstraint constraint) {
			// do nothing	
		}
	};
	
	// Constraints, joints and forces
	public final List<constraint> constraintList = new LinkedList<constraint>();  
	private final List<Force> forces = new LinkedList<Force>(); 
	
	// Create a contact graph classifier, used by the contact graph for determining
	// fixed bodies, i.e. bodies considered to have infinite mass. 
	private final HashMapComponentGraph.NodeClassifier<Body> classifier = 
		new HashMapComponentGraph.NodeClassifier<Body>() {
			public boolean isDelimitor(Body node) {
				return node.isFixed();
			}
	};
	
	// Create the contact graph using the classifier above
	private final ComponentGraph<Body,Constraint> contactGraph = new HashMapComponentGraph<Body,Constraint>(classifier);

	// Set of maintained contact constraints and generators
	private final Map<Pair<Body>,ContactConstraint> contactConstraints = new HashMap<Pair<Body>,ContactConstraint>();
	private final Map<Pair<Geometry>,ContactGenerator> contactGenerators = new HashMap<Pair<Geometry>,ContactGenerator>();
	
	// Setup a broad-phase handler. The handler ensures that ContactConstraints are properly inserted and 
	// removed from the contact graph, whenever the broad-phase collision detection detects overlaps and separations
	private final BroadphaseCollisionDetection.Handler handler = 
		new BroadphaseCollisionDetection.Handler() {
			@Override
			public final void overlap(Pair<Geometry> inputpair) {
				//retrieve the bodies associated with overlapping geometries
				Body a = inputpair.getFirst().getBody();
				Body b = inputpair.getSecond().getBody();
				
				//ignore overlaps stemming from the same body				
				if ( a == b) return;
				//ignore overlaps for non-body geometries
				if ( a == null || b == null) return;
				//ignore overlaps of fixed bodies
				if ( a.isFixed() && b.isFixed() ) return;

//				new Gear(g, new Vector3(-10,-5,-20), 25, 3);
//				new Gear(g, new Vector3(-10,-5,-20), 25, 3);

				
				//always order bodies and geometries the same way, so that normals 
				//will be pointing the right direction
				Pair<Body> bpair;
				Pair<Geometry> gpair;
				if ( a.hashCode() > b.hashCode() ) {
					bpair = new Pair<Body>(a,b);
					gpair = new Pair<Geometry>(inputpair.getFirst(), inputpair.getSecond());
				} else {
					bpair = new Pair<Body>(b,a);
					gpair = new Pair<Geometry>(inputpair.getSecond(), inputpair.getFirst());
				}
				
				
				ContactConstraint contactConstraint = null;

				//a contact constraint already exists 
				if (contactConstraints.containsKey(bpair)) {
					contactConstraint = contactConstraints.get(bpair);
															
					//add a new contact generator to this contact constraint
					ContactGenerator generator = getContactGenerator(gpair);
					contactGenerators.put(gpair, generator);
					contactConstraint.addGenerator(generator);

				//no contact constraint is present
				} else {					
					//do not act if some other constraint(joint) is already present
					//in the contact graph
					if (contactGraph.getEdge(bpair) == null)  {
						//create a new contact generator
						ContactGenerator generator = getContactGenerator(gpair);
						
						//create a new contact constraint
//						contactConstraint = new FrictionalContactConstraint(bpair.getFirst(),bpair.getSecond(),generator);
//						contactConstraint = new SimplifiedContactConstraint(bpair.getFirst(),bpair.getSecond(),generator);

						// try custom contact constraint generators
						for ( ContactConstraintCreator c : contactConstraintCreators) {
							contactConstraint = c.createContactConstraint(bpair.getFirst(), bpair.getSecond(), generator);
							if (contactConstraint != null)
								break;
						}
						
						// if no contact constraint was obtained, use the default creator
						if ( contactConstraint == null) {
							contactConstraint = defaultcreator.createContactConstraint(bpair.getFirst(), bpair.getSecond(), generator);
						}
								
						//insert into data structures
						contactConstraints.put(bpair, contactConstraint);
						contactGenerators.put(gpair, generator);
						contactGraph.addEdge( bpair, contactConstraint);
					}
				}
			}
			
			@Override
			public final void separation(Pair<Geometry> pair) {
				//retrieve the bodies associated with overlapping geometries
				Body a = pair.getFirst().getBody();
				Body b = pair.getSecond().getBody();
				Pair<Body> bpair = new Pair<Body>(a,b);

				//ignore overlaps stemming from the same body				
				if ( a == b) return;
				//ignore overlaps for non-body geometries
				if ( a == null || b == null) return;
				//ignore overlaps of fixed bodies
				if ( a.isFixed() && b.isFixed() ) return;
				
				//if this geometry pair has an acting contact constraint,
				//we must remove the contact generator
				if (contactConstraints.containsKey(bpair)) {
					//check that we have the generator (if not, something is very wrong)
					if (contactGenerators.containsKey(pair)) {
				
						//remove the generator from the contact constraint
						ContactConstraint constraint = contactConstraints.get(bpair);
						ContactGenerator cg = contactGenerators.get(pair);
						
						//notify contact generator
						cg.remove();
						
						// remove from contact constraint
						constraint.removeGenerator(cg);
						
						//remove the generator from our list
						contactGenerators.remove(pair);

						//if the contact constraint has no more generators, also
						//remove the contact constraint
						if (constraint.getNumberOfGenerators() < 1 ) {
							contactConstraints.remove(bpair);
							contactGraph.removeEdge(bpair);	
						}
						
						
					} else {
						//this is not good, report an error
						System.out.println("missing contact generator");
						System.exit(0);
					}
				} else {
					//TODO enable this and do tests
					//this is not good, report an error
					System.out.println("no constraint pressent");
					//System.exit(0);
				}
			}
	};
	
	// Broad phase collision detection implementation 
	private BroadphaseCollisionDetection broadfase = new SweepAndPrune(handler);
//	private BroadfaseCollisionDetection broadfase = new AllPairsTest(handler);

	//Create a linear complementarity problem solver
//	private Solver solver = new ProjectedGaussSeidel(35);
	private Solver solver = new NonsmoothNonlinearConjugateGradient(35, false);
	//time-step size
	private double dt = 0.01; 

	public Engine() {		
		// create some initial ContactGeneratorClassifiers
		// The Sphere - Sphere classifier
		geometryClassifiers.add(new ContactGeneratorClassifier() {
			@Override
			public final ContactGenerator getGenerator(Geometry a,
					Geometry b) {
				if ( a instanceof jinngine.geometry.Sphere && b instanceof jinngine.geometry.Sphere) {
					return new SphereContactGenerator((jinngine.geometry.Sphere)a, (jinngine.geometry.Sphere)b);
					//return new SamplingSupportMapContacts(a.getBody(),b.getBody(), (SupportMap3)a, (SupportMap3)b);
				}
				//not recognised
				return null;	
			}
		});
		
		// The Sphere - SupportMap classifier
		geometryClassifiers.add(new ContactGeneratorClassifier() {
			@Override
			public final ContactGenerator getGenerator(Geometry a,
					Geometry b) {
				if ( a instanceof jinngine.geometry.SupportMap3 && b instanceof jinngine.geometry.Sphere) {
					return new SupportMapSphereContactGenerator(a.getBody(), a, (jinngine.geometry.SupportMap3)a, b.getBody(), (jinngine.geometry.Sphere)b);
					//return new SamplingSupportMapContacts(a.getBody(),b.getBody(), (SupportMap3)a, (SupportMap3)b);
				}
				if ( a instanceof jinngine.geometry.Sphere && b instanceof jinngine.geometry.SupportMap3) {
					return new SupportMapSphereContactGenerator(a.getBody(), (jinngine.geometry.Sphere)a, b.getBody(), b, (jinngine.geometry.SupportMap3)b);
					//return new SamplingSupportMapContacts(a.getBody(),b.getBody(), (SupportMap3)a, (SupportMap3)b);
				}

				
				
				//not recognised
				return null;	
			}
		});
		
		
		// General convex support maps
		geometryClassifiers.add(new ContactGeneratorClassifier() {
			@Override
			public final ContactGenerator getGenerator(Geometry a,
					Geometry b) {
				if ( a instanceof SupportMap3 && b instanceof SupportMap3) {
//					return new FeatureSupportMapContactGenerator((SupportMap3)a, a,  (SupportMap3)b, b);
					return new SupportMapContactGenerator((SupportMap3)a, a,  (SupportMap3)b, b);
//					return new BulletNativeContactGenerator(a,b);
				}
				//not recognised
				return null;	
			}
		});
	}

	@Override
	public final void tick() {
		// Clear acting forces and auxillary variables
		for (Body c:bodies) {
			c.clearForces();
			c.deltavelocity.assign(Vector3.zero);
			c.deltaomega.assign(Vector3.zero);
		}

        // Apply all forces	
		for (Force f: forces) {
			f.apply(dt);
		}

		// Run the broad-phase collision detection (this automatically updates the contactGraph,
		// through the BroadfaseCollisionDetection.Handler type)
		broadfase.run();
		
		// Create a special iterator to be used with joints. All joints use this iterator to 
		// insert constraint entries into the constraintList
		constraintList.clear();
		ListIterator<constraint> constraintIterator = constraintList.listIterator();
				
		// Iterate through groups/components in the contact graph
		Iterator<ComponentGraph.Component> components = contactGraph.getComponents();		
		while (components.hasNext()) {
			//The component 
			ComponentGraph.Component g = components.next();
			
			Iterator<Constraint> constraints = contactGraph.getEdgesInComponent(g);
			while (constraints.hasNext()) {
				constraints.next().applyConstraints(constraintIterator, dt);
			}
		} //while groups

		
		//run the solver
		solver.solve( constraintList, bodies, 0.0 );
		
		// Apply delta velocities and integrate positions forward
		for (Body c : bodies ) {						
			// Apply computed forces to bodies
			if ( !c.isFixed() ) {
				c.state.velocity.assign( c.state.velocity.add( c.deltavelocity));
				c.state.omega.assign( c.state.omega.add( c.deltaomega));	
				Matrix3.multiply(c.state.inertia, c.state.omega, c.state.L);
				c.state.P.assign(c.state.velocity.multiply(c.state.mass));
			}

			//integrate forward on positions
			c.advancePositions(dt);
		}
	} //time-step


	@Override
	public void setTimestep(double dt) {
		this.dt = dt;
	}

	@Override
	public void addForce( Force f ) {
		forces.add(f);
	}

	@Override
	public void removeForce(Force f) {
		forces.remove(f);
	}

	@Override
	public void addBody( Body c) {
		bodies.add(c);
		c.updateTransformations();
		
		//install geometries into the broad-phase collision detection
		Iterator<Geometry> i = c.getGeometries();
		while (i.hasNext()) {
			Geometry g = i.next();
			broadfase.add(g);
		}
	}
	
	@Override
	public void addConstraint(Constraint joint) {
		contactGraph.addEdge(joint.getBodies(), joint);
	}
	
	@Override
	public Iterator<Constraint> getConstraints() {
		List<Constraint> list = new ArrayList<Constraint>();
		Iterator<Component> ci = contactGraph.getComponents();
		while(ci.hasNext()) {
			Iterator<Constraint> ei = contactGraph.getEdgesInComponent(ci.next());
			while(ei.hasNext())
				list.add(ei.next());
		}
			
		return list.iterator();
	}
	
	public final void removeConstraint(Constraint c) {
		if (c!=null) {
			contactGraph.removeEdge(c.getBodies());
		} else {
			System.out.println("Engine: attempt to remove null constraint");
		}
	}

	@Override
	public final BroadphaseCollisionDetection getBroadfase() {
		return broadfase;
	}  
	
	private ContactGenerator getContactGenerator(Pair<Geometry> pair) {
		for ( ContactGeneratorClassifier gc: geometryClassifiers) {
			ContactGenerator g = gc.getGenerator(pair.getFirst(), pair.getSecond());
			
			if (g!=null) {
				return g;
			}
		}
		return null;
	}
	
	/**
	 * Add a new ContactConstraintCreator
	 */
	public final void addContactConstraintCreator( ContactConstraintCreator c) {
		contactConstraintCreators.add(c);
	}

	/**
	 * Remove an existing contact constraint creator
	 * @param c
	 */
	public final void removeContactConstraintCreator( ContactConstraintCreator c) {
		contactConstraintCreators.remove(c);
	}

	@Override
	public final void removeBody(Body body) {
		//remove associated geometries from collision detection
		Iterator<Geometry> i = body.getGeometries();
		while( i.hasNext()) {
			broadfase.remove(i.next());			
		}
		
		//finally remove from body list
		bodies.remove(body);
		
	}

	@Override
	public final Solver getSolver() {
		return solver;
	}

	@Override
	public final void setSolver(Solver s) {
		solver = s;
	}

	@Override
	public Iterator<Body> getBodies() {
		return bodies.iterator();
	}

	@Override
	public void fixBody(Body b, boolean fixed) {
		// this may seem a bit drastic, but it is necessary. If one
		// just changes the fixed setting directly on bodies during animation,
		// really bad thing will happen, because the contact graph will become
		// corrupted and will eventually crash jinngine
		
		//check if the body is in the animation
		if (!bodies.contains(b))
			return;
		
		// check if body is already the at the correct 
		// fixed setting, in which case do nothing
		if (b.isFixed() == fixed) 
			return;
		
		// remove the body from simulation 
		removeBody(b);

		//change the fixed setting
		b.setFixed(fixed);

		// reinsert body
		addBody(b);
		
	}
	
	public Iterator<ContactConstraint> getContactConstraints() {
		// iterator for the entry set in contactConstraints
		final Iterator<Entry<Pair<Body>,ContactConstraint>> iter = contactConstraints.entrySet().iterator();
		
		//wrap the iterator into an iterator over contact constraints
		return new Iterator<ContactConstraint>() {
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}
			@Override
			public ContactConstraint next() {
				return iter.next().getValue();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		
	}

}
