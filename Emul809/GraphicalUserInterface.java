import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Defines a dynamic GUI that displays details of the emulator and contains buttons
 * enabling access to the required functionality.
 * 
 * @author Robert Wilson
 * 
 */
public class GraphicalUserInterface extends JFrame implements ActionListener {

	/**
	 * Instance variables
	 */
	private static final long serialVersionUID = 1L;
	private String addressToModify, mList, progCountString, accAString,
			accBString, accDString, xString, yString, dpString, hsString,
			usString, cFlagSet, zFlagSet, eFlagSet, hFlagSet, vFlagSet,
			nFlagSet, iFlagSet, fFlagSet;
	private int memAddress;
	private JMenuBar menuBar;
	private JMenu fileMenu, helpMenu;
	private JMenuItem openMenuItem, openUserGuide;
	private JTextArea codeIn, displayOutput;
	private S_Record sRec;
	/** GUI JButtons */
	private JButton runButton, saveButton, stepButton, clearButton,
			modifyMemoryButton, interruptButton;
	private JPanel displayPanel;
	/** focus listener for code entry area */
	private FocusListener focusListener;
	/** Text fields for registers */
	private JTextField pcField, xField, yField, aField, bField, dField,
			usField, hsField, dpField, E, F, H, I, N, Z, V, C;
	/** instances of the various classes */
	private CCR ccrFlags = new CCR();
	private ProgramCounter progCount = new ProgramCounter();
	private AccumulatorA accA = new AccumulatorA();
	private AccumulatorB accB = new AccumulatorB();
	private AccumulatorD accD = new AccumulatorD();
	private DirectPage dP = new DirectPage();
	private HardwareStackPointer hardStack = new HardwareStackPointer();
	private UserStackPointer userStack = new UserStackPointer();
	private IndexX xIndex = new IndexX();
	private IndexY yIndex = new IndexY();
	private MemoryHandling arrayForUpdate = new MemoryHandling(accA, accB,
			accD, progCount, ccrFlags, xIndex, yIndex, hardStack, userStack, dP);

	public GraphicalUserInterface() {

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("Emul809 - Motorola MC6809 emulator by Robert Wilson");
		setSize(1024, 680);
		setLocation(10, 0);

		// Menu bar
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		// Menu bar headings and sub-menus
		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		helpMenu = new JMenu("Help");
		menuBar.add(helpMenu);

		// Menu items
		openMenuItem = new JMenuItem("Open S Record file");
		fileMenu.add(openMenuItem);
		openMenuItem.addActionListener(this);
		openUserGuide = new JMenuItem("User Guide");
		helpMenu.add(openUserGuide);
		openUserGuide.addActionListener(this);

		layoutLeft();
		layoutRight();
		layoutDisplay();
		layoutBottom();
	}

