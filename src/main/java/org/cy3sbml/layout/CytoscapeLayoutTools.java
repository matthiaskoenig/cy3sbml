package org.cy3sbml.layout;

import giny.view.NodeView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.ext.layout.BoundingBox;
import org.sbml.jsbml.ext.layout.Dimensions;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.sbml.jsbml.ext.layout.Point;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

import cysbml.CySBMLConstants;
import cysbml.CySBML;
import cysbml.gui.NavigationPanel;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.CyNetworkView;

public class CytoscapeLayoutTools {
	
	public static void saveLayoutOfCurrentViewInFile(File file){
		CyNetworkView view = Cytoscape.getCurrentNetworkView();
		if (view != null){
			saveLayoutOfViewInFile(view, file);
		}
	}
	
	public static void saveLayoutOfViewInFile(CyNetworkView view, File file){
		CyNetwork network = view.getNetwork();
	    @SuppressWarnings("unchecked")
		List<CyNode> nodes = network.nodesList();
	    List<CyBoundingBox> boxes = new LinkedList<CyBoundingBox>(); 
	    for (CyNode node : nodes){
	    	NodeView nodeView = view.getNodeView(node);
	    	
	    	String nodeId = node.getIdentifier();
	    	double xpos = nodeView.getXPosition();
	    	double ypos = nodeView.getYPosition();
			double height = nodeView.getHeight();
			double width = nodeView.getWidth();
			CyBoundingBox box = new CyBoundingBox(nodeId, xpos, ypos, height, width);
			boxes.add(box);
	    }
	    // Creates the XML file
	    XMLInterface.writeXMLFileForLayout(file, boxes);
	}
	
	public static void loadLayoutOfCurrentViewFromFile(File file){
		CyNetworkView view = Cytoscape.getCurrentNetworkView();
		if (view != null){
			loadLayoutForViewFromFile(view, file);
		}
	}
	
	public static void loadLayoutForViewFromFile(CyNetworkView view, File file){
		CyNetwork network = view.getNetwork();
	    
	    HashMap<String, CyBoundingBox> boxesMap = XMLInterface.readLayoutFromXML(file);
	    if (boxesMap != null){
	    
	    	@SuppressWarnings("unchecked")
			List<CyNode> nodes = network.nodesList(); 
	    	for (CyNode node : nodes){
	    		// if position is stored
	    		String nodeId = node.getIdentifier();
	    		if (boxesMap.containsKey(nodeId)){
	    			CyBoundingBox box = boxesMap.get(nodeId);
	    			NodeView nodeView = view.getNodeView(node);		
	    			nodeView.setXPosition(box.getXpos());
	    			nodeView.setYPosition(box.getYpos());
	    			// nodeView.setHeight(box.getHeight());
	    			// nodeView.setWidth(box.getWidth());
	    		}
	    	}
	    	view.updateView();
	    } else {
	    	System.out.println("CySBML[INFO] -> Layout information could not be loaded from file.");
	    }
	}
		
	
	// USING THE LAYOUT EXTENSION FOR STORAGE /// 
    // ??? FIXME: is the current Layout saved
	@Deprecated
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
						/*
						ListOf<SpeciesReferenceGlyph> listOfSpeciesReferencesGlyph = new ListOf<SpeciesReferenceGlyph>();
						Reaction reaction = model.getReaction(nodeId);
						for (SpeciesReference sRef : reaction.getListOfProducts()){
							SpeciesReferenceGlyph sRefGlyph = 
							listOfSpeciesReferencesGlyph.add(new SpeciesReferenceGlyph(sRef.getId()));
						}
						for (SpeciesReference sRef : reaction.getListOfReactants()){
							listOfSpeciesReferencesGlyph.add(new SpeciesReferenceGlyph(sRef.getId()));
						}
						rGlyph.setListOfSpeciesReferencesGlyph(listOfSpeciesReferencesGlyph);
						*/
						layout.addReactionGlyph(rGlyph);
					}
	    		}
	    	}
	    }
	    return layout;
	}	
}
