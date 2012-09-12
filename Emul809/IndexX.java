import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

/**
 * This class defines the Index X register, used in indexed mode addressing
 * 
 * @author Robert Wilson
 * 
 */
public class IndexX {

	public static final String BOUND_X = "bound X";
	private String boundX = "-X-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private long xInt;

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
	public void setX(int a) {
		xInt = a;

		// convert to String and pass to method for display
		String accBin = Long.toBinaryString(xInt);
		String formattedX = String.format(
				"hex: %04x      dec: %05d      bin: %016d", xInt, xInt,
				Long.parseLong(accBin));
		setBoundX(formattedX);
	}

	public long getX() {
		return xInt;
	}

	public String getBoundX() {
		return boundX;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundX - the String representing the memory array
	 */
	public void setBoundX(String boundX) {
		String oldValue = this.boundX;
		String newValue = boundX;
		this.boundX = newValue;
		spcSupport.firePropertyChange(BOUND_X, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
