package org.cy3sbml.biomodelrest.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import org.codefx.libfx.control.webview.WebViewHyperlinkListener;
import org.codefx.libfx.control.webview.WebViews;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import org.cy3sbml.ResourceExtractor;

import org.cy3sbml.biomodelrest.QueryHistory;
import org.cy3sbml.biomodelrest.BiomodelsQueryResult;
import org.cy3sbml.biomodelrest.rest.Biomodel;
import org.cy3sbml.biomodelrest.rest.QuerySuggestions;
import org.cy3sbml.biomodelrest.rest.BiomodelsQuery;

import org.cy3sbml.util.OpenBrowser;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;


/** 
 * The javafx controller for the GUI.
 * 
 * The GUI is created with JavaFX SceneBuilder from the 
 *  /gui/query.fxml
 * For GUI changes load the fxml in the SceneBuilder and update it.
 * 
 * The HTML part can be debugged separately, i.e. with respective 
 * HTML/JS/CSS tools.
 */
@SuppressWarnings("restriction")
public class QueryFXMLController implements Initializable{
	
	private QuerySuggestions suggestions;
	
	// browser
	@FXML private ImageView imageBiomodelLogo;
	@FXML private ImageView imageHelp;
	@FXML private ImageView imageSBML;
	@FXML private WebView webView;
	
	// -- Log --
	@FXML private TextArea log;
	private Logger logger;
	
	// --- Query Builder ---
    @FXML private TextField searchTerm;
    @FXML private TextField keyword;
    @FXML private TextField keywordTerm;
    @FXML private ListView<String> keywordList;

    HashSet<String> searchTerms;
    HashMap<String, String> filters;

    @FXML private Button addSearchTermButton;
    @FXML private Button addKeywordButton;
    private AutoCompletionBinding<String> termBinding;
    private AutoCompletionBinding<String> keywordBinding;
    
    // --- Kinetic Law entries ---
    @FXML private TextArea entry;
    @FXML private Button addEntryButton;

    // --- Query History ---
    @FXML private ListView<String> historyList;

    // -- REST Query --
    @FXML private TextArea queryText;
    @FXML private Button queryButton;
	@FXML private Button cancelButton;
    @FXML private Button resetButton;

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Text statusCode;
    @FXML private Text statusCodeLabel;
    @FXML private Text time;
    @FXML private Text timeLabel;
    
    // -- REST Results --
    @FXML private Text entryLabel;
    @FXML private TableView biomodelsTable;
    @FXML private TableColumn idCol;
    @FXML private TableColumn nameCol;
    @FXML private Button loadButton;

    private static QueryHistory queryHistory;
    private BiomodelsQueryResult queryResult;
    private HashMap<String, Biomodel> biomodelsMap;


    Thread queryThread = null;

    /** 
     * Adds keyword:keywordTerm filter to the query.
     */
    @FXML protected void handleAddKeywordAction(ActionEvent event) {
    	String key = keyword.getText();
    	String value = keywordTerm.getText();

    	if (key == null || key.length()==0 || value.length() == 0) {
            logger.warn("No search filter provided. Select keyword and search keywordTerm in the Query Builder.");
    	} else {
    	    if (filters.containsKey(key)){
                logger.warn("Existing filter is overwritten for key: %s.".format(key));
            }

            filters.put(key, value);
            logger.info("<" + key + "|" + value + "> added to query.");
            updateQueryText();
        }
    }

    @FXML protected void handleAddSearchTermAction(ActionEvent event){
        String term = searchTerm.getText();
        if (term == null || term.length()==0) {
            logger.warn("No search term provided.");
        } else {
            if (searchTerms.contains(term)){
                logger.warn("Search term already in query.");
            }
            searchTerms.add(term);
            updateQueryText();
        }

    }

    /**
     * Sets query text based on last query.
     */
    private void updateQueryText(){

        // search terms
        String text = "/search?query=";
        if (searchTerms.size() == 0){
            text += "*:*";
        } else {
            text += String.join("+", searchTerms);
        }

        // filters
        for (String key : filters.keySet()){
            String value = filters.get(key);
            text += " AND " + key + ":" + value;
        }
        text += "&format=json";
        queryText.setText(text);
    }
    
