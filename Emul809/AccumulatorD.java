import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

/** 
 * This class represents Accumulator D register which comprises the sum
 * of accumulators A and B
 * 
 * @author Robert Wilson
 */
public class AccumulatorD {

	public static final String BOUND_ACCD = "bound accD";
	private String boundAccD = "-D-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private long accDInt;

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
	 * Method to set value of accumulator D
	 * 
	 * @param a - integer representing value of accumulator D
	 */
	public void setAccD(long a) {
		accDInt = a;

		// convert to String and pass to method for display
		String accBin = Long.toBinaryString(accDInt);
		String formattedAccA = String.format(
				"hex: %04x      dec: %05d      bin: %016d", accDInt, accDInt,
				Long.parseLong(accBin));
		setBoundAccD(formattedAccA);
	}

	public long getAccD() {
		return accDInt;
	}

	public String getBoundAccD() {
		return boundAccD;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundAccD - the String representing the memory array
	 */
	public void setBoundAccD(String boundAccD) {
		String oldValue = this.boundAccD;
		String newValue = boundAccD;
		this.boundAccD = newValue;
		spcSupport.firePropertyChange(BOUND_ACCD, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
