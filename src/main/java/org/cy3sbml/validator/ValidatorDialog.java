package org.cy3sbml.validator;

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

import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ListSelectionModel;

@SuppressWarnings("serial")
public class ValidatorDialog extends JDialog implements ListSelectionListener, TaskObserver {
	private static final Logger logger = LoggerFactory.getLogger(ValidatorDialog.class);

	private ServiceAdapter adapter;
	private JEditorPane errorPane;
	private JTable errorTable;
	private Validator validator;

	/** Constructor */
	private ValidatorDialog(JFrame parentFrame) {
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

	public ValidatorDialog(ServiceAdapter adapter) {
		this(adapter.cySwingApplication.getJFrame());
		this.adapter = adapter;

		
		logger.info("ValidatorDialog created");
	}
	
	public void runValidation(SBMLDocument document){
		
		String title = "cy3sbml validator: " + document.getModel().getId();
		setTitle(title);
		// run validation task
		ValidatorTaskFactory validationTaskFactory = new ValidatorTaskFactory(document);
		TaskIterator iterator = validationTaskFactory.createTaskIterator();
		// adapter.synchronousTaskManager.execute(iterator);
		logger.info("run validation");
		adapter.taskManager.execute(iterator, this);
	}
	
	@Override
	public void taskFinished(ObservableTask task) {
		logger.info("taskFinished in ValidatorDialog");
		
		// execute task with task observer to be able to get results back
		Validator validator = (Validator) task.getResults(Validator.class);
		this.validator = validator;
		setErrorTable();
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
	}
	
	
	private void setErrorTable(){
		if (validator.getErrorMap() != null) {
			errorTable.setModel(new DefaultTableModel(
					validator.getErrorTable(),
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
		List<SBMLError> eList = new LinkedList<>();
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
