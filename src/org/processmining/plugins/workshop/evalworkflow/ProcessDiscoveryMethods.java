package org.processmining.plugins.workshop.evalworkflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.hybridilpminer.parameters.XLogHybridILPMinerParametersImpl;
import org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.bpmn.Bpmn;
import org.processmining.plugins.etm.ETM;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.model.narytree.conversion.NAryTreeToProcessTree;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.processmining.plugins.etm.parameters.ETMParamFactory;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class ProcessDiscoveryMethods {
	
	public Petrinet applyHILP(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes, int index) {
		//		Petrinet net = context.tryToFindOrConstructFirstNamedObject(Petrinet.class, "ILP-Based Process Discovery",
		//				Connection.class, "", log);
		//		return net;
//		context.log("Started Hybrid ILP Miner", MessageLevel.NORMAL);
		XLogHybridILPMinerParametersImpl param = new XLogHybridILPMinerParametersImpl(context, log, new XEventNameClassifier());
		HybridILPMinerPlugin hilp = new HybridILPMinerPlugin();
		Object[] obj = hilp.applyParams(context, log, param);
		Petrinet pnet = (Petrinet) obj[0];
		return pnet;

	}

	public Petrinet applyInductiveMiner(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes, int index) {
		//		XEventClassifier classifier = MiningParameters.getDefaultClassifier();
//		context.log("Started Inductive Miner", MessageLevel.NORMAL);
		MiningParametersIMf param = new MiningParametersIMf();
		IMPetriNet impetrinet = new IMPetriNet();
		Object[] obj = impetrinet.minePetriNetParameters(context, log, param);
		Petrinet pnet = (Petrinet) obj[0];
		
		//call all conformance checking techniques here
		ConformanceCheckingMethods ccm = new ConformanceCheckingMethods();
		PNRepResult repResult1 = ccm.applyPNLogReplayer(context, log, pnet);
		double traceFitness1 = ccm.getTraceFitness(repResult1);
		
		String evLogDescr = "Event Log " + Integer.toString(index);
		eRes.add(new EvaluationResults(evLogDescr, "Inductive Miner", "PNReplayer", traceFitness1));
		
		PNRepResult repResult2 = ccm.applyDecomposedReplayer(context, log, pnet);
		double traceFitness2 = ccm.getTraceFitness(repResult2);
		
		eRes.add(new EvaluationResults(evLogDescr, "Inductive Miner", "PNReplayer", traceFitness2));
		
		return pnet;
	}

	@SuppressWarnings("deprecation")
	public Petrinet applyETM(PluginContext context, XLog log, ArrayList<EvaluationResults> eRes, int index) {

		ETMParam param = ETMParamFactory.buildStandardParam(log);
		ETM etm = new ETM(param);
		etm.run();
		NAryTree tree = etm.getResult();
		ProcessTree processTree = NAryTreeToProcessTree.convert(tree, param.getCentralRegistry().getEventClasses());
		PetrinetWithMarkings petrinetwithmarkings = null;
		try {
			petrinetwithmarkings = ProcessTree2Petrinet.convert(processTree);
		} catch (NotYetImplementedException e) {
			e.printStackTrace();
		} catch (InvalidProcessTreeException e) {
			e.printStackTrace();
		}
		return petrinetwithmarkings.petrinet;
	}

	public static Petrinet applySplitMiner(XLog log, ArrayList<EvaluationResults> eRes, int index) throws IOException {
		String logPath = null, statement = null;
		XAttributeMap attrMap = log.getAttributes();
		logPath = attrMap.get("path").toString();
		String outputPath = " C:\\Users\\I519745\\Desktop\\Thesis\\Thesis\\BPMNModelsSM\\bpmnmodelprom ";
		statement = "java -cp sm2.jar;lib\\* au.edu.unimelb.services.ServiceProvider SM2 " + logPath
				+ outputPath + "0.05";
		Runtime rt = Runtime.getRuntime();
		File dir = new File("C:\\Users\\I519745\\Desktop\\Thesis\\Thesis\\split-miner-2.0");
		Process pr = rt.exec(new String[]{"java", "-cp", "sm2.jar;lib\\*", "au.edu.unimelb.services.ServiceProvider", "SM2", logPath, ".\\bpmnmodelprom", "0.05"}, null, dir);

		try {
			Bpmn bpmnModel = importBPMN("C:\\Users\\I519745\\Desktop\\Thesis\\Thesis\\split-miner-2.0\\bpmnmodelprom.bpmn");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public static Bpmn importBPMN(String pathToModel) throws Exception {
		File bpmnFile = new File(pathToModel);
//		long fileSizeInBytes = bpmnFile.length();
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
//		BpmnDiagrams diagrams = new BpmnDiagrams();
//		diagrams.setBpmn(bpmn);
//		diagrams.setName(filename);
//		diagrams.addAll(bpmn.getDiagrams());
		return bpmn;

	}


}
