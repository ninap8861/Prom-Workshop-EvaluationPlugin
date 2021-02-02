package org.processmining.plugins.workshop.evalworkflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetFactory;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentParameters;
import org.processmining.antialignments.ilp.antialignment.AntiAlignmentPlugin;
import org.processmining.decomposedreplayer.parameters.DecomposedReplayParameters;
import org.processmining.decomposedreplayer.plugins.DecomposedReplayPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.EvClassLogPetrinetConnectionFactoryUI;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import nl.tue.astar.AStarException;

public class ConformanceCheckingMethods {
	
	public double getTraceFitness(PNRepResult replayResult) {
		
		if (!replayResult.isEmpty()) {
			double fit = (Double) replayResult.getInfo().get(PNRepResult.TRACEFITNESS);
			return fit;
		}
		return 0.0;
	}
	
	public double getMaxFitnessCost(PNRepResult replayResult) {
		
		if (!replayResult.isEmpty()) {
			double fit = (Double) replayResult.getInfo().get(PNRepResult.MAXFITNESSCOST);
			return fit;
		}
		return 0.0;
	}
	
	public double getCalculationTime(PNRepResult replayResult) {
		
		if (!replayResult.isEmpty()) {
			double fit = (Double) replayResult.getInfo().get(PNRepResult.TIME);
			return fit;
		}
		return 0.0;
	}
	
	public double getRawFitnessCost(PNRepResult replayResult) {
		
		if (!replayResult.isEmpty()) {
			double fit = (Double) replayResult.getInfo().get("Raw Fitness Cost");
			return fit;
		}
		return 0.0;
	}
	
	public double getCalcTime(PNRepResult replayResult) {
		
		if (!replayResult.isEmpty()) {
			double time = (Double) replayResult.getInfo().get("Calculation Time (ms)");
			return time;
		}
		return 0.0;
	}
	