    /**
     * Add biomodel ids to the query.
     */
    @FXML protected void handleAddBiomodelsAction(ActionEvent event) {
    	String text = entry.getText();
    	if (text == null || text.length() == 0){
    		logger.warn("A list of Biomodel Ids is required.");
    		return;
    	}
  
    	// parse ids
    	HashSet<String> ids = parseIds(text);
    	if (ids.isEmpty()){
    		logger.error("No Biomodel Ids could be parsed from input: <" + entry.getText() + ">. Ids should be separated by ' ', ',', or ';'.");
    		return;
    	}

    	//TODO: create biomodels from ids

    }

    /**
     * Update set of biomodels from given list of biomodel identifiers.
     * @param biomodelIds
     */
    private void updateBiomodels(HashSet<String> biomodelIds){

    }


    /**
     * Parses Biomodel Ids from given text string.
     *
     * The ids can be separated by different separators, i.e.
     * 	'\n', '\t', ' ', ';' or','
     */
    private static HashSet<String> parseIds(String text){
        HashSet<String> ids = new HashSet<>();

        // unify separators
        text = text.replace("\n", ",");
        text = text.replace("\t", ",");
        text = text.replace(" ", ",");
        text = text.replace(";", ",");

        String[] tokens = text.split(",");
        for (String t : tokens){
            // single entry parsing
            if (t.length() == 0){
                continue;
            }

            // FIXME: check against regular expression
            ids.add(t);
        }
        return ids;
    }


    /**
     * Run SABIO-RK web service query.
     */
    @FXML protected void handleQueryAction(ActionEvent event) {
        String queryString = queryText.getText();

        if (queryString == null || queryString.length() == 0){
            logger.warn("Query is empty. No web service call performed.");
            return;
        }

    	// necessary to run long running request in separate 
    	// thread to allow GUI updates.
    	// GUI updates have to be passed to the JavaFX Thread using runLater()
    	queryThread = new Thread(){
            public void run() {
            	// query to perform

            	// update initial GUI
            	showQueryStatus(true);
            	Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        queryButton.setDisable(true);
                        cancelButton.setDisable(false);
                        statusCode.setStyle("-fx-fill: black;");
                		progressIndicator.setStyle("-fx-progress-color: dodgerblue;");
                    }
                });
        		setProgress(-1);

            	// perform search query
        		long startTime = System.currentTimeMillis();
        		logger.info("GET <"+ queryString + ">");
        		logger.info("... waiting for BioModels response ...");
        		queryResult = (new BiomodelsQuery()).performSearchQuery(queryString);
        		Integer returnCode = queryResult.getStatus();
        		long endTime = System.currentTimeMillis();
        		long duration = (endTime - startTime);


                // TODO: get the biomodel information from the query
                HashSet<String> biomodelIds = queryResult.getBiomodelIdsFromSearch();
                ArrayList<Biomodel> biomodels = queryResult.getBiomodelsFromIds(biomodelIds);

        		
            	Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                    	statusCode.setText(returnCode.toString());
                		if (returnCode != 200){
                			logger.warn("BioModels returned status <" + returnCode + ">");
                			statusCode.setStyle("-fx-fill: red;");
                			progressIndicator.setStyle("-fx-progress-color: red;");
                		}else {
                			// successful
                			logger.info("BioModels returned status <" + returnCode + "> after " + duration + " [ms]");
                			
                			// handle empty test call
                			final ObservableList<Biomodel> data = FXCollections.observableArrayList(biomodels);

                			if (! data.isEmpty()){
                				biomodelsTable.setItems(data);
                				biomodelsTable.setDisable(false);
                    	    	loadButton.setDisable(false);
                                biomodelsTable.getSelectionModel().select(0);
                                imageSBML.setVisible(true);
                            }
                		}
                		time.setText(duration + " [ms]");    	
                        queryButton.setDisable(false);
                        cancelButton.setDisable(true);
                        
