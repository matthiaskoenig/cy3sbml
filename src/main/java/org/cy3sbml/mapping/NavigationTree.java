package org.cy3sbml.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handling the NavigationTree model.
 * This is some relict of the older version. 
 * Only the objectMapping is really used. 
 * TODO: make sure this is created exactly once!, not every time the SBMLDocument is switched.
 *
 */
public class NavigationTree {
	private static final Logger logger = LoggerFactory.getLogger(NavigationTree.class);
	
	public static final String COMPARTMENTS = "Compartments";
	public static final String PARAMETERS = "Parameters";
	public static final String SPECIES = "Species";
	public static final String REACTIONS = "Reactions";
	
	public static final String QUAL_SPECIES = "QualitativeSpecies";
	public static final String QUAL_TRANSITIONS = "Transitions";
	
	public static final String FBC_GENE_PRODUCTS = "GeneProducts";
	
	private boolean SBMLNetwork;
	private Map<String, NamedSBase> objectMap;
	private Map<String, TreePath>   objectPathMap;
	private DefaultTreeModel treeModel;
	
	/** Constructor */
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
			
			// adding supported listOfs to the Navigation tree
			addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(PARAMETERS), model.getListOfParameters());
			addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(COMPARTMENTS), model.getListOfCompartments());
			addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(SPECIES), model.getListOfSpecies());
			addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(REACTIONS), model.getListOfReactions());
	
	        QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
			if (qualModel != null){
				addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(QUAL_SPECIES), qualModel.getListOfQualitativeSpecies());
				addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(QUAL_TRANSITIONS), qualModel.getListOfTransitions());
			}
			FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
			if (fbcModel != null){
				addListOfNamedSBaseToTreeModel(top, createTreeNodeForName(FBC_GENE_PRODUCTS), fbcModel.getListOfGeneProducts());
			}
		} catch (Throwable t) {
			logger.error("Navigation tree could not be created");
			t.printStackTrace();
		}
	}
	
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
	
	public NamedSBase getNamedSBaseById(String id){
		NamedSBase nsb = null;
		// NamedSBases have to be supported in the objectMap
		if (objectMap.containsKey(id)){
			return objectMap.get(id);
		}
		return nsb;
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
	
	@Deprecated
	/** ? where is this used ?
	 * Not necessary due to change to SUIDs.
	 * @param model
	 * @return
	 */
	private String getModelNameFromModel(Model model){
		String name = model.getId();
		if (name.equals("")){
			name = model.getName();
		}
		return name;
	}
}
