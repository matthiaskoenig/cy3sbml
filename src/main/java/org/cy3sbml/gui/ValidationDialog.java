package org.cy3sbml.gui;

import java.awt.Font;
import java.util.LinkedList;
import java.util.List;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.validator.Validator;
import org.cy3sbml.validator.ValidatorTask;
import org.cy3sbml.validator.ValidatorTaskFactory;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;


import javax.swing.ListSelectionModel;

@SuppressWarnings("serial")
public class ValidationDialog extends JDialog implements ListSelectionListener {

	private JEditorPane errorPane;
	private JTable errorTable;
	private Validator validator;

	/** Constructor */
	private ValidationDialog(JFrame parentFrame) {
		super(parentFrame, true);
		this.setSize(600, 800);
		this.setResizable(true);
		this.setLocationRelativeTo(parentFrame);
		getContentPane().setLayout(null);
		getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		getContentPane().add(panel);
		panel.setLayout(new BorderLayout(0, 0));

		JSplitPane splitPane = new JSplitPane();
		panel.add(splitPane);
		splitPane.setResizeWeight(0.1);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		JScrollPane errorScrollPane = new JScrollPane();
		splitPane.setRightComponent(errorScrollPane);

		errorPane = new JEditorPane();
		errorPane.setFont(new Font("Dialog", Font.PLAIN, 10));
		errorPane.setEditable(false);
		errorPane.setContentType("text/html");
		errorScrollPane.setViewportView(errorPane);
		
		errorTable = new JTable();
		errorTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		splitPane.setLeftComponent(errorTable);
		errorTable.setModel(new DefaultTableModel(
			new Object[][] {
				{"Info", ""},
				{"Warning", ""},
				{"Error", ""},
				{"Fatal", ""},
				{"All", ""},
			},
			new String[] {
				"Severity", "Error count"
			}
		));
		errorTable.getSelectionModel().addListSelectionListener(this);
	}

	public ValidationDialog(ServiceAdapter adapter, SBMLDocument document) {
		this(adapter.cySwingApplication.getJFrame());

		String title = "cy3sbml Validator : " + document.getModel().getId();
		setTitle(title);
 
		// validation task
		SBMLDocument doc = SBMLManager.getInstance().getCurrentSBMLDocument();
		ValidatorTaskFactory validationTaskFactory = new ValidatorTaskFactory(doc);
		TaskIterator iterator = validationTaskFactory.createTaskIterator();
		adapter.synchronousTaskManager.execute(iterator);

		
		// TODO: Get back the Validator (listen to it) -> do in observer (i.e. get notified)
		validator = ValidatorTask.getValidator(document);
		setErrorTableFromValidator(validator);
	}
	
	private void setErrorTableFromValidator(final Validator sbmlValidator){
		if (sbmlValidator.getErrorMap() != null) {
			errorTable.setModel(new DefaultTableModel(
					sbmlValidator.getErrorTable(),
					new String[] { "Severity", "Error count" }));
			errorTable.setRowSelectionInterval(2, 3);
			updateErrorTable();
		} else {
			errorPane.setText("Online SBML validation currently not possible.");
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		updateErrorTable();
	}
	
	private void updateErrorTable(){
		List<SBMLError> eList = new LinkedList<SBMLError>();
		// if categories selected get the errors
		if (errorTable.getSelectedRowCount() > 0){
			int[] selectedRows = errorTable.getSelectedRows();
			String[] keys = new String[selectedRows.length];
			for (int k=0; k<keys.length; ++k){
				keys[k] = (String) errorTable.getModel().getValueAt(selectedRows[k], 0);
			}
			eList = validator.getErrorListForKeys(keys);
		}
		errorPane.setText(validator.getErrorListString(eList));
	}
}
