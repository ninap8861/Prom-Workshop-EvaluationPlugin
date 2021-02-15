package org.processmining.plugins.workshop.evalworkflow;

public class EvaluationResults {
	String eventLog;
	String processDisc;
	String confCheck;
	double calcTime;
	double traceFitness;
	double maxFitnessCost;
	double rawFitnessCost;
	double precision;
	double generalization;
	
	public EvaluationResults(String eventLog, String processDiscovery, String conformanceChecking, double calcTime, double traceFitness, double maxFitnessCost, double rawFitnessCost,
			double precision, double generalization) {
		// TODO Auto-generated constructor stub
		this.eventLog = eventLog;
		this.processDisc = processDiscovery;
		this.confCheck = conformanceChecking;
		this.calcTime = calcTime;
		this.maxFitnessCost = maxFitnessCost;
		this.traceFitness = traceFitness;
		this.rawFitnessCost = rawFitnessCost;
		this.precision = precision;
		this.generalization = generalization;
	}

}
