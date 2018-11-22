package it.unibo.cs.gauss.istar2SGO;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;


public class istar2SGO {
	
	static String infile = "data/input/pistar_smarthome_v6.txt";
	static String outfile = "data/output/pistar_smarthome_v6.ttl";
	
	public static void main(String[] args) throws RDFHandlerException, IOException{
		
		HashMap<String, Object> parsedData = parsePiStarFile(infile);
		HashMap<String, Actor> actors = (HashMap<String, Actor>)parsedData.get("actors");
		HashMap<String, Link> links = (HashMap<String, Link>)parsedData.get("links");
		HashMap<String, Node> nodes = (HashMap<String, Node>)parsedData.get("nodes");
		
		ModelBuilder builder = new ModelBuilder();
		builder
			.setNamespace("asgo", "http://www.gauss.it/extensions/sgo/adaptive/")
			.setNamespace("gsh", "http://www.gauss.it/examples/smarthome/")
			.setNamespace("owl", "http://www.w3.org/2002/07/owl#")
			.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
			.setNamespace("sgo", "http://purl.org/gauss-unibo/sgo/")
			.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
			
		for (String linkId : links.keySet()) {
			
			Link link = links.get(linkId);
			
			if (isLinkContribution(link)) {
				// CONTRIBUTION LINKS
				String contributionId = link.getId();
				String contributionType = link.getLabel();
				
				String sourceId = link.getSource();
				Node source = nodes.get(sourceId);
				String sourceType = source.getType();
				
				if (testTask(source)) {
					String targetId = link.getTarget();
					Node target = nodes.get(targetId);
					String targetType = target.getType();
					
					// ISTAR METRIC
					builder.subject("gsh:istarMetric")
						.add(RDF.TYPE, "sgo:OrdinalMetric")
						.add("sgo:hasMaxValueItem", "gsh:istarMetric_makeValueItem")
						.add("sgo:hasMinValueItem", "gsh:istarMetric_breakValueItem");
					builder.subject("gsh:istarMetric_makeValueItem")
						.add(RDF.TYPE, "sgo:ValueItem")
						.add("sgo:hasContentValue", "gsh:istarMetric_makeValue")
						.add("sgo:hasNext", "gsh:istarMetric_helpValueItem");
					builder.subject("gsh:istarMetric_helpValueItem")
						.add(RDF.TYPE, "sgo:ValueItem")
						.add("sgo:hasContentValue", "gsh:istarMetric_helpValue")
						.add("sgo:hasNext", "gsh:istarMetric_zeroValueItem");
					builder.subject("gsh:istarMetric_zeroValueItem")
						.add(RDF.TYPE, "sgo:ValueItem")
						.add("sgo:hasContentValue", "gsh:istarMetric_zeroValue")
						.add("sgo:hasNext", "gsh:istarMetric_hurtValueItem");
					builder.subject("gsh:istarMetric_hurtValueItem")
						.add(RDF.TYPE, "sgo:ValueItem")
						.add("sgo:hasContentValue", "gsh:istarMetric_hurtValue")
						.add("sgo:hasNext", "gsh:istarMetric_brekValueItem");
					builder.subject("gsh:istarMetric_breakValueItem")
						.add(RDF.TYPE, "sgo:ValueItem")
						.add("sgo:hasContentValue", "gsh:istarMetric_breakValue");
					builder.subject("gsh:istarMetric_makeValue")
						.add(RDF.TYPE, "sgo:Value")
						.add("sgo:value", 2);
					builder.subject("gsh:istarMetric_helpValue")
						.add(RDF.TYPE, "sgo:Value")
						.add("sgo:value", 1);
					builder.subject("gsh:istarMetric_zeroValue")
						.add(RDF.TYPE, "sgo:Value")
						.add("sgo:value", 0);
					builder.subject("gsh:istarMetric_hurtValue")
						.add(RDF.TYPE, "sgo:Value")
						.add("sgo:value", -1);
					builder.subject("gsh:istarMetric_breakValue")
						.add(RDF.TYPE, "sgo:Value")
						.add("sgo:value", -2);
					
					// FERCONFIGURATION POLICY DEFINITION
					builder.subject("asgo:ReconfigurationPolicy")
						.add(RDF.TYPE, OWL.CLASS)
						.add(RDFS.SUBCLASSOF, "sgo:Task");

					// TASK
					builder.subject("gsh:" + sourceId)
						.add(RDF.TYPE, "asgo:ReconfigurationPolicy")
						.add(RDFS.LABEL, source.getText());
					
					// MOTIVATION ELEMENT
					if (targetType.equals("istar.Goal")) {
						// Never (in iStar)
						;
					} else if (targetType.equals("istar.Quality")) {
						builder.subject("gsh:" + targetId)
							.add(RDF.TYPE, "sgo:Quality")
							.add(RDFS.LABEL, target.getText());						
					} else {
						System.err.println("ERROR in target element");
					}
					
					// INFLUENCE
					builder.subject("gsh:" + contributionId)
						.add(RDF.TYPE,	"sgo:Influence")
						.add("sgo:hasSubject", "gsh:" + sourceId)
						.add("sgo:contributesTo", "gsh:" + targetId)
						.add("sgo:hasValue", "gsh:istarMetric_" + contributionType + "Value")
						.add("sgo:basedOnMetric", "gsh:istarMetric");
				}	
			} 
			else if (link.getType().equals("istar.OrRefinementLink") || link.getType().equals("istar.AndRefinementLink")) {
				// REFINEMENT LINKS
				String refinementId = link.getId();
				String refinementType = "";
				if (link.getType().equals("istar.OrRefinementLink")) {
					refinementType = "orRefinement";
				} else {
					refinementType = "andRefinement";
				}
				
				String sourceId = link.getSource();
				Node source = nodes.get(sourceId);
				String sourceType = source.getType();
				
				String targetId = link.getTarget();
				Node target = nodes.get(targetId);
				String targetType = target.getType();
				
				// SGO defines Task refinements
				if (true) {//sourceType.equals("istar.Task") && targetType.equals("istar.Task")) {
					builder.subject("gsh:" + sourceId)
						//.add(RDF.TYPE, "sgo:Task")
						.add("sgo:" + refinementType, "gsh:" + targetId);
					//builder.subject("gsh:" + targetId)
					//	.add(RDF.TYPE, "sgo:Task");
				}
			}
		}
		
		for (String nodeId : nodes.keySet()) {
			Node node = nodes.get(nodeId);
			if (node.getType().equals("istar.Goal")) {
				builder.subject("gsh:" + nodeId)
					.add(RDF.TYPE, "sgo:Goal");
			} else if (node.getType().equals("istar.Task")) {
				builder.subject("gsh:" + nodeId)
					.add(RDF.TYPE, "sgo:Task");
			} else if (node.getType().equals("istar.Quality")) {
				builder.subject("gsh:" + nodeId)
					.add(RDF.TYPE, "sgo:Quality");
			}
			builder.subject("gsh:" + nodeId)
				.add(RDFS.LABEL, node.getText());
		}
		
		Model model = builder.build();
		Rio.write(model, System.out, RDFFormat.TURTLE);
		Rio.write(model, new FileWriter(outfile), RDFFormat.TURTLE);
	}
	
