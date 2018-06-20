package org.cy3sbml.biomodelrest;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * History of the last SABIO-RK queries.
 */
@SuppressWarnings("restriction")
public class SabioQueryHistory {
	private ObservableList<String> items;
	
	public SabioQueryHistory(){
		items = FXCollections.observableArrayList(
				"kineticLaws/14792",
                "kineticLaws?kinlawids=48020,49160,44091,48027",
				"searchKineticLaws/sbml?q=Organism:\"Homo sapiens\" AND Pathway:\"galactose metabolism\"",
                "searchKineticLaws/sbml?q=Tissue:\"spleen\" AND Organism:\"Homo sapiens\""
		);
	}
	
	/**
     * Add query string at beginning of history list.
     * If the query string is already in the list the term is not added to the history.
     */
	public void add(String query){
	    if (items.contains(query)){
            items.remove(query);
        }
        // always add at beginning
        items.add(0, query);
	}

    /**
     * Get element at given index.
     * @param index
     * @return
     */
	public String get(int index){
	    return items.get(index);
	}

    /**
     * Get all elements.
     * @return
     */
    public ObservableList<String> getAll(){
        return items;
    }

    /**
     * Print overview of the query history.
     */
	public void print() {
	    System.out.println("-------------------------\n" +
                "Query History\n" +
                "-------------------------");
        for (String item: items){
            System.out.println(item);
        }

        System.out.println("-------------------------");
    }
}
