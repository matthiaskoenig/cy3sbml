package org.cy3sbml.gui;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.codefx.libfx.control.webview.WebViewHyperlinkListener;
import org.codefx.libfx.control.webview.WebViews;

import org.cy3sbml.actions.HelpAction;
import org.cy3sbml.util.GUIUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.TaskIterator;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.actions.ExamplesAction;
import org.cy3sbml.actions.ImportAction;
import org.cy3sbml.actions.ValidationAction;
import org.cy3sbml.biomodel.BioModelDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Browser for displaying HTML.
 * This can be embedded in Swing using a JFXPanel.
 */
public class Browser extends Region {
    private static final Logger logger = LoggerFactory.getLogger(Browser.class);


    private final WebView webView;
	private final WebEngine webEngine;
    private final OpenBrowser openBrowser;
	private final File appDirectory;

	public Browser(File appDirectory, OpenBrowser openBrowser) {
	    webView = new WebView();
        webEngine = webView.getEngine();

		this.appDirectory = appDirectory;
        this.openBrowser = openBrowser;
        logger.info("WebView version: " + webEngine.getUserAgent());
        logger.info("appDirectory: " + appDirectory);

		//add the web view to the scene
		getChildren().add(webView);

        // add hyperlink listener which processes the WebView hyperlink events.
        WebViewHyperlinkListener eventPrintingListener = event -> {
            System.out.println(WebViews.hyperlinkEventToString(event));
            return false;
        };

        /**
         * Handle hyperlink events in WebView.
         * Either opens browser for given hyperlink or triggers Cytoscape actions
         * for subsets of special hyperlinks.
         *
         * This provides an easy solution for integrating app functionality
         * with click on hyperlinks.
         * Alternative javascript upcalls could be performed.
         */
        WebViewHyperlinkListener eventProcessingListener = event -> {
            System.out.println(WebViews.hyperlinkEventToString(event));

            // clicked url
            URL url = event.getURL();

            if (url != null) {
                String s = url.toString();

                // Cytoscape Action
                if (GUIConstants.URLS_ACTION.contains(s)){
                    ServiceAdapter adapter = WebViewPanel.getInstance().getAdapter();

                    // BioModels
                    if (s.equals(GUIConstants.URL_BIOMODELS)){
                        BioModelDialog bioModelsDialog = BioModelDialog.getInstance(adapter);
                        bioModelsDialog.setVisible(true);
                        return true;
                    }
                    // ChangeState
                    if (s.equals(GUIConstants.URL_CHANGESTATE)){
                        WebViewPanel.getInstance().changeState();
                        return true;
                    }
                    // Import
                    if (s.equals(GUIConstants.URL_IMPORT)){
                        ImportAction importAction = new ImportAction(adapter);
                        importAction.actionPerformed(null);
                        return true;
                    }
                    // Validation
                    if (s.equals(GUIConstants.URL_VALIDATION)){
                        ValidationAction.openValidationPanel(adapter);
                        return true;
                    }
                    // Examples
                    if (s.equals(GUIConstants.URL_EXAMPLES)){
                        ExamplesAction examplesAction = new ExamplesAction(adapter.cySwingApplication);
                        examplesAction.actionPerformed(null);
                        return true;
                    }
                    // Examples
                    if (s.equals(GUIConstants.URL_HELP)){
                        HelpAction helpAction = new HelpAction(adapter.cySwingApplication);
                        helpAction.actionPerformed(null);
                        return true;
                    }

                }

                // Example networks
                if (GUIConstants.EXAMPLE_SBML.containsKey(s)){
                    String resource = GUIConstants.EXAMPLE_SBML.get(s);
                    loadExampleFromResource(resource);
                    return true;
                }

                // SBML file
                if (s.equals(GUIConstants.URL_SBMLFILE)){
                    GUIUtil.openCurrentSBMLInBrowser(openBrowser);
                    return true;
                }

                // HTML info
                if (s.equals(GUIConstants.URL_HTMLFILE)){
                    GUIUtil.openCurrentHTMLInBrowser(openBrowser);
                    return true;
                }

                // HTML links
                openURLinExternalBrowser(s);
                return true;
            }
            // This is a link we should load, do not cancel.
            return false;
        };

        // WebViews.addHyperlinkListener(webView, eventPrintingListener);
        // only listening to the clicks
        WebViews.addHyperlinkListener(webView, eventProcessingListener, HyperlinkEvent.EventType.ACTIVATED);

    }


    /** Open url in external webView. */
    private void openURLinExternalBrowser(String url){
        if (openBrowser != null){
            logger.info("Open in external webView <" + url +">");
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    openBrowser.openURL(url);
                }
            });
        } else {
            logger.error("No external webView available.");
        }
    }

    /**
     * Loads an SBML example file from the given resource.
     * Needs access to the LoadNetworkFileTaskFaktory and the SynchronousTaskManager.
     *
     * TODO: make this a general function.
     *
     * @param resource
     */
    private void loadExampleFromResource(String resource){
        InputStream instream = getClass().getResourceAsStream(resource);
        File tempFile;
        try {
            tempFile = File.createTempFile("tmp-example", ".xml");
            tempFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tempFile);
            IOUtils.copy(instream, out);

            // read the file
            ServiceAdapter adapter = WebViewPanel.getInstance().getAdapter();
            TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(tempFile);
            adapter.synchronousTaskManager.execute(iterator);
        } catch (Exception e) {
            logger.warn("Could not read example");
            e.printStackTrace();
        }
    }

	/**
	 * Load local resource;
	 */
	public void loadPageFromResource(String resource) {
		File file = new File(appDirectory, resource);
		URI fileURI = file.toURI();
		logger.info("resource to load:" + fileURI);
		loadPage(fileURI.toString());
	}

    /** Load page in webView; */
	public void loadPage(String url) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webEngine.load(url);
            }
        });
    }

    /**
	 * Load HTML text in the webEngine.
	 */
    public void loadText(String text){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webEngine.loadContent(text);
            }
        });

	}

	@Override
    protected void layoutChildren() {
		double w = getWidth();
		double h = getHeight();
		layoutInArea(webView,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
	}

	@Override
    protected double computePrefWidth(double height) {
		return 900;
	}

	@Override
    protected double computePrefHeight(double width) {
		return 600;
	}

}