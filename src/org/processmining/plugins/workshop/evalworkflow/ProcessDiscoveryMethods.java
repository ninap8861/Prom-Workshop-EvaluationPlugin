package org.processmining.plugins.workshop.evalworkflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.bpmn.Bpmn;
import org.processmining.plugins.bpmn.plugins.BpmnSelectDiagramPlugin;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnml.base.Pnml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.raffaeleconforti.conversion.bpmn.BPMNToPetriNetConverter;

public class ProcessDiscoveryMethods {

	public void applyHILP(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes, int index) {
		//		if (index == 3 || index == 4) { //doesnt work for sap logs too large
		//			return null;
		//		}
		System.out.println("Starting HILP");
		HybridILPMinerPlugin hilp = new HybridILPMinerPlugin();
		XEventClassifier classifierMap = XLogInfoImpl.STANDARD_CLASSIFIER;
		Object[] obj = hilp.applyExpress(context, log, classifierMap);
		Petrinet pnet = (Petrinet) obj[0];

		//call all conformance checking techniques here
		ConformanceCheckingMethods ccm = new ConformanceCheckingMethods();
		PNRepResult repResult1 = ccm.applyPNLogReplayer(context, log, pnet);
		double traceFitness1 = ccm.getTraceFitness(repResult1);
		double calcTime = ccm.getCalculationTime(repResult1);
		double maxFitness = ccm.getMaxFitnessCost(repResult1);

		String evLogDescr = "Event Log " + Integer.toString(index);
		eRes.add(new EvaluationResults(evLogDescr, "HILP", "CostBased", calcTime, traceFitness1, maxFitness, 0.0));

		if (index != 5) { //BCP Event log doesnt work with decomposed
			PNRepResult repResult2 = ccm.applyDecomposedReplayer(context, log, pnet);
			double rawFitnessCost = ccm.getRawFitnessCost(repResult2);
			calcTime = ccm.getCalcTime(repResult2);
			eRes.add(new EvaluationResults(evLogDescr, "HILP", "Decomposed", calcTime, 0.0, 0.0, rawFitnessCost));
		}

		PNRepResult repResult3 = ccm.applyApproximationAlignment(context, log, pnet);
		double traceFitness3 = ccm.getTraceFit(repResult3);
		calcTime = ccm.getCalcTime(repResult3);
		eRes.add(new EvaluationResults(evLogDescr, "HILP", "AntiAlignment", calcTime, traceFitness3, 0.0, 0.0));

		System.out.println("Finishing HILP");
		String path = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinethilpminer"
				+ Integer.toString(index) + ".pnml";
		try {
			savePetrinetToPnml(pnet, null, new File(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Saved Petri Net HILP Miner");
	}

	public void applyInductiveMiner(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes, int index) {
		//		XEventClassifier classifier = MiningParameters.getDefaultClassifier();
		//		context.log("Started Inductive Miner", MessageLevel.NORMAL);
		System.out.println("Starting Inductive Miner");
		MiningParametersIMf param = new MiningParametersIMf();
		IMPetriNet impetrinet = new IMPetriNet();
		Object[] obj = impetrinet.minePetriNetParameters(context, log, param);
		Petrinet pnet = (Petrinet) obj[0];

		//call all conformance checking techniques here
		ConformanceCheckingMethods ccm = new ConformanceCheckingMethods();
		PNRepResult repResult1 = ccm.applyPNLogReplayer(context, log, pnet);
		String evLogDescr = "Event Log " + Integer.toString(index);
		double traceFitness1 = ccm.getTraceFitness(repResult1);
		double calcTime = ccm.getCalculationTime(repResult1);
		double maxFitness = ccm.getMaxFitnessCost(repResult1);

		eRes.add(new EvaluationResults(evLogDescr, "Inductive Miner", "CostBased", calcTime, traceFitness1, maxFitness,
				0.0));

		if (index != 5) { //BCP Event log is the first and doesnt work with decomposed
			PNRepResult repResult2 = ccm.applyDecomposedReplayer(context, log, pnet);
			double rawFitnessCost = ccm.getRawFitnessCost(repResult2);
			calcTime = ccm.getCalcTime(repResult2);
			eRes.add(new EvaluationResults(evLogDescr, "Inductive Miner", "Decomposed", calcTime, 0.0, 0.0,
					rawFitnessCost));
		}
		PNRepResult repResult3 = ccm.applyApproximationAlignment(context, log, pnet);
		double traceFitness3 = ccm.getTraceFit(repResult3);
		calcTime = ccm.getCalcTime(repResult3);
		eRes.add(new EvaluationResults(evLogDescr, "Inductive Miner", "AntiAlignment", calcTime, traceFitness3, 0.0,
				0.0));

		System.out.println("Finished Inductive Miner");
		String path = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinetinductiveminer"
				+ Integer.toString(index) + ".pnml";
		try {
			savePetrinetToPnml(pnet, null, new File(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Saved Petri Net Inductive Miner");
	}

	@SuppressWarnings("deprecation")
	//	public Petrinet applyETM(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes, int index) {
	//
	//		ETMParam param = ETMParamFactory.buildStandardParam(log);
	//		ETM etm = new ETM(param);
	//		etm.run();
	//		NAryTree tree = etm.getResult();
	//		ProcessTree processTree = NAryTreeToProcessTree.convert(tree, param.getCentralRegistry().getEventClasses());
	//		PetrinetWithMarkings petrinetwithmarkings = null;
	//		try {
	//			petrinetwithmarkings = ProcessTree2Petrinet.convert(processTree);
	//		} catch (NotYetImplementedException e) {
	//			e.printStackTrace();
	//		} catch (InvalidProcessTreeException e) {
	//			e.printStackTrace();
	//		}
	//
	//		//call all conformance checking techniques here
	//		ConformanceCheckingMethods ccm = new ConformanceCheckingMethods();
	//		PNRepResult repResult1 = ccm.applyPNLogReplayer(context, log, petrinetwithmarkings.petrinet);
	//		double traceFitness1 = ccm.getTraceFitness(repResult1);
	//		
	//		String evLogDescr = "Event Log " + Integer.toString(index);
	//		double calcTime = ccm.getCalculationTime(repResult1);
	//		double maxFitness = ccm.getMaxFitnessCost(repResult1);
	//
	//		eRes.add(new EvaluationResults(evLogDescr, "ETM", "CostBased", calcTime, traceFitness1, maxFitness, 0.0));
	//
	//		PNRepResult repResult2 = ccm.applyDecomposedReplayer(context, log, petrinetwithmarkings.petrinet);
	//		double rawFitnessCost = ccm.getRawFitnessCost(repResult2);
	//		calcTime = ccm.getCalcTime(repResult2);
	//
	//		eRes.add(new EvaluationResults(evLogDescr, "ETM", "Decomposed", calcTime, 0.0, 0.0, rawFitnessCost));
	//
	//		return petrinetwithmarkings.petrinet;
	//	}

	public static void applySplitMiner(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes, int index)
			throws IOException {
		System.out.println("Starting Split Miner");
		String logPath = null;
		XAttributeMap attrMap = log.getAttributes();
		logPath = attrMap.get("path").toString();
		String outputPath = ".\\Outputs\\bpmnmodelprom" + Integer.toString(index);
		String importPath = "C:\\Users\\I519745\\Desktop\\Thesis\\Thesis\\split-miner-2.0\\Outputs\\bpmnmodelprom"
				+ Integer.toString(index) + ".bpmn";
		Runtime rt = Runtime.getRuntime();
		File dir = new File("C:\\Users\\I519745\\Desktop\\Thesis\\Thesis\\split-miner-2.0");
		Process pr = rt.exec(new String[] { "java", "-cp", "sm2.jar;lib\\*", "au.edu.unimelb.services.ServiceProvider",
				"SM2", logPath, outputPath, "0.05" }, null, dir);
		Bpmn bpmnModel = null;
		try {
			TimeUnit.SECONDS.sleep(2);
			bpmnModel = importBPMN(importPath);
			if (bpmnModel == null) {
				System.out.println("BPMN Empty");
				return;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Petrinet pnet = convertBPMNToPetriNet(context, bpmnModel);

		//call all conformance checking techniques here
		ConformanceCheckingMethods ccm = new ConformanceCheckingMethods();
		PNRepResult repResult1 = ccm.applyPNLogReplayer(context, log, pnet);
		double traceFitness1 = ccm.getTraceFitness(repResult1);

		String evLogDescr = "Event Log " + Integer.toString(index);
		double calcTime = ccm.getCalculationTime(repResult1);
		double maxFitness = ccm.getMaxFitnessCost(repResult1);

		eRes.add(new EvaluationResults(evLogDescr, "Split Miner", "CostBased", calcTime, traceFitness1, maxFitness,
				0.0));

		if (index != 5) { //BCP Event log is the first and doesnt work with decomposed
			PNRepResult repResult2 = ccm.applyDecomposedReplayer(context, log, pnet);
			double rawFitnessCost = ccm.getRawFitnessCost(repResult2);
			calcTime = ccm.getCalcTime(repResult2);
			eRes.add(
					new EvaluationResults(evLogDescr, "Split Miner", "Decomposed", calcTime, 0.0, 0.0, rawFitnessCost));
		}
		PNRepResult repResult3 = ccm.applyApproximationAlignment(context, log, pnet);
		double traceFitness3 = ccm.getTraceFit(repResult3);
		calcTime = ccm.getCalcTime(repResult3);
		eRes.add(new EvaluationResults(evLogDescr, "Split Miner", "AntiAlignment", calcTime, traceFitness3, 0.0, 0.0));

		System.out.println("Finished Split Miner");
		String path = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinetsplitminer" + Integer.toString(index)
				+ ".pnml";
		savePetrinetToPnml(pnet, null, new File(path));
		System.out.println("Saved Petri Net Split Miner");
	}

	public static void applyStructuredMiner(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes,
			int index) throws IOException {
		System.out.println("Starting Structured Miner");
		String logPath = null;
		XAttributeMap attrMap = log.getAttributes();
		logPath = attrMap.get("path").toString();
		String outputPath = ".\\outputs\\bpmnmodelprom" + Integer.toString(index);
		String importPath = "C:\\Users\\I519745\\Desktop\\Thesis\\Thesis\\structuredminer\\outputs\\bpmnmodelprom"
				+ Integer.toString(index) + ".bpmn";
		Runtime rt = Runtime.getRuntime();
		File dir = new File("C:\\Users\\I519745\\Desktop\\Thesis\\Thesis\\structuredminer");
		Process pr = rt.exec(new String[] { "java", "-jar", "StructuredMiner.jar", "hm", logPath, outputPath }, null,
				dir);

		Bpmn bpmnModel = null;
		try {
			TimeUnit.SECONDS.sleep(2);
			bpmnModel = importBPMN(importPath);
			if (bpmnModel == null) {
				System.out.print("BPMN Empty");
				return;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Petrinet pnet = convertBPMNToPetriNet(context, bpmnModel);

		//call all conformance checking techniques here
		ConformanceCheckingMethods ccm = new ConformanceCheckingMethods();
		PNRepResult repResult1 = ccm.applyPNLogReplayer(context, log, pnet);
		double traceFitness1 = ccm.getTraceFitness(repResult1);

		String evLogDescr = "Event Log " + Integer.toString(index);
		double calcTime = ccm.getCalculationTime(repResult1);
		double maxFitness = ccm.getMaxFitnessCost(repResult1);

		eRes.add(new EvaluationResults(evLogDescr, "Structured Miner", "CostBased", calcTime, traceFitness1, maxFitness,
				0.0));

		if (index != 5) { //BCP Event log is the first and doesnt work with decomposed
			PNRepResult repResult2 = ccm.applyDecomposedReplayer(context, log, pnet);
			double rawFitnessCost = ccm.getRawFitnessCost(repResult2);
			calcTime = ccm.getCalcTime(repResult2);
			eRes.add(new EvaluationResults(evLogDescr, "Structured Miner", "Decomposed", calcTime, 0.0, 0.0,
					rawFitnessCost));
		}
		PNRepResult repResult3 = ccm.applyApproximationAlignment(context, log, pnet);
		double traceFitness3 = ccm.getTraceFit(repResult3);
		calcTime = ccm.getCalcTime(repResult3);
		eRes.add(new EvaluationResults(evLogDescr, "Structured Miner", "AntiAlignment", calcTime, traceFitness3, 0.0,
				0.0));

		System.out.println("Finishing Structured Miner");
		String path = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinetstructured" + Integer.toString(index)
				+ ".pnml";
		savePetrinetToPnml(pnet, null, new File(path));
		System.out.println("Saved Petri Net Structured Miner");
	}

	public static Petrinet convertBPMNToPetriNet(PluginContext context, Bpmn bpmn) {
		BPMNToPetriNetConverter bpmnToPn = new BPMNToPetriNetConverter();
		BpmnSelectDiagramPlugin bpmnToDiagram = new BpmnSelectDiagramPlugin();
		BPMNDiagram bpmnDiagram = bpmnToDiagram.selectDefault(context, bpmn);
		Object[] object = bpmnToPn.convert(bpmnDiagram);
		Petrinet pn = (Petrinet) object[0];
		return pn;
	}

	public static void savePetrinetToPnml(Petrinet net, Marking marking, File file) throws IOException {

		if (marking == null)
			marking = new Marking();

		GraphLayoutConnection layout = new GraphLayoutConnection(net);
		HashMap<PetrinetGraph, Marking> markedNets = new HashMap<PetrinetGraph, Marking>();
		markedNets.put(net, marking);
		Pnml pnml = new Pnml().convertFromNet(markedNets, layout);
		pnml.setType(Pnml.PnmlType.PNML);
		String text = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" + pnml.exportElement(pnml);

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		bw.write(text);
		bw.close();
	}

	public static Bpmn importBPMN(String pathToModel) throws Exception {
		File bpmnFile = new File(pathToModel);
		//		long fileSizeInBytes = bpmnFile.length();
		if (!bpmnFile.exists()) {
			return null;
		}
		InputStream input = new FileInputStream(bpmnFile);

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(input, null);
		int eventType = xpp.getEventType();
		Bpmn bpmn = new Bpmn();

		while (eventType != XmlPullParser.START_TAG) {
			eventType = xpp.next();
		}
		if (xpp.getName().equals(bpmn.tag)) {
			bpmn.importElement(xpp, bpmn);
		} else {
			bpmn.log(bpmn.tag, xpp.getLineNumber(), "Expected " + bpmn.tag + ", got " + xpp.getName());
		}
		if (bpmn.hasErrors()) {
			//			context.getProvidedObjectManager().createProvidedObject("Log of BPMN import", bpmn.getLog(), XLog.class,
			//					context);
			return null;
		}

		return bpmn;

	}

}
