
package jinngine.collision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jinngine.geometry.Geometry;
import jinngine.math.Vector3;
import jinngine.util.Pair;

/**
 * Better Sweep-and-prune implementation, replacing the old SweepAndPrune.java
 */
public class SAP2 implements BroadphaseCollisionDetection {

    private final class SweepPoint {
        public final Geometry geo;
        public final boolean begin;
        public final int axis;
        public final Vector3 bounds = new Vector3();

        public SweepPoint(final Geometry geo, final boolean begin, final int axis) {
            super();
            this.geo = geo;
            this.begin = begin;
            this.axis = axis;

            // do an initial check on value
            if (Double.isNaN(value())) {
                throw new IllegalStateException("Geometry has a NaN as bounding value");
            }
        }

        public final double value() {
            if (begin) {
                return geo.getMinBounds(bounds).get(axis);
            } else {
                return geo.getMaxBounds(bounds).get(axis);
            }
        }
    }

    private final class Counter {
        public int overlaps;
    }

    private final ArrayList<Handler> handlers = new ArrayList<Handler>();
    private final ArrayList<Pair<Geometry>> overlaps = new ArrayList<Pair<Geometry>>();
    private final Map<Pair<Geometry>, Counter> counters = new HashMap<Pair<Geometry>, Counter>();
    private final ArrayList<Geometry> geometries = new ArrayList<Geometry>();
    private final ArrayList<SweepPoint> axis1 = new ArrayList<SweepPoint>();
    private final ArrayList<SweepPoint> axis2 = new ArrayList<SweepPoint>();
    private final ArrayList<SweepPoint> axis3 = new ArrayList<SweepPoint>();

    private final void sortAxis(final ArrayList<SweepPoint> axis) {
        // insertion sort from Cormen et al, Introduction to Algorithms
        for (int j = 1; j < axis.size(); j++) {
            final SweepPoint keyelement = axis.get(j);
            final double key = keyelement.value();

            if (Double.isNaN(key)) {
                throw new IllegalStateException("Geometry has NaN in its bounding box values"
                        + keyelement.geo.getBody().identifier);
            }

            int i = j - 1;
            while (i >= 0 && axis.get(i).value() > key) {
                // swap
                final SweepPoint swapper = axis.get(i);

                if (keyelement.begin && !swapper.begin) {
                    // increment overlap (end before begin)
                    final Pair<Geometry> pair = new Pair<Geometry>(keyelement.geo, swapper.geo);
                    if (counters.containsKey(pair)) {
                        final Counter c = counters.get(pair);
                        c.overlaps++;

                        // check for overlap condition
                        if (c.overlaps == 3) {
                            // add the overlap
                            overlaps.add(pair);

                            // notify handlers
                            for (final Handler h : handlers) {
                                h.overlap(pair);
                            }
                        }

                    } else {
                        final Counter counter = new Counter();
                        counter.overlaps = 1;
                        counters.put(pair, counter);
                    }
                }

                if (!keyelement.begin && swapper.begin) {
                    // decrement overlap (begin before end)
                    final Pair<Geometry> pair = new Pair<Geometry>(keyelement.geo, swapper.geo);

                    if (counters.containsKey(pair)) {
                        final Counter c = counters.get(pair);
                        c.overlaps--;

                        // check for a separation condition
                        if (c.overlaps == 2) {
                            // add the overlap
                            overlaps.remove(pair);

                            // notify handlers
                            for (final Handler h : handlers) {
                                h.separation(pair);
                            }
                        } else if (c.overlaps == 0) {
                            // remove the counter from the map
                            counters.remove(pair);
                        }

                    } else {
                        // ignore this case
                        System.out.println("hmm?");
                    }
                }

                axis.set(i + 1, swapper);
                i = i - 1;
            }
            axis.set(i + 1, keyelement);
        }
    }

    @Override
    public void add(final Geometry g) {
        if (!geometries.contains(g)) {
            geometries.add(g);

            // create new sweep points
            axis1.add(new SweepPoint(g, true, 0));
            axis1.add(new SweepPoint(g, false, 0));
            axis2.add(new SweepPoint(g, true, 1));
            axis2.add(new SweepPoint(g, false, 1));
            axis3.add(new SweepPoint(g, true, 2));
            axis3.add(new SweepPoint(g, false, 2));

        } else {
            throw new IllegalArgumentException("Given geometry already exsist");
        }
    }

    @Override
    public void addHandler(final Handler h) {
        handlers.add(h);
    }

    @Override
    public Set<Pair<Geometry>> getOverlappingPairs() {
        return new HashSet<Pair<Geometry>>(overlaps);
    }

    @Override
    public void remove(final Geometry g) {
        if (geometries.contains(g)) {
            // remove
            geometries.remove(g);

            // remove sweep points
            removeSweepPoint(axis1, g);
            removeSweepPoint(axis2, g);
            removeSweepPoint(axis3, g);

            // go through counters, and delete the ones that involve g. If the counter var
            // an overlapping counter, signal an separation event
            final Iterator<Entry<Pair<Geometry>, Counter>> iter = counters.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<Pair<Geometry>, Counter> entry = iter.next();
                final Counter c = entry.getValue();
                final Pair<Geometry> pair = entry.getKey();
                if (pair.getFirst() == g || pair.getSecond() == g) {
                    if (c.overlaps == 3) {
                        // notify handlers
                        for (final Handler h : handlers) {
                            h.separation(pair);
                        }

                        overlaps.remove(pair);
                    }

                    // remove counter
                    iter.remove();
                }
            }
        } else {
            throw new IllegalArgumentException("Given geometry does not exist");
        }

    }

    private final void removeSweepPoint(final List<SweepPoint> list, final Geometry g) {
        final ListIterator<SweepPoint> iter = list.listIterator();
        while (iter.hasNext()) {
            final SweepPoint p = iter.next();
            if (p.geo == g) {
                iter.remove();
            }
        }
    }

    @Override
    public void removeHandler(final Handler h) {
        handlers.remove(h);
    }

    @Override
    public void run() {
        // sort each axis and update counters
        sortAxis(axis1);
        sortAxis(axis2);
        sortAxis(axis3);

        // int countingOverlaps = 0;
        // // go through all counters
        // Iterator<Entry<Pair<Geometry>,Counter>> iter = counters.entrySet().iterator();
        // while (iter.hasNext()) {
        // Entry<Pair<Geometry>,Counter> entry = iter.next();
        // Counter c = entry.getValue();
        // Pair<Geometry> pair = entry.getKey();
        //
        // if (c.overlaps == 3)
        // countingOverlaps ++;
        //
        // if (c.wasOverlapping) {
        // // report separation
        // if (c.overlaps < 3 ) {
        // overlaps.remove(pair);
        // c.wasOverlapping = false;
        //
        // // notify handlers
        // for (Handler h: handlers) {
        // h.separation(pair);
        // }
        // }
        // } else {
        // // report overlap
        // if (c.overlaps > 2) {
        // overlaps.add(pair);
        // c.wasOverlapping = true;
        //
        // // notify handlers
        // for (Handler h: handlers) {
        // h.overlap(pair);
        // }
        //
        // }
        // } // if was overlapping
        //
        // // if counter is zero at this
        // // point, remove it
        // if (c.overlaps < 1) {
        // iter.remove();
        // }
        // } // for all counters
    }

}
