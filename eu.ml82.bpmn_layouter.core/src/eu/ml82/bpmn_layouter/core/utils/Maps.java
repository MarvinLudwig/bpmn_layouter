package eu.ml82.bpmn_layouter.core.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Maps {
		
	public static <K, V> Set<K> getKeysSortedByValue (Map<K, V> map) {
		
       Comparator<Entry<K, V>> valueComparator = new Comparator<Entry<K, V>>() {
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
				Comparable value1 = (Comparable<V>) o1.getValue();
				Comparable value2 = (Comparable<V>) o2.getValue();
			    return value1.compareTo(value2);
			}
        };
		
		Set<Entry<K, V>> set = map.entrySet();		
		List<Entry<K, V>> sortedList = new LinkedList<Entry<K, V>>(set);
		Collections.sort(sortedList,valueComparator);
		Set<K> returnSet = new LinkedHashSet<K>();
		for (Entry<K, V> entry : sortedList){
			returnSet.add(entry.getKey());
		}
		return returnSet;

	}
	
}
