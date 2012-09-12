import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

/** 
 * This class represents the program counter
 * @author Robert Wilson
 *
 */
public class ProgramCounter {

	public static final String BOUND_PC = "bound PC";
	private String boundPC = "-P-C-0-0-0-0-0-0-0-0-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private int pC;

	/**
	 * The following methods allow the display to be updated
	 * 
	 * @return spcSupport - the SwingPropertyChangeSupport object
	 */
	public SwingPropertyChangeSupport getSpcSupport() {
		return spcSupport;
	}

	public void setSpcSupport(SwingPropertyChangeSupport spcSupport) {
		this.spcSupport = spcSupport;
	}

	/**
	 * Method to set value of accumulator B
	 * 
	 * @param a - integer representing value of accumulator B
	 */
	public void setPC(int a) {
		pC = a;

		// convert to String and pass to method for display
		String PCBin = Integer.toBinaryString(pC);
		String formattedPC = String.format(
				"hex: %04x      dec: %05d      bin: %016d", pC, pC,
				Long.parseLong(PCBin));
		setBoundPC(formattedPC);

	}

	public int getPC() {
		return pC;
	}

	public String getBoundPC() {
		return boundPC;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundPC - the String representing the memory array
	 */
	public void setBoundPC(String boundPC) {
		String oldValue = this.boundPC;
		String newValue = boundPC;
		this.boundPC = newValue;
		spcSupport.firePropertyChange(BOUND_PC, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