	public PNRepResult applyPNLogReplayer(PluginContext context, XLog log, Petrinet net) {
		PNLogReplayer replayer = new PNLogReplayer();
		PetrinetReplayerWithoutILP replayerWithoutILP = new PetrinetReplayerWithoutILP();;
		TransEvClassMapping transEventMap = computeTransEventMapping(log, net);
		
		XEventClassifier classifierMap = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifierMap);
		AcceptingPetriNet apn = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		CostBasedCompleteParam parameters = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(), transEventMap.getDummyEventClass(), apn.getNet().getTransitions(), 2, 5);
		parameters.getMapEvClass2Cost().remove(transEventMap.getDummyEventClass());
		parameters.getMapEvClass2Cost().put(transEventMap.getDummyEventClass(), 1);
		parameters.setGUIMode(false);
		parameters.setCreateConn(false);
		parameters.setInitialMarking(apn.getInitialMarking());
		Marking[] finalMarkings = new Marking[] {getFinalMarking(net)};		
		parameters.setFinalMarkings(finalMarkings);
		parameters.setMaxNumOfStates(200000);
		
		try {
			return replayer.replayLog(context, net, log, transEventMap, replayerWithoutILP, parameters);
		} catch (AStarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
	
	public PNRepResult applyDecomposedReplayer(PluginContext context, XLog log, Petrinet net) {
		DecomposedReplayPlugin replayer = new DecomposedReplayPlugin();
		AcceptingPetriNet apn = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		DecomposedReplayParameters parameters = new DecomposedReplayParameters(log, apn);
//		return replayer.apply(context, log, apn, parameters);
		PNRepResult result = replayer.run(context, log, apn, parameters);
		return result;
		
		
	}
	
	public PNRepResult applyApproximationAlignment(PluginContext context, XLog log, Petrinet net) {
		PNLogReplayer replayer = new PNLogReplayer();
		PetrinetReplayerWithoutILP replayerWithoutILP = new PetrinetReplayerWithoutILP();;
		TransEvClassMapping transEventMap = computeTransEventMapping(log, net);
		
		XEventClassifier classifierMap = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifierMap);
		AcceptingPetriNet apn = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		CostBasedCompleteParam replayerParams = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(), transEventMap.getDummyEventClass(), apn.getNet().getTransitions(), 2, 5);
		replayerParams.getMapEvClass2Cost().remove(transEventMap.getDummyEventClass());
		replayerParams.getMapEvClass2Cost().put(transEventMap.getDummyEventClass(), 1);
		replayerParams.setGUIMode(false);
		replayerParams.setCreateConn(false);
		replayerParams.setInitialMarking(apn.getInitialMarking());
		Marking[] finalMarkings = new Marking[] {getFinalMarking(net)};		
		replayerParams.setFinalMarkings(finalMarkings);
		replayerParams.setMaxNumOfStates(200000);
		
		PNRepResult alignments = null;
		try {
			alignments =  replayer.replayLog(context, net, log, transEventMap, replayerWithoutILP, replayerParams);
		} catch (AStarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		AntiAlignmentPlugin aap = new AntiAlignmentPlugin();
		AntiAlignmentParameters aaParams = new AntiAlignmentParameters(5, 1, 1, 2); //default params
		if(!alignments.isEmpty()) {
			PNRepResult result = aap.measurePrecision(context, net, log, alignments, aaParams);
			return result;
		}
		
		return null;
	}
	
	
	private Marking getFinalMarking(Petrinet net) {
		Marking finalMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}

		return finalMarking;
	}

	public static  TransEvClassMapping computeTransEventMapping(XLog log, PetrinetGraph net) {
		XEventClass evClassDummy = EvClassLogPetrinetConnectionFactoryUI.DUMMY;
		TransEvClassMapping mapping = new TransEvClassMapping(XLogInfoImpl.STANDARD_CLASSIFIER, evClassDummy);
		XEventClasses ecLog = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.STANDARD_CLASSIFIER).getEventClasses();
		for (Transition t : net.getTransitions()) {
			//TODO: this part is rather hacky, I'll admit.
			XEventClass eventClass = ecLog.getByIdentity(t.getLabel() + "+complete");
			if (eventClass == null) {
				eventClass = ecLog.getByIdentity(t.getLabel() + "+COMPLETE");
			}
			if (eventClass == null) {
				eventClass = ecLog.getByIdentity(t.getLabel());
			}
			
			if (eventClass != null) {
				mapping.put(t, eventClass);
			} else {
				mapping.put(t, evClassDummy);
				t.setInvisible(true);
			}
		}
		return mapping;
	}
	
//	public double traceFitness(XTrace trace) {
//		Map<List<String>, Double> fitnessMap = new HashMap<List<String>, Double>();
//		
//		List<String> traceLabelList = traceToLabelList(trace);
//		if (fitnessMap.containsKey(traceLabelList)) {
//			return fitnessMap.get(traceLabelList);
//		}
//		
//		XLog log2 = XFactoryRegistry.instance().currentDefault().createLog();
//		log2.add(trace);
//
//		try {
//			PNRepResult replayRes = applyPNLogRepla;
//			if (!replayRes.isEmpty()) {
//				double fit = (Double) replayRes.getInfo().get(PNRepResult.TRACEFITNESS);
//				fitnessMap.put(traceLabelList, fit);
//				return fit;
//			}
//
//		} catch (AStarException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NullPointerException e) {
//			e.printStackTrace();
//		}
//		return 0.0;
//	}
	
	
	public static List<String> traceToLabelList(XTrace trace) {
		List<String> res = new ArrayList<String>();
		Iterator<XEvent> eventIter = trace.iterator();
		while (eventIter.hasNext()) {
			XEvent xEvent = eventIter.next();
			res.add(XConceptExtension.instance().extractName(xEvent));
		}
		return res;
	}
//
//	
//
//
//	public boolean isConformant(XTrace trace) {
//		return (traceFitness(trace) == 1.0);
//	}

//
//	public double computeConformance(XTrace trace, ConformanceMode conformanceMode) {
//		if (conformanceMode == ConformanceMode.FITNESS) {
//			return traceFitness(trace);
//		}
//		if (isConformant(trace)) {
//			return 1.0;
//		}
//		return 0.0;
//	}

}
