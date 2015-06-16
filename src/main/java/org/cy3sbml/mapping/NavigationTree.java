package org.cy3sbml.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NavigationTree {
	private static final Logger logger = LoggerFactory.getLogger(NavigationTree.class);
	
	public static final String COMPARTMENTS = "Compartments";
	public static final String SPECIES = "Species";
	public static final String REACTIONS = "Reactions";
	public static final String QUALITATIVE_SPECIES = "QualitativeSpecies";
	public static final String TRANSITIONS = "Transitions";
	
	private boolean SBMLNetwork;
	private Map<String, NamedSBase> objectMap;
	private Map<String, TreePath>   objectPathMap;
	private DefaultTreeModel treeModel;
	
	public Map<String, NamedSBase> getObjectMap(){
		return objectMap;
	}
	
	public Map<String, TreePath> getObjectPathMap(){
		return objectPathMap;
	}
	
	public DefaultTreeModel getTreeModel(){
		return treeModel;
	}
	
	public boolean isSBMLNetwork(){
		return SBMLNetwork;
	}
	
	/////////////////// TREE MODELS ////////////////////////////////
	
	public NavigationTree(){
		objectMap = new HashMap<String, NamedSBase>(); 
		objectPathMap = new HashMap<String, TreePath>();
		SBMLNetwork = false;
		treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("sbml"));
	}
	
	public NavigationTree(SBMLDocument document){
		this();
		logger.info("Create NavigationTree for SBMLDocument");
		if (document == null){
			logger.info("No SBMLDocument");
			return;
		}
	
		try{
			Model model = document.getModel();
			String modelName = getModelNameFromModel(model);
			SBMLNetwork = true;
			
			DefaultMutableTreeNode top = new DefaultMutableTreeNode(modelName);
			treeModel = new DefaultTreeModel(top);
			addListOfCompartmentsToTreeModel(top, model.getListOfCompartments());
			addListOfSpeciesToTreeModel(top, model.getListOfSpecies());
			addListOfReactionsToTreeModel(top, model.getListOfReactions());
	
	        QualModelPlugin qModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
			if (qModel != null){
				addListOfQualitativeSpeciesToTreeModel(top, qModel.getListOfQualitativeSpecies());
				addListOfTransitionsToTreeModel(top, qModel.getListOfTransitions());
			}
		} catch (Throwable t) {
			logger.error("Navigation tree creation");
		}
	}
	
	public NamedSBase getNamedSBaseById(String id){
		NamedSBase nsb = null;
		if (objectMap.containsKey(id)){
			return objectMap.get(id);
		}
		return nsb;
	}
	
	private void addListOfCompartmentsToTreeModel(DefaultMutableTreeNode top, ListOf<Compartment> compartmentList){		
		addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(COMPARTMENTS), compartmentList);
	}
	private void addListOfSpeciesToTreeModel(DefaultMutableTreeNode top, ListOf<Species> speciesList){		
		addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(SPECIES), speciesList);
	}
	private void addListOfReactionsToTreeModel(DefaultMutableTreeNode top, ListOf<Reaction> reactionList){		
		addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(REACTIONS), reactionList);
	}

	private void addListOfQualitativeSpeciesToTreeModel(DefaultMutableTreeNode top, ListOf<QualitativeSpecies> qualitativeSpeciesList){		
		addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(QUALITATIVE_SPECIES), qualitativeSpeciesList);
	}
	private void addListOfTransitionsToTreeModel(DefaultMutableTreeNode top, ListOf<Transition> transitionList){		
		addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(TRANSITIONS), transitionList);
	}

	
	private void addListOfNamedSBaseToTreeModel(DefaultMutableTreeNode top, DefaultMutableTreeNode category, ListOf<? extends NamedSBase> namedSBaseList){
		if (namedSBaseList.size() > 0){
        	top.add(category);
        	for (NamedSBase namedSBase : namedSBaseList){
        		DefaultMutableTreeNode node = new DefaultMutableTreeNode(namedSBase, false);
        		category.add(node);
        		String id = namedSBase.getId();
        		TreePath path = new TreePath(node.getPath());
        		objectMap.put(id, namedSBase);
        		objectPathMap.put(id, path);
        	}
        }
	}
	
	private DefaultMutableTreeNode createTreeNodeForName(String name){
		return new DefaultMutableTreeNode(name, true);
	}
	
	private String getModelNameFromModel(Model model){
		String name = model.getId();
		if (name.equals("")){
			name = model.getName();
		}
		return name;
	}
}
