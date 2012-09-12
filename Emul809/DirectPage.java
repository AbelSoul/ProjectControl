import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

/**
 * This class represents the Direct Page register
 * 
 * @author Robert Wilson
 * 
 */
public class DirectPage {

	public static final String BOUND_DP = "bound DP";
	private String boundDP = "-D-P-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private int dpInt;

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

	public String getBoundDP() {
		return boundDP;
	}

	/**
	 * Method to set direct page register
	 * @param a - integer representing the dp data
	 */
	public void setDP(int a) {

		dpInt = a;

		// convert to String and pass to method for display
		String accBin = Integer.toBinaryString(dpInt);
		String formattedAccA = String.format(
				"hex: %02x      dec: %03d      bin: %08d", dpInt, dpInt,
				Integer.parseInt(accBin));
		setBoundDP(formattedAccA);
	}

	public int getDP() {
		return dpInt;

	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundDP - the String representing the DP
	 */
	public void setBoundDP(String boundDP) {
		String oldValue = this.boundDP;
		String newValue = boundDP;
		this.boundDP = newValue;
		spcSupport.firePropertyChange(BOUND_DP, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
