package org.netkernel.neo4j.embedded.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Processing
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;

// Author: Tom Geudens
// Date  : 2012/10/03

// The usual suspects for an accessor
import org.netkernel.layer0.meta.impl.SourcedArgumentMetaImpl;
import org.netkernel.layer0.nkf.INKFLocale;
import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFRequestReadOnly;
import org.netkernel.layer0.nkf.NKFException;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.neo4j.embedded.representation.Neo4jInstance;
import org.netkernel.neo4j.embedded.representation.Neo4jNode;

public class Neo4jRetrieveAccessor extends StandardAccessorImpl {

	/**
	 * Argument for GraphDatabaseService.isAvailable(), which defines the
	 * duration after which the database is considered unavailable.
	 */
	private static final long NEO4J_TIMEOUT = 200;

	public Neo4jRetrieveAccessor() {
		this.declareThreadSafe();
		this.declareSourceRepresentation(Neo4jNode[].class);
		this.declareArgument(new SourcedArgumentMetaImpl("graph", null, null, new Class[] { String.class }));
		this.declareArgument(new SourcedArgumentMetaImpl("entity", null, null, new Class[] { String.class }));
		this.declareArgument(new SourcedArgumentMetaImpl("properties", null, null, new Class[] { HashMap.class }));
		this.declareArgument(new SourcedArgumentMetaImpl("instance", null, null, new Class[] { Neo4jInstance.class }));
	}

	@SuppressWarnings("unchecked")
	public void onSource(INKFRequestContext aContext) throws Exception {
		// SOURCE requires one argument instance and either the graph or the
		// entity argument.
		// The properties argument is optional.

		// Verify graph
		String aGraph = null;
		if (aContext.getThisRequest().argumentExists("graph")) {
			aGraph = aContext.getThisRequest().getArgumentValue("graph");
			if (aGraph.startsWith("pbv:graph")) {
				aGraph = aContext.source("arg:graph", String.class);
			}
		}
		if ("".equals(aGraph)) {
			throw new NKFException("request does not have a valid - graph - argument");
		}
		else if (aGraph!=null) {
			aContext.logRaw(INKFLocale.LEVEL_INFO, "aGraph is not null, aGraph="+aGraph);
		}

		// Verify entity
		String aEntity = null;
		if (aContext.getThisRequest().argumentExists("entity")) {
			aEntity = aContext.getThisRequest().getArgumentValue("entity");
			if (aEntity.startsWith("pbv:entity")) {
				aEntity = aContext.source("arg:entity", String.class);
			}
		}
		if ("".equals(aEntity)) {
			throw new NKFException("request does not have a valid - entity - argument");
		}
		else if (aEntity!=null) {
			aContext.logRaw(INKFLocale.LEVEL_INFO, "aEntity is not null, aEntity="+aEntity);
		}

		// Verify we have either graph or entity, but not both
		if ( ! ( (aEntity == null) ^ (aGraph == null) ) ) {
			throw new NKFException("request requires either (!) a graph or an entity argument");
		}

		// Verify properties
		HashMap<String, Object> aProperties = null;
		if (aContext.getThisRequest().argumentExists("properties")) {
			aProperties = aContext.source("arg:properties", HashMap.class);
		}

		// Verify instance
		Neo4jInstance aInstanceRepresentation = null;
		if (aContext.getThisRequest().argumentExists("instance")) {
			aInstanceRepresentation = aContext.source("arg:instance", Neo4jInstance.class);
		} else {
			throw new NKFException("request does not have the required - instance - argument");
		}
		GraphDatabaseService vInstance = aInstanceRepresentation.getInstance();
		if (!vInstance.isAvailable(NEO4J_TIMEOUT)) {
			throw new NKFException("request does not have a valid - instance - argument");
		}

		List<Neo4jNode> vResult = new ArrayList<Neo4jNode>();
		ResourceIterator<Node> iterator = null;
		if (aEntity != null) {
			// We will look for a node with a specific label
			aContext.logRaw(INKFLocale.LEVEL_INFO, "DEBUG POSITION 4");
			iterator = vInstance.findNodes(Label.label(aEntity)); // TODO : why does this call never returns?
			aContext.logRaw(INKFLocale.LEVEL_INFO, "DEBUG POSITION 5");
		}
		else {
			// We will return the whole graph
			aContext.logRaw(INKFLocale.LEVEL_INFO, "DEBUG POSITION 1");
			ResourceIterable<Node> resourceIterable = vInstance.getAllNodes(); // TODO : why does this call never returns?
			aContext.logRaw(INKFLocale.LEVEL_INFO, "DEBUG POSITION 2");
			iterator = resourceIterable.iterator();
			aContext.logRaw(INKFLocale.LEVEL_INFO, "DEBUG POSITION 3");
		}

		aContext.logRaw(INKFLocale.LEVEL_INFO,"iterator.hasNext()="+iterator.hasNext());
		while (iterator.hasNext()) {
			Node vNode = iterator.next();
			Boolean vToAdd = true;

			if (aProperties != null) {
				// We iterate over the given properties
				for (Map.Entry<String, Object> vEntry : aProperties.entrySet()) {
					aContext.logRaw(INKFLocale.LEVEL_INFO,"vEntry found="+vEntry.getKey());
					// If the current node has a property matching the current property key...
					if (vNode.hasProperty(vEntry.getKey())) {
						aContext.logRaw(INKFLocale.LEVEL_INFO,"vNode found with property="+vNode.getProperty(vEntry.getKey()));
						// ...we may add the node but still want to check if the values are equal
						if (!vEntry.getValue().equals(vNode.getProperty(vEntry.getKey()))) {
							// If the keys match but the values differ, we don't return this node
							vToAdd = false;
						}
					} else {
						// If the current node don't have a property with the current key, we don't return this node
						vToAdd = false;
					}
				}
			}
			if (vToAdd) {
				aContext.logRaw(INKFLocale.LEVEL_INFO, "Before calling active:neo4jnode with SOURCE verb");
				INKFRequest subrequest = aContext.createRequest("active:neo4jnode");
				subrequest.addArgumentByValue("id", vNode.getId());
				subrequest.addArgumentByValue("instance", aInstanceRepresentation);
				subrequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
				subrequest.setRepresentationClass(Neo4jNode.class);

				vResult.add((Neo4jNode) aContext.issueRequest(subrequest));
				aContext.logRaw(INKFLocale.LEVEL_INFO, "node with given property added");
			}
		}
		aContext.createResponseFrom((Neo4jNode[]) vResult.toArray((new Neo4jNode[0])));
	}
}