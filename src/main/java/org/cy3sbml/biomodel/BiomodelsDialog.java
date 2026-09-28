package org.cy3sbml.biomodel;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The dialog to search BioModels and load models from it.
 * <p>
 * The dialog only depends on the search, which accesses the BioModels web service, and on
 * callbacks to open a URL and to load models, so it can be used without Cytoscape. All
 * web service access runs in the background; the dialog state is only accessed on the
 * Swing event dispatch thread.
 */
public final class BiomodelsDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(BiomodelsDialog.class);

    private final SearchBioModel searchBioModel;
    private final Consumer<List<String>> loadModels;

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

    /** The current search result, null if none. */
    private SearchBioModel.Result searchResult;
    /** The details of the models of the current search result looked up so far, by id. */
    private final Map<String, Biomodel> details = new HashMap<>();
    /** The ids of the models of the current search result whose details are being looked up. */
    private final Set<String> loading = new HashSet<>();
    /** The position of every model in the information, by id. */
    private Map<String, Integer> modelOffsets = Map.of();
    /** The running search, null if none. */
    private SwingWorker<?, ?> busyWorker;

    /**
     * @param parent the parent frame of the dialog
     * @param searchBioModel the BioModels search
     * @param openUrl opens a URL in the web browser
     * @param loadModels loads the models with the given ids, called on the event dispatch thread
     */
    @SuppressWarnings("rawtypes")
    public BiomodelsDialog(
            Frame parent, SearchBioModel searchBioModel, Consumer<String> openUrl, Consumer<List<String>> loadModels) {
        super(parent, true);
        this.searchBioModel = searchBioModel;
        this.loadModels = loadModels;

        logger.info("BioModelGUIDialog created");

        this.setSize(1000, 754);
        this.setResizable(true);
        this.setTitle("CySBML BioModel Import");
        this.setLocationRelativeTo(parent);
        panel = new JPanel();
        getContentPane().setLayout(null);
        panel.setBounds(0, 0, 1000, 768);
        getContentPane().add(panel, BorderLayout.NORTH);
        panel.setLayout(null);
        getContentPane().setLayout(new BorderLayout(0, 0));

        // Labels
        JLabel lblLoadByBiomodel = new JLabel("BioModel Ids");
        lblLoadByBiomodel.setBounds(33, 576, 160, 15);
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
        loadIdsButton.setBounds(170, 689, 102, 25);
        panel.add(loadIdsButton);

        loadIdsButton.addActionListener(event -> loadBioModelByIdsAndDisposeDialog());

        parseIdsButton = new JButton("Parse Ids");
        parseIdsButton.setToolTipText("Parse BioModel Ids from text.");
        parseIdsButton.setBounds(33, 689, 102, 25);
        panel.add(parseIdsButton);
        parseIdsButton.addActionListener(event -> parseBioModelByIds());

        // Search Button
        searchButton = new JButton("Search");
        searchButton.setToolTipText("Search Biomodels");
        searchButton.setBounds(33, 71, 102, 25);
        panel.add(searchButton);
        searchButton.addActionListener(event -> searchBioModels());

        // Reset Button
        resetButton = new JButton("Reset");
        resetButton.setToolTipText("Reset Search Fields");
        resetButton.setBounds(170, 71, 102, 25);
        panel.add(resetButton);
        resetButton.addActionListener(event -> resetFields());
        // Load Selected Models
        loadSelectedButton = new JButton("Load Selected");
        loadSelectedButton.setToolTipText("Load selected BioModels from the List");
        loadSelectedButton.setBounds(33, 534, 160, 25);
        loadSelectedButton.setEnabled(false);
        panel.add(loadSelectedButton);
        loadSelectedButton.addActionListener(event -> loadSelectedBioModelsAndDisposeDialog());

        // ScrollBars
        JScrollPane listScrollPane = new JScrollPane();
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        listScrollPane.setBounds(33, 108, 239, 414);
        panel.add(listScrollPane);

        // Set the empty Lists
        biomodelsList = new JList();
        biomodelsList.setToolTipText("Search results, select for information.");

        biomodelsList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedButton.setEnabled(!biomodelsList.isSelectionEmpty());
                handleModelSelectionInModelList();
            }
        });

        listScrollPane.setViewportView(biomodelsList);

        // information area
        infoScrollPane = new JScrollPane();
        infoScrollPane.setBounds(288, 12, 687, 701);
        panel.add(infoScrollPane);

        infoPane = new JEditorPane();
        infoPane.setToolTipText("Information Area");
        infoPane.setContentType("text/html");
        infoPane.setEditable(false);
        // the dialog scrolls the information itself, a new text must not scroll to the caret
        ((DefaultCaret) infoPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        infoPane.setText(BioModelDialogText.getInfo());
        infoPane.addHyperlinkListener(evt -> {
            URL url = evt.getURL();
            if (url != null && evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                openUrl.accept(url.toString());
            }
        });
        infoScrollPane.setViewportView(infoPane);

        JSeparator separator = new JSeparator();
        separator.setBounds(33, 571, 239, 2);
        panel.add(separator);

        JLabel lblComposeBy = new JLabel("Compose by");
        lblComposeBy.setBounds(33, 44, 102, 15);
        panel.add(lblComposeBy);

        chckbxAND = new JCheckBox("AND");
        chckbxAND.setBounds(125, 40, 61, 23);
        panel.add(chckbxAND);
        chckbxOR = new JCheckBox("OR");
        chckbxOR.setBounds(188, 40, 61, 23);
        panel.add(chckbxOR);
        chckbxAND.addChangeListener(event -> chckbxOR.setSelected(!chckbxAND.isSelected()));
        chckbxOR.addChangeListener(event -> chckbxAND.setSelected(!chckbxOR.isSelected()));
        chckbxAND.setSelected(true);

        JScrollPane idsScrollPane = new JScrollPane();
        idsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        idsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        idsScrollPane.setBounds(33, 593, 239, 90);
        panel.add(idsScrollPane);

        idTextArea = new JTextArea();
        idsScrollPane.setViewportView(idTextArea);
        idTextArea.setWrapStyleWord(true);
        idTextArea.setToolTipText("Past BioModel Ids to load.");
        idTextArea.setLineWrap(true);
        idTextArea.setRows(4);
        idTextArea.setTabSize(4);
        idTextArea.setText("BIOMD0000000070, BIOMD0000000071");

        // closing the dialog stops a running search
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                if (busyWorker != null) {
                    busyWorker.cancel(true);
                }
            }
        });
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

    /// ////// BACKGROUND WORK ////////////

    /**
     * Runs the given web service access off the event dispatch thread while the dialog
     * shows the busy state, then hands its result or failure to the given handlers on the
     * event dispatch thread. A cancelled access shows the current result again.
     */
    private <T> void runInBackground(
            String busyText, Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        setBusy(true);
        infoPane.setText(busyText);
        busyWorker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                busyWorker = null;
                setBusy(false);
                try {
                    onSuccess.accept(get());
                } catch (CancellationException e) {
                    // keep the current result, which the list still shows
                    if (searchResult != null) {
                        showInformation(null);
                    } else {
                        infoPane.setText(BioModelDialogText.getInfo());
                    }
                } catch (ExecutionException e) {
                    onFailure.accept(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(e);
                }
            }
        };
        busyWorker.execute();
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

    /**
     * Looks up the details of the given models of the current search result in the
     * background, unless they are known or being looked up, and shows them when they
     * arrive. Does not block the dialog.
     */
    private void requestDetails(List<String> ids) {
        List<String> missing = new ArrayList<>();
        for (String id : ids) {
            if (!details.containsKey(id) && !loading.contains(id)) {
                missing.add(id);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        loading.addAll(missing);
        SearchBioModel.Result requestResult = searchResult;
        new SwingWorker<Map<String, Biomodel>, Void>() {
            @Override
            protected Map<String, Biomodel> doInBackground() {
                return searchBioModel.getDetails(missing);
            }

            @Override
            protected void done() {
                if (!Objects.equals(searchResult, requestResult)) {
                    // a new search replaced the result
                    return;
                }
                try {
                    details.putAll(get());
                } catch (ExecutionException e) {
                    logger.warn(
                            "Could not look up the BioModels {}: {}",
                            missing,
                            e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                missing.forEach(loading::remove);
                showInformation(null);
            }
        }.execute();
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

    /**
     * Shows the given search result, none if null, and looks up the details of parsed ids.
     */
    private void showSearchResult(SearchBioModel.Result result) {
        searchResult = result;
        details.clear();
        loading.clear();
        updateModelListInDialog(result != null ? result.modelIds() : List.of());
        if (result == null) {
            return;
        }
        if (result.isParsed()) {
            requestDetails(result.modelIds());
        }
        setInformation(BiomodelsHtml.searchResult(result, details, loading, getListOfSelectedModelIds()));
        infoScrollPane.validate();
        infoScrollPane.getVerticalScrollBar().setValue(0);
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

    /**
     * Shows the information of the current search result, it does not access the web
     * service. Scrolls the given model to the top; if null, keeps the model at the top of
     * the view in place, so details that arrive above it do not move it.
     */
    private void showInformation(String scrollToId) {
        if (searchResult == null) {
            return;
        }
        JScrollBar scrollBar = infoScrollPane.getVerticalScrollBar();
        int scrollPosition = scrollBar.getValue();
        String topId = scrollToId == null ? modelAt(scrollPosition) : null;
        int topOffset = topId != null ? scrollPosition - modelPosition(topId) : 0;

        setInformation(BiomodelsHtml.searchResult(searchResult, details, loading, getListOfSelectedModelIds()));
        // lay out the new text, so the scroll bar has its range and the models their position
        infoScrollPane.validate();
        if (scrollToId != null) {
            scrollBar.setValue(Math.max(0, modelPosition(scrollToId)));
        } else if (topId != null && modelPosition(topId) >= 0) {
            scrollBar.setValue(modelPosition(topId) + topOffset);
        } else {
            scrollBar.setValue(scrollPosition);
        }
    }

    /** Shows the given information and reads the position of every model in it. */
    private void setInformation(String html) {
        infoPane.setText(html);
        Map<String, Integer> offsets = new HashMap<>();
        HTMLDocument.Iterator anchors = ((HTMLDocument) infoPane.getDocument()).getIterator(HTML.Tag.A);
        for (; anchors.isValid(); anchors.next()) {
            if (anchors.getAttributes().getAttribute(HTML.Attribute.NAME) instanceof String name) {
                offsets.put(name, anchors.getStartOffset());
            }
        }
        modelOffsets = offsets;
    }

    /** Returns the vertical position of the given model in the information, -1 if unknown. */
    private int modelPosition(String modelId) {
        Integer offset = modelOffsets.get(modelId);
        if (offset == null) {
            return -1;
        }
        try {
            Rectangle2D view = infoPane.modelToView2D(offset);
            return view != null ? (int) view.getY() : -1;
        } catch (BadLocationException e) {
            return -1;
        }
    }

    /**
     * Returns the id of the last model of the current search result that starts at or
     * above the given position, null if none.
     */
    private String modelAt(int position) {
        List<String> ids = searchResult.modelIds();
        // the models are shown in the order of the ids
        int low = 0;
        int high = ids.size() - 1;
        String found = null;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int modelPosition = modelPosition(ids.get(middle));
            if (modelPosition < 0) {
                return null;
            }
            if (modelPosition <= position) {
                found = ids.get(middle);
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return found;
    }

    /// ////// SELECT MODELS ////////////

    /**
     * Looks up the details of the selected models and scrolls to the model selected last.
     */
    private void handleModelSelectionInModelList() {
        if (searchResult == null) {
            return;
        }
        List<String> selected = getListOfSelectedModelIds();
        requestDetails(selected);
        int lead = biomodelsList.getLeadSelectionIndex();
        String scrollToId = null;
        if (lead >= 0 && lead < searchResult.modelIds().size() && biomodelsList.isSelectedIndex(lead)) {
            scrollToId = searchResult.modelIds().get(lead);
        }
        showInformation(scrollToId);
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
     * Closes the dialog and loads the given BioModels.
     */
    private void loadBioModelsAndDisposeDialog(List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        logger.info("Load BioModels: {}", ids);
        dispose();
        loadModels.accept(ids);
    }

    /**
     * Lists the BioModel ids parsed from the text and looks up their details.
     */
    public void parseBioModelByIds() {
        Set<String> ids = parseBioModelIdsFromString(idTextArea.getText());
        idTextArea.setText(String.join(" ", ids));
        showSearchResult(SearchBioModel.fromIds(ids));
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
