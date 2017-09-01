package gr.uoa.di.madgik.panagiotisl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

public class Community {

	private Set<String> seedSet;
	private Map<String, Double> members;
	private String[] groundTruth;
	
	public Community(Set<String> seedSet){
		this(seedSet, null);
	}
	
	public Community(Set<String> seedSet, String[] groundTruh){
		if(seedSet == null)
			throw new IllegalArgumentException();
		this.seedSet = new HashSet<String>();
		seedSet.forEach(seed -> {this.seedSet.add(seed);});
		this.members = new HashMap<String, Double>();
		seedSet.forEach((seed)->{members.put(seed, 1.0);});
		this.groundTruth = groundTruh;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((seedSet == null) ? 0 : seedSet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Community other = (Community) obj;
		if (members == null) {
			if (other.members != null)
				return false;
		} else if (!members.equals(other.members))
			return false;
		if (seedSet == null) {
			if (other.seedSet != null)
				return false;
		} else if (!seedSet.equals(other.seedSet))
			return false;
		return true;
	}

	public boolean contains(String node) {
		return this.members.containsKey(node);
	}

	public void put(String node, Double value) {
		this.members.put(node, value);
	}

	public Double get(String key) {
		return this.members.get(key);
	}
	
	public Set<String> keyset() {
		return this.members.keySet();
	}
	
	public int size() {
		return members.size();
	}
	
	public boolean isSeed(String node) {
		return this.seedSet.contains(node);
	}
	
	public String[] getGroundTruth() {
		return this.groundTruth;
	}

	public void pruneCommunity(DoubleCountMinSketch commCMS, DoubleCountMinSketch degreeCMS, int size) {
		this.members = findGreatest(this.members, size);
		this.seedSet.forEach((seed)->{
			if(!this.members.containsKey(seed))
				this.members.put(seed, 1.0);
		});
	}
	
	public Community getPrunedCommunity(DoubleCountMinSketch commCMS, DoubleCountMinSketch degreeCMS, int size) {
		Map<String, Double> newMembers = findGreatest(this.members, size);
		this.seedSet.forEach((seed)->{
			if(!newMembers.containsKey(seed))
				newMembers.put(seed, 1.0);
		});
		Community comm = new Community(seedSet);
		newMembers.forEach((member, value) -> {comm.put(member, value);});
		return comm;
	}

    private static <K, V extends Comparable<? super V>> Map<K, V> 
        findGreatest(Map<K, V> map, int size)
    {
        Comparator<? super Entry<K, V>> comparator = 
            new Comparator<Entry<K, V>>()
        {
            @Override
            public int compare(Entry<K, V> e0, Entry<K, V> e1)
            {
                V v0 = e0.getValue();
                V v1 = e1.getValue();
                return v0.compareTo(v1);
            }
        };
        PriorityQueue<Entry<K, V>> highest = 
            new PriorityQueue<Entry<K,V>>(size, comparator);
        for (Entry<K, V> entry : map.entrySet())
        {
            highest.offer(entry);
            while (highest.size() > size)
            {
                highest.poll();
            }
        }

        Map<K, V> result = new HashMap<K,V>();
        while (highest.size() > 0)
        {
        	Entry<K, V> entry = highest.poll();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

	public List<Entry<String, Double>> getSortedCommunity() {
		final List<Entry<String, Double>> sorted = this.members.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		return sorted;
	}


	public double getMeanValue() {
		double total = 0.0;
		for(double value : this.members.values()) {
			total += value;
		}
		return total / this.members.size();
	}
	
	public double getMedianValue() {
		List<Entry<String, Double>> sorted = this.getSortedCommunity();
		if (sorted.size() % 2 == 0)
		    return  (sorted.get(sorted.size()/2).getValue() + sorted.get(sorted.size()/2 - 1).getValue())/2;
		else
		    return sorted.get(sorted.size()/2).getValue();
	}
	
	public double getVarianceValue() {
        double mean = getMeanValue();
        double temp = 0;
        for(double value : this.members.values())
            temp += ( value - mean ) * ( value - mean );
        return temp / this.members.size();
	}
	
}
