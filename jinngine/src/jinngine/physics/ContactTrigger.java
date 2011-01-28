/**
 * Copyright (c) 2010-2011 Morten Silcowitz
 *
 * This file is part of jinngine.
 *
 * jinngine is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://code.google.com/p/jinngine/>.
 */

package jinngine.physics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import jinngine.physics.constraint.Constraint;
import jinngine.physics.constraint.contact.ContactConstraint;
import jinngine.physics.constraint.contact.ContactConstraintManager;
import jinngine.physics.solver.Solver.NCPConstraint;
import jinngine.util.Pair;

/**
 * The contact trigger monitors a specific body for contact interactions. When the total contact force of some contact
 * interaction exceeds the user defined value, an event is triggered. Likewise, when the total force drops below the
 * threshold, or the contact vanishes all together, another event is triggered.
 */
public class ContactTrigger implements Trigger {

    private final Body body;
    private final double impulsethreshold;
    private final Callback callback;

    // helper variables
    private double totalforce = 0;
    private int numberOfNcpConstraints = 0;

    // handler to install in the contact constraint manager
    private ContactConstraintManager.Handler contactConstraintHandler;

    // constraints that have not triggered an event
    private final List<ContactConstraint> monitoredconstraints = new ArrayList<ContactConstraint>();

    // constraints that have triggered an event
    private final List<ContactConstraint> triggeredconstraints = new ArrayList<ContactConstraint>();

    // interface for callback from this trigger type
    public interface Callback {
        public void contactAboveThreshold(Body interactingBody, ContactConstraint constraint);

        public void contactBelowThreshold(Body interactingBody, ContactConstraint constraint);
    }

    /**
     * Create new contact trigger
     * 
     * @param body
     *            Body to monitor
     * @param impulsethreshold
     *            the total normal impulse exerted by the contact in last time-step
     */
    public ContactTrigger(final Body body, final double impulsethreshold, final ContactTrigger.Callback callback) {
        this.body = body;
        this.impulsethreshold = impulsethreshold;
        this.callback = callback;
    }

    @Override
    public void update(final Scene s) {
        // timestep = s.getTimestep();

        // see if the monitored constraints can trigger an event
        final ListIterator<ContactConstraint> monitored = monitoredconstraints.listIterator();
        while (monitored.hasNext()) {
            final ContactConstraint constraint = monitored.next();
            totalforce = 0;
            numberOfNcpConstraints = 0;

            // sum up the applied contact force
            for (final NCPConstraint ncp : constraint) {
                totalforce += ncp.lambda;
                numberOfNcpConstraints += 1;
            }

            // check condition
            if (totalforce > impulsethreshold) {

                // move constraint to triggered list
                monitored.remove();
                triggeredconstraints.add(constraint);

                // perform trigger event callback
                // Pair<Body> bodies = constraint.getBodies();
                // Body interacting = bodies.getFirst()==body? bodies.getSecond(): bodies.getFirst();
                final Body interacting = constraint.getBody1() == body ? constraint.getBody2() : body;
                callback.contactAboveThreshold(interacting, constraint);

            } // if force > forcethreshold
        } // for monitored constraints

        // see if triggered constraints should be moved back to monitored
        final ListIterator<ContactConstraint> triggered = triggeredconstraints.listIterator();
        while (triggered.hasNext()) {

            final ContactConstraint constraint = triggered.next();
            totalforce = 0;
            numberOfNcpConstraints = 0;

            // sum up the applied contact force
            // Iterator<NCPConstraint> ncps = constraint.getNcpConstraints();
            // while(ncps.hasNext()) {
            // NCPConstraint ncp = ncps.next();
            for (final NCPConstraint ncp : constraint) {
                totalforce += ncp.lambda;
                numberOfNcpConstraints += 1;
            }

            // check condition
            if (totalforce < impulsethreshold) {

                // move constraint to monitored list
                triggered.remove();
                monitoredconstraints.add(constraint);

                // perform trigger event callback
                // Pair<Body> bodies = constraint.getBodies();
                // Body interacting = bodies.getFirst()==body? bodies.getSecond(): bodies.getFirst();
                final Body interacting = constraint.getBody1() == body ? constraint.getBody2() : body;
                callback.contactBelowThreshold(interacting, constraint);

            } // if force > forcethreshold
        } // for monitored constraints
    }

    @Override
    public void cleanup(final Scene scene) {
        // remove the handler that we previously installed in the contact constraint manager
        final ContactConstraintManager manager = scene.getContactConstraintManager();
        manager.removeHandler(contactConstraintHandler);
    }

    @Override
    public void setup(final Scene scene) {
        // first of, find contact constraints that involves the body we want to monitor
        final Iterator<Constraint> iter = scene.getConnectedConstraints(body);
        while (iter.hasNext()) {
            final Constraint constraint = iter.next();
            // if the constraint is a contact constraint, add it to the monitoring list
            if (constraint instanceof ContactConstraint) {
                monitoredconstraints.add((ContactConstraint) constraint);
            }
        }// while constraints

        // setup a handler that tells us when contact constraints are created and deleted
        contactConstraintHandler = new ContactConstraintManager.Handler() {
            @Override
            public void contactConstraintCreated(final Pair<Body> bodies, final ContactConstraint contact) {
                // System.out.println("created");
                // if the constraint involves the body that we are monitoring, add it to our
                // internal list of contact constraints
                if (bodies.contains(body)) {
                    monitoredconstraints.add(contact);
                }
            }

            @Override
            public void contactConstraintRemoved(final Pair<Body> bodies, final ContactConstraint contact) {
                // System.out.println("deleted");

                if (bodies.contains(body)) {
                    // if we had this constraint on our list of triggerd constraints, signal an event
                    if (triggeredconstraints.contains(contact)) {

                        // perform the call back
                        final Body interacting = bodies.getFirst() == body ? bodies.getSecond() : bodies.getFirst();
                        callback.contactBelowThreshold(interacting, contact);

                        // remove from internal list
                        triggeredconstraints.remove(contact);
                    }

                    // if the constraint is just on the monitor list, simply remove it
                    if (monitoredconstraints.contains(contact)) {
                        // remove from monitor list
                        monitoredconstraints.remove(contact);
                    }

                } // if contains body
            }
        };

        // install the handler
        final ContactConstraintManager manager = scene.getContactConstraintManager();
        manager.addHandler(contactConstraintHandler);

    }

}
