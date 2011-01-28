/**
 * Copyright (c) 2010-2011 Morten Silcowitz
 *
 * This file is part of jinngine.
 *
 * jinngine is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://code.google.com/p/jinngine/>.
 */

package jinngine.physics.constraint.contact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jinngine.collision.BroadphaseCollisionDetection;
import jinngine.geometry.Geometry;
import jinngine.geometry.SupportMap3;
import jinngine.geometry.contact.ContactGenerator;
import jinngine.geometry.contact.ContactGeneratorClassifier;
import jinngine.geometry.contact.SupportMapContactGenerator;
import jinngine.physics.Body;
import jinngine.physics.DefaultScene;
import jinngine.physics.Scene;
import jinngine.physics.constraint.Constraint;
import jinngine.util.ComponentGraph;
import jinngine.util.Pair;

/**
 * Maintains constraints in a graph structure
 */
public class DefaultContactConstraintManager implements ContactConstraintManager {
    private final List<Handler> handlers = new ArrayList<Handler>();

    // the external broadphase collision detection
    private final BroadphaseCollisionDetection broadphase;

    // the broad phase collision handler installed by this contact constraint manager
    private final BroadphaseCollisionDetection.Handler handler;

    // list of geometry classifiers
    private final List<ContactGeneratorClassifier> geometryClassifiers = new ArrayList<ContactGeneratorClassifier>();

    // list of contact constraint creators
    private final List<ContactConstraintCreator> contactConstraintCreators = new ArrayList<ContactConstraintCreator>();

    // set of maintained contact constraints and generators
    private final Map<Pair<Body>, ContactConstraint> contactConstraints = new LinkedHashMap<Pair<Body>, ContactConstraint>();
    private final Map<Pair<Geometry>, ContactGenerator> contactGenerators = new LinkedHashMap<Pair<Geometry>, ContactGenerator>();

    // the constraint graph to be updated by this contact constraint manager
    private final ComponentGraph<Body, Constraint, DefaultScene.ConstraintGroup> constraintGraph;

    // the default contact constraint creator
    private final ContactConstraintCreator defaultcreator = new ContactConstraintCreator() {
        @Override
        public final ContactConstraint createContactConstraint(final Body b1, final Body b2, final ContactGenerator g) {
            return new FrictionalContactConstraint(b1, b2, g);
            // return new SimplifiedContactConstraint(b1,b2,g);
            // return new StabilisationContactConstraint(b1,b2,g);
            // return new BaumgardeContactConstraint(b1,b2,g);
            //
        }

        @Override
        public void removeContactConstraint(final ContactConstraint constraint) {}
    };

