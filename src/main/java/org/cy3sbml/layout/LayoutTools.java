package org.cy3sbml.layout;


import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cy3sbml.SBML;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with layouts.
 * 
 * Saving and storing node positions to file.
 * Applying layouts from files to network views, ...
 */
public class LayoutTools {
	private static final Logger logger = LoggerFactory.getLogger(LayoutTools.class);
	
	private ServiceAdapter adapter; 
	
	public LayoutTools(ServiceAdapter adapter){
		this.adapter = adapter;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// SAVE LAYOUTS
	///////////////////////////////////////////////////////////////////////////

	/** Save layout of current view in file. */
	public void saveLayoutOfCurrentViewInFile(File file){
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		if (view != null){
			saveLayoutOfViewInFile(view, file);
		}
	}
	
	/** Save layout of given view in file. */
	public void saveLayoutOfViewInFile(CyNetworkView view, File file){
		CyNetwork network = view.getModel();
		
		List<CyNode> nodes = network.getNodeList();
	    List<CyBoundingBox> boxes = new LinkedList<CyBoundingBox>(); 
	    for (CyNode node : nodes){
	    	View<CyNode> nodeView = view.getNodeView(node);
	    	// id column is used for mapping positions 
	    	String nodeId = AttributeUtil.get(network, node, SBML.ATTR_ID, String.class);
	    	Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
	    	Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
	    	Double height = nodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT);
	    	Double width = nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
			CyBoundingBox box = new CyBoundingBox(nodeId, x, y, height, width);
			boxes.add(box);
	    }
	    // Creates the XML file
	    XMLInterface.writeXMLFileForLayout(file, boxes);
	}

	///////////////////////////////////////////////////////////////////////////
	// LOAD LAYOUTS
	///////////////////////////////////////////////////////////////////////////
	public void loadLayoutOfCurrentViewFromFile(File file){
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		if (view != null){
			loadLayoutForViewFromFile(view, file);
		}
	}
	
