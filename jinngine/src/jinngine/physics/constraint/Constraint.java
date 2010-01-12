package jinngine.physics.constraint;
import java.util.ListIterator;

import jinngine.physics.Body;
import jinngine.physics.solver.*;
import jinngine.physics.solver.Solver.constraint;
import jinngine.util.Pair;


public interface Constraint {
        /**
         * Insert the ConstraintEntries of this Constraint into the list modelled by iterator
         * @param iterator
         * @param dt
         */
        public void applyConstraints( ListIterator<constraint> iterator, double dt );
        
        /**
         * Return the pair of bodies that this constraint is acting upon 
         */
        public Pair<Body> getBodies();

}
