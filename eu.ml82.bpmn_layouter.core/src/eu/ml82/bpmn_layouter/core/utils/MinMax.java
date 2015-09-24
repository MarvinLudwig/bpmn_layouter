package eu.ml82.bpmn_layouter.core.utils;

public class MinMax <T extends Number & Comparable<? super T>>{
	
	private T min = null;
	private T max = null;
	
	public T getMin() {
		return min;
	}

	public T getMax() {
		return max;
	}

	public void addValue(T newValue){
		if (max == null || newValue.compareTo(max) > 0) max = newValue;
		if (min == null || newValue.compareTo(min) < 0) min = newValue;
	}

}