	/**
	 * Tests if the provided link is a Contribution link (i.e. Make, Help, Hurt, Break)
	 * @param l
	 * @return
	 */
	public static boolean isLinkContribution(Link l) {
		if (l.getLabel() != null) {
			//System.out.println(l.getLabel());
			return true;
		}
		//System.out.println("False");
		return false;
	}
	
	/**
	 * Tests if the provided node is a Task.
	 * Other tests (i.e. check if the task name contains "<policy>") could be implemented here.
	 * @param n
	 * @return
	 */
	public static boolean testTask(Node n) {
		// TODO: other tests?
		if (n.getType().equals("istar.Task")) {
			return true;
		}
		return false;
	}
	
	public static HashMap<String, Object> parsePiStarFile(String filename) throws FileNotFoundException {
		HashMap<String, Object> res = new HashMap<String, Object>();
		HashMap<String, Actor> actors = new HashMap<String, Actor>();
		res.put("actors", actors);
		HashMap<String, Link> links = new HashMap<String, Link>();
		res.put("links", links);
		HashMap<String, Node> nodes = new HashMap<String, Node>();
		res.put("nodes", nodes);
		
		BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
		
	    Gson gson = new Gson();
	    Object json = gson.fromJson(bufferedReader, Object.class);
		//System.out.println(json.getClass());
		//System.out.println(json.toString());
		LinkedTreeMap<String,ArrayList<Object>> jsonMap = (LinkedTreeMap<String,ArrayList<Object>>)json;
		
		/**
		 * Actors
		 */
		Object jsonActors = jsonMap.get("actors");
		//System.out.println("\tActors");
		ArrayList<Object> jsonActorsList = (ArrayList<Object>)jsonActors;
		//int size = actors_list.size();
		
		for (Object jsonActor : jsonActorsList) {
			
			Actor actor = new Actor();
			
			//System.out.println("\t\tActor");
			LinkedTreeMap<String,Object> jsonActorMap = (LinkedTreeMap<String,Object>) jsonActor;
			//actor.setId((String)jsonActorMap.get("id"));
			//actors.add(actor);
			String id = (String)jsonActorMap.get("id");
			actor.setId(id);
			actors.put(id, actor);
			
			actor.setText((String)jsonActorMap.get("text"));
			actor.setType((String)jsonActorMap.get("type"));
			actor.setX((Double)jsonActorMap.get("x"));
			actor.setY((Double)jsonActorMap.get("y"));
			ArrayList<Object> jsonNodesList = (ArrayList<Object>)jsonActorMap.get("nodes");
			for (Object jsonNode : jsonNodesList) {
				LinkedTreeMap<String,Object> jsonNodeMap = (LinkedTreeMap<String,Object>) jsonNode;
				Node node = new Node();
				//node.setId((String)jsonNodeMap.get("id"));
				String nodeId = (String)jsonNodeMap.get("id");
				node.setId(nodeId);
				// Nodes are both in Actor and in the node list
				nodes.put(nodeId, node);
				actor.addNode(node);
				node.setText((String)jsonNodeMap.get("text"));
				node.setType((String)jsonNodeMap.get("type"));
				node.setX((Double)jsonNodeMap.get("x"));
				node.setY((Double)jsonNodeMap.get("y"));
				
			}
		}
		
		/**
		 * Links
		 */
		//System.out.println("\n\n\tLinks");
		Object jsonLinks = jsonMap.get("links");
		ArrayList<Object> jsonLinksList = (ArrayList<Object>)jsonLinks;
		for (Object jsonLink : jsonLinksList) {
			
			Link link = new Link();
			
			//System.out.println("\t\tLink");
			LinkedTreeMap<String,Object> jsonLinkMap = (LinkedTreeMap<String,Object>) jsonLink;
			//link.setId((String)jsonLinkMap.get("id"));
			//links.add(link);
			String id = (String)jsonLinkMap.get("id");
			link.setId(id);
			links.put(id, link);
			
			link.setType((String)jsonLinkMap.get("type"));
			link.setSource((String)jsonLinkMap.get("source"));
			link.setTarget((String)jsonLinkMap.get("target"));
			if (jsonLinkMap.containsKey("label")) {
				link.setLabel((String)jsonLinkMap.get("label"));
			}
		}
		
		return res;
	}
	
}
