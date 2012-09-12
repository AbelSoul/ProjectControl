import java.beans.PropertyChangeListener;
import java.util.Stack;

import javax.swing.event.SwingPropertyChangeSupport;

/**
 * This class defines the User Stack Pointer register which contains the address
 * that points to the top of a push-down, pop-up stack
 * 
 * @author Robert Wilson
 * 
 */
public class UserStackPointer {

	public static final String BOUND_US = "bound US";
	private String boundUS = "-U-S-0-0-0-0-0-0-0-0-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private int userSP;
	private Stack<Integer> userStack = new Stack<Integer>();

	/**
	 * Default constructor
	 */
	public UserStackPointer() {

	}

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
	 * Method to set value of user stack pointer
	 * 
	 * @param a - integer representing value of user stack pointer
	 */
	public void setUSP(int a) {
		userSP = a;

		// convert to String and pass to method for display
		String accBin = Integer.toBinaryString(userSP);
		String formattedAccA = String.format(
				"hex: %04x      dec: %05d      bin: %016d", userSP, userSP,
				Long.parseLong(accBin));
		setBoundUS(formattedAccA);
		System.out.println("us set!");
	}

	public int getUSP() {
		return userSP;
	}

	public String getBoundUS() {
		return boundUS;
	}

	/**
	 * method to push items onto stack
	 * @param r - the data to be pushed
	 */
	public void hardPush(int r) {

		userStack.push(r);
		System.out.println(r + " pushed onto the us stack");
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundUS - the String representing the memory array
	 */
	public void setBoundUS(String boundUS) {
		String oldValue = this.boundUS;
		String newValue = boundUS;
		this.boundUS = newValue;
		spcSupport.firePropertyChange(BOUND_US, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
