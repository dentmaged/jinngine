/**
 * Copyright (c) 2010-2011 Morten Silcowitz
 *
 * This file is part of jinngine.
 *
 * jinngine is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://code.google.com/p/jinngine/>.
 */

package jinngine.collision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jinngine.geometry.BoundingBox;
import jinngine.geometry.Geometry;
import jinngine.math.Vector3;
import jinngine.util.Pair;

/**
 * Sweep and Prune implementation of the {@link BroadphaseCollisionDetection} interface. Sweep and Prune is especially
 * effective in taking advantage of temporal coherence, i.e. the fact that a physical configurations changes only
 * slightly during one single time-step. If, on the other hand, Sweep and prune was to be applied to some obscure
 * configuration, where object positions would change wildly during each time step, it would perform very poorly. When
 * temporal coherence is high, the computation time is roughly linear in the number of objects.
 * 
 * @author moo
 * 
 */
public class SweepAndPrune implements BroadphaseCollisionDetection {
    private final int MAX_GEOMETRIES = 2500;
    private int geometries = 0;
    private final List<Handler> handlers = new ArrayList<Handler>();
    private final SweepPoint[] xAxis = new SweepPoint[MAX_GEOMETRIES];
    private final SweepPoint[] yAxis = new SweepPoint[MAX_GEOMETRIES];
    private final SweepPoint[] zAxis = new SweepPoint[MAX_GEOMETRIES];
    private final Map<Pair<Geometry>, Integer> counters = new LinkedHashMap<Pair<Geometry>, Integer>();
    private final Set<Pair<Geometry>> overlappingPairs = new LinkedHashSet<Pair<Geometry>>();
    private final Set<Pair<Geometry>> incomming = new LinkedHashSet<Pair<Geometry>>();
    private final Set<Pair<Geometry>> leaving = new LinkedHashSet<Pair<Geometry>>();

    public SweepAndPrune() {}

    /**
     * 
     * @param handler
     *            A handler to receive events from the sweep and prune implementation
     */
    public SweepAndPrune(final Handler handler) {
        // this.contactGraph = graph;
        handlers.add(handler);
    }

    @Override
    public void add(final Geometry a) {
        // System.out.println("Geometry added");
        // insert sweep points
        xAxis[geometries * 2] = new SweepPoint(a, 0, true);
        xAxis[geometries * 2 + 1] = new SweepPoint(a, 0, false);
        yAxis[geometries * 2] = new SweepPoint(a, 1, true);
        yAxis[geometries * 2 + 1] = new SweepPoint(a, 1, false);
        zAxis[geometries * 2] = new SweepPoint(a, 2, true);
        zAxis[geometries * 2 + 1] = new SweepPoint(a, 2, false);
        geometries++;
    }

    /**
     * Internal method. An implementation of insertion sort that observes when elements are interchanged.
     * 
     * @param A
     * @param counters
     * @param pairs
     */
    private final void sort(final SweepPoint[] A, final Map<Pair<Geometry>, Integer> counters,
            final Set<Pair<Geometry>> pairs, final Set<Pair<Geometry>> incomming, final Set<Pair<Geometry>> leaving) {
        for (int i = 1; i < geometries * 2; i++) {
            // SweepPoint pivot = a.get(i);
            final SweepPoint pivot = A[i];

            // TODO ineffective, sweep values are now updateded in each iteration to allow
            // changes in fixed and sleeping geometry at any time
            // if(!pivot.geometry.getBody().isFixed() && !pivot.geometry.getBody().sleepy )
            pivot.updateValue();

            final double ei = pivot.value;

            int j = i - 1;
            while (j >= 0) {
                // SweepPoint e = a.get(j);
                final SweepPoint e = A[j];

                // if ( e.geometry.getBody() != null )
                // if(!e.geometry.getBody().isFixed() && !e.geometry.getBody().sleepy)
                e.updateValue();

                if (e.value > ei) {
                    // System.out.println(e.value + " > " + ei);
                    // System.out.println("Event");

                    // swap elements a(j) and a(j+1), and decrement j
                    A[j + 1] = A[j];
                    A[j] = pivot;

                    j--;

                    // handle counters
                    if (!e.begin && pivot.begin) {
                        // an end-point was put before a begin point, we increment
                        Integer counter = counters.get(new Pair<Geometry>(e.geometry, pivot.geometry));
                        if (counter == null) {
                            counter = new Integer(0);
                        }
                        counters.put(new Pair<Geometry>(e.geometry, pivot.geometry), ++counter);
                        // System.out.println("vounter="+counter);
                        // overlap was found
                        if (counter == 3) {
                            final Pair<Geometry> pair = new Pair<Geometry>(e.geometry, pivot.geometry);
                            pairs.add(pair);
                            incomming.add(pair);
                            leaving.remove(pair);
                        }
                    }

                    if (e.begin && !pivot.begin) {
                        // a begin point was put before an end point, we decrement
                        Integer counter = counters.get(new Pair<Geometry>(e.geometry, pivot.geometry));
                        if (counter == null) {
                            System.out.println("why does this happen?");
                            System.exit(0);
                            break;
                            // counter = new Integer(0);
                        }
                        counters.put(new Pair<Geometry>(e.geometry, pivot.geometry), --counter);
                        // System.out.println("vounter="+counter);
                        // overlap vanished
                        if (counter == 2) { // counter < 3 (but ==2 is more effective)
                            // O(k) operation
                            final Pair<Geometry> pair = new Pair<Geometry>(e.geometry, pivot.geometry);
                            pairs.remove(pair);
                            if (!incomming.remove(pair)) {
                                leaving.add(pair);
                            }

                        }
                    }

                } else {
                    // done
                    break;
                }
            }
        }
    }

