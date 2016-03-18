package org.cy3sbml.cofactors;

import java.util.HashMap;
import java.util.List;


/* Information which nodes map to which cofactor nodes.
 * 
 * The cofactor nodes are created and existing in parallel to the
 * original nodes (depending on the selected view (with or without
 * cofactors) these have to be removed from the network temporarily.
 */
public class CofactorMapping extends HashMap<Long, List<Long>> {
	private static final long serialVersionUID = 1L;

	public String toString(){
		String string = "";
		for (Long key : this.keySet()){
			string += "\t(" +  key + ")\n";
			List<Long> clones = this.get(key);
			for (Long clone : clones){
				string += "\t\t- " + clone + "\n";
			}
		}
		return string;
	}
	
}
