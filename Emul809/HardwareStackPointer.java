import java.beans.PropertyChangeListener;
import java.util.Stack;

import javax.swing.event.SwingPropertyChangeSupport;

/**
 * This class defines the Hardware Stack Pointer
 * 
 * @author Robert Wilson
 * 
 */
public class HardwareStackPointer {

	public static final String BOUND_HS = "bound HS";
	private String boundHS = "-H-S-0-0-0-0-0-0-0-0-0-0-0-0-0-0-";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private int hardSP;
	private Stack<Integer> hardWare = new Stack<Integer>();

	/**
	 * Default constructor
	 */
	public HardwareStackPointer() {

		// create empty stack

	}

	/**
	 * method to push items onto stack
	 * @param r - the data to be pushed
	 */
	public void hardPush(int r) {

		hardWare.push(r);
		System.out.println(r + " pushed onto the hw stack");
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
	 * Method to set value of accumulator D
	 * 
	 * @param a - integer representing value of accumulator D
	 */
	public void setHSP(int a) {
		hardSP = a;

		// convert to String and pass to method for display
		String accBin = Integer.toBinaryString(hardSP);
		String formattedAccA = String.format(
				"hex: %04x      dec: %05d      bin: %016d", hardSP, hardSP,
				Long.parseLong(accBin));
		setBoundHS(formattedAccA);
	}

	public int getHSP() {
		return hardSP;
	}

	public String getBoundHS() {
		return boundHS;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundHS - the String representing the memory array
	 */
	public void setBoundHS(String boundHS) {
		String oldValue = this.boundHS;
		String newValue = boundHS;
		this.boundHS = newValue;
		spcSupport.firePropertyChange(BOUND_HS, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}
}