    public Iterator<Pair<Geometry>> overlappingPairs() {
        return overlappingPairs.iterator();
    }

    @Override
    public void remove(final Geometry a) {
        // System.out.println("delete");
        // Mark deleted sweep points, deleted points will not report overlaps.
        // This is ofcourse not optimal, as the sweep points will remain inside
        // the SAP algorithm and consume resources. However, it is non trivial
        // to remove a sweep point. The naive way would be to reset counters,
        // remove the points and do a o(nlgn) sort and sweep the line to recalculate
        // the counters.

        // remove the deleted sweep points
        // int j=0;
        // for ( int i=0; i<geometries*2-j; i++) {
        // SweepPoint p = xAxis[i];
        //
        // //overwrite the deleted sweeppoint
        // if (p.geometry == a)
        // j++;
        //
        // xAxis[i] = xAxis[i+j];
        // }
        //
        // j=0;
        // for ( int i=0; i<geometries*2-j; i++) {
        // SweepPoint p = yAxis[i];
        //
        // //overwrite the deleted sweeppoint
        // if (p.geometry == a)
        // j++;
        //
        // xAxis[i] = xAxis[i+j];
        // }
        //
        // j=0;
        // for ( int i=0; i<geometries*2-j; i++) {
        // SweepPoint p = zAxis[i];
        //
        // //overwrite the deleted sweeppoint
        // if (p.geometry == a)
        // j++;
        //
        // xAxis[i] = xAxis[i+j];
        // }

        int i = 0;
        int j = 0;
        while (j < geometries * 2) {
            final SweepPoint p = xAxis[j];

            if (p.geometry == a) {
                j++;
                continue;
            }

            xAxis[i] = xAxis[j];
            i++;
            j++;
        }

        i = 0;
        j = 0;
        while (j < geometries * 2) {
            final SweepPoint p = yAxis[j];

            if (p.geometry == a) {
                j++;
                continue;
            }

            yAxis[i] = yAxis[j];
            i++;
            j++;
        }

        i = 0;
        j = 0;
        while (j < geometries * 2) {
            final SweepPoint p = zAxis[j];

            if (p.geometry == a) {
                j++;
                continue;
            }

            zAxis[i] = zAxis[j];
            i++;
            j++;
        }

        final Iterator<Pair<Geometry>> iter = overlappingPairs.iterator();
        while (iter.hasNext()) {
            final Pair<Geometry> gp = iter.next();
            // deleted geometry is part of overlaps
            if (gp.contains(a)) {
                counters.remove(gp);

                // invoke event handler to report
                // vanishing overlap
                for (final Handler handler : handlers) {
                    handler.separation(gp);
                }

                iter.remove();
            }
        }

        // one less geometry in the algorithm by now
        geometries--;
    }

    @Override
    public void run() {
        incomming.clear();
        leaving.clear();
        // Sort sweep lines
        sort(xAxis, counters, overlappingPairs, incomming, leaving);
        sort(yAxis, counters, overlappingPairs, incomming, leaving);
        sort(zAxis, counters, overlappingPairs, incomming, leaving);

        // report overlaps
        for (final Pair<Geometry> p : incomming) {
            for (final Handler handler : handlers) {
                handler.overlap(p);
            }
        }

        // report separations
        for (final Pair<Geometry> p : leaving) {
            for (final Handler handler : handlers) {
                handler.separation(p);
            }
        }

    }

    // inner private class SweepPoint
    private final class SweepPoint {

        public SweepPoint(final Geometry geo, final int axis, final boolean begin) {
            geometry = geo;
            aabb = geo;
            this.begin = begin;
            this.axis = axis;

            updateValue();
            // System.out.println("Sweep point value: " + value);

        }

        public final Geometry geometry;
        public final BoundingBox aabb;
        public final boolean begin;
        public final int axis;
        public double value;
        public boolean delete = false;

        public final void updateValue() {
            // get the correct axis bounds for each body's AABB
            final Vector3 thisBounds = new Vector3();
            if (begin) {
                aabb.getMinBounds(thisBounds);
            } else {
                thisBounds.assign(aabb.getMaxBounds(null));
            }

            value = thisBounds.get(axis);
            // thisBounds.print();
            // System.out.println("Sweep point value: " + value);

        }

    }

    @Override
    public void addHandler(final Handler h) {
        handlers.add(h);
    }

    @Override
    public void removeHandler(final Handler h) {
        handlers.remove(h);

    }

    @Override
    public Set<Pair<Geometry>> getOverlappingPairs() {
        return new HashSet<Pair<Geometry>>(overlappingPairs);
    }
}
