/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
package jinngine.geometry;

import java.util.List;
import jinngine.math.*;
import jinngine.physics.Body;

/**
 * A box geometry implementation. Represented using a simple support mapping. 
 * Uses a simple axis aligned bounding box implementation.
 * @author mo 
 *
 */
public class Box implements SupportMap3, Geometry, Material {

	private  Body body;
	private final Matrix3 localtransform = new Matrix3();
	private final Matrix3 localrotation = new Matrix3();
	private final Vector3 localdisplacement = new Vector3();
	private final Vector3 bounds = new Vector3();
	private double envelope = 0;
	
	//box properties
	private double xl,yl,zl;
	private double mass;
	
	// auxiliary user reference
	private Object auxiliary;
	
	// material settings (defaults)
	private double restitution = 0.7;
	private double friction = 0.5;
	
	/**
	 * Create a box with the given side lengths
	 * @param x Box x-axis extend
	 * @param y Box y-axis extend
	 * @param z Box z-axis extend
	 */
	public Box(double x, double y, double z) {
		this.xl = x; this.yl = y; this.zl = z;
		mass = xl*yl*zl;
		
		//set the local transform
		setLocalTransform( Matrix3.identity(), new Vector3() );
	}

	/**
	 * Create a new box with the given side lengths and a local translation
	 * @param x Box x-axis extend
	 * @param y Box y-axis extend
	 * @param z Box z-axis extend
	 * @param posx Box local x-axis translation
	 * @param posy Box local y-axis translation
	 * @param posz Box local z-axis translation
	 */
	public Box(double x, double y, double z, double posx, double posy, double posz) {
		this.xl = x; this.yl = y; this.zl = z;
		mass = xl*yl*zl;
		
		//set the local transform
		setLocalTransform( Matrix3.identity(), new Vector3(posx,posy,posz) );
	}

	/** 
	 * Set new side lengths for this box. Keep in mind that altering geometry changes mass and 
	 * inertia properties of bodies. This method automatically re-finalises the attached body, 
	 * should this box be attached to one. This operation is relatively expensive.
	 */
	public final void setBoxSideLengths( double xl, double yl, double zl) {
		this.xl = xl; this.yl = yl; this.zl = zl;
		mass = xl*yl*zl;
		
		// re-finialize body if any present
		if ( body != null)
			body.finalize();
	}
	
	// user auxiliary methods
	public Object getAuxiliary() { return auxiliary; }
	public void setAuxiliary(Object auxiliary) { this.auxiliary = auxiliary; }

	@Override
	public Vector3 supportPoint(Vector3 direction) {
		Vector3 v = body.state.rotation.multiply(localrotation).transpose().multiply(direction);
		double sv1 = v.x<0?-0.5:0.5;
		double sv2 = v.y<0?-0.5:0.5;
		double sv3 = v.z<0?-0.5:0.5;
		//return Matrix4.multiply(transform4, new Vector3(sv1, sv2, sv3), new Vector3());
		return body.state.rotation.multiply(localtransform.multiply(new Vector3(sv1, sv2, sv3)).add(localdisplacement)).add(body.state.position);
	}

	@Override
	public Body getBody() { return body; }
	@Override
	public void setBody(Body b) { this.body = b; }

	@Override
	public InertiaMatrix getInertialMatrix() {
		InertiaMatrix I = new InertiaMatrix();
		
		Matrix3.set( I,
				(1.0f/12.0f)*mass*(yl*yl+zl*zl), 0.0f, 0.0f,
				0.0f, (1.0f/12.0f)*mass*(xl*xl+zl*zl), 0.0f,
				0.0f, 0.0f, (1.0f/12.0f)*mass*(yl*yl+xl*xl) );

		return I;
	}
	
	@Override
	public double getEnvelope(double dt) {
		return envelope;
	}

	@Override
	public void setEnvelope(double envelope) {
		this.envelope = envelope;
	}

	@Override
	public void setLocalTransform(Matrix3 rotation, Vector3 displacement) {
		this.localdisplacement.assign(displacement);
		this.localrotation.assign(rotation);

		//set the local transform (including scaling)
		localtransform.assign( localrotation.multiply(new Matrix3(new Vector3(xl,0,0), new Vector3(0,yl,0), new Vector3(0,0,zl))));
		
		//extremal point on box
		double max = Matrix3.multiply(localtransform, new Vector3(0.5,0.5,0.5), new Vector3()).norm();
		bounds.assign(new Vector3(max,max,max));
		//System.out.println("max="+max);
	}

	@Override
	public void getLocalTransform(Matrix3 R, Vector3 b) {
		R.assign(localrotation);
		b.assign(localdisplacement);
	}

	@Override
	public Vector3 getMaxBounds() {
		Vector3 displacement = new Vector3();	
		displacement.assign(Matrix3.multiply(body.state.rotation, localdisplacement, new Vector3()));
		return bounds.add(displacement).add(body.state.position);
	}

	@Override
	public Vector3 getMinBounds() {
		Vector3 displacement = new Vector3();
		displacement.assign(Matrix3.multiply(body.state.rotation, localdisplacement, new Vector3()));
		return bounds.multiply(-1).add(displacement).add(body.state.position);
	}

	@Override
	public Matrix4 getTransform() {
		return Matrix4.multiply(body.getTransform(), Transforms.transformAndTranslate4(localtransform, localdisplacement), new Matrix4());
	}	