    public DefaultContactConstraintManager(final BroadphaseCollisionDetection broadphase,
            final ComponentGraph<Body, Constraint, DefaultScene.ConstraintGroup> constraintgraph) {

        // store the broadphase and the constraint graph references
        this.broadphase = broadphase;
        constraintGraph = constraintgraph;

        // Setup a broad-phase handler. The handler ensures that ContactConstraints are properly inserted and
        // removed from the contact graph, whenever the broad-phase collision detection detects overlaps and separations
        handler = new BroadphaseCollisionDetection.Handler() {
            @Override
            public final void overlap(final Pair<Geometry> inputpair) {
                // System.out.println("overlap");
                // retrieve the bodies associated with overlapping geometries
                final Body b1 = inputpair.getFirst().getBody();
                final Body b2 = inputpair.getSecond().getBody();

                // ignore overlaps stemming from the same body
                if (b1 == b2) {
                    return;
                }
                // ignore overlaps for non-body geometries
                if (b1 == null || b2 == null) {
                    return;
                }
                // ignore overlaps of fixed bodies
                if (b1.isFixed() && b2.isFixed()) {
                    return;
                }

                // always order bodies and geometries the same way, so that normals
                // will be pointing the right direction
                Pair<Body> bodypair;
                Pair<Geometry> geometrypair;

                bodypair = new Pair<Body>(b1, b2);

                // if ( b1.hashCode() > b2.hashCode() ) {
                // bodypair = new Pair<Body>(b1,b2);
                // geometrypair = new Pair<Geometry>(inputpair.getFirst(), inputpair.getSecond());
                // } else {
                // bodypair = new Pair<Body>(b2,b1);
                // geometrypair = new Pair<Geometry>(inputpair.getSecond(), inputpair.getFirst());
                // }

                ContactConstraint contactConstraint = null;

                // a contact constraint already exists
                if (contactConstraints.containsKey(bodypair)) {
                    contactConstraint = contactConstraints.get(bodypair);

                    // order the geometry pair to match the order of the contact constraint.
                    // this is necessary to keep normals pointing in the right direction when
                    // contact constraints have more than one contact generator
                    final Pair<Body> orderedpair = new Pair<Body>(contactConstraint.getBody1(),
                            contactConstraint.getBody2());
                    if (orderedpair.getFirst() == bodypair.getFirst()) {
                        // same order
                        geometrypair = inputpair;
                    } else {
                        // swap
                        geometrypair = new Pair<Geometry>(inputpair.getSecond(), inputpair.getFirst());
                    }

                    // add a new contact generator to this contact constraint
                    final ContactGenerator generator = getContactGenerator(geometrypair);
                    contactGenerators.put(geometrypair, generator);
                    contactConstraint.addGenerator(generator);

                    // no contact constraint is present
                } else {
                    // do not act if some other constraint(joint) is already present
                    // in the contact graph
                    // if (constraintGraph.getEdge(bodypair) == null) {
                    if (!constraintGraph.containsEdge(b1, b2)) {

                        // create a new contact generator
                        final ContactGenerator generator = getContactGenerator(inputpair);

                        // try custom contact constraint generators
                        for (final ContactConstraintCreator c : contactConstraintCreators) {
                            contactConstraint = c.createContactConstraint(bodypair.getFirst(), bodypair.getSecond(),
                                    generator);
                            if (contactConstraint != null) {
                                break;
                            }
                        }

                        // if no contact constraint was obtained, use the default creator
                        if (contactConstraint == null) {
                            contactConstraint = defaultcreator.createContactConstraint(bodypair.getFirst(),
                                    bodypair.getSecond(), generator);
                        }

                        // insert into data structures
                        contactConstraints.put(bodypair, contactConstraint);
                        contactGenerators.put(inputpair, generator);
                        constraintGraph.addEdge(bodypair, contactConstraint);

                        // signal handlers
                        for (final Handler h : handlers) {
                            h.contactConstraintCreated(bodypair, contactConstraint);
                        }

                    }
                }
            }

            @Override
            public final void separation(final Pair<Geometry> geometrypair) {
                // retrieve the bodies associated with overlapping geometries
                final Body a = geometrypair.getFirst().getBody();
                final Body b = geometrypair.getSecond().getBody();
                final Pair<Body> bodypair = new Pair<Body>(a, b);

                // ignore overlaps stemming from the same body
                if (a == b) {
                    return;
                }
                // ignore overlaps for non-body geometries
                if (a == null || b == null) {
                    return;
                }
                // ignore overlaps of fixed bodies
                if (a.isFixed() && b.isFixed()) {
                    return;
                }

                // if this geometry pair has an acting contact constraint,
                // we must remove the contact generator
                if (contactConstraints.containsKey(bodypair)) {
                    // check that we have the generator (if not, something is very wrong)
                    if (contactGenerators.containsKey(geometrypair)) {

                        // remove the generator from the contact constraint
                        final ContactConstraint constraint = contactConstraints.get(bodypair);
                        final ContactGenerator cg = contactGenerators.get(geometrypair);

                        // notify contact generator (for possible clean-up)
                        cg.remove();

                        // remove from contact constraint
                        constraint.removeGenerator(cg);

                        // remove the generator from our list
                        contactGenerators.remove(geometrypair);

                        // if the contact constraint has no more generators, also
                        // remove the contact constraint
                        if (constraint.getNumberOfGenerators() < 1) {
                            contactConstraints.remove(bodypair);
                            constraintGraph.removeEdge(bodypair);

                            // signal event handlers
                            for (final Handler h : handlers) {
                                h.contactConstraintRemoved(bodypair, constraint);
                            }
                        }

                    } else {
                        // attempt to remove a contact generator that was not there
                        throw new IllegalStateException(
                                "DefaultContactConstraintManager: contact generator missing from ContactConstraint?");
                    }
                } else {
                    // no contact constraint present
                    // this could be due to bodies that are connected with other constraints
                    // than contact constraints.
                    // System.out.println("no constraint pressent");
                    // System.exit(0);
                }
            } // separation()
        }; // new broadphase handler

        // add the handler to the broad phase
        broadphase.addHandler(handler);

        // create some initial ContactGeneratorClassifiers

        // // The Sphere - Sphere classifier
        // geometryClassifiers.add(new ContactGeneratorClassifier() {
        // @Override
        // public final ContactGenerator getGenerator(Geometry a,
        // Geometry b) {
        // if ( a instanceof jinngine.geometry.Sphere && b instanceof jinngine.geometry.Sphere) {
        // return new SphereContactGenerator((jinngine.geometry.Sphere)a, (jinngine.geometry.Sphere)b);
        // }
        // //not recognised
        // return null;
        // }
        // });

        // The Sphere - SupportMap classifier
        // geometryClassifiers.add(new ContactGeneratorClassifier() {
        // @Override
        // public final ContactGenerator getGenerator(Geometry a,
        // Geometry b) {
        // if ( a instanceof jinngine.geometry.SupportMap3 && b instanceof jinngine.geometry.Sphere) {
        // return new SupportMapSphereContactGenerator(a.getBody(), a, (jinngine.geometry.SupportMap3)a, b.getBody(),
        // (jinngine.geometry.Sphere)b);
        // }
        // if ( a instanceof jinngine.geometry.Sphere && b instanceof jinngine.geometry.SupportMap3) {
        // return new SupportMapSphereContactGenerator(a.getBody(), (jinngine.geometry.Sphere)a, b.getBody(), b,
        // (jinngine.geometry.SupportMap3)b);
        // }
        // //not recognised
        // return null;
        // }
        // });

        // General convex support maps
        geometryClassifiers.add(new ContactGeneratorClassifier() {
            @Override
            public final ContactGenerator getGenerator(final Geometry a, final Geometry b) {
                if (a instanceof SupportMap3 && b instanceof SupportMap3) {
                    return new SupportMapContactGenerator((SupportMap3) a, a, (SupportMap3) b, b);
                }
                // not recognised
                return null;
            }
        });

    }

    /**
     * Remove the broad-phase collision detection handler, that was previously installed by this contact constraint
     * manager
     */
    public void removeBroadphaseHandler() {
        broadphase.removeHandler(handler);
    }

    /**
     * Add a new ContactConstraintCreator
     */
    public final void addContactConstraintCreator(final ContactConstraintCreator c) {
        contactConstraintCreators.add(c);
    }

    /**
     * Remove an existing contact constraint creator
     * 
     * @param c
     */
    public final void removeContactConstraintCreator(final ContactConstraintCreator c) {
        contactConstraintCreators.remove(c);
    }

    private ContactGenerator getContactGenerator(final Pair<Geometry> pair) {
        for (final ContactGeneratorClassifier gc : geometryClassifiers) {
            final ContactGenerator g = gc.getGenerator(pair.getFirst(), pair.getSecond());

            if (g != null) {
                return g;
            }
        }
        return null;
    }

    @Override
    public void cleanup(final Scene scene) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setup(final Scene scene) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addHandler(final Handler handler) {
        handlers.add(handler);
    }

    @Override
    public void removeHandler(final Handler handler) {
        handlers.remove(handler);
    }

}
