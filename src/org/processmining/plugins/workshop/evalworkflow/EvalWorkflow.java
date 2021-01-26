package org.processmining.plugins.workshop.evalworkflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

@SuppressWarnings("deprecation")
public class EvalWorkflow {

	@Plugin(name = "Evaluation Workflow", parameterLabels = { "Event Log 1" }, returnLabels = { "Petri Net IM1",
			"Petri Net IM2", "Petri Net IM3" }, returnTypes = { Petrinet.class, Petrinet.class,
					Petrinet.class }, userAccessible = true, help = "Outputs the Petri Net from HILP")

	@UITopiaVariant(affiliation = "Uni Mannheim", author = "Antonina Prendi", email = "aprendi@mail.uni-mannheim.de")

	public Object[] applyAll(UIPluginContext context, XLog example) {
		return convertToArray(context);
	}

	public Object[] convertToArray(UIPluginContext context) {
		Collection<XLog> logs = null;
		Collection<Petrinet> pnCollection = new ArrayList<Petrinet>();
		try {
			logs = importLog("C:/Users/I519745/Desktop/Thesis/Thesis/EventLogs/");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ProcessDiscoveryMethods pd = new ProcessDiscoveryMethods();
		
		for (XLog log : logs) {
			Petrinet pn1, pn2, pn3, pn4 = null;
//			pn1 = pd.applyHILP(context, log);
//			pn2 = pd.applyInductiveMiner(context, log);
//			try {
//				pn3 = pd.applyETM(context, log); //<--------------------------------Change this
//			} catch (ConnectionCannotBeObtained e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			try {
				pn4 = pd.applySplitMiner(log);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			pnCollection.add(pn1);
//			pnCollection.add(pn2);
//			pnCollection.add(pn3);
			pnCollection.add(pn4);
		}
		

		Object[] object = pnCollection.toArray(new Object[pnCollection.size()]);
		return object;

	}

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
