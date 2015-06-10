package org.cy3sbml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cy3sbml.mapping.NamedSBaseToNodeMapping;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.property.CyProperty;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SBMLNetworkViewReader extends AbstractTask implements CyNetworkReader {
	private static final Logger logger = LoggerFactory.getLogger(SBMLNetworkViewReader.class);
	
	private static final int BUFFER_SIZE = 16384;
		
	private final InputStream stream;
	private final CyNetworkFactory networkFactory;
	private final CyNetworkViewFactory viewFactory;
	private final CyProperty<Properties> cy3sbmlProperties;
	private final VisualMappingManager visualMappingManager;
	private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
	private final SynchronousTaskManager taskManager;

	private SBMLDocument document;
	private CyNetwork network;

	public SBMLNetworkViewReader(InputStream stream, CyNetworkFactory networkFactory, CyNetworkViewFactory viewFactory,
								 CyProperty<Properties> cy3sbmlProperties,
								 VisualMappingManager visualMappingManager, CyLayoutAlgorithmManager cyLayoutAlgorithmManager, 
								 SynchronousTaskManager taskManager) {
		this.stream = stream;
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
		this.cy3sbmlProperties = cy3sbmlProperties;
		this.visualMappingManager = visualMappingManager;
		this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
		this.taskManager = taskManager;
	}

	@SuppressWarnings("deprecation")
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		logger.info("Start Reader.run()");
		try {
		
		String version = JSBML.getJSBMLVersionString();
		logger.info("JSBML version: " + version);
		
		String xml = readString(stream);
		document = JSBML.readSBMLFromString(xml);
		network = networkFactory.createNetwork();
		Model model = document.getModel();
		
		// Create a node for each Species
		Map<String, CyNode> speciesById = new HashMap<String, CyNode>();
		for (Species species : model.getListOfSpecies()) {
			CyNode node = network.addNode();
			speciesById.put(species.getId(), node);
			CyRow attributes = network.getRow(node);
			checkNodeSchema(attributes);
			attributes.set(SBML.NODE_NAME_ATTR_LABEL, species.getName());
			attributes.set(SBML.SBML_TYPE_ATTR, SBML.SBML_TYPE_SPECIES);
			attributes.set(SBML.SBML_ID_ATTR, species.getId());

			attributes.set(SBML.SBML_INITIAL_CONCENTRATION_ATTR, species.getInitialConcentration());
			attributes.set(SBML.SBML_INITIAL_AMOUNT_ATTR, species.getInitialAmount());
			attributes.set(SBML.SBML_CHARGE_ATTR, species.getCharge());
			
			String compartment = species.getCompartment();
			if (compartment != null) {
				attributes.set(SBML.SBML_COMPARTMENT_ATTR, compartment);
			}
		}
		
		// Create a node for each Reaction
		Map<String, CyNode> reactionsById = new HashMap<String, CyNode>();
		for (Reaction reaction : model.getListOfReactions()) {
			CyNode node = network.addNode();
			reactionsById.put(reaction.getId(), node);
			CyRow attributes = network.getRow(node);
			checkNodeSchema(attributes);
			String name = reaction.getName();
			if (name == null) {
				attributes.set(SBML.NODE_NAME_ATTR_LABEL, reaction.getId());
			} else {
				attributes.set(SBML.NODE_NAME_ATTR_LABEL, name);
			}
			attributes.set(SBML.SBML_TYPE_ATTR, SBML.SBML_TYPE_REACTION);
			attributes.set(SBML.SBML_ID_ATTR, reaction.getId());
			
			for (SpeciesReference product : reaction.getListOfProducts()) {
				CyNode sourceNode = speciesById.get(product.getSpecies());
				CyEdge edge = network.addEdge(sourceNode, node, true);
				CyRow edgeAttributes = network.getRow(edge);
				checkEdgeSchema(edgeAttributes);
				edgeAttributes.set(SBML.INTERACTION_TYPE_ATTR, SBML.INTERACTION_TYPE_REACTION_PRODUCT);
			}
			
			for (SpeciesReference reactant : reaction.getListOfReactants()) {
				CyNode sourceNode = speciesById.get(reactant.getSpecies());
				CyEdge edge = network.addEdge(sourceNode, node, true);
				CyRow edgeAttributes = network.getRow(edge);
				checkEdgeSchema(edgeAttributes);
				edgeAttributes.set(SBML.INTERACTION_TYPE_ATTR, SBML.INTERACTION_TYPE_REACTION_REACTANT);
			}
			
			for (ModifierSpeciesReference modifier : reaction.getListOfModifiers()) {
				CyNode sourceNode = speciesById.get(modifier.getSpecies());
				CyEdge edge = network.addEdge(sourceNode, node, true);
				CyRow edgeAttributes = network.getRow(edge);
				checkEdgeSchema(edgeAttributes);
				edgeAttributes.set(SBML.INTERACTION_TYPE_ATTR, SBML.INTERACTION_TYPE_REACTION_MODIFIER);
			}
			
			KineticLaw law = reaction.getKineticLaw();
			if (law != null) {
				for (LocalParameter parameter : law.getListOfParameters()) {
					String parameterName = parameter.getName();
					String key = String.format(SBML.KINETIC_LAW_ATTR_TEMPLATE, parameterName);
					checkSchema(attributes, key, Double.class);
					attributes.set(key, parameter.getValue());
					
					String units = parameter.getUnits();
					if (units != null) {
						String unitsKey = String.format(SBML.KINETIC_LAW_UNITS_ATTR_TEMPLATE, parameterName);
						checkSchema(attributes, unitsKey, String.class);
						attributes.set(unitsKey, units);
					}
				}
			}
		}
		logger.info("End Reader.run()");
		
		} catch (Throwable t){
			logger.error("Could not read SBML into Cytoscape!", t);
			t.printStackTrace();
		}
	}
	
	private void checkEdgeSchema(CyRow attributes) {
		checkSchema(attributes, SBML.INTERACTION_TYPE_ATTR, String.class);
	}

	private void checkNodeSchema(CyRow attributes) {
		checkSchema(attributes, SBML.SBML_TYPE_ATTR, String.class);
		checkSchema(attributes, SBML.SBML_ID_ATTR, String.class);
		checkSchema(attributes, SBML.SBML_INITIAL_CONCENTRATION_ATTR, Double.class);
		checkSchema(attributes, SBML.SBML_INITIAL_AMOUNT_ATTR, Double.class);
		checkSchema(attributes, SBML.SBML_CHARGE_ATTR, Integer.class);
		checkSchema(attributes, SBML.SBML_COMPARTMENT_ATTR, String.class);
	}

	private <T> void checkSchema(CyRow attributes, String attributeName, Class<T> type) {
		if (attributes.getTable().getColumn(attributeName) == null)
			attributes.getTable().createColumn(attributeName, type, false);
	}

	private static String readString(InputStream source) throws IOException {
		StringWriter writer = new StringWriter();
		BufferedReader reader = new BufferedReader(new InputStreamReader(source));
		try {
			char[] buffer = new char[BUFFER_SIZE];
			int charactersRead = reader.read(buffer, 0, buffer.length);
			while (charactersRead != -1) {
				writer.write(buffer, 0, charactersRead);
				charactersRead = reader.read(buffer, 0, buffer.length);
			}
		} finally {
			reader.close();
		}
		return writer.toString();
	}

	public void cancel() {
	}

	@Override
	public CyNetwork[] getNetworks() {
		return new CyNetwork[] { network };
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {
		// create view
		CyNetworkView view = viewFactory.createNetworkView(network); 
		doPostProcessing(network, view);
		
		
		return view;
	}
	
	/**
	 *  PostProcessing of the network after the file is read.
	 *  - applies CySBML visual style
	 *  - applies standard layout to the network
	 *  - selects the important SBML attributes in the data browser
	 */  
	public void doPostProcessing(CyNetwork network, CyNetworkView view) {
		logger.info("doPostProcessing of network and view");
		
		// apply layout
		CyLayoutAlgorithm layout = cyLayoutAlgorithmManager.getLayout("force-directed");
		layout.createLayoutContext();
		TaskIterator layoutTaskIterator = layout.createTaskIterator(view, layout.createLayoutContext(),
																	layout.ALL_NODE_VIEWS, layout.getName());
		
		// We use the synchronous task manager otherwise the visual style and updateView()
		// may occur before the view is relayed out:
		taskManager.execute(layoutTaskIterator);
		
		// Apply cy3sbml style		
		String styleName = (String) cy3sbmlProperties.getProperties().get("cy3sbml.visualStyle");
		// view.fitContent();
		VisualStyle style = getVisualStyleByName(styleName);
		visualMappingManager.setVisualStyle(style, view);
		style.apply(view);
		view.updateView();
		
		// Select SBML Attributes in Data Panel
		// TODO: ? how do new tables work
		// selectSBMLTableAttributes();
		
		// Update cy3sbml Navigator
		
		// TODO: register the SBML in the Manager 
		SBMLManager sbmlManager = SBMLManager.getInstance(null, null);
		NamedSBaseToNodeMapping mapping = new NamedSBaseToNodeMapping(document, network);
		sbmlManager.addSBML2NetworkEntry(document, network, mapping);
		
		/*
		public void putSBMLDocumentForLayout(String networkName, SBMLDocument document, CyNetwork network, Layout layout){
			NamedSBaseToNodeMapping mapping = new NamedSBaseToNodeMapping(layout);
			putSBMLDocument(networkName, document, mapping);
		}
		public void putSBMLDocumentForGRN(String networkName, SBMLDocument document, CyNetwork network){
			NamedSBaseToNodeMapping mapping = new NamedSBaseToNodeMapping(network);
			putSBMLDocument(networkName, document, mapping);
		}
		*/
		
		
		
		// Preload SBML WebService information
		// TODO: NamedSBaseInfoThread.preloadAnnotationInformationForModel(document.getModel());
		
		// Arrange Windows and fit views (for all networks)
		// 
		//CyDesktopManager.arrangeFrames(CyDesktopManager.Arrange.GRID);
		// for(CyNetworkView view: Cytoscape.getNetworkViewMap().values()){
		// 	view.fitContent();
		// }
	}
	
	private VisualStyle getVisualStyleByName(String styleName){
		Set<VisualStyle> styles = visualMappingManager.getAllVisualStyles();
		for (VisualStyle style: styles){
			if (style.getTitle().equals(styleName)){
				return style;
			}
		}
		logger.warn("cy3sbml style not found in VisualStyles, default style used.");
		return visualMappingManager.getDefaultVisualStyle();
	}
	
	
	/*
	protected void applyLayout(CyNetwork network) {
		if (nodeIds.size() > LAYOUT_NODE_NUMBER){
			CySBML.LOGGER.info(String.format("More than %d nodes, no layout applied.", LAYOUT_NODE_NUMBER));
		} else { 
			CyLayoutAlgorithm layout = CyLayouts.getLayout("force-directed");
			CyNetworkView view = Cytoscape.getNetworkView(network.getIdentifier());
			view.applyLayout(layout);
		}
	}
	*/

	/*
	private void selectSBMLTableAttributes(){
		String[] nAtts = {CySBMLConstants.ATT_TYPE,
						  CySBMLConstants.ATT_NAME,
						  CySBMLConstants.ATT_COMPARTMENT,
						  CySBMLConstants.ATT_METAID,
						  CySBMLConstants.ATT_SBOTERM};
		
		String[] eAtts = {Semantics.INTERACTION,
						  CySBMLConstants.ATT_STOICHIOMETRY,
						  CySBMLConstants.ATT_METAID,
						  CySBMLConstants.ATT_SBOTERM};
		AttributeUtils.selectTableAttributes(Arrays.asList(nAtts), Arrays.asList(eAtts));
	}
	*/
}
