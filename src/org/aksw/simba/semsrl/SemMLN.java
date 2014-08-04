package org.aksw.simba.semsrl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.aksw.simba.semsrl.controller.AlchemyGraphTranslator;
import org.aksw.simba.semsrl.controller.CSVCrawler;
import org.aksw.simba.semsrl.controller.Crawler;
import org.aksw.simba.semsrl.controller.MappingFactory;
import org.aksw.simba.semsrl.controller.SparqlCrawler;
import org.aksw.simba.semsrl.io.Bundle;
import org.aksw.simba.semsrl.model.ConnectedGroup;
import org.aksw.simba.semsrl.model.DataSource;
import org.aksw.simba.semsrl.model.Mapping;
import org.aksw.simba.semsrl.model.ResourceGraph;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


/**
 * Statistical Relational Learning of Semantic Links using Markov Logic Networks.
 * 
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class SemMLN {

	/*
	 * Logger.
	 */
	private static Logger logger = Logger.getLogger(SemMLN.class);
	private static HashMap<String, Crawler> crawlers;
	
	private String propFile;

	public String getArgs() {
		return propFile;
	}

	public SemMLN(String propFile) {
		this.propFile = propFile;
		
		logger.setResourceBundle(ResourceBundle.getBundle("log4j"));
		Bundle.setBundleName(propFile);

		crawlers = new HashMap<>();
		crawlers.put("sparql", new SparqlCrawler());
		crawlers.put("csv", new CSVCrawler());
	}
	
	/**
	 * SemMLN main algorithm.
	 * TODO Part of this code is shared with SemSRL: arrange it better.
	 * 
	 * @throws IOException 
	 */
	public void learn() throws IOException {
		System.out.println("SemMLN started");
		Mapping mapping = MappingFactory.createMapping(propFile);
		
		ResourceGraph graph = new ResourceGraph(propFile);
		graph.setMapping(mapping);
		int iter = 0;
		
		for(ConnectedGroup cg : mapping.getGroups()) {
			Map<DataSource, String> map = cg.getMap();
			System.out.println(map);
			for(DataSource ds : map.keySet()) {
				System.out.println("source: "+ds);
				Crawler crawler;
				try {
					crawler = crawlers.get(ds.getStoreType());
				} catch (Exception e) {
					System.err.println("Error: store type "+ds.getStoreType()+" not recognised.");
					continue;
				}
				ResourceGraph rg = crawler.crawl(ds, map.get(ds));
				graph.merge(rg);
			}
			// add sameAs links
			addSameAs(cg, graph);

//			if(++iter == 1)
//				break;
		}
		
		AlchemyGraphTranslator gtran = new AlchemyGraphTranslator(graph);
		gtran.translate();
	}

	private void addSameAs(ConnectedGroup cg, ResourceGraph graph) { 
		Property sameAs = ResourceFactory.createProperty(Bundle.getString("owl_same_as"));
		ArrayList<Resource> res = new ArrayList<>();
		Map<DataSource, String> map = cg.getMap();
		for(DataSource ds : map.keySet())
			res.add(ResourceFactory.createResource(ds.getNamespace() + map.get(ds)));
		// now mutually create sameAs links
		for(Resource s : res)
			for(Resource o : res)
				if(s != o)
					graph.addLink(s, sameAs, o);
	}

	/**
	 * @param args The property file path.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SemMLN srl = new SemMLN(args[0]);
		srl.learn();
	}

}