import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

/** 
 * This class represents Accumulator B register
 * 
 * @author Robert Wilson
 */
public class AccumulatorB {

	public static final String BOUND_ACCB = "bound accB";
	private String boundAccB = "-B-0-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private int accBInt;

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
	public void setAccB(int a) {
		accBInt = a;

		// convert to String and pass to method for display
		String accBin = Integer.toBinaryString(accBInt);
		String formattedAccA = String.format(
				"hex: %02x      dec: %03d      bin: %08d", accBInt, accBInt,
				Integer.parseInt(accBin));
		setBoundAccB(formattedAccA);
	}

	public int getAccB() {
		return accBInt;
	}

	public String getBoundAccB() {
		return boundAccB;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundAccB - the String representing the memory array
	 */
	public void setBoundAccB(String boundAccB) {
		String oldValue = this.boundAccB;
		String newValue = boundAccB;
		this.boundAccB = newValue;
		spcSupport.firePropertyChange(BOUND_ACCB, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
