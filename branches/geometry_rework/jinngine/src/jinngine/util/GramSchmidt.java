/**
 * Copyright (c) 2010-2011 Morten Silcowitz
 *
 * This file is part of jinngine.
 *
 * jinngine is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://code.google.com/p/jinngine/>.
 */
package jinngine.util;

import jinngine.math.*;

public class GramSchmidt {
	/**
	 * Given the vector v, return an orthonormal basis B with its first basis vector aligned with v
	 * @param v
	 * @return
	 */
	public static Matrix3 run(Vector3 v) {
		Vector3 t1 = v.normalize();
		Vector3 t2 = new Vector3(1,0,0);
		t2.assign( t2.sub( t1.multiply(t1.dot(t2))));
	
		//if t1 and t2 is linearly dependent, chose another vector, not aligned with t2
		if (t2.norm() < 1e-10) {
			t2.assign( new Vector3(0,0,1));
			t2.assign( t2.sub( t1.multiply(t1.dot(t2))));
		}
		
		t2.assign(t2.normalize());
		
		//having two orthogonal vectors we obtain the third by crossing
		Vector3 t3 = t1.cross(t2).normalize();

		return new Matrix3(t1,t2,t3);
	}

	/**
	 * Normalise t1, and compute normalised othorgonal tangents in t2 and t3
	 */
	public static void run(Vector3 t1, Vector3 t2, Vector3 t3) {
		t1.assignNormalize();
		t2.assign(1,0,0);
		//t2.assign( t2.sub( t1.multiply(t1.dot(t2))));
		Vector3.multiplyAndAdd(t1, -t1.dot(t2), t2);

		//if t1 and t2 is linearly dependent, chose another vector, not aligned with t2
		if (t2.norm() < 1e-10) {
			t2.assign(0,0,1);
			//t2.assign( t2.sub( t1.multiply(t1.dot(t2))));
			Vector3.multiplyAndAdd(t1, -t1.dot(t2), t2);
		}
		
		t2.assignNormalize();
		
		//having two orthogonal vectors we obtain the third by crossing
		Vector3.crossProduct(t1, t2, t3);
		t3.assignNormalize();
	}

	
	public static Matrix3 run(Vector3 v1, Vector3 v2) {
		Vector3 t1 = v1.normalize();
		Vector3 t2 = new Vector3(v2);
		t2.assign( t2.sub( t1.multiply(t1.dot(t2))));
	
		//if t1 and t2 is linearly dependent, chose another vector, not aligned with t2
		if (t2.norm() < 1e-10) {
			t2.assign( new Vector3(0,0,1));
			t2.assign( t2.sub( t1.multiply(t1.dot(t2))));
		}
		
		t2.assign(t2.normalize());
		
		//having two orthogonal vectors we obtain the third by crossing
		Vector3 t3 = t1.cross(t2).normalize();

		return new Matrix3(t1,t2,t3);

	}
}
