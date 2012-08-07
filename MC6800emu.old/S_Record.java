import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * This class handles loading and saving of Motorola S-Record files which take
 * the following format
 * 
 * +-------------------//------------------//-----------------------+
 * | Type | Count | Address  |            Data           | Checksum |
 * +-------------------//------------------//-----------------------+
 *
 *  The Type field contains two characters which describe the type of record, i.e. S0-S9
 * 
 *  The Count field contains two hexadecimal characters representing the count of
 *  remaining character pairs in the record
 *  
 *  The Address field contains 4, 6, or 8 hexadecimal characters representing the
 *  address at which the data field is to be loaded into memory 
 *  The length of the field depends on the number of bytes necessary to hold the address 
 *  A 2-byte address uses 4 characters, a 3-byte address uses 6 characters, and a 
 *  4-byte address uses 8 characters 
 *  
 *  The Data field contains between 0 and 64 hexadecimal characters representing the 
 *  memory loadable data or descriptive information 
 *  
 *  The Checksum field contains two hexadecimal characters representing the checksum, 
 *  which provides a means of detecting data which has been corrupted during transmission
 *  
 *  Each S-record is terminated with a line feed and there are nine possible types, S0-S9
 *  
 * @author Robert Wilson
 *
 */
public class S_Record {

	/** Strings representing input S-Record file names */
	private String sIn, sOut;

	/** Instance of the GUI class */
	private EmuGUI gui;

	/** This method takes an input S-Record file and separates into lines
	 * 
	 * @param fileName - the name of the file
	 */
	public void readRecord(File fileName) {

		// create file reader
		try {
			FileReader reader = null;
			try {
				// open input file
				reader = new FileReader(fileName);

				// create scanner to read from file reader
				Scanner in = new Scanner(reader);

				// read each line
				while (in.hasNextLine()) {
					String line = in.nextLine();
					System.out.println(line);

					// pass to parseRecord method
					parseRecord(line);
				}

			} finally {
				// close reader assuming it was successfully opened
				if (reader != null)
					reader.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** 
	 * method to create S-Record files
	 */
	public void writeRecord() {

		// create file writer
		PrintWriter writer = null;

		// establish number of records / headers / etc ? TODO

		// display S-Record file in gui
		gui = new EmuGUI();

		// gui.layoutCenter()
	}

	public void parseRecord(String record) {

		// integers representing index characters
		int type = 1;
		int addr = 4;
		int dat = 6; // this can vary to according to size of address

		// Check if S0 record (block header)
		if (record.charAt(type) == '0') {
			System.out.println("SO - block header, address field not used");

			// convert module name to ASCII TODO

			// convert version and revision numbers to integers TODO

			// handle description field TODO

			// handle checksum TODO

		}

		// Check if S1 record (memory loadable data)
		if (record.charAt(type) == '1') {
			System.out.println("S" + record.charAt(type)
					+ " - data sequences, 2 byte address");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S2 record (memory loadable data)
		if (record.charAt(type) == '2') {
			System.out.println("S" + record.charAt(type)
					+ " - data sequences, 3 byte address");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S3 record (memory loadable data)
		if (record.charAt(type) == '3') {
			System.out.println("S" + record.charAt(type)
					+ " - data sequences, 4 byte address");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S5 record (record count)
		if (record.charAt(type) == '5') {
			System.out.println("S5 - record count 0f S1-3, 2 byte address");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S7 record (end of block)
		if (record.charAt(type) == '7') {
			System.out.println("S" + record.charAt(type)
					+ " - 4 byte execution address, end of block");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S8 record (end of block)
		if (record.charAt(type) == '8') {
			System.out.println("S" + record.charAt(type)
					+ " - 3 byte execution address, end of block");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S9 record (end of block)
		if (record.charAt(type) == '9') {
			System.out.println("S" + record.charAt(type)
					+ " - 2 byte execution address, end of block");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}
	}
}
