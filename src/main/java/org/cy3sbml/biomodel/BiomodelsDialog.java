package org.cy3sbml.biomodel;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import org.apache.commons.text.StringEscapeUtils;
import org.cy3sbml.ServiceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BiomodelsDialog.
 */
public final class BiomodelsDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(BiomodelsDialog.class);

    private final ServiceAdapter adapter;
    private final BiomodelsQuery biomodelsQuery;
    private final SearchBioModel searchBioModel;

    private final JTextArea idTextArea;
    private final JTextField nameField;

    private final JCheckBox chckbxAND;
    private final JCheckBox chckbxOR;

    private final JButton loadSelectedButton;
    private final JButton loadIdsButton;
    private final JButton parseIdsButton;
    private final JButton searchButton;
    private final JButton resetButton;
    private final JPanel panel;
    private final JScrollPane infoScrollPane;
    private final JEditorPane infoPane;

    @SuppressWarnings("rawtypes")
    private JList biomodelsList;

    /** The current search result, only accessed on the event dispatch thread. */
    private SearchBioModel.Result searchResult;

    @SuppressWarnings("rawtypes")
    public BiomodelsDialog(final ServiceAdapter adapter, final BiomodelsQuery biomodelsQuery) {
        // call with parentFrame
        super(adapter.cySwingApplication.getJFrame(), true);
        this.adapter = adapter;
        this.biomodelsQuery = biomodelsQuery;

        logger.info("BioModelGUIDialog created");

        this.setSize(1000, 886);
        this.setResizable(true);
        this.setTitle("CySBML BioModel Import");
        JFrame parentFrame = adapter.cySwingApplication.getJFrame();
        this.setLocationRelativeTo(parentFrame);
        panel = new JPanel();
        getContentPane().setLayout(null);
        panel.setBounds(0, 0, 1000, 900);
        getContentPane().add(panel, BorderLayout.NORTH);
        panel.setLayout(null);
        getContentPane().setLayout(new BorderLayout(0, 0));

        // Labels
        JLabel lblLoadByBiomodel = new JLabel("BioModel Ids");
        lblLoadByBiomodel.setBounds(33, 708, 160, 15);
        panel.add(lblLoadByBiomodel);
        JLabel lblName = new JLabel("Name");
        lblName.setBounds(33, 17, 160, 15);
        panel.add(lblName);

        // Search By Name Field
        nameField = new JTextField();
        nameField.setToolTipText("Search terms, searched in the whole BioModels entry");
        nameField.setBounds(112, 12, 160, 25);
        nameField.setText("glycolysis");
        nameField.setColumns(10);
        panel.add(nameField);
        nameField.addKeyListener(new EnterKeyAdapter());

        // Load Ids Button
        loadIdsButton = new JButton("Load Ids");
        loadIdsButton.setToolTipText("Parse BioModel Ids and load the models.");
        loadIdsButton.setBounds(170, 821, 102, 25);
        panel.add(loadIdsButton);

        loadIdsButton.addActionListener(event -> loadBioModelByIdsAndDisposeDialog());

        parseIdsButton = new JButton("Parse Ids");
        parseIdsButton.setToolTipText("Parse BioModel Ids from text.");
        parseIdsButton.setBounds(33, 821, 102, 25);
        panel.add(parseIdsButton);
        parseIdsButton.addActionListener(event -> parseBioModelByIds());

        // Search Button
        searchButton = new JButton("Search");
        searchButton.setToolTipText("Search Biomodels");
        searchButton.setBounds(33, 203, 102, 25);
        panel.add(searchButton);
        searchButton.addActionListener(event -> searchBioModels());

        // Reset Button
        resetButton = new JButton("Reset");
        resetButton.setToolTipText("Reset Search Fields");
        resetButton.setBounds(170, 203, 102, 25);
        panel.add(resetButton);
        resetButton.addActionListener(event -> resetFields());
        searchBioModel = new SearchBioModel(biomodelsQuery);
        // Load Selected Models
        loadSelectedButton = new JButton("Load Selected");
        loadSelectedButton.setToolTipText("Load selected BioModels from the List");
        loadSelectedButton.setBounds(33, 666, 160, 25);
        loadSelectedButton.setEnabled(false);
        panel.add(loadSelectedButton);
        loadSelectedButton.addActionListener(event -> loadSelectedBioModelsAndDisposeDialog());

        // ScrollBars
        JScrollPane listScrollPane = new JScrollPane();
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        listScrollPane.setBounds(33, 240, 239, 414);
        panel.add(listScrollPane);

        // Set the empty Lists
        biomodelsList = new JList();
        biomodelsList.setToolTipText("Search results, select for information.");

        biomodelsList.addListSelectionListener(event -> {
            // activate load button & get information for selection
            if (biomodelsList.isSelectionEmpty()) {
                loadSelectedButton.setEnabled(false);
            } else {
                loadSelectedButton.setEnabled(true);
            }
            handleModelSelectionInModelList();
        });

        listScrollPane.setViewportView(biomodelsList);

        // information area
        infoScrollPane = new JScrollPane();
        infoScrollPane.setBounds(288, 12, 687, 833);
        panel.add(infoScrollPane);

        infoPane = new JEditorPane();
        infoPane.setToolTipText("Information Area");
        infoPane.setContentType("text/html");
        infoPane.setEditable(false);
        infoPane.setText(BioModelDialogText.getInfo());
        infoPane.addHyperlinkListener(evt -> {
            URL url = evt.getURL();
            if (url != null) {
                if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    adapter.openBrowser.openURL(url.toString());
                }
            }
        });
        infoScrollPane.setViewportView(infoPane);

        JSeparator separator = new JSeparator();
        separator.setBounds(33, 703, 239, 2);
        panel.add(separator);

        JLabel lblComposeBy = new JLabel("Compose by");
        lblComposeBy.setBounds(33, 176, 102, 15);
        panel.add(lblComposeBy);

        chckbxAND = new JCheckBox("AND");
        chckbxAND.setBounds(125, 172, 61, 23);
        panel.add(chckbxAND);
        chckbxOR = new JCheckBox("OR");
        chckbxOR.setBounds(188, 172, 61, 23);
        panel.add(chckbxOR);
        chckbxAND.addChangeListener(event -> chckbxOR.setSelected(!chckbxAND.isSelected()));
        chckbxOR.addChangeListener(event -> chckbxAND.setSelected(!chckbxOR.isSelected()));
        chckbxAND.setSelected(true);

        JScrollPane idsScrollPane = new JScrollPane();
        idsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        idsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        idsScrollPane.setBounds(33, 725, 239, 90);
        panel.add(idsScrollPane);

        idTextArea = new JTextArea();
        idsScrollPane.setViewportView(idTextArea);
        idTextArea.setWrapStyleWord(true);
        idTextArea.setToolTipText("Past BioModel Ids to load.");
        idTextArea.setLineWrap(true);
        idTextArea.setRows(4);
        idTextArea.setTabSize(4);
        idTextArea.setText("BIOMD0000000070, BIOMD0000000071");
    }

    class EnterKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent keyE) {
            int key = keyE.getKeyCode();
            if (key == KeyEvent.VK_ENTER && searchButton.isEnabled()) {
                searchBioModels();
            }
        }
    }

    public void showBioModelsPanel() {

        JFrame frame = new JFrame("CySBML BioModel Import");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add content to the window.
        frame.getContentPane().add(this);
        frame.setSize(600, 600);
        frame.setResizable(true);
        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    /// ////// BACKGROUND WORK ////////////

    /**
     * Runs the given web service access off the event dispatch thread while the dialog
     * shows the busy state, then hands its result or failure to the given handlers on the
     * event dispatch thread.
     */
    private <T> void runInBackground(
            String busyText, Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        setBusy(true);
        infoPane.setText(busyText);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    onFailure.accept(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(e);
                }
            }
        }.execute();
    }

    /** Disables the actions and shows the wait cursor while a web service access runs. */
    private void setBusy(boolean busy) {
        for (JButton button : List.of(searchButton, resetButton, parseIdsButton, loadIdsButton)) {
            button.setEnabled(!busy);
        }
        loadSelectedButton.setEnabled(!busy && !biomodelsList.isSelectionEmpty());
        biomodelsList.setEnabled(!busy);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    /// ////// SEARCH MODELS ////////////
    public void searchBioModels() {
        SearchContent searchContent = getSearchContent();
        logger.info("Search BioModels: {}", searchContent.namesToString(" "));
        runInBackground(
                BioModelDialogText.performBioModelSearch(),
                () -> searchBioModel.search(searchContent),
                this::showSearchResult,
                error -> {
                    logger.warn("BioModels search failed: {}", error.getMessage());
                    showSearchResult(null);
                    infoPane.setText(BioModelDialogText.getWebserviceError());
                });
    }

    public SearchContent getSearchContent() {
        String mode = SearchContent.CONNECT_AND;
        if (chckbxOR.isSelected()) {
            mode = SearchContent.CONNECT_OR;
        }
        return new SearchContent(
                Map.of(SearchContent.CONTENT_NAME, nameField.getText(), SearchContent.CONTENT_MODE, mode));
    }

    /// ////// UPDATE GUI ////////////

    /** Shows the given search result, none if null. */
    private void showSearchResult(SearchBioModel.Result result) {
        searchResult = result;
        updateModelListInDialog(result != null ? result.modelIds() : List.of());
        if (result == null) {
            return;
        }
        updateBioModelInformation(getListOfSelectedModelIds());

        int topPosition = 0;
        infoPane.setCaretPosition(topPosition);
        JScrollBar scrollBar = infoScrollPane.getVerticalScrollBar();
        scrollBar.setValue(topPosition);
    }

    // working on raw JList - yes this should be like that
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void updateModelListInDialog(final List<String> modelIds) {
        biomodelsList.setModel(new AbstractListModel() {
            List<String> values = modelIds;

            @Override
            public int getSize() {
                return values.size();
            }

            @Override
            public Object getElementAt(int index) {
                return values.get(index);
            }
        });
    }

    /** Shows the information of the current search result, it does not access the web service. */
    public void updateBioModelInformation(List<String> selectedModelIds) {
        if (searchResult == null) {
            return;
        }
        final int caretPosition = infoPane.getCaretPosition();
        final int scrollPosition = infoScrollPane.getVerticalScrollBar().getValue();
        Point location = infoScrollPane.getViewport().getLocation();

        infoPane.setText(SearchBioModel.getHTMLInformation(searchResult, selectedModelIds));
        try {
            infoPane.setCaretPosition(caretPosition);
        } catch (java.lang.IllegalArgumentException e) {
            logger.debug("Could not restore the caret position", e);
        }

        infoScrollPane.getViewport().setLocation(location);

        javax.swing.SwingUtilities.invokeLater(
                () -> infoScrollPane.getVerticalScrollBar().setValue(scrollPosition));
    }

    /// ////// SELECT MODELS ////////////
    private void handleModelSelectionInModelList() {
        updateBioModelInformation(getListOfSelectedModelIds());
    }

    public List<String> getListOfSelectedModelIds() {
        @SuppressWarnings("unchecked")
        List<String> selected = biomodelsList.getSelectedValuesList();
        return selected;
    }

    /// ////// LOAD MODELS ////////////
    public void loadSelectedBioModelsAndDisposeDialog() {
        if (!biomodelsList.isSelectionEmpty()) {
            loadBioModelsAndDisposeDialog(getListOfSelectedModelIds());
        }
    }

    public void loadBioModelByIdsAndDisposeDialog() {
        loadBioModelsAndDisposeDialog(List.copyOf(parseBioModelIdsFromString(idTextArea.getText())));
    }

    /**
     * Downloads the SBML of the given BioModels in the background, then closes the dialog,
     * loads the downloaded models and reports the ones that could not be downloaded.
     */
    private void loadBioModelsAndDisposeDialog(List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        logger.info("Load BioModels: {}", ids);
        runInBackground(
                BioModelDialogText.getWebserviceSBMLRequest(),
                () -> LoadBioModelTaskFactory.download(ids, biomodelsQuery, adapter),
                factories -> {
                    dispose();
                    String errors = "";
                    for (LoadBioModelTaskFactory factory : factories) {
                        if (factory.isReady()) {
                            adapter.dialogTaskManager.execute(factory.createTaskIterator());
                        } else {
                            errors += String.format(
                                    "<br><b>%s</b>: %s",
                                    factory.getId(), StringEscapeUtils.escapeHtml4(factory.getError()));
                        }
                    }
                    if (!errors.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                adapter.cySwingApplication.getJFrame(),
                                "<html>No SBML could be loaded for the BioModels:" + errors + "</html>");
                    }
                },
                error -> {
                    logger.error("Could not load the BioModels {}", ids, error);
                    JOptionPane.showMessageDialog(
                            this,
                            String.format(
                                    "<html>Could not load the BioModels: <b>%s</b><br>%s</html>",
                                    String.join(", ", ids), StringEscapeUtils.escapeHtml4(String.valueOf(error))));
                });
    }

    public void parseBioModelByIds() {
        Set<String> ids = parseBioModelIdsFromString(idTextArea.getText());
        idTextArea.setText(String.join(" ", ids));
        runInBackground(
                BioModelDialogText.getWebserviceSBMLRequest(),
                () -> searchBioModel.getInformation(ids),
                this::showSearchResult,
                error -> {
                    logger.warn("BioModels information failed: {}", error.getMessage());
                    showSearchResult(null);
                    infoPane.setText(BioModelDialogText.getWebserviceError());
                });
    }

    /**
     * Returns set of BioModel identifiers from given text.
     */
    public static Set<String> parseBioModelIdsFromString(String text) {
        Set<String> ids = new LinkedHashSet<String>();
        String bioModelPattern = "((BIOMD|MODEL)\\d{10})|(BMID\\d{12})";
        Pattern pattern = Pattern.compile(bioModelPattern);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String id = matcher.group();
            ids.add(id);
        }
        return ids;
    }

    // Clear fields
    public void resetFields() {
        String reset = "";
        nameField.setText(reset);
    }
}
