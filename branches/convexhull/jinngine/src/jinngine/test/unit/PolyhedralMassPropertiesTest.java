/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available
 * at http://www.gnu.org/copyleft/gpl.html.
 */
package jinngine.test.unit;

import java.util.ArrayList;
import java.util.List;
import jinngine.geometry.util.PolyhedralMassProperties;
import jinngine.math.InertiaMatrix;
import jinngine.math.Matrix3;
import jinngine.math.Vector3;
import org.junit.Test;
import static junit.framework.Assert.*;
import quickhull3d.QuickHull3D;

/**
 *
 * @author Pierre LABATUT
 */
public class PolyhedralMassPropertiesTest {

    /**
     * Asser that the 9 dimension of a given matrix are clsoe to a reference
     * @param exprected reference not null matrix
     * @param actual tested not null matrix
     * @param delta tolerance of the test
     */
    private static void assertMatrixEquals(Matrix3 exprected, Matrix3 actual, double delta) {
        assertEquals(exprected.a11, actual.a11, delta);
        assertEquals(exprected.a12, actual.a12, delta);
        assertEquals(exprected.a13, actual.a13, delta);
        assertEquals(exprected.a21, actual.a21, delta);
        assertEquals(exprected.a22, actual.a22, delta);
        assertEquals(exprected.a23, actual.a23, delta);
        assertEquals(exprected.a31, actual.a31, delta);
        assertEquals(exprected.a32, actual.a32, delta);
        assertEquals(exprected.a33, actual.a33, delta);
    }

    @Test
    public void testCube() {
        /**
         * Cube dimensions
         */
        final double xs = 5;
        final double ys = 7;
        final double zs = 11;

        /**
         * Compute a reference inertia matrix and mass
         */
        final double massRef = xs * ys * zs;
        final Matrix3 inertiaRef = Matrix3.scaleMatrix(
                (1.0f / 12.0f) * (ys * ys + zs * zs) * massRef,
                (1.0f / 12.0f) * (xs * xs + zs * zs) * massRef,
                (1.0f / 12.0f) * (ys * ys + xs * xs) * massRef);

        /**
         * Create grometry standing for the cube
         */
        final List<Vector3> vectors = new ArrayList<Vector3>();
        vectors.add(new Vector3(-xs * .5, -ys * .5, -zs * .5));
        vectors.add(new Vector3(+xs * .5, -ys * .5, -zs * .5));
        vectors.add(new Vector3(-xs * .5, +ys * .5, -zs * .5));
        vectors.add(new Vector3(-xs * .5, -ys * .5, +zs * .5));
        vectors.add(new Vector3(+xs * .5, +ys * .5, -zs * .5));
        vectors.add(new Vector3(-xs * .5, +ys * .5, +zs * .5));
        vectors.add(new Vector3(+xs * .5, -ys * .5, +zs * .5));
        vectors.add(new Vector3(+xs * .5, +ys * .5, +zs * .5));
        /**
         * Compute mass properties PolyhedralMassProperties
         */
        final QuickHull3D q = new QuickHull3D(Vector3.toArray(vectors));
        final PolyhedralMassProperties pmp = new PolyhedralMassProperties(q, 1.);
        final InertiaMatrix actualInertia = pmp.getInertiaMatrix();
        final double actualMass = pmp.getMass();

        assertEquals(massRef, actualMass, 1E-4);
        assertMatrixEquals(inertiaRef, actualInertia, 1E-3);

    }

    @Test
    public void testSphere() {
        /**
         * Sphere dimension
         */
        final double radius = 5;
        /**
         * Compute a reference
         */
        final double massRef = (4.0 / 3.0) * Math.PI * radius * radius * radius;
        final InertiaMatrix inertiaRef = new InertiaMatrix();
        inertiaRef.assignScale((2 / 5f) * radius * radius*massRef );


        /**
         * Create sphere grometry
         * for alpha from -PI to PT in 128 steps
         * for beta from -PI/2 to PT/2 in 64 steps
         * (x' ,y' ,z' ) = rotation(z,alpha) * (x ,y ,z )
         * (x'',y'',z'') = rotation(y',beta) * (x',y',z')
         */
        final List<Vector3> vectors = new ArrayList<Vector3>(); // resulting vertex
        double inc = Math.PI / 64.; // angular increment
        for (double beta = -Math.PI * .5; beta < Math.PI * .5; beta += inc) {
            for (double alpha = -Math.PI; alpha < Math.PI; alpha += inc) {
                double ca = Math.cos(beta) * 5.;
                double sa = Math.sin(beta) * 5.;
                vectors.add(new Vector3(Math.cos(alpha) * ca, Math.sin(alpha) * ca, sa));
            }
        }
        
        /**
         * Compute mass properties with PolyhedralMassProperties
         */
        final QuickHull3D q = new QuickHull3D(Vector3.toArray(vectors));
        final PolyhedralMassProperties pmp = new PolyhedralMassProperties(q, 1.);
        final InertiaMatrix actualInertia = pmp.getInertiaMatrix();
        final double actualMass = pmp.getMass();


        assertEquals(massRef, actualMass, 1.);
        assertMatrixEquals(inertiaRef, actualInertia, 10.);
    }
}