                        // add query to history
                        queryHistory.add(queryString);
                        // update the history view
                        logger.info("query added to history: <" + queryString +">");
                    }
                });
        		setProgress(1);

            }
        };
        queryThread.start();
    }
    
    /**
     * Reset GUI to original state.
     */
    @FXML protected void handleResetAction(ActionEvent event) {
    	logger.info("Reset GUI.");
    	queryText.clear();
    	keyword.clear();
    	keywordTerm.clear();
    	searchTerm.clear();
    	entry.clear();
    	statusCode.setText("?");
    	showQueryStatus(false);
    	progressIndicator.setStyle("-fx-progress-color: dodgerblue;");
    	
    	// clear table
    	biomodelsTable.setItems(FXCollections.observableArrayList());
    	biomodelsTable.setDisable(true);
    	loadButton.setDisable(true);
    	cancelButton.setDisable(true);
        queryButton.setDisable(false);
    	imageSBML.setVisible(false);
    	keywordList.getSelectionModel().clearSelection();

        setProgress(-1);
        String status = "UP";
        if (status.equals("UP")){
            setProgress(1.0);
        }

        setBiomodelCount(null);
    	setHelp();
    }

    /**
     * Cancel webservice request.
     * @param event
     */
    @FXML protected void handleCancelAction(ActionEvent event) {
        logger.info("Cancel request thread");
        if (queryThread != null){
            if(queryThread.getState() != Thread.State.TERMINATED){
                // thread exists and is still running
                // FIXME: this is inherently unsafe, but works for now
                queryThread.stop();
                String abortedQuery = queryText.getText();
                handleResetAction(null);
                queryText.setText(abortedQuery);
            }
		}
    }
    
    /**
     * Load SABIO-RK entries in Cytoscape.
     */
    @FXML protected void handleLoadAction(ActionEvent event) {
    	logger.info("Loading Kinetic Laws in Cytoscape ...");
    	if (WebViewSwing.sbmlReader != null){
    	    SBMLDocument doc = null;
            String sbml = null;
            try {
                sbml = JSBML.writeSBMLToString(doc);
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
            if (sbml != null){
    			logger.info("... loading ...");
    			WebViewSwing.sbmlReader.loadNetworkFromSBML(sbml);
    			logger.info("Networks loaded in Cytoscape. Dialog closed.");
    			WebViewSwing.dialog.setVisible(false);
    		} else {
    			logger.error("No SBML in request result.");
    		}
    	} else {
    		logger.error("No SBMLReader available in controller.");
    	}
    }
    
    // --------------------------------------------------------------------
    // JavaScript interface object
    // --------------------------------------------------------------------
    public class JavaApp {
    	public String query;
    	
    	public void setQuery() {
            logger.info("<Upcall WebView> : "+ query);
            resetButton.fire();
            queryText.setText(query);
        }
    }
    
    // --------------------------------------------------------------------
    // GUI helpers
    // --------------------------------------------------------------------
    /** Set help information. */
    private void setHelp(){
        URI infoURI = ResourceExtractor.fileURIforResource("/biomodels/gui/info.html");
		webView.getEngine().load(infoURI.toString());
    }
    
    /**
     * Set information for biomodel in WebView.
     */
	private void setInfoForBiomodel(String biomodelId){
		String biomodelURI =  biomodelId;
		logger.info("Load information for Biomodel<" + biomodelId + ">");
		webView.getEngine().load(biomodelURI);
	}
    
    /** Focus given scene Node. */
    private void focusNode(Node node){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                keywordTerm.requestFocus();
            }
        });
    }
    
    /** Set number of entries in the query. */
    private void setBiomodelCount(Integer count){
    	Platform.runLater(new Runnable() {
            @Override
            public void run() {
            	String text = "Biomodels";
            	if (count != null && count != 0){
            		text += " (" + count.toString() + ")";
            	} 
            	entryLabel.setText(text);
            }
    	});	
    }
	
    /** Set GUI elements for query status visible. */
    private void showQueryStatus(Boolean show){
    	Platform.runLater(new Runnable() {
            @Override
            public void run() {
		    	statusCode.setVisible(show);
		    	statusCodeLabel.setVisible(show);
		    	time.setVisible(show);
		    	timeLabel.setVisible(show);
            }
        });
    }
    
    /** Set progress in progress indicator. */
    private void setProgress(double progress){
    	Platform.runLater(new Runnable() {
            @Override
            public void run() {
		    	progressIndicator.setProgress(progress);
            }
        });
    }
    
    /** Open url in external browser. */
    private void openURLinExternalBrowser(String url){
    	if (WebViewSwing.openBrowser != null){
	    	logger.info("Open in external browser <" + url +">");
    		SwingUtilities.invokeLater(new Runnable() {
    		     public void run() {
    		    	 WebViewSwing.openBrowser.openURL(url);
    		     }
    		});
        } else {
       	 	logger.error("No external browser available.");
        }
    }

    /*
	 * Check if given link is an external link.
	 * File links, and links to kineticLawInformation are opened in the WebView.
	 */
	private Boolean isExternalLink(String link){
		Boolean external = true;

		if (link.startsWith("http://sabiork.h-its.org/kineticLawEntry.jsp")){
			external = false;
		} else if (link.startsWith("file:///")){
			external = false;
		}
		return external;
	}
    
    // --------------------------------------------------------------------
    // Initialize
    // --------------------------------------------------------------------
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		logger = new Logger(this.log);
		suggestions = QuerySuggestions.fromStaticData();
		searchTerms = new HashSet<>();
		filters = new HashMap<>();

		queryHistory = new QueryHistory();
		
		// ---------------------------
		// Images
		// ---------------------------
		imageBiomodelLogo.setImage(new Image(ResourceExtractor.fileURIforResource("/biomodels/gui/images/biomodels_logo.png").toString()));
		imageBiomodelLogo.setOnMousePressed(me -> {
			openURLinExternalBrowser("http://sabiork.h-its.org/");
	    });
		
		imageHelp.setImage(new Image(ResourceExtractor.fileURIforResource("/biomodels/gui/images/icon-help.png").toString()));
		imageHelp.setOnMousePressed(me -> {
            setHelp();
	    });

		imageSBML.setImage(new Image(ResourceExtractor.fileURIforResource("/biomodels/gui/images/logo-sbml.png").toString()));
        imageSBML.setOnMousePressed(me -> {
            logger.info("Open SBML for query");
            SBMLDocument doc = null;
            try {
                // write to tmp file and open in browser
                File temp = File.createTempFile("cy3sabiork", ".xml");
                TidySBMLWriter.write(doc, temp.getAbsolutePath(), ' ', (short) 2);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        OpenBrowser.openURL("file://" + temp.getAbsolutePath());
                    }
                });
            } catch (SBMLException | XMLStreamException| IOException e) {
                logger.error("SBML opening failed.");
                e.printStackTrace();
            }
        });

		// ---------------------------
		// Table for Biomodels
		// ---------------------------
		biomodelsTable.setEditable(false);

		idCol.setCellValueFactory(new PropertyValueFactory<Biomodel, String>("publicationId"));
		nameCol.setCellValueFactory(new PropertyValueFactory<Biomodel, String>("name"));
		
		biomodelsTable.setOnMousePressed(me -> {
	        if (me.isPrimaryButtonDown() && me.getClickCount() == 1) {
	        	Object selected = biomodelsTable.getSelectionModel().getSelectedItem();
	        	if (selected != null){
	        		String biomodelId = ((Biomodel) selected).getPublicationIdentifier();
	        		setInfoForBiomodel(biomodelId);
	        	}                   
	        }
		});
		
		// SelectionChange Listener (Important if selection via error keys change)
		biomodelsTable.getSelectionModel().selectedItemProperty().addListener(
	            new ChangeListener<Biomodel>() {
	                public void changed(ObservableValue<? extends Biomodel> ov,
	                    Biomodel oldValue, Biomodel newValue) {
	                		String biomodelId = newValue.getPublicationIdentifier();
	                		setInfoForBiomodel(biomodelId);
	            }
	        });
		
		//-----------------------
		// Webengine & Webview
		//-----------------------
		WebEngine webEngine = webView.getEngine();
		setHelp();
		webView.setZoom(1.0);

		// Listening to hyperlink events
		WebViewHyperlinkListener eventProcessingListener = event -> {
			System.out.println(WebViews.hyperlinkEventToString(event));

			URL url = event.getURL();
			if (url == null){
			    // for instance if netscape javascript is not available
			    return true;
            }
			if (isExternalLink(url.toString())) {
				openURLinExternalBrowser(url.toString());
				return true;
			}
			// This is a link we should load, do not cancel.
			return false;
		};
		WebViews.addHyperlinkListener(webView, eventProcessingListener, HyperlinkEvent.EventType.ACTIVATED);

		// WebView Javascript -> Java upcalls using JavaApp
        // see https://groups.google.com/forum/#!topic/cytoscape-helpdesk/Sl_MwfmLTx0
        webEngine.getLoadWorker().stateProperty().addListener(
            new ChangeListener<State>() {
                @Override
                public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {    
                	if (newState == State.SUCCEEDED) {
                		try{
                			JSObject win = (JSObject) webEngine.executeScript("window");
                            win.setMember("app", new JavaApp());	
                		} catch(NoClassDefFoundError e){
                			System.out.println("netscape.javascript not accessible in Cytoscape");
                		}                        
                	}
                }
            }
        );
        
		// ---------------------------
		// Keywords (List & Text)
		// ---------------------------
		ObservableList<String> items = FXCollections.observableArrayList(suggestions.getKeywords());
		keywordList.setItems(items);
	
		keywordList.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> ov, 
                    String oldValue, String newValue) {
                		logger.info("Keyword <" + newValue + "> selected.");
                		// set keyword in field
                        keyword.setText(newValue);
                        // focus keywordTerm field
                        focusNode(keywordTerm);
            }
        });
		
		// autocomplete on keywords
		keywordBinding = TextFields.bindAutoCompletion(keyword, suggestions.getKeywords());
		keywordBinding.setOnAutoCompleted(e -> {
			// select in list on autocomplete
			keywordList.getSelectionModel().select(keyword.getText());
			}
		);	
		
		keyword.setOnKeyPressed(ke -> {
			// check if keyword in list, if yes select in list
			String key = keyword.getText();
			if (suggestions.getKeywords().contains(keyword.getText()) ){
				keywordList.getSelectionModel().select(key);
			} else if (ke.getCode() == KeyCode.ENTER){
				focusNode(keywordTerm);
			}
        });
		
		// ---------------------------
		// Term
		// ---------------------------
		// dynamical autocomplete
		keywordTerm.focusedProperty().addListener(new ChangeListener<Boolean>(){
		    @Override
		    public void changed(ObservableValue<? extends Boolean> arg0, Boolean ov, Boolean nv){
		        // textfield focused
		    	if (nv){
		    		// update dynamic autocomplete on terms
		    		TreeSet<String> termSet = suggestions.getSuggestionsForKeyword(keyword.getText());
                    if (termSet != null){
                    	if (termBinding != null){
                    		termBinding.dispose();
                    	}
                    	termBinding = TextFields.bindAutoCompletion(keywordTerm, termSet);
    					termBinding.setOnAutoCompleted(e -> {
    						// add entry on autocomplete
    						focusNode(addKeywordButton);
    						}
    					);	
                    } else {
                    	if (termBinding != null){
                    		termBinding.dispose();
                    	}
                    }
		        }
		    }
		});
		
		keywordTerm.setOnKeyPressed(ke -> {
			if (ke.getCode() == KeyCode.ENTER){
				addKeywordButton.fire();
			}
        });

        // ---------------------------
        // History (List)
        // ---------------------------
        historyList.setItems(queryHistory.getAll());

        // on selection update the query keywordTerm
        historyList.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<String>() {
                    public void changed(ObservableValue<? extends String> ov,
                                        String oldValue, String newValue) {
                        logger.info("History query <" + newValue + "> selected.");
                        // set keyword in field
                        queryText.setText(newValue);
                    }
                }
        );

		// ---------------------------
		// Logging
		// ---------------------------
		log.textProperty().addListener(new ChangeListener<Object>() {
		    @Override
		    public void changed(ObservableValue<?> observable, Object oldValue,
		            Object newValue) {
		    	// FIXME: this is not working like expected
		        log.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
		        //use Double.MIN_VALUE to scroll to the top
		    }
		});
		
		//-----------------------
		// SabioStatus
		//-----------------------
		showQueryStatus(false);
        handleResetAction(null);
	}
	
}