package org.processmining.plugins.workshop.evalworkflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

@SuppressWarnings("deprecation")
public class EvalWorkflow {

	@Plugin(name = "Evaluation Workflow", parameterLabels = { "Event Log 1" }, returnLabels = { "Evaluation Results" }, returnTypes = { EvaluationResults.class }, userAccessible = true, help = "Outputs the Petri Net from HILP")

	public static void main(String[] args) throws Exception {

		EvalWorkflow plugin = new EvalWorkflow();
		plugin.applyAll(null);

		System.out.println("Done.");
	}

	@UITopiaVariant(affiliation = "University of Mannheim", author = "Antonina Prendi", email = "aprendi@mail.uni-mannheim.de")
	@PluginVariant(variantLabel = "Default Run", requiredParameterLabels = {})
	public void applyAll(UIPluginContext context) {
		//		return convertToArray(context);

		Collection<XLog> logs = null;
		Collection<Petrinet> pnCollection = new ArrayList<Petrinet>();
		try {
			logs = importLog("C:/Users/I519745/Desktop/Thesis/Thesis/EventLogs/");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ProcessDiscoveryMethods pd = new ProcessDiscoveryMethods();
		ArrayList<EvaluationResults> eRes = new ArrayList<EvaluationResults>();
		
		int index = 0;
		for (XLog log : logs) {
			index = index +1;
			Petrinet pn1, pn2, pn3, pn4 = null;

			//			pn1 = pd.applyHILP(context, log, eRes, index);

			pn2 = pd.applyInductiveMiner(context, log, eRes, index);

			//			try {
			//				pn3 = pd.applyETM(context, log, eRes, index); //<--------------------------------Change this
			//			} catch (ConnectionCannotBeObtained e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}

			//			try {
			//				pn4 = pd.applySplitMiner(log, eRes, index);
			//			} catch (IOException e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}

			//			pnCollection.add(pn1);
			pnCollection.add(pn2);
			//			pnCollection.add(pn3);
			//			pnCollection.add(pn4);
		}

	}

//	public Object[] convertToArray(UIPluginContext context) {
//
//		Object[] object = pnCollection.toArray(new Object[pnCollection.size()]);
//		return object;
//
//	}

	public Collection<XLog> importLog(String pathToLog) throws Exception {
		return importLog(new File(pathToLog));
	}

	public Collection<XLog> importLog(File logFiles) throws Exception {
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

}
