package org.processmining.plugins.workshop.evalworkflow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

public class EvalWorkflow {

	@Plugin(name = "Evaluation Workflow", parameterLabels = { "Event Log 1" }, returnLabels = {
			"Petri Net 1" }, returnTypes = { Petrinet.class }, userAccessible = true, help = "Outputs the Petri Nets")

	@UITopiaVariant(affiliation = "University of Mannheim", author = "Antonina Prendi", email = "aprendi@mail.uni-mannheim.de")
	//	@PluginVariant(variantLabel = "Default Run", requiredParameterLabels = {})
	public static Object[] applyAll(UIPluginContext context, XLog log) {
		return convertToArray(context);
	}

	public static Object[] convertToArray(UIPluginContext context) {
		Collection<XLog> logs = null;
		//		Collection<Petrinet> pnCollection = new ArrayList<Petrinet>();
		try {
			logs = importLog("C:/Users/I519745/Desktop/Thesis/Thesis/EventLogs/");
			//			logs = importLog("C:/Users/I519745/Desktop/Thesis/Thesis/structuredminer/logs/");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ProcessDiscoveryMethods pd = new ProcessDiscoveryMethods();
		ArrayList<EvaluationResults> eRes = new ArrayList<EvaluationResults>();

		int index = 0;
		try {
			FileUtils.cleanDirectory(new File("C:/Users/I519745/Desktop/Thesis/Thesis/PetriNets/"));
			FileUtils.cleanDirectory(new File("C:/Users/I519745/Desktop/Thesis/Thesis/split-miner-2.0/Outputs/"));
			FileUtils.cleanDirectory(new File("C:/Users/I519745/Desktop/Thesis/Thesis/structuredminer/Outputs/"));
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		for (XLog log : logs) {
			index = index + 1;
			System.out.println("Event Log " + index);				

			pd.applyHILP(context, log, eRes, index);

			pd.applyInductiveMiner(context, log, eRes, index);

			try {
				pd.applySplitMiner(context, log, eRes, index);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (!log.getAttributes().get("name").toString().equals("9.Artificial4.xes")
					&& !log.getAttributes().get("name").toString().equals("14.RoadTrafficManagement.xes")) {
				try {
					pd.applyStructuredMiner(context, log, eRes, index);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		//		Object[] object = pnCollection.toArray(new Object[pnCollection.size()]);

		try {
			createCSVFile(eRes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public static void createCSVFile(ArrayList<EvaluationResults> eRes) throws IOException {
		FileWriter fw = new FileWriter("C:/Users/I519745/Desktop/Thesis/Thesis/evaluationresults.csv");
		try (CSVPrinter printer = new CSVPrinter(fw,
				CSVFormat.DEFAULT
						.withHeader("Event Log", "Process Discovery Method", "Conformance Checking Method",
								"Calculation Time ms", "Trace Fitness", "Max Fitness Cost", "Raw Fitness Cost", "Precision", "Generalization")
						.withRecordSeparator("\n"))) {
			for (EvaluationResults result : eRes) {
				printer.printRecord(result.eventLog, result.processDisc, result.confCheck, result.calcTime,
						result.traceFitness, result.maxFitnessCost, result.rawFitnessCost, result.precision, result.generalization);
			}
			fw.flush();
			fw.close();
			System.out.println("Created CSV File with results");
		} catch (IOException e) {
			e.printStackTrace();
		}

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
				logs.iterator().next().getAttributes().put("name",
						new XAttributeLiteralImpl("Name", logFile.getName()));
				collectionLogs.add(logs.iterator().next());
			}
		}
		return collectionLogs;
	}

}