	/**
	 * adds a display area for array
	 */
	public void layoutDisplay() {
		displayPanel = new JPanel();
		add(displayPanel, BorderLayout.CENTER);
		displayOutput = new JTextArea(32, 38);
		displayOutput.setBorder(javax.swing.BorderFactory
				.createTitledBorder("Memory Display Area"));
		JScrollPane memScroll = new JScrollPane(displayOutput);
		add(memScroll, BorderLayout.CENTER);
		displayOutput.setFont(new Font("Courier", Font.PLAIN, 14));
		displayOutput.addFocusListener(focusListener);

		mList = arrayForUpdate.getBoundProperty();
		displayOutput.setText(mList);
		displayOutput.setEditable(false);

		arrayForUpdate.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(
						MemoryHandling.BOUND_PROPERTY)) {
					mList = (pcEvt.getNewValue().toString());
					displayOutput.setText(mList);
				}
			}
		});
	}

	/**
	 * adds left hand side elements to left of GUI
	 */
	public void layoutLeft() {
		JPanel left = new JPanel();
		left.setBackground(new java.awt.Color(200, 244, 255));
		add(left, BorderLayout.WEST);
		codeIn = new JTextArea(22, 38);
		codeIn.setBorder(javax.swing.BorderFactory
				.createTitledBorder("Machine Code Area"));
		left.add(codeIn, BorderLayout.NORTH);
		codeIn.setFont(new Font("Courier", Font.PLAIN, 14));
		codeIn.addFocusListener(focusListener);
	}

	/**
	 * adds right hand side elements to right of GUI
	 */
	public void layoutRight() {
		JPanel right = new JPanel();
		right.setBackground(new java.awt.Color(200, 244, 255));
		add(right, BorderLayout.EAST);

		// Group layout
		GroupLayout layout = new GroupLayout(right);
		right.setLayout(layout);

		// automatic gap insertion
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		// define components
		JLabel statusLabel = new JLabel("Status register");
		JLabel pcLabel = new JLabel("Program counter");
		pcField = new JTextField(8);
		pcField.setEditable(false);

		JLabel xLabel = new JLabel("Index X");
		xField = new JTextField(8);
		xField.setEditable(false);
		xString = xIndex.getBoundX();
		// adds property change support for updating display
		xIndex.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(IndexX.BOUND_X)) {
					xString = (pcEvt.getNewValue().toString());
					xField.setText(xString);
				}
			}
		});
		xField.setText(xString);

		JLabel yLabel = new JLabel("Index Y");
		yField = new JTextField(8);
		yField.setEditable(false);
		yString = yIndex.getBoundY();
		// adds property change support for updating display
		yIndex.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(IndexY.BOUND_Y)) {
					yString = (pcEvt.getNewValue().toString());
					yField.setText(yString);
				}
			}
		});
		yField.setText(yString);

		JLabel aLabel = new JLabel("Accumulator A");
		aField = new JTextField(8);
		aField.setEditable(false);
		accAString = accA.getBoundAccA();
		// adds property change support for updating display
		accA.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(AccumulatorA.BOUND_ACCA)) {
					accAString = (pcEvt.getNewValue().toString());
					aField.setText(accAString);
				}
			}
		});
		aField.setText(accAString);

		JLabel bLabel = new JLabel("Accumulator B");
		bField = new JTextField(8);
		bField.setEditable(false);
		accBString = accB.getBoundAccB();
		// adds property change support for updating display
		accB.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(AccumulatorB.BOUND_ACCB)) {
					accBString = (pcEvt.getNewValue().toString());
					bField.setText(accBString);
				}
			}
		});
		bField.setText(accBString);

		JLabel dLabel = new JLabel("Accumulator D");
		dField = new JTextField(8);
		dField.setEditable(false);
		accDString = accD.getBoundAccD();
		// adds property change support for updating display
		accD.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(AccumulatorD.BOUND_ACCD)) {
					accDString = (pcEvt.getNewValue().toString());
					dField.setText(accDString);
				}
			}
		});
		dField.setText(accDString);

		JLabel hsLabel = new JLabel("Hardware Stack");
		hsField = new JTextField(8);
		hsField.setEditable(false);
		hsString = hardStack.getBoundHS();
		// adds property change support for updating display
		hardStack.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(
						HardwareStackPointer.BOUND_HS)) {
					hsString = (pcEvt.getNewValue().toString());
					hsField.setText(hsString);
				}
			}
		});
		hsField.setText(hsString);

		JLabel usLabel = new JLabel("User Stack");
		usField = new JTextField(8);
		usField.setEditable(false);
		usString = userStack.getBoundUS();
		// adds property change support for updating display
		userStack.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(UserStackPointer.BOUND_US)) {
					usString = (pcEvt.getNewValue().toString());
					usField.setText(usString);
				}
			}
		});
		usField.setText(usString);

		JLabel dpLabel = new JLabel("Direct Page");
		dpField = new JTextField(8);
		dpField.setEditable(false);
		dpString = dP.getBoundDP();
		// adds property change support for updating display
		dP.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(DirectPage.BOUND_DP)) {
					dpString = (pcEvt.getNewValue().toString());
					dpField.setText(dpString);
				}
			}
		});
		dpField.setText(dpString);

		progCountString = progCount.getBoundPC();
		// adds property change support for updating display
		progCount.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(ProgramCounter.BOUND_PC)) {
					progCountString = (pcEvt.getNewValue().toString());

					pcField.setText(progCountString);
				}
			}
		});

		pcField.setText(progCountString);

		/**
		 * JPanel for CCR status flags
		 */
		JPanel ccrPanel = new JPanel();
		ccrPanel.setBackground(Color.LIGHT_GRAY);
		E = new JTextField(1);
		E.setEditable(false);
		JLabel Elabel = new JLabel("E");
		ccrPanel.add(Elabel);
		ccrPanel.add(E);

		eFlagSet = ccrFlags.getBound_E();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_E)) {
					eFlagSet = (pcEvt.getNewValue().toString());
					E.setText(eFlagSet);
				}
			}
		});
		E.setText(eFlagSet);

		F = new JTextField(1);
		F.setEditable(false);
		JLabel Flabel = new JLabel("F");
		ccrPanel.add(Flabel);
		ccrPanel.add(F);

		fFlagSet = ccrFlags.getBound_F();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_F)) {
					fFlagSet = (pcEvt.getNewValue().toString());
					F.setText(fFlagSet);
				}
			}
		});
		F.setText(fFlagSet);

		H = new JTextField(1);
		H.setEditable(false);
		JLabel Hlabel = new JLabel("H");
		ccrPanel.add(Hlabel);
		ccrPanel.add(H);

		hFlagSet = ccrFlags.getBound_H();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_H)) {
					hFlagSet = (pcEvt.getNewValue().toString());
					H.setText(hFlagSet);
				}
			}
		});
		H.setText(hFlagSet);

		I = new JTextField(1);
		I.setEditable(false);
		JLabel Ilabel = new JLabel("I");
		ccrPanel.add(Ilabel);
		ccrPanel.add(I);

		iFlagSet = ccrFlags.getBound_I();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_I)) {
					iFlagSet = (pcEvt.getNewValue().toString());
					I.setText(iFlagSet);
				}
			}
		});
		I.setText(iFlagSet);

		N = new JTextField(1);
		JLabel Nlabel = new JLabel("N");
		N.setEditable(false);
		ccrPanel.add(Nlabel);
		ccrPanel.add(N);

		nFlagSet = ccrFlags.getBound_N();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_N)) {
					nFlagSet = (pcEvt.getNewValue().toString());
					N.setText(nFlagSet);
				}
			}
		});
		N.setText(nFlagSet);

		Z = new JTextField(1);
		JLabel Zlabel = new JLabel("Z");
		Z.setEditable(false);
		ccrPanel.add(Zlabel);
		ccrPanel.add(Z);

		zFlagSet = ccrFlags.getBound_Z();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_Z)) {
					zFlagSet = (pcEvt.getNewValue().toString());
					Z.setText(zFlagSet);
				}
			}
		});
		Z.setText(zFlagSet);

		V = new JTextField(1);
		JLabel Vlabel = new JLabel("V");
		V.setEditable(false);
		ccrPanel.add(Vlabel);
		ccrPanel.add(V);

		vFlagSet = ccrFlags.getBound_V();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_V)) {
					vFlagSet = (pcEvt.getNewValue().toString());
					V.setText(vFlagSet);
				}
			}
		});
		V.setText(vFlagSet);

		C = new JTextField(1);
		JLabel Clabel = new JLabel("C");
		C.setEditable(false);
		ccrPanel.add(Clabel);
		ccrPanel.add(C);
		ccrPanel.setBorder(javax.swing.BorderFactory
				.createTitledBorder("CCR flags"));

		cFlagSet = ccrFlags.getBound_C();

		// adds property change support for updating display
		ccrFlags.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent pcEvt) {
				if (pcEvt.getPropertyName().equals(CCR.BOUND_C)) {
					cFlagSet = (pcEvt.getNewValue().toString());
					C.setText(cFlagSet);
				}
			}
		});
		C.setText(cFlagSet);

		/**
		 * JPanel for Program Counter display
		 */
		JPanel PCPanel = new JPanel();
		PCPanel.setBackground(new java.awt.Color(0, 204, 255));
		PCPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PC"));

		// define groups and add components
		layout.setVerticalGroup(layout
				.createSequentialGroup()
				.addComponent(statusLabel)
				.addComponent(ccrPanel)
				.addGroup(
						layout.createSequentialGroup().addComponent(pcLabel)
								.addComponent(pcField).addComponent(xLabel)
								.addComponent(xField).addComponent(yLabel)
								.addComponent(yField).addComponent(aLabel)
								.addComponent(aField).addComponent(bLabel)
								.addComponent(bField).addComponent(dLabel)
								.addComponent(dField).addComponent(hsLabel)
								.addComponent(hsField).addComponent(usLabel)
								.addComponent(usField).addComponent(dpLabel)
								.addComponent(dpField)));

		layout.setHorizontalGroup(layout
				.createParallelGroup(Alignment.LEADING, false)
				.addComponent(statusLabel).addComponent(ccrPanel)
				.addComponent(pcLabel).addComponent(pcField)
				.addComponent(xLabel).addComponent(xField).addComponent(yLabel)
				.addComponent(yField).addComponent(aLabel).addComponent(aField)
				.addComponent(bLabel).addComponent(bField).addComponent(dLabel)
				.addComponent(dField).addComponent(hsLabel)
				.addComponent(hsField).addComponent(usLabel)
				.addComponent(usField).addComponent(dpLabel)
				.addComponent(dpField));
	}

	/**
	 * adds bottom elements to bottom of GUI
	 */
	public void layoutBottom() {
		JPanel bottom = new JPanel();
		bottom.setBackground(Color.LIGHT_GRAY);
		clearButton = new JButton("Clear Code Area");
		clearButton.addActionListener(this);
		// loadButton = new JButton("Load");
		// loadButton.addActionListener(this);
		bottom.add(clearButton);
		// bottom.add(loadButton);
		runButton = new JButton("Run");
		runButton.setEnabled(false);
		runButton.addActionListener(this);
		bottom.add(runButton);
		interruptButton = new JButton("Interrupt");
		interruptButton.setEnabled(false);
		interruptButton.addActionListener(this);
		bottom.add(interruptButton);
		saveButton = new JButton("Save");
		saveButton.addActionListener(this);
		bottom.add(saveButton);
		add(bottom, BorderLayout.SOUTH);
		modifyMemoryButton = new JButton("Load into Memory");
		modifyMemoryButton.addActionListener(this);
		stepButton = new JButton("Step");
		stepButton.setEnabled(false);
		bottom.add(modifyMemoryButton);
		bottom.add(stepButton);

		// Combo box to select type of break
		JComboBox breakSelector = new JComboBox();
		breakSelector.addItem("Break Disabled");
		breakSelector.addItem("Program Line No.");
		breakSelector.setEnabled(false);
		bottom.add(breakSelector);

		// Spinner to set break line number
		JSpinner breakAt = new JSpinner();
		JLabel lineNo = new JLabel("Break at:");
		breakAt.setEnabled(false);
		bottom.add(lineNo);
		bottom.add(breakAt);
	}

	/**
	 * Process button clicks.
	 * 
	 * @param ae - the ActionEvent
	 */
	public void actionPerformed(ActionEvent ae) {

		// action if load into memory clicked
		if (ae.getSource() == modifyMemoryButton) {

			// first check if any code entered
			if (codeIn.getText().trim().length() != 0) {

				// call modifyMemory() method
				modifyArray();

			} else
				JOptionPane.showMessageDialog(null,
						"Please enter some machine code first.");
		}
		if (ae.getSource() == openMenuItem) {
			// call processInputFile method
			processInputFile();
		}
		if (ae.getSource() == openUserGuide) {
			// call display guide method
			displayGuide();
		}
		if (ae.getSource() == runButton) {
			// call method to process running of code
			processRun();
		}
		if (ae.getSource() == clearButton) {
			// clear code entry area
			codeIn.setText("");
		}
		if (ae.getSource() == interruptButton) {
			// call method to process interrupt
			processInterrupt();
		}
		if (ae.getSource() == saveButton) {
			// first check if any code entered
			if (codeIn.getText().trim().length() != 0) {

				// call method to process save
				try {
					processSave();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else
				JOptionPane.showMessageDialog(null,
						"Please enter some machine code first.");
		}
	}

	/**
	 * method to process save
	 * @throws IOException 
	 */
	private void processSave() throws IOException {

		// show dialog to retrieve entered address
		addressToModify = (String) JOptionPane
				.showInputDialog("At which memory address? \n "
						+ "(if hexadecimal please signify\n with a '$' symbol, e.g. $1000)");

		// confirm if a string was entered
		if ((addressToModify != null) && (addressToModify.length() > 0)) {

			// check if entered address is hexadecimal
			if (addressToModify.charAt(0) == '$') {

				// create substring to remove $ symbol
				addressToModify = addressToModify.substring(1);

				// convert to integer if hexadecimal address entered
				memAddress = Integer.parseInt(addressToModify, 16);

				if (memAddress > 65525) {
					JOptionPane
							.showMessageDialog(null,
									"Sorry, memory locations between $FFF6 & $FFFF reserved by Motorola.");
				} else {
					// create S1 record
					StringBuilder saveRecord = new StringBuilder("S1");
					String codeString = codeIn.getText().trim();
					// codeString = codeString.replaceAll("\\s+", "");
					int codePairs = codeString.length() / 2;
					String hexPairs = String.format("%02x", codePairs);
					String checkSum = "FC";
					saveRecord.append(hexPairs);
					String hexAddress = String.format("%04x", memAddress);
					saveRecord.append(hexAddress);
					saveRecord.append(codeString);
					saveRecord.append(checkSum);

					JOptionPane.showMessageDialog(null,
							"Code will be saved in S-Record format as a single S1 record: \n"
									+ saveRecord);

					sRec = new S_Record(arrayForUpdate);
					sRec.writeRecord(saveRecord.toString());
				}
			}

			else {
				// convert to integer if decimal address entered
				memAddress = Integer.parseInt(addressToModify);

				if (memAddress > 65525) {
					JOptionPane
							.showMessageDialog(null,
									"Sorry, memory locations between $FFF6 & $FFFF reserved by Motorola.");
				} else {
					// create S1 record
					StringBuilder saveRecord = new StringBuilder("S1");
					String codeString = codeIn.getText().trim();
					int codePairs = codeString.length() / 2;
					String hexPairs = String.format("%02x", codePairs);
					String checkSum = "FC";
					saveRecord.append(hexPairs);
					String hexAddress = String.format("%04x", memAddress);
					saveRecord.append(hexAddress);
					saveRecord.append(codeString);
					saveRecord.append(checkSum);

					JOptionPane.showMessageDialog(null,
							"Code will be saved in S-Record format as a single S1 record: \n"
									+ saveRecord);

					sRec = new S_Record(arrayForUpdate);
					sRec.writeRecord(saveRecord.toString());
				}
			}
			System.out.println(" address as hex String = " + addressToModify
					+ "\n address as decimal int = " + memAddress);

		} else
			JOptionPane.showMessageDialog(clearButton,
					"Sorry, not a valid address.");

	}

	/**
	 *  method to process interrupt
	 */
	private void processInterrupt() {
		// TODO
	}

	/**
	 * method to process modify array
	 */
	public void modifyArray() {

		// show dialog to retrieve entered address
		addressToModify = (String) JOptionPane
				.showInputDialog("At which memory address? \n "
						+ "(if hexadecimal please signify\n with a '$' symbol, e.g. $1000)");

		@SuppressWarnings("unused")
		TextAreaOutputStream conOut = new TextAreaOutputStream();
		updateProgCounter(addressToModify);

		// confirm if a string was entered
		if ((addressToModify != null) && (addressToModify.length() > 0)) {

			// check if entered address is hexadecimal
			if (addressToModify.charAt(0) == '$') {

				// create substring to remove $ symbol
				addressToModify = addressToModify.substring(1);

				// convert to integer if hexadecimal address entered
				memAddress = Integer.parseInt(addressToModify, 16);
			}

			else {
				// convert to integer if decimal address entered
				memAddress = Integer.parseInt(addressToModify);
			}
			System.out.println(" Address to modify as hex String = "
					+ addressToModify
					+ "\n Address to modify as decimal int = " + memAddress);

			// pass as integer
			processInput(memAddress);

			// enable run and save buttons
			runButton.setEnabled(true);
			saveButton.setEnabled(true);

		} else
			JOptionPane.showMessageDialog(clearButton,
					"Sorry, not a valid address.");
	}

	/** 
	 * This method refreshes the PC element of the GUI
	 * @param pc - String representing the program counter
	 */
	public void updateProgCounter(String pc) {
		progCount.setBoundPC(pc);
	}

	public void processInput(int a) {

		String newValue = codeIn.getText();
		arrayForUpdate.instructionsIn(newValue, a);
	}

	/**
	 * Method to process input of S-record file
	 */
	public void processInputFile() {

		// create S_Record object
		sRec = new S_Record(arrayForUpdate);

		// add file chooser to "open" menu
		JFileChooser chooser = new JFileChooser();
		JLabel resultLabel = new JLabel("");
		int returnVal = chooser.showOpenDialog(getParent());

		// if file chosen then confirm details
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			// create the file
			File file = chooser.getSelectedFile();

			// open console output window
			@SuppressWarnings("unused")
			TextAreaOutputStream conOut = new TextAreaOutputStream();

			System.out.println(" You chose to open the following file: \n "
					+ file);

			// pass to readRecord method in S_Record class
			sRec.readRecord(file);

			resultLabel.setText("You chose to open this file: "
					+ file.getName());

			// enable run and save buttons
			runButton.setEnabled(true);
			saveButton.setEnabled(true);

		} else
			resultLabel.setText("You did not choose a file to open");
	}

	/**
	 * method to process user guide menu operation
	 */
	public void displayGuide() {
		// create new guide window object
		UserGuide guideWindow = new UserGuide();
		// display the guide window
		guideWindow.setVisible(true);
	}

	/**
	 * method to process run button operation
	 */
	public void processRun() {

		// show dialog to retrieve entered address
		addressToModify = (String) JOptionPane
				.showInputDialog("From which starting address? \n "
						+ "(if hexadecimal please signify\n with a '$' symbol, e.g. $1000)");

		updateProgCounter(addressToModify);

		// confirm if a string was entered
		if ((addressToModify != null) && (addressToModify.length() > 0)) {

			// check if entered address is hexadecimal
			if (addressToModify.charAt(0) == '$') {

				// create substring to remove $ symbol
				addressToModify = addressToModify.substring(1);

				// convert to integer if hexadecimal address entered
				memAddress = Integer.parseInt(addressToModify, 16);
			}

			else {
				// convert to integer if decimal address entered
				memAddress = Integer.parseInt(addressToModify);
			}
			System.out.println(" Start address as hex String = "
					+ addressToModify + "\n Start address as decimal int = "
					+ memAddress);
			arrayForUpdate.runLoop(memAddress);
		}
	}
}
