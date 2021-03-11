package org.processmining.plugins.workshop.evalworkflow;

import ressources.GlobalConformanceResult;

public class SampleResults {
	double fitness; 
	double computationTime; 
	
	public SampleResults(GlobalConformanceResult result, long time) {
		fitness = result.getFitness();		
		computationTime = (double) time/1000000; //convert from nano to milliseconds
	}

}
