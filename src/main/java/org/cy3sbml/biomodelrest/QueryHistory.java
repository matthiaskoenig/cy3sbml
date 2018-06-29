package org.cy3sbml.biomodelrest;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * History of the last queries.
 */
@SuppressWarnings("restriction")
public class QueryHistory {
	private ObservableList<String> items;
	
	public QueryHistory(){
		items = FXCollections.observableArrayList(
				"/search?query=repressilator&format=json",
                "/search?query=glucose&format=json"
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