	@Override
	public void supportFeature(Vector3 d, double epsilon, List<Vector3> featureList) {
		//final double epsilon = 0.03;  //123+132  213 231 312+321   
		//get d into the canonical box space
		Vector3 v = body.state.rotation.multiply(localrotation).transpose().multiply(d);
		//Vector3 v = body.state.rotation.transpose().multiply(d);
		//Vector3 v = d.copy();
		//List<Vector3> featureList = new ArrayList<Vector3>();

		int numberOfZeroAxis = 0;
		int[] zeroAxisIndices = new int[3];
		int numberOfNonZeroAxis = 0;
		int[] nonZeroAxisIndices = new int[3];
		
		if (Math.abs(v.x) < epsilon ) {
			zeroAxisIndices[numberOfZeroAxis++]=0;
		} else { nonZeroAxisIndices[ numberOfNonZeroAxis++] = 0; }
		if (Math.abs(v.y) < epsilon ) {
			zeroAxisIndices[numberOfZeroAxis++]=1;
		} else { nonZeroAxisIndices[ numberOfNonZeroAxis++] = 1; }
		if (Math.abs(v.z) < epsilon ) {
			zeroAxisIndices[numberOfZeroAxis++]=2;
		} else { nonZeroAxisIndices[ numberOfNonZeroAxis++] = 2; }
		
		if (numberOfZeroAxis == 0) {
			//eight possible points

			double sv1 = v.x<0?-0.5:0.5;
			double sv2 = v.y<0?-0.5:0.5;
			double sv3 = v.z<0?-0.5:0.5;
			//return Matrix4.multiply(transform4, new Vector3(sv1, sv2, sv3), new Vector3());
			featureList.add( body.state.rotation.multiply(localtransform.multiply(new Vector3(sv1, sv2, sv3)).add(localdisplacement)).add(body.state.position) );
		}

		else if (numberOfZeroAxis == 1) {
			//System.out.println("edge case");

			//four possible edges
			Vector3 p1 = new Vector3(v.x<0?-0.5:0.5, v.y<0?-0.5:0.5, v.z<0?-0.5:0.5 );
			Vector3 p2 = new Vector3(v.x<0?-0.5:0.5, v.y<0?-0.5:0.5, v.z<0?-0.5:0.5 );
			p1.set( zeroAxisIndices[0], 0.5);
			p2.set( zeroAxisIndices[0], -0.5);
			
			featureList.add( body.state.rotation.multiply(localtransform.multiply(p1).add(localdisplacement)).add(body.state.position) );
			featureList.add( body.state.rotation.multiply(localtransform.multiply(p2).add(localdisplacement)).add(body.state.position) );
		}

		else if (numberOfZeroAxis == 2) {
			//System.out.println("face case");
			//two possible faces
			//four possible edges
			Vector3 p1 = new Vector3(v.x<0?-0.5:0.5, v.y<0?-0.5:0.5, v.z<0?-0.5:0.5 );
			Vector3 p2 = new Vector3(v.x<0?-0.5:0.5, v.y<0?-0.5:0.5, v.z<0?-0.5:0.5 );
			Vector3 p3 = new Vector3(v.x<0?-0.5:0.5, v.y<0?-0.5:0.5, v.z<0?-0.5:0.5 );
			Vector3 p4 = new Vector3(v.x<0?-0.5:0.5, v.y<0?-0.5:0.5, v.z<0?-0.5:0.5 );


			
			//make face turn counter-clock wise
			if ( v.get(nonZeroAxisIndices[0]) > 0 ) { 
				p1.set( zeroAxisIndices[0], 0.5);
				p1.set( zeroAxisIndices[1], 0.5);

				p2.set( zeroAxisIndices[0], -0.5);
				p2.set( zeroAxisIndices[1],  0.5);

				p3.set( zeroAxisIndices[0], -0.5);
				p3.set( zeroAxisIndices[1], -0.5);     

				p4.set( zeroAxisIndices[0],  0.5);      
				p4.set( zeroAxisIndices[1], -0.5);
			} else {
				p1.set( zeroAxisIndices[0], 0.5);
				p1.set( zeroAxisIndices[1], 0.5);

				p2.set( zeroAxisIndices[0],  0.5);
				p2.set( zeroAxisIndices[1],  -0.5);

				p3.set( zeroAxisIndices[0], -0.5);
				p3.set( zeroAxisIndices[1], -0.5);

				p4.set( zeroAxisIndices[0],  -0.5);
				p4.set( zeroAxisIndices[1],   0.5);				
			}
				
			
			//return transformed vertices
			featureList.add( body.state.rotation.multiply(localtransform.multiply(p1).add(localdisplacement)).add(body.state.position) );
			featureList.add( body.state.rotation.multiply(localtransform.multiply(p2).add(localdisplacement)).add(body.state.position) );
			featureList.add( body.state.rotation.multiply(localtransform.multiply(p3).add(localdisplacement)).add(body.state.position) );
			featureList.add( body.state.rotation.multiply(localtransform.multiply(p4).add(localdisplacement)).add(body.state.position) );			
		}

		else if (numberOfZeroAxis == 3) {
			//should never happen, undefinded result
		//System.out.println("DOOOOOOOHHHHHHH");
			//return null;
		}
	}

	//Material getters and setters
	@Override
	public double getFrictionCoefficient() {
		return friction;
	}

	@Override
	public double getRestitution() {
		return restitution;
	}

	@Override
	public void setFrictionCoefficient(double f) {
		this.friction = f;
	}

	@Override
	public void setRestitution(double e) {
		this.restitution = e;
		
	}

	@Override
	public double getMass() {
		return mass;
	}

	public void setMass(double mass) {
		this.mass = mass;
	}
	
	/**
	 * Return the side lengths of this box
	 * @return
	 */
	public Vector3 getDimentions() {
		return new Vector3(xl,yl,zl);
	}

	@Override
	public void getLocalTranslation(Vector3 t) {
		t.assign(localdisplacement);
		
	}

}
