package org.processmining.plugins.workshop.evalworkflow;

public class EvaluationResults {
	String description;
	double traceFitness;
	
	public EvaluationResults(String eventLog, String processDiscovery, String conformanceChecking, double traceFitness) {
		// TODO Auto-generated constructor stub
		this.description = eventLog + " " + processDiscovery + " " + conformanceChecking;
		this.traceFitness = traceFitness;
	}

}
