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
import org.processmining.contexts.uitopia.UIPluginContext;
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

import conformance.IncrementalConformanceChecker;
import conformance.traceAnalysis.IncrementalTraceAnalyzer;
import conformance.traceAnalysis.TraceAnalyzerFactory;
import nl.tue.astar.AStarException;
import qualitychecking.QualityCheckManager;
import resourcedeviations.ResourceAssignment;
import ressources.GlobalConformanceResult;
import ressources.IccParameter;

public class ConformanceCheckingMethods {

	public double getTraceFitness(PNRepResult replayResult) {

		if (!replayResult.isEmpty()) {
			double fit = (Double) replayResult.getInfo().get(PNRepResult.TRACEFITNESS);
			return fit;
		}
		return 0.0;
	}

	public double getTraceFit(PNRepResult replayResult) {

		if (replayResult != null) {
			double fit = Double.parseDouble((String) replayResult.getInfo().get("Trace Fitness"));
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

		if (replayResult != null) {
			double time = (Double) replayResult.getInfo().get("Calculation Time (ms)");
			return time;
		}
		return 0.0;
	}

	public PNRepResult applyPNLogReplayer(PluginContext context, XLog log, Petrinet net) {
		System.out.println("Starting Alignment Replayer");
		PNLogReplayer replayer = new PNLogReplayer();
		PetrinetReplayerWithoutILP replayerWithoutILP = new PetrinetReplayerWithoutILP();
		;
		TransEvClassMapping transEventMap = computeTransEventMapping(log, net);

		XEventClassifier classifierMap = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifierMap);
		AcceptingPetriNet apn = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		CostBasedCompleteParam parameters = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(),
				transEventMap.getDummyEventClass(), apn.getNet().getTransitions(), 2, 5);
		parameters.getMapEvClass2Cost().remove(transEventMap.getDummyEventClass());
		parameters.getMapEvClass2Cost().put(transEventMap.getDummyEventClass(), 1);
		parameters.setGUIMode(false);
		parameters.setCreateConn(false);
		parameters.setInitialMarking(apn.getInitialMarking());
		Marking[] finalMarkings = new Marking[] { getFinalMarking(net) };
		parameters.setFinalMarkings(finalMarkings);
		parameters.setMaxNumOfStates(200000);

		try {
			System.out.println("Finished Alignment Replayer");
			return replayer.replayLog(context, net, log, transEventMap, replayerWithoutILP, parameters);
		} catch (AStarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public PNRepResult applyDecomposedReplayer(PluginContext context, XLog log, Petrinet net) {
		System.out.println("Starting Decomposed Replayer");
		DecomposedReplayPlugin replayer = new DecomposedReplayPlugin();
		AcceptingPetriNet apn = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		DecomposedReplayParameters parameters = new DecomposedReplayParameters(log, apn);
		//		return replayer.apply(context, log, apn, parameters);
		PNRepResult result = replayer.run(context, log, apn, parameters);
		System.out.println("Finished Decomposed Replayer");
		return result;

	}

	public PNRepResult applyApproximationAlignment(PluginContext context, XLog log, Petrinet net) {
		System.out.println("Starting Approximation Alignment");
		PNLogReplayer replayer = new PNLogReplayer();
		PetrinetReplayerWithoutILP replayerWithoutILP = new PetrinetReplayerWithoutILP();
		;
		TransEvClassMapping transEventMap = computeTransEventMapping(log, net);

		XEventClassifier classifierMap = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifierMap);
		AcceptingPetriNet apn = AcceptingPetriNetFactory.createAcceptingPetriNet(net);
		CostBasedCompleteParam replayerParams = new CostBasedCompleteParam(logInfo.getEventClasses().getClasses(),
				transEventMap.getDummyEventClass(), apn.getNet().getTransitions(), 2, 5);
		replayerParams.getMapEvClass2Cost().remove(transEventMap.getDummyEventClass());
		replayerParams.getMapEvClass2Cost().put(transEventMap.getDummyEventClass(), 1);
		replayerParams.setGUIMode(false);
		replayerParams.setCreateConn(false);
		replayerParams.setInitialMarking(apn.getInitialMarking());
		Marking[] finalMarkings = new Marking[] { getFinalMarking(net) };
		replayerParams.setFinalMarkings(finalMarkings);
		replayerParams.setMaxNumOfStates(200000);

		PNRepResult alignments = null;
		try {
			alignments = replayer.replayLog(context, net, log, transEventMap, replayerWithoutILP, replayerParams);
		} catch (AStarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		AntiAlignmentPlugin aap = new AntiAlignmentPlugin();
		AntiAlignmentParameters aaParams = new AntiAlignmentParameters(5, 1, 1, 2); //default params
		if (!alignments.isEmpty()) {
			PNRepResult result = aap.measurePrecision(context, net, log, alignments, aaParams);
			System.out.println("Finished Approximation Alignments");
			return result;
		}

		return null;
	}

	public GlobalConformanceResult applySampleBasedApproximation(UIPluginContext context, XLog log, Petrinet net)
			throws Exception {
		System.out.println("Starting Sample Based Approximation");
		TransEvClassMapping mapping = computeTransEventMapping(log, net);
		XEventClassifier classifierMap = XLogInfoImpl.STANDARD_CLASSIFIER;

		//get resource deviations
		IccParameter settings = new IccParameter(0.01, 0.99, 0.01, 0.2, IncrementalConformanceChecker.Goals.FITNESS,
				false, false, false);
		PetrinetGraph netGraph = net;
		XLog copyLog = (XLog) log.clone();
//		Replayer replayer = ReplayerFactory.createReplayer(netGraph, copyLog, mapping, classifierMap, true);
		ResourceAssignment resAssignment = new ResourceAssignment();
		IncrementalTraceAnalyzer<?> analyzer = TraceAnalyzerFactory.createTraceAnalyzer(settings, mapping,
				classifierMap, copyLog, netGraph, resAssignment);
		long start = System.currentTimeMillis();
		GlobalConformanceResult result = checkForGlobalConformanceWithICC(context, net, copyLog, analyzer, settings,
				null, null);
		long end = System.currentTimeMillis();
		System.out.println("Finishing Sample Based Approximation");
		System.out.println("Fitness: " + result.getFitness());
		System.out.println(end-start);
		return result;

	}

	private GlobalConformanceResult checkForGlobalConformanceWithICC(UIPluginContext context, PetrinetGraph net,
			XLog log, IncrementalTraceAnalyzer<?> analyzer, IccParameter iccParameters,
			QualityCheckManager internalQualityCheckManager, QualityCheckManager externalQualityCheckManager) {
		IncrementalConformanceChecker icc = new IncrementalConformanceChecker(analyzer, iccParameters);
		return icc.apply(context, log, net, IncrementalConformanceChecker.SamplingMode.BINOMIAL);
	}

	private Marking getFinalMarking(Petrinet net) {
		Marking finalMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}

		return finalMarking;
	}

	public static TransEvClassMapping computeTransEventMapping(XLog log, PetrinetGraph net) {
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

	public static List<String> traceToLabelList(XTrace trace) {
		List<String> res = new ArrayList<String>();
		Iterator<XEvent> eventIter = trace.iterator();
		while (eventIter.hasNext()) {
			XEvent xEvent = eventIter.next();
			res.add(XConceptExtension.instance().extractName(xEvent));
		}
		return res;
	}

}