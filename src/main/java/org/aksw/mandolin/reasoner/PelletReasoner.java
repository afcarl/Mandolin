package org.aksw.mandolin.reasoner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Iterator;

import org.aksw.mandolin.util.Timer;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Pellet-Jena reasoner. The inferred closure model is saved in file; it will
 * not be available as an in-memory object.
 * 
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class PelletReasoner {

	private final static Logger logger = LogManager.getLogger(PelletReasoner.class);
	
	public static void main(String[] args) {
		testThis();
//		 run("eval/0001");
	}

	/**
	 * Add OWL rules and compute the forward chain.
	 * 
	 * @param base
	 * @param datasetPaths
	 */
	public static void run(String base) {

		Reasoner reasoner = PelletReasonerFactory.theInstance().create();
		OntModel ontModel = ModelFactory
				.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);

		String path = System.getProperty("user.dir");
		RDFDataMgr.read(infModel, "file://" + path + "/" + base + "/model.nt");

		logger.info("Model size = " + ontModel.size());

		ValidityReport report = infModel.validate();
		printIterator(report.getReports(), "Validation Results");

		logger.info("Inferred model size = " + infModel.size());

		infModel.enterCriticalSection(Lock.READ);

		try {
			RDFDataMgr.write(new FileOutputStream(new File(base
					+ "/model-fwc.nt")), infModel, Lang.NT);
			logger.info("Model generated.");
		} catch (FileNotFoundException e) {
			logger.fatal(e.getMessage());
			throw new RuntimeException("Necessary file model-fwc.nt was not generated.");
		} finally {
			infModel.leaveCriticalSection();
		}

		new File(base + "/model.nt").delete();

	}
	
	public static void closure(String input, String output) {
		
		Reasoner reasoner = PelletReasonerFactory.theInstance().create();
		OntModel ontModel = ModelFactory
				.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);

		String path = System.getProperty("user.dir");
		RDFDataMgr.read(infModel, "file://" + path + "/" + input);

		logger.info("Model = "+input+", size = " + ontModel.size());

		ValidityReport report = infModel.validate();
		printIterator(report.getReports(), "Validation Results");

		logger.info("Inferred model size = " + infModel.size());

		infModel.enterCriticalSection(Lock.READ);

		try {
			RDFDataMgr.write(new FileOutputStream(new File(output)), 
					infModel, Lang.NT);
			logger.info("Model generated at "+output);
		} catch (FileNotFoundException e) {
			logger.fatal(e.getMessage());
			throw new RuntimeException("Necessary file "+output+" was not generated.");
		} finally {
			infModel.leaveCriticalSection();
		}
		
	}

	private static void testThis() {
		
		Timer t = new Timer();

		Reasoner reasoner = PelletReasonerFactory.theInstance().create();
		OntModel ontModel = ModelFactory
				.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);
		
		t.lap();

		String path = System.getProperty("user.dir");

		String[] paths = { "file://" + path + "/datasets/DBLPL3S-100.nt",
				"file://" + path + "/datasets/LinkedACM-100.nt",
				"file://" + path + "/linksets/DBLPL3S-LinkedACM-100.nt" };

		StreamRDF dataStream = new StreamRDF() {

			@Override
			public void start() {
			}

			@Override
			public void quad(Quad quad) {
			}

			@Override
			public void base(String base) {
			}

			@Override
			public void prefix(String prefix, String iri) {
			}

			@Override
			public void finish() {
			}

			@Override
			public void triple(Triple triple) {
				Node node = triple.getObject();
				if (node.isLiteral()) {
					if (!node.getLiteral().isWellFormed()) {
						// known issue: fix gYear literals
						if (node.getLiteralDatatypeURI() != null) {
							if (node.getLiteralDatatypeURI().equals(
									XSD.gYear.getURI())
									|| node.getLiteralDatatypeURI().equals(
											XSD.gYear.getLocalName())) {
								Node newNode = NodeFactory.createLiteral(node
										.getLiteral().toString()
										.substring(0, 4)
										+ "^^" + XSD.gYear);
								triple = new Triple(triple.getSubject(),
										triple.getPredicate(), newNode);
//								logger.warn("Bad-formed literal: "
//										+ node + " - Using: " + newNode);
							}
						}
					}
				}

				Resource s = infModel.createResource(triple.getSubject()
						.getURI());
				Property p = infModel.createProperty(triple.getPredicate()
						.getURI());
				RDFNode o = infModel.asRDFNode(triple.getObject());

				infModel.add(s, p, o);
			}

		};

		for (String p : paths)
			RDFDataMgr.parse(dataStream, p);

		t.lap();

		logger.info("Model size = " + ontModel.size());

		ValidityReport report = infModel.validate();
		printIterator(report.getReports(), "Validation Results");

		logger.info("Inferred model size = " + infModel.size());

		infModel.enterCriticalSection(Lock.READ);

		String f = "tmp/test-this.nt";
		try {
			RDFDataMgr.write(new FileOutputStream(new File(f)),
					infModel, Lang.NT);
			logger.info("Model generated.");
		} catch (FileNotFoundException e) {
			logger.fatal(e.getMessage());
			throw new RuntimeException("Necessary file "+f+" was not generated.");
		} finally {
			infModel.leaveCriticalSection();
		}

		t.lap();

		logger.info("Reasoner init (ms): " + t.getLapMillis(0));
		logger.info("Model load (ms): " + t.getLapMillis(1));
		logger.info("Model load (ms/triple): " + t.getLapMillis(1)
				/ infModel.size());
		logger.info("Validation (ms): " + t.getLapMillis(2));
		logger.info("Save inferred model (ms): " + t.getLapMillis(3));
		printIterator(report.getReports(), "Validation Results");

	}

	private static void printIterator(Iterator<?> i, String header) {
		logger.info(header);

		if (i.hasNext()) {
			while (i.hasNext())
				logger.info(i.next());
		} else
			logger.info("<Nothing to say.>");

		logger.info("");
	}

}
