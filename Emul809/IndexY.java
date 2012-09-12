import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

/** 
 * This class represents the Y index register
 * 
 * @author Robert Wilson
 *
 */
public class IndexY {

	public static final String BOUND_Y = "bound Y";
	private String boundY = "-Y-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private long yInt;

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
	 * Method to set value of X index
	 * 
	 * @param a - integer representing value of X index
	 */
	public void setY(long a) {
		yInt = a;

		// convert to String and pass to method for display
		String accBin = Long.toBinaryString(yInt);
		String formattedY = String.format(
				"hex: %04x      dec: %05d      bin: %016d", yInt, yInt,
				Long.parseLong(accBin));
		setBoundY(formattedY);
	}

	public long getY() {
		return yInt;
	}

	public String getBoundY() {
		return boundY;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundY - the String representing the memory array
	 */
	public void setBoundY(String boundY) {
		String oldValue = this.boundY;
		String newValue = boundY;
		this.boundY = newValue;
		spcSupport.firePropertyChange(BOUND_Y, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
