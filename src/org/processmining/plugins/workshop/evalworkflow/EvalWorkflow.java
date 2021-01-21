package org.processmining.plugins.workshop.evalworkflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
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
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.PetrinetWithMarkings;

@SuppressWarnings("deprecation")
public class EvalWorkflow {

	@Plugin(name = "Evaluation Workflow", parameterLabels = { "Event Log 1" }, returnLabels = { "Petri Net IM1",
			"Petri Net IM2", "Petri Net IM3" }, returnTypes = { Petrinet.class, Petrinet.class,
					Petrinet.class }, userAccessible = true, help = "Outputs the Petri Net from HILP")

	@UITopiaVariant(affiliation = "Uni Mannheim", author = "Antonina Prendi", email = "aprendi@mail.uni-mannheim.de")

	public static Object[] applyAll(UIPluginContext context, XLog example) {
		return convertToArray(context);
	}

	public static Object[] convertToArray(UIPluginContext context) {
		Collection<XLog> logs = null;
		Collection<Petrinet> pnCollection = new ArrayList<Petrinet>();
		try {
			logs = importLog("C:/Users/I519745/Desktop/Thesis/Thesis/Event Logs/");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (XLog log : logs) {
			Petrinet pn1, pn2, pn3 = null;
			pn1 = applyHILP(context, log);
			pn2 = applyInductiveMiner(context, log);
			try {
				pn3 = applyETM(context, log); //<--------------------------------Change this
			} catch (ConnectionCannotBeObtained e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pnCollection.add(pn1);
			pnCollection.add(pn2);
			pnCollection.add(pn3);
		}
		Object[] object = pnCollection.toArray(new Object[pnCollection.size()]);
		return object;

	}

	public static Collection<XLog> importLog(String pathToLog) throws Exception {
		return importLog(new File(pathToLog));
	}

	public static Collection<XLog> importLog(File logFiles) throws Exception {
		Collection<XLog> collectionLogs = new ArrayList<XLog>();
		for (File logFile : logFiles.listFiles()) {
			XUniversalParser parser = new XUniversalParser();
			Collection<XLog> logs = parser.parse(logFile);
			if (logs.size() > 0) {
				collectionLogs.add(logs.iterator().next());
			}
		}
		return collectionLogs;
	}

//	public static Petrinet applySplitMiner(XLog log) throws IOException {
//		Runtime rt = Runtime.getRuntime();
//		Process pr = rt.exec("java -cp sm2.jar;lib\\* au.edu.unimelb.services.ServiceProvider SM2 .\\repair.xes .\\bpmnmodel 0.05");
//	}

	public static Petrinet applyHILP(PluginContext context, XLog log) {
		//		Petrinet net = context.tryToFindOrConstructFirstNamedObject(Petrinet.class, "ILP-Based Process Discovery",
		//				Connection.class, "", log);
		//		return net;
		context.log("Started Hybrid ILP Miner", MessageLevel.NORMAL);
		XLogHybridILPMinerParametersImpl param = new XLogHybridILPMinerParametersImpl(context);
		HybridILPMinerPlugin hilp = new HybridILPMinerPlugin();
		Object[] obj = hilp.applyParams(context, log, param);
		Petrinet pnet = (Petrinet) obj[0];
		return pnet;

	}

	public static Petrinet applyInductiveMiner(PluginContext context, XLog log) {
		//		XEventClassifier classifier = MiningParameters.getDefaultClassifier();
		context.log("Started Inductive Miner", MessageLevel.NORMAL);
		MiningParametersIMf param = new MiningParametersIMf();
		IMPetriNet impetrinet = new IMPetriNet();
		Object[] obj = impetrinet.minePetriNetParameters(context, log, param);
		Petrinet pnet = (Petrinet) obj[0];
		return pnet;
	}

	@SuppressWarnings("deprecation")
	public static Petrinet applyETM(PluginContext context, XLog log) throws ConnectionCannotBeObtained {

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

}
