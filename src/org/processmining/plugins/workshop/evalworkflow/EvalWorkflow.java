package org.processmining.plugins.workshop.evalworkflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.base.Pnml;

public class EvalWorkflow {

	@Plugin(name = "Evaluation Workflow", parameterLabels = { "Event Log 1" }, returnLabels = { "Petri Net 1",
			"Petri Net 2", "Petri Net 3", "Petri Net 4" }, returnTypes = { Petrinet.class, Petrinet.class,
					Petrinet.class, Petrinet.class }, userAccessible = true, help = "Outputs the Petri Nets")

	@UITopiaVariant(affiliation = "University of Mannheim", author = "Antonina Prendi", email = "aprendi@mail.uni-mannheim.de")
	//	@PluginVariant(variantLabel = "Default Run", requiredParameterLabels = {})
	public static Object[] applyAll(UIPluginContext context, XLog log) {
		return convertToArray(context);
	}

	public static Object[] convertToArray(UIPluginContext context) {
		Collection<XLog> logs = null;
		Collection<Petrinet> pnCollection = new ArrayList<Petrinet>();
		try {
			//			logs = importLog("C:/Users/I519745/Desktop/Thesis/Thesis/EventLogs/");
			logs = importLog("C:/Users/I519745/Desktop/Thesis/Thesis/structuredminer/logs/");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ProcessDiscoveryMethods pd = new ProcessDiscoveryMethods();
		ArrayList<EvaluationResults> eRes = new ArrayList<EvaluationResults>();

		int index = 0;
		for (XLog log : logs) {
			index = index + 1;
			Petrinet pn1 = null, pn2 = null, pn3 = null, pn4 = null, pn5 = null;

			pn1 = pd.applyHILP(context, log, eRes, index);
			pnCollection.add(pn1);
			String path1 = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinetHILP" + Integer.toString(index) + ".pnml";
			File pn1f = new File(path1);
			try {
				savePetrinetToPnml(pn1, null, pn1f);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			pn2 = pd.applyInductiveMiner(context, log, eRes, index);
			context.log("Finished Inductive Miner", MessageLevel.NORMAL);
			pnCollection.add(pn2);
			String path2 = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinetInductive" + Integer.toString(index) + ".pnml";
			File pn2f = new File(path2);
			try {
				savePetrinetToPnml(pn2, null, pn2f);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			//						try {
			//							pn3 = pd.applyETM(context, log, eRes, index); //<--------------------------------Change this
			//						} catch (ConnectionCannotBeObtained e) {
			//							// TODO Auto-generated catch block
			//							e.printStackTrace();
			//						}
			//						pnCollection.add(pn3);

			try {
				pn4 = pd.applySplitMiner(context, log, eRes, index);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pnCollection.add(pn4);
			String path4 = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinetsplitminer" + Integer.toString(index) + ".pnml";
			File pn4f = new File(path4);
			try {
				savePetrinetToPnml(pn4, null, pn4f);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				pn5 = pd.applyStructuredMiner(context, log, eRes, index);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pnCollection.add(pn5);
			String path5 = "C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/petrinetstructured" + Integer.toString(index) + ".pnml";
			File pn5f = new File(path5);
			try {
				savePetrinetToPnml(pn5, null, pn5f);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		
		Object[] object = pnCollection.toArray(new Object[pnCollection.size()]);

		try {
			createCSVFile(eRes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return object;

	}

	public static void createCSVFile(ArrayList<EvaluationResults> eRes) throws IOException {
		FileWriter fw = new FileWriter("C:/Users/I519745/Desktop/Thesis/Thesis/evaluationresults.csv");
		try (CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT
				.withHeader("Event Log", "Process Discovery Method", "Conformance Checking Method", "Calculation Time ms", "Trace Fitness", "Max Fitness Cost", "Raw Fitness Cost").withRecordSeparator("\n"))) {
			for (EvaluationResults result : eRes) {
				printer.printRecord(result.eventLog, result.processDisc, result.confCheck, result.calcTime, result.traceFitness, result.maxFitnessCost, result.rawFitnessCost);
			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static void savePetrinetToPnml(Petrinet net, Marking marking, File file) throws IOException {

        if(marking==null)
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

	public static Collection<XLog> importLog(String pathToLog) throws Exception {
		return importLog(new File(pathToLog));
	}

	public static Collection<XLog> importLog(File logFiles) throws Exception {
		Collection<XLog> collectionLogs = new ArrayList<XLog>();
		for (File logFile : logFiles.listFiles()) {
			XUniversalParser parser = new XUniversalParser();
			Collection<XLog> logs = parser.parse(logFile);
			if (logs.size() > 0) {
				logs.iterator().next().getAttributes().put("path",
						new XAttributeLiteralImpl("Path", logFile.getAbsolutePath()));
				collectionLogs.add(logs.iterator().next());
			}
		}
		return collectionLogs;
	}

}
