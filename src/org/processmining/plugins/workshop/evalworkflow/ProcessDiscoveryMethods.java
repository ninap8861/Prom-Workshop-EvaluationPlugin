package org.processmining.plugins.workshop.evalworkflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.hybridilpminer.parameters.XLogHybridILPMinerParametersImpl;
import org.processmining.hybridilpminer.plugins.HybridILPMinerPlugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
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
		PNRepResult repResult = ccm.applyPNLogReplayer(context, log, pnet);
		double traceFitness = ccm.getTraceFitness1(repResult);
		
		String evLogDescr = "Event Log " + Integer.toString(index);
		eRes.add(new EvaluationResults(evLogDescr, "Inductive Miner", "PNReplayer", traceFitness));
		
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
		logPath = findLogPath(log);

		statement = "java -cp sm2.jar;lib\\* au.edu.unimelb.services.ServiceProvider SM2" + logPath
				+ ".\\bpmnmodel 0.05";
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(statement);

		return null;
	}

	public static String findLogPath(XLog log) {
		String path = null;
		File logFiles = new File("C:/Users/I519745/Desktop/Thesis/Thesis/EventLogs/");
		for (File logFile : logFiles.listFiles()) {
			XUniversalParser parser = new XUniversalParser();
			Collection<XLog> parsedLog = null;
			try {
				parsedLog = parser.parse(logFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (parsedLog.iterator().next().equals(log)) { //doesnt work
				path = logFile.getAbsolutePath();
				break;
			}
		}
		if (path != null) {
			return path;
		} else {
			return null;
		}

	}


}
