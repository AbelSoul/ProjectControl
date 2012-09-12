/**
 * This is the main class of an emulator for the Motorola MC6809 microprocessor</br>
 * This class contains the main method which launches the graphical user
 * interface, giving user access to the program functions
 * 
 * @author Robert Wilson
 * 
 */
public class Main {
	/**
	 * 
	 * The main method sets the GUI class to visible
	 * 
	 * @param arg
	 *            the default parameter
	 */
	public static void main(String[] arg) {
		GraphicalUserInterface display = new GraphicalUserInterface();
		display.setVisible(true);
	}
}
