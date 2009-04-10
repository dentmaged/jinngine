package jinngine.collision;

import java.util.List;

import jinngine.geometry.SupportMap3;
import jinngine.math.Vector3;


public class RayCast {
	public double run( 
			final SupportMap3 Sb, 
			final Vector3 point, 
			final Vector3 direction) 
	{
		final GJK3 gjk = new GJK3();
		final double epsilon = 1e-7;		
		double lambda=0;
		final Vector3 x = point.copy();
		Vector3 n = new Vector3();
		Vector3 pb = new Vector3(), pa = new Vector3();
		
		//System.out.println("(*) RayCast");
		
		SupportMap3 Sa = new SupportMap3() {
			@Override
			public Vector3 supportPoint(Vector3 direction) {
				return x.copy();
			}

			@Override
			public List<Vector3> supportFeature(Vector3 d) {
				// TODO Auto-generated method stub
				return null;
			}
		};

		gjk.run(Sa,Sb,pa,pb,Double.POSITIVE_INFINITY);
		Vector3 c = new Vector3();
		c.assign(pb);
		pb.print();
		
		while ( x.minus(c).norm() > epsilon ) {
			//System.out.println("iteration");			
			
			n.assign(x.minus(c));
			if ( n.normalize().dot(direction) >= 0) {
				System.out.println("RayCast: miss, lambda="+lambda);
				return Double.POSITIVE_INFINITY;
			} else {
				lambda = lambda - n.dot(n) / n.dot(direction);
				x.assign(point.add(direction.multiply(lambda)));
				System.out.println("lambda="+lambda);
				
				gjk.run(Sa,Sb,pa,pb,Double.POSITIVE_INFINITY);
				c.assign(pb);
			}			
		}
		
		System.out.println("RayCast: Hitpoint lambda=" + lambda);
		n.print();
		return lambda;
	}
}