	public void loadLayoutForViewFromFile(CyNetworkView view, File file){
		CyNetwork network = view.getModel();
	    
	    HashMap<String, CyBoundingBox> boxesMap = XMLInterface.readLayoutFromXML(file);
	    if (boxesMap != null){
	    
	    	
	    	List<CyNode> nodes = network.getNodeList();
	    	for (CyNode node : nodes){
	    		// if position is stored
	    		String nodeId = AttributeUtil.get(network, node, SBML.ATTR_ID, String.class);
	    		if (boxesMap.containsKey(nodeId)){
	    			CyBoundingBox box = boxesMap.get(nodeId);
	    			View<CyNode> nodeView = view.getNodeView(node);
	    			nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, box.getXpos());
	    			nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, box.getYpos());
	    			// nodeView.setVisualProperty(VisualPropertyKey.NODE_HEIGHT, box.getHeight());
	    			// nodeView.setVisualProperty(VisualPropertyKey.NODE_WIDTH, box.getWidth());
	    		}
	    	}
	    	view.updateView();
	    } else {
	    	logger.warn("Layout information could not be loaded from file.");
	    }
	}
}

	///////////////////////////////////////////////////////////////////////////
	// SBML Layout extension
	///////////////////////////////////////////////////////////////////////////
	
	/*
	public static void saveLayoutOfCurrentViewInSBMLFile(File file){
		CyNetworkView view = Cytoscape.getCurrentNetworkView();
		SBMLDocument doc = writeLayoutOfNetworkViewToSBMLDocument(view);
		if (doc != null){
			try {
				SBMLWriter.write(doc, file, CySBML.NAME, CySBML.VERSION);
				
			} catch (SBMLException e) {
				e.printStackTrace();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("CySBML[INFO] -> No SBMLDocument found for current Network view");
		}
	}
		
	private static SBMLDocument writeLayoutOfNetworkViewToSBMLDocument(CyNetworkView view){
		CyNetwork network = view.getNetwork();
		SBMLDocument doc = NavigationPanel.getInstance().getSBMLDocumentForCyNetwork(network);
		if (doc != null){
			int level = 3;
			int version = 1;
			doc.setLevelAndVersion(level, version);
			Model model = doc.getModel();
			model.setLevel(level);
			model.setVersion(version);
			
			// Get LayoutModel
			LayoutModelPlugin layoutModel = LayoutExtension.getOrCreateLayoutModel(doc);
			// Add the layout
			Layout layout = createSBMLLayoutForView(view, model);
			layoutModel.add(layout);
		}
		return doc;
	}
	
	private static Layout createSBMLLayoutForView(CyNetworkView view, Model model){
		int level = model.getLevel();
		int version = model.getVersion();
		Layout layout = new Layout(level, version);
		String layoutId = view.getIdentifier();
		layout.setId(layoutId);
		layout.setName(layoutId);
		
		CyNetwork network = view.getNetwork();
	    @SuppressWarnings("unchecked")
		List<CyNode> nodes = network.nodesList();
	    CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
	    
	    for (CyNode node : nodes){
	    	NodeView nodeView = view.getNodeView(node);
	    	// Test if reaction or species node
	    	String nodeId = node.getIdentifier();
	    	String nodeType = (String) nodeAttributes.getAttribute(nodeId, CySBMLConstants.ATT_TYPE);
	    	if (nodeType!=null){
	    		if (nodeType.equals(CySBMLConstants.NODETYPE_REACTION) || nodeType.equals(CySBMLConstants.NODETYPE_SPECIES)){
	    			
	    			double xpos = nodeView.getXPosition();
					double ypos = nodeView.getYPosition();
					double zpos = 0.0;
					double height = nodeView.getHeight();
					double width = nodeView.getWidth();
					double depth = 0.0;
					Dimensions dim = new Dimensions(width, height, depth, level, version);
					Point point = new Point(xpos, ypos, zpos, level, version);
					
					BoundingBox box = new BoundingBox();
					box.setLevel(level);
					box.setVersion(version);
					box.setPosition(point);
					box.setDimensions(dim);
					
					if (nodeType.equals(CySBMLConstants.NODETYPE_SPECIES)){
						SpeciesGlyph sGlyph = new SpeciesGlyph();
						sGlyph.setId(nodeId);
						sGlyph.setVersion(version);
						sGlyph.setLevel(level);
						sGlyph.setBoundingBox(box);
						sGlyph.setSpecies(nodeId);
						layout.addSpeciesGlyph(sGlyph);
						
					}
					else if (nodeType.equals(CySBMLConstants.NODETYPE_REACTION)){
						ReactionGlyph rGlyph = new ReactionGlyph();
						rGlyph.setVersion(version);
						rGlyph.setLevel(level);
						rGlyph.setId(nodeId);
						rGlyph.setBoundingBox(box);
						rGlyph.setReaction(nodeId);
						
						//write the speciesReferences
						
//						ListOf<SpeciesReferenceGlyph> listOfSpeciesReferencesGlyph = new ListOf<SpeciesReferenceGlyph>();
//						Reaction reaction = model.getReaction(nodeId);
//						for (SpeciesReference sRef : reaction.getListOfProducts()){
//							SpeciesReferenceGlyph sRefGlyph = 
//							listOfSpeciesReferencesGlyph.add(new SpeciesReferenceGlyph(sRef.getId()));
//						}
//						for (SpeciesReference sRef : reaction.getListOfReactants()){
//							listOfSpeciesReferencesGlyph.add(new SpeciesReferenceGlyph(sRef.getId()));
//						}
//						rGlyph.setListOfSpeciesReferencesGlyph(listOfSpeciesReferencesGlyph);
						
						layout.addReactionGlyph(rGlyph);
					}
	    		}
	    	}
	    }
	    return layout;
	}
	*/

