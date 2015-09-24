package eu.ml82.bpmn_layouter.core.utils;

public class PointMinMax<T extends Number & Comparable<? super T>>{
	
	private MinMax<T> x = new MinMax<T>();
	private MinMax<T> y = new MinMax<T>();
	
	public T getMinX(){
		return x.getMin();
	}
	
	public T getMaxX(){
		return x.getMax();
	}
	
	public T getMinY(){
		return y.getMin();
	}
	
	public T getMaxY(){
		return y.getMax();
	}
	
	public void addX(T newValue){
		x.addValue(newValue);
	}
	
	public void addY(T newValue){
		y.addValue(newValue);	
	}

	@Override
	public String toString() {
		return "PointMinMax [getMinX()=" + getMinX() + ", getMaxX()="
				+ getMaxX() + ", getMinY()=" + getMinY() + ", getMaxY()="
				+ getMaxY() + "]";
	}

}
