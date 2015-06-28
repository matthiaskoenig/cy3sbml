package org.cy3sbml.layout;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;

/** Helper class for loading the layout extension. 
 * 
 * TODO: do in main: 
 * 
 */
public class LayoutExtension {
	
	// TODO: where is the namespace definied ?
	final static String LAYOUT_NS = "http://www.sbml.org/sbml/level3/version1/layout/version1";
	
	/** Get the extension. 
	 * Changed in the build. */
	public static boolean existLayoutInSBMLDocument(SBMLDocument doc){
		boolean existLayout = false;
		LayoutModelPlugin layoutModel = (LayoutModelPlugin) doc.getModel().getExtension(LAYOUT_NS);
		if (layoutModel != null){
			existLayout = (layoutModel.getListOfLayouts().size() > 0); 
		}
		return existLayout;
	}
	
	/** Returns the registered layoutModel or registers a new one
	 * to the model in the document.
	 */
	public static LayoutModelPlugin getOrCreateLayoutModel(SBMLDocument doc){
		LayoutModelPlugin layoutModel = (LayoutModelPlugin) doc.getModel().getExtension(LAYOUT_NS);
		if (layoutModel == null){
			Model model = doc.getModel();
			// model should exist in the document now
			if (model == null){
				System.err.println("Model missing in SBMLDocument.");
				return null;
			}
			layoutModel = new LayoutModelPlugin(model);
			model.addExtension(LAYOUT_NS, layoutModel);
		}
		return layoutModel;
	}
	
	
	public static ListOf<Layout> getLayoutsInSBMLDocument(SBMLDocument doc){
		LayoutModelPlugin layoutModel = (LayoutModelPlugin) doc.getModel().getExtension(LAYOUT_NS);
		return layoutModel.getListOfLayouts();
	}	
	
}
