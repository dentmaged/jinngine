package jinngine.demo.graphics;

import java.util.List;

import jinngine.math.InertiaMatrix;
import jinngine.math.Matrix3;
import jinngine.math.Matrix4;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import jinngine.geometry.*;

public class Line implements SupportMap3, Geometry {

	public Object getAuxiliary() {
		return auxiliary;
	}

	public void setAuxiliary(Object auxiliary) {
		this.auxiliary = auxiliary;
	}

	//world vectors
	private final Vector3 p1, p2;
	private Object auxiliary;
	
	public Line(Vector3 p1, Vector3 p2) {
		this.p1 = p1.copy();
		this.p2 = p2.copy();
	}
	
	@Override
	public Vector3 supportPoint(Vector3 direction) {
		return direction.dot(p1) > direction.dot(p2) ? p1.copy():p2.copy();
	}

	@Override
	public Body getBody() {
		return null;
	}

	@Override
	public double getEnvelope(double dt) {
		return 0;
	}

	@Override
	public InertiaMatrix getInertialMatrix() {
		//lines have no inertia
		return null;
	}

	@Override
	public Matrix4 getTransform() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBody(Body b) {
		// ignore

	}

	@Override
	public void setEnvelope(double envelope) {
		// do nothing
	}

	@Override
	public void setLocalTransform(Matrix3 B, Vector3 b2) {
		//do nothing
	}


	@Override
	public Vector3 getMaxBounds() {
		return new Vector3( p1.x > p2.x? p1.x : p2.x, 
				            p1.y > p2.y? p1.y : p2.y,
					        p1.z > p2.z? p1.z : p2.z);
	}

	@Override
	public Vector3 getMinBounds() {
		return new Vector3( p1.x < p2.x? p1.x : p2.x, 
	                        p1.y < p2.y? p1.y : p2.y,
	         	            p1.z < p2.z? p1.z : p2.z);
	}


	@Override
	public void supportFeature(Vector3 d, double epsilon, List<Vector3> face) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getLocalTransform(Matrix3 R, Vector3 b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getMass() {
		// TODO Auto-generated method stub
		return 0;
	}

}
