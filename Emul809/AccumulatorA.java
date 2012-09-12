import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

/** 
 * This class represents Accumulator A register
 * 
 * @author Robert Wilson
 */
public class AccumulatorA {

	public static final String BOUND_ACCA = "bound accA";
	private String boundAccA = "-A-0-0-0-0-0-0-0-";
	private int accAInt;
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);

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
	 * Method to set value of accumulator A
	 * 
	 * @param a - integer representing value of accumulator A
	 */
	public void setAccA(int a) {
		accAInt = a;

		// convert to String and pass to method for display
		String accBin = Integer.toBinaryString(accAInt);
		String formattedAccA = String.format(
				"hex: %02x      dec: %03d      bin: %08d", accAInt, accAInt,
				Integer.parseInt(accBin));
		setBoundAccA(formattedAccA);
	}

	public int getAccA() {
		return accAInt;
	}

	public String getBoundAccA() {
		return boundAccA;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundAccA - the String representing the memory array
	 */
	public void setBoundAccA(String boundAccA) {
		String oldValue = this.boundAccA;
		String newValue = boundAccA;
		this.boundAccA = newValue;
		spcSupport.firePropertyChange(BOUND_ACCA, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
