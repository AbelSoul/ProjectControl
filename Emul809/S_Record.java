import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * This class handles loading and saving of Motorola S-Record files which take
 * the following format</br></br>
 * 
 * +-------------------//------------------//-----------------------+</br>
 * | Type | Count | Address  |  ----- Data ----          | Checksum |</br>
 * +-------------------//------------------//-----------------------+</br></br>
 *
 *  The Type field contains two characters which describe the type of record, i.e. S0-S9,</br>
 * </br>
 *  The Count field contains two hexadecimal characters representing the count of</br>
 *  remaining character pairs in the record</br>
 * </br> 
 *  The Address field contains 4, 6, or 8 hexadecimal characters representing the</br>
 *  address at which the data field is to be loaded into memory </br>
 *  The length of the field depends on the number of bytes necessary to hold the address</br> 
 *  A 2-byte address uses 4 characters, a 3-byte address uses 6 characters, and a </br>
 *  4-byte address uses 8 characters </br>
 *  </br>
 *  The Data field contains between 0 and 64 hexadecimal characters representing the</br> 
 *  data to be loaded into memory or descriptive information </br>
 *  </br>
 *  The Checksum field contains two hexadecimal characters representing the checksum,</br> 
 *  which provides a means of detecting data which has been corrupted during transmission</br>
 *  </br>
 *  Each S-record is terminated with a line feed and there are nine possible types, S0-S9</br>
 *  </br>
 * @author Robert Wilson
 *
 */
public class S_Record {

	/** Strings representing input S-Record file names */
	// private String sIn, sOut;

	/** Instance of MemoryMap class */
	private MemoryHandling memMap;

	/** instance variables for index integers for parsing records */
	private final int type = 1; // the type of record
	private final int addr = 4; // starting position of address
	private final int mN = 8; // starting position for module name and binary
								// data
	private final int chunkLength = 2; // the length of each chunk of code

	/**
	 * Default constructor takes the memory array as a parameter
	 * 
	 * @param m - the memory array
	 */
	public S_Record(MemoryHandling m) {
		memMap = m;
	}

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

				// read each line and remove whitespace
				while (in.hasNextLine()) {
					String line = in.nextLine().trim();

					// check if S-record
					if ((line.length() > 1) && (line.charAt(0) == 'S')) {

						// pass to parseRecord method
						parseRecord(line);
					} else {
						JOptionPane.showMessageDialog(null,
								"Not an S-Record file. Operation aborted.");
						break;
					}
				}

			} finally {
				// close reader assuming it was successfully opened
				if (reader != null)
					reader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 
	 * method to create S-Record files
	 */
	public void writeRecord(String sR) throws IOException {

		String newSRecord = sR;
		String filename = File.separator + "txt";

		// create file chooser
		JFileChooser fc = new JFileChooser(new File(filename));

		// show save dialog
		fc.showSaveDialog(fc);
		File selFile = fc.getSelectedFile();

		// create file writer
		PrintWriter writer = null;

		try {
			try {
				// open output file and write to it
				writer = new PrintWriter(selFile);
				writer.write(newSRecord);
			} finally {
				// close writer assuming it was successfully opened
				if (writer != null)
					writer.close();
			}
		} catch (IOException e) {
			System.out.println("File error");
		}
	}

	/**
	 * Method to parse S-Record
	 * 
	 * @param record
	 */
	public void parseRecord(String record) {

		// String builder for S0 output string
		StringBuilder sb = new StringBuilder();

		// Check if S0 record (block header)
		if (record.charAt(type) == '0') {
			System.out
					.println(" SO - header, 4-digit address field not used\n");

			// create substring (8 from start, 2 from end) and convert to ASCII
			String modNameHex = record.substring(mN, (record.length() - 2));

			// traverse header string and split into 2 digit chunks
			for (int i = 0; i < modNameHex.length(); i += chunkLength) {
				String chunk = modNameHex.substring(i,
						Math.min(modNameHex.length(), i + chunkLength));

				// first convert to decimal
				int decimal = Integer.parseInt(chunk, 16);

				// then convert to ASCII and append to string builder
				sb.append((char) decimal);
			}
			System.out.println("\n S0 info in ASCII = " + sb.toString() + "\n");
			// String s = sb.toString();

			// pass to gui for display

			// handle checksum TODO
		}

		// Check if S1 record (data to load into memory)
		if (record.charAt(type) == '1') {
			System.out.println(" S" + record.charAt(type)
					+ " - data sequences, 2 byte address");

			// create address substring 4 from start , 4 long
			String addrString = record.substring(addr, (addr + 4));
			System.out.println(" S1 address as hex string: " + addrString);

			// convert from hex to decimal
			int s1Address = Integer.parseInt(addrString, 16);
			System.out.println(" S1 address as decimal: " + s1Address);

			// create binary data substring (8 from start, 2 from end)
			String dataString = record.substring(mN, (record.length() - 2));

			System.out.println(" S1 data string = " + dataString);

			// pass data string as String parameter to InstructionsIn
			// method in MemoryMap class
			memMap.instructionsIn(dataString, s1Address);
		}

		// Check if S2 record (data to load into memory) - not used in MC6809 as
		// 3
		// byte address?
		if (record.charAt(type) == '2') {
			System.out.println(" S" + record.charAt(type)
					+ " - data sequences, 3 byte address");

			// retrieve address

			// (maybe) convert from hex to decimal
		}

		// Check if S3 record (data to load into memory) not used in MC6809 as 4
		// byte address?
		if (record.charAt(type) == '3') {
			System.out.println(" S" + record.charAt(type)
					+ " - data sequences, 4 byte address");

			// retrieve address

			// (maybe) convert from hex to decimal
		}

		// Check if S5 record (record count)
		if (record.charAt(type) == '5') {
			System.out.println(" S5 - record count 0f S1-3, 2 byte address");

			// create address substring 4 from start , 4 long
			String addrString = record.substring(addr, (addr + 4));
			System.out.println(" S5 address as hex string: " + addrString);

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S7 record (end of block)
		if (record.charAt(type) == '7') {
			System.out.println(" S" + record.charAt(type)
					+ " - 4 byte execution address, end of block");

			// TODO retrieve address

			// TODO (maybe) convert from hex to decimal
		}

		// Check if S8 record (end of block) not used in MC6809 as 3 byte
		// address?
		if (record.charAt(type) == '8') {
			System.out.println(" S" + record.charAt(type)
					+ " - 3 byte execution address, end of block");

			// retrieve address

			// (maybe) convert from hex to decimal
		}

		// Check if S9 record (end of block)
		if (record.charAt(type) == '9') {
			System.out.println(" S" + record.charAt(type)
					+ " - 2 byte execution address, end of block");

			// TODO deal with empty record
			if (record.length() > addr) {
				// create address substring 4 from start , 4 long
				String addrString = record.substring(addr, (addr + 4));
				System.out.println(" S9 address as hex string: " + addrString);

			} else
				System.out.println(" no address as no more data");
		}
	}
}
