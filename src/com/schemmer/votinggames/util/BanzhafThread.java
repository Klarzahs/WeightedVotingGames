package com.schemmer.votinggames.util;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

public class BanzhafThread implements Callable<Double>{
	private PowerSet<Integer> pset;
	private long sum = 0;
	private int target;
	private int weights[];
	private int quota;
	private double n;
	
	public BanzhafThread(PowerSet<Integer> pset, int target, int[] weights, int quota, double n){
		this.pset = pset;
		this.target = target;
		this.weights = weights;
		this.quota = quota;
		this.n = n;
	}
	
	@Override
	public Double call() throws Exception {
		Iterator<Set<Integer>> it = pset.iterator();
		while(it.hasNext()){
			Set<Integer> set = it.next();
			if(set.contains(target)){
				long a = characteristicFunction(set, true);
				long b = characteristicFunction(set, false);
//				Log.d("Set : "+set.toString()+", target: "+target+", weight: "+weights[target]);
//				Log.d("a: "+a);
//				Log.d("b: "+b);
//				Log.d("");
				sum += (a - b);
			}
		}
		return (n * sum);
	}
	
	public int characteristicFunction(Set<Integer> set, boolean includeTarget){
		int sum = 0;
		for(int i : set){
			if(i != target){
				sum += weights[i];
			}else{
				if(includeTarget){
					sum += weights[i];
				}
			}
		}
		if(sum >= quota)
			return 1;
		return 0;
	}
	
}
