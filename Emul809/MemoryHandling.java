import java.beans.PropertyChangeListener;

import javax.swing.JOptionPane;
import javax.swing.event.SwingPropertyChangeSupport;

/**
 * This class maintains a list representing the 65536 memory locations
 * and provides methods to access and manipulate them along with a routine
 * for handling each operation code
 * 
 * @author Robert Wilson
 * 
 */

public class MemoryHandling {

	public static final String BOUND_PROPERTY = "bound property";
	private String boundProperty = "";
	private SwingPropertyChangeSupport spcSupport = new SwingPropertyChangeSupport(
			this);
	private int[] memoryArray;
	/** class constant representing the total number of memory locations */
	private final int MEM_LOCATIONS = 65536;
	/** StringBuilder object for displaying memory */
	private StringBuilder mList;
	private int opCode, opCode2, dataByte, dataByte2, result, accByte,
			progCounter;
	private String firstByte, clear = "0", set = "1";
	private AccumulatorA accA;
	private AccumulatorB accB;
	private AccumulatorD accD;
	private ProgramCounter progCountClass;
	private CCR ccrFlagState;
	private IndexX xIndex;
	private IndexY yIndex;
	private HardwareStackPointer hardStack;
	private UserStackPointer userStack;
	private DirectPage dirPage;
	private boolean emuRunning = false, compare = false;
	private long effectiveAddress, index16bit;

	/**
	 * default constructor to instantiate array of empty 
	 * memory objects and set reserved memory locations
	 */
	public MemoryHandling(AccumulatorA a, AccumulatorB b, AccumulatorD d,
			ProgramCounter pc, CCR c, IndexX x, IndexY y,
			HardwareStackPointer hs, UserStackPointer us, DirectPage dp) {

		accA = a;
		accB = b;
		accD = d;
		progCountClass = pc;
		ccrFlagState = c;
		xIndex = x;
		yIndex = y;
		hardStack = hs;
		userStack = us;
		dirPage = dp;

		memoryArray = new int[MEM_LOCATIONS];
		for (int i = 0; i < memoryArray.length; i++) {
			memoryArray[i] = 0;
		}

		// FF00 to FF03 & FF20 to FF23 reserved for PIAs
		memoryArray[65280] = 255;
		memoryArray[65281] = 255;
		memoryArray[65282] = 255;
		memoryArray[65283] = 255;
		memoryArray[65312] = 255;
		memoryArray[65313] = 255;
		memoryArray[65314] = 255;
		memoryArray[65315] = 255;

		// FFF0 & FFF1 reserved by Motorola
		memoryArray[65520] = 255;
		memoryArray[65521] = 255;

		// FFF2 & FFF3 reserved for SWI3, 11-3F (17-63)
		memoryArray[65522] = 17;
		memoryArray[65523] = 63;

		// FFF4 & FFF5 reserved for SWI2, 10-3F (16-63)
		memoryArray[65524] = 16;
		memoryArray[65525] = 63;

		// FIRQ vector at addresses $FFF6 & $FFF7
		memoryArray[65526] = 255;
		memoryArray[65527] = 255;

		// IRQ vector at FFF8 & FFF9
		memoryArray[65528] = 255;
		memoryArray[65529] = 255;

		// FFFA & FFFB reserved for SWI, 3F (63)
		memoryArray[65530] = 63;
		memoryArray[65531] = 63;

		// FFFC & FFFD reserved for NMI
		memoryArray[65532] = 255;
		memoryArray[65533] = 255;

		// reset vector at addresses $FFFE (65534) & $FFFF (65535)
		memoryArray[65534] = 160; // 160 is the first reset opCode in decimal
		memoryArray[65535] = 39;

		// call method to create String for display
		setArrayyDisplayString();
	}

	/** 
	 * method to create formatted string of array
	 */
	public void setArrayyDisplayString() {

		// create StringBuilder for display in memory tab
		mList = new StringBuilder();
		for (int i = 0; i < memoryArray.length; i++) {

			mList.append(String.format("%10s %04x %10s %02x", "Address:   ", i,
					"Value:  ", memoryArray[i]));
			mList.append("\n");
		}
		setBoundProperty(mList.toString());
	}

	/**
	 * This method takes in a string representing input data passed
	 *  through from the GUI and a memory location to place that data
	 *  in the memory array
	 * 
	 * @param codeIn - String representing the input data
	 * @param loc - int representing the memory location
	 */
	public void instructionsIn(String codeIn, int loc) {

		// check address is less than $8000 (32768)
		if (loc < 32768) {
			progCountClass.setPC(loc);

			String code = codeIn.trim();
			code = code.replaceAll("\\s+", "");

			int len = code.length();
			int chunkLength = 2; // the length of each chunk of code
			int i = 0;

			// traverse entered code and split into 2 digit chunks
			for (i = 0; i < len; i += chunkLength) {

				String chunk = code
						.substring(i, Math.min(len, i + chunkLength));
				int oc = Integer.parseInt(chunk, 16);

				// add the data to the memory map array
				setArrayData(loc, oc);
				loc++;
			}
		} else {
			JOptionPane
					.showMessageDialog(
							null,
							"Error: All memory locations from $8000 to $FFFF are reserved.\n Operation aborted.");
		}
	}

	/**
	 * method to add data to the array
	 * 
	 * @param a - integer representing array index
	 * @param memData - integer representing the data
	 */
	public void setArrayData(int a, int memData) {

		memoryArray[a] = memData;
		setArrayyDisplayString();
	}

	/**
	 * method to set if compare A true
	 * 
	 * @param sca - boolean stating whether CMPA true or false
	 */
	public void setCompareA(boolean sca) {
		compare = sca;
		System.out.println(" CMPA = " + compare);
	}

	public boolean getCompareA() {
		return compare;
	}

	/**
	 * method to set if compare X true
	 * 
	 * @param scx - boolean stating whether CMPX true or false
	 */
	public void setCompareX(boolean scx) {
		compare = scx;
		System.out.println(" CMPX = " + compare);
	}

	public boolean getCompareX() {
		return compare;
	}

	public SwingPropertyChangeSupport getSpcSupport() {
		return spcSupport;
	}

	public void setSpcSupport(SwingPropertyChangeSupport spcSupport) {
		this.spcSupport = spcSupport;
	}

	public String getBoundProperty() {
		return boundProperty;
	}

	/**
	 * Method to implement changes to array for display
	 * 
	 * @param boundProperty - the String representing the memory array
	 */
	public void setBoundProperty(String boundProperty) {
		String oldValue = this.boundProperty;
		String newValue = boundProperty;
		this.boundProperty = newValue;
		spcSupport.firePropertyChange(BOUND_PROPERTY, oldValue, newValue);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		spcSupport.addPropertyChangeListener(listener);
	}

	/**
	 * This method implements the main loop routine to
	 * fetch, decode and interpret information from
	 * the memory array
	 * 
	 * @param startAddress - the value of the program counter
	 */
	public void runLoop(int startAddress) {

		progCounter = startAddress;

		// boolean representing emulator state
		emuRunning = true;

		// while loop is running
		while ((emuRunning) && (progCounter < MEM_LOCATIONS)) {

			// fetch first opCode (byte)
			opCode = memoryArray[progCounter];

			// convert to hex string for comparison
			firstByte = Integer.toHexString(opCode);

			// interpret each op code and operate accordingly
			if (firstByte.equals("0")) {

				System.out.println(" NEG, direct, 6 cycles, 2 Bytes, uaaaa");
				System.out
						.println(" Negate \n H undefined \n"
								+ " N Set if bit 7 of result is set \n"
								+ " Z Set if all bits of the result are clear\n"
								+ " V Set if original operand was 10000000\n"
								+ " C Set if the operation did not cause a carry from bit 7 in the ALU \n"
								+ " move PC on two\n");
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("3")) {

				System.out.println(" COM, direct, 6 cycles, 2 Bytes, -aa01");
				// Complement
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C set
				// ccrFlagState.setCBit(true);
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("4")) {
				// Logical Shift Right
				System.out.println(" LSR, direct, 6 cycles, 2 Bytes, -0a-s");
				// obtain operand
				dataByte = memoryArray[progCounter + 1];
				// convert to binary String
				firstByte = String.format("%8s",
						Integer.toBinaryString(dataByte)).replace(" ", "0");
				// create substring from first seven bits and prefix with 0
				// String shiftedString = "0" + firstByte.substring(0, 7);
				// C Loaded with bit 0 of the original operand
				// shift bit eight into carry bit
				ccrFlagState.setBound_C(firstByte.substring(7, 8));
				System.out.println(firstByte.substring(7, 8));
				// H not affected
				// N cleared
				ccrFlagState.getBound_N();
				// Z Set if all bits of the result are clear
				// V not affected
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("6")) {

				System.out.println(" ROR, direct, 6 cycles, 2 Bytes, -aa-s");
				// Rotate Right
				// obtain operand
				dataByte = memoryArray[progCounter + 1];
				// convert to binary String
				firstByte = String.format("%8s",
						Integer.toBinaryString(dataByte)).replace(" ", "0");
				// create substring from first seven bits and prefix with C
				// shift bit eight into carry bit
				ccrFlagState.setBound_C(firstByte.substring(7, 8));
				System.out.println(firstByte.substring(7, 8));
				// H not affected
				// N cleared
				ccrFlagState.getBound_N();
				// Z Set if all bits of the result are clear
				// V not affected
				// C Loaded with bit 0 of the previous operand
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("7")) {

				System.out.println(" ASR, direct, 6 cycles, 2 Bytes, uaa-s");
				// Arithmetic shift right
				// obtain operand
				dataByte = memoryArray[progCounter + 1];
				// convert to binary String
				firstByte = String.format("%8s",
						Integer.toBinaryString(dataByte)).replace(" ", "0");
				// create substring from first seven bits and prefix with first
				// bit
				String shiftedString = firstByte.substring(0, 1)
						+ firstByte.substring(0, 7);
				System.out.println(shiftedString);
				// shift bit eight into carry bit
				ccrFlagState.setBound_C(firstByte.substring(7, 8));
				System.out.println(firstByte.substring(7, 8));
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V unaffected
				// C loaded with bit 0 of the original operand
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("8")) {

				System.out.println(" LSL/ASL, dir, 6 cycles, 2 Bytes, naaas");
				// Arithmetic shift left
				// obtain operand
				dataByte = memoryArray[progCounter + 1];
				// convert to binary String
				firstByte = String.format("%8s",
						Integer.toBinaryString(dataByte)).replace(" ", "0");
				// create substring from last seven bits and suffix with 0
				String shiftedString = firstByte.substring(1, 8) + "0";
				System.out.println(shiftedString);
				// shift first bit into carry bit
				ccrFlagState.setBound_C(firstByte.substring(0, 1));
				System.out.println(firstByte.substring(0, 1));
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V loaded with result of (b7 XOR b6) of the original operand
				// C loaded with bit 7 of the original operand
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("9")) {

				System.out.println(" ROL, direct, 6 cycles, 2 bytes, -aaas");
				// Rotate Left
				// obtain operand
				dataByte = memoryArray[progCounter + 1];
				// convert to binary String
				firstByte = String.format("%8s",
						Integer.toBinaryString(dataByte)).replace(" ", "0");
				// create substring from last seven bits and suffix with C
				String shiftedString = firstByte.substring(1, 8)
						+ ccrFlagState.getBound_C();
				System.out.println(shiftedString);
				// shift first bit into carry bit
				ccrFlagState.setBound_C(firstByte.substring(0, 1));
				System.out.println(firstByte.substring(0, 1));
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Loaded with the result of (b7 XOR b6) of the original
				// operand
				// C Loaded with bit 7 of the original operand
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("A")) {

				// Decrement
				System.out.println(" DEC, direct, 6 cycles, 2 Bytes, -aaa-");
				// retrieve EA from memory
				opCode2 = memoryArray[progCounter + 1];
				// retrieve data byte
				dataByte = memoryArray[opCode2];
				// decrement and store back to same location
				memoryArray[opCode2] = dataByte - 1;
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 10000000
				// C unaffected
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("C")) {

				System.out.println(" INC, direct, 6 cycles, 2 Bytes, -aaa-");
				// Increment
				// retrieve EA from memory
				opCode2 = memoryArray[progCounter + 1];
				// retrieve data byte
				dataByte = memoryArray[opCode2];
				// increment and store back to same location
				memoryArray[opCode2] = dataByte + 1;
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 01111111
				// C unaffected
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("D")) {

				System.out.println(" TST, direct, 6 cycles, 2 Bytes, -aa0-");
				// Test
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("E")) {

				System.out.println(" JMP, direct, 3 cycles, 2 Bytes, -----");
				System.out.println(" Jump to effective address");
				// retrieve EA
				opCode2 = memoryArray[progCounter + 1];
				dataByte = memoryArray[opCode2];
				// CCR unaffected
				// Program control transferred to location equivalent to EA
				// move PC on to EA
				progCounter = opCode2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("F")) {

				System.out.println(" CLR, direct, 6 cycles, 2 Bytes, -0100");
				// sets Z flag
				ccrFlagState.setBound_Z(set);
				// System.out.println(ccrFlagState.zBit() + " z bit");
				// clears N, V & C
				ccrFlagState.setBound_N(clear);
				ccrFlagState.setBound_V(clear);
				ccrFlagState.setBound_C(clear);
				// AACX or M is loaded with 00000000. C flag cleared for 6800
				// compatibility
				// move PC on two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("10")) {

				System.out.println(" PAGE1+, variant, 1 cycle, 1 Byte, +++++");
				// Expanding opCode - retrieve 2nd byte
				opCode2 = memoryArray[progCounter + 1];
				// pass to method to deal with two byte codes
				p1Codes(Integer.toHexString(opCode2));
				// move PC on one
				progCounter = progCounter + 1;
				// progCountClass.setPC(progCounter);

			} else if (firstByte.equalsIgnoreCase("11")) {

				System.out.println(" PAGE2+, variant, 1 cycle, 1 Byte, +++++");
				// Expanding opCode - retrieve 2nd byte
				opCode2 = memoryArray[progCounter + 1];
				// pass to method to deal with two byte codes
				p2Codes(Integer.toHexString(opCode2));
				// move PC on one
				progCounter = progCounter + 1;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("12")) {

				System.out.println(" NOP, inherent, 2 cycles, 1 Byte, -----");
				// No operation, only PC incremented by one
				progCounter = progCounter + 1;
				// CCR unaffected
			}

			else if (firstByte.equalsIgnoreCase("13")) {

				System.out.println(" SYNC, inherent, 2 cycles, 1 Byte, -----");
				// move PC on one
				progCounter = progCounter + 1;
				// progCountClass.setPC(progCounter);
				// Synchronise to External Event
				// CCR unaffected
			}

			else if (firstByte.equalsIgnoreCase("16")) {

				// move PC on three
				progCounter = progCounter + 3;
				// progCountClass.setPC(progCounter);
				System.out.println(" LBRA, relative, 5 cycles, 3 Bytes, -----");
				// Branch Always
				// CCR unaffected
				// Causes an unconditional branch
			}

			else if (firstByte.equalsIgnoreCase("17")) {

				System.out.println(" LBSR, relative, 9 cycles, 3 Bytes, -----");
				// Branch to Subroutine
				// CCR unaffected
				// Program counter is pushed onto the stack. The PC is then
				// loaded with the sum of the PC and the memory immediate offset
				// move PC on three
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("19")) {

				System.out.println(" DAA, inherent, 2 cycles, 1 Byte, -aa0a");
				// Decimal Addition Adjust
				// H unaffected
				// N set if MSB of result is set
				// Z set if all bits of result are clear
				// V not defined
				// C set if the operation caused a carry from bit 7 in the ALU
				// or if the carry flag was set before the operation
				// move PC on one
				progCounter = progCounter + 1;
				// progCountClass.setPC(progCounter);
			}

			else if (firstByte.equalsIgnoreCase("1A")) {

				// move PC on two
				progCounter = progCounter + 2;
				System.out
						.println(" ORCC, immediate, 3 cycles, 2 Bytes, ddddd");
				// Inclusive OR Memory-Immediate into Register
				// Performs an inclusive OR between contents of CCR & MI and the
				// result placed in the CCR
			}

			else if (firstByte.equalsIgnoreCase("1C")) {

				System.out
						.println(" ANDCC, immediate, 3 cycles, 2 Bytes, ddddd\n"
								+ "Logical AND Immediate Memory into CCR");
				// Performs a logical AND between the CCR and MI byte and
				// places the result into the CCR
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("1D")) {

				System.out.println(" SEX, inherent, 2 cycles, 1 Byte, -aa0-");
				// Sign Extended
				// H unaffected
				// N Set if MSB of result set
				// Z Set if all bits of ACCD are clear
				// V Not affected
				// C Not affected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("1E")) {

				System.out.println(" EXG, inherent, 8 cycles, 2 Bytes, ccccc");
				// Exchange Registers
				opCode2 = memoryArray[progCounter + 1];
				// ascertain which registers to swap
				if ((opCode2 == 18) || (opCode2 == 33)) {
					System.out.println(" exg oc2 = " + opCode2);
					// x swaps with y
					dataByte = (int) xIndex.getX();
					dataByte2 = (int) yIndex.getY();
					xIndex.setX(dataByte2);
					yIndex.setY(dataByte);
				}

				if ((opCode2 == 52) || (opCode2 == 67)) {
					// user swaps with hardware stack
					dataByte = userStack.getUSP();
					dataByte2 = hardStack.getHSP();
					userStack.setUSP(dataByte2);
					hardStack.setHSP(dataByte);
				}

				if ((opCode2 == 137) || (opCode2 == 152)) {
					// acc A swaps with acc B
					dataByte = accA.getAccA();
					dataByte2 = accB.getAccB();
					accA.setAccA(dataByte2);
					accB.setAccB(dataByte);
				}
				// CCR unaffected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("1F")) {

				System.out.println(" TFR, inherent, 7 cycles, 2 Bytes, -aa0a");
				// Transfer Register to Register
				opCode2 = memoryArray[progCounter + 1];
				System.out.println(" oc2 = " + opCode2);
				// ascertain which registers to transfer
				if (opCode2 == 18) {
					// x transfers into y
					dataByte = (int) xIndex.getX();
					yIndex.setY(dataByte);
				}

				if (opCode2 == 33) {
					// y transfers into x
					dataByte = (int) yIndex.getY();
					xIndex.setX(dataByte);
				}

				if (opCode2 == 52) {
					// user transfers to hardware stack
					dataByte = userStack.getUSP();
					hardStack.setHSP(dataByte);
				}

				if (opCode2 == 67) {
					// hardware transfers to user stack
					dataByte2 = hardStack.getHSP();
					userStack.setUSP(dataByte2);
				}

				if (opCode2 == 137) {
					// acc A transfers to acc B
					dataByte = accA.getAccA();
					accB.setAccB(dataByte);
				}

				if (opCode2 == 152) {
					// acc B transfers to acc A
					dataByte2 = accB.getAccB();
					accA.setAccA(dataByte2);

				}
				// CCR unaffected (unless R2 = CCR)
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("20")) {

				// Causes an unconditional branch
				System.out.println(" BRA, relative, 3 cycles, 2 Bytes, -----");
				// Branch Always
				// retrieve operand
				dataByte = memoryArray[progCounter + 1];
				// assembler subtracts 8 from operand so add again
				dataByte = dataByte + 8;
				// effective address is operand + PC
				progCounter = progCounter + dataByte;
				// CCR unaffected
			}

			else if (firstByte.equalsIgnoreCase("21")) {

				System.out.println(" BRN, relative, 3 cycles, 2 Bytes, -----");
				// Branch Never
				// CCR unaffected
				// Causes no branch
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("22")) {

				System.out.println(" BHI, relative, 3 cycles, 2 Bytes, -----");
				// Branch if Higher
				// CCR unaffected
				// Causes branch if previous operation caused neither a carry
				// nor a zero result
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("23")) {

				System.out.println(" BLS, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Lower or Same
				// CCR unaffected
				// Causes a branch if the previous operation caused either a
				// carry
				// or zero result
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("24")) {

				System.out
						.println(" BHS/BCC, relative, 3 cycles, 2 Bytes, -----");
				// BHS
				// Branch if Higher or Same
				// CCR unaffected
				// BCC
				// Branch on Carry Clear
				// CCR unaffected
				// Checks C bit and causes a branch if C is clear
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("25")) {

				System.out
						.println(" BLO/BCS, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Lower
				// Branch on Carry Set
				// CCR unaffected
				// Checks C bit and causes a branch if C is set
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("26")) {

				System.out.println(" BNE, relative, 3 cycles, 2 Bytes, -----");
				// Branch Not Equal - ascertain previous instruction
				int prev2byte = memoryArray[progCounter - 2];
				int prev3byte = memoryArray[progCounter - 3];
				// check if previous was CMPA (hex:81, dec:129)
				if (prev2byte == 129) {
					// branch if CMPA state not true
					if (getCompareA() == true) {
						// cross-assembler gives hex value representing
						// branch-to value in 2nd byte. Subtract 256 from
						// this
						// value and
						// reset PC back that much - e.g.: 2nd byte = FC (252)
						// then
						// PC
						// set back by (252-256) or -4
						opCode2 = memoryArray[progCounter + 1];
						int pcOffset = opCode2 - 256;
						System.out.println(" offset PC by :" + pcOffset);
						progCounter = progCounter + pcOffset;
					} else {
						// increment PC by two
						progCounter = progCounter + 2;
					}
				}
				// check if previous was CMPX (hex:8C, dec:140)
				if (prev3byte == 140) {
					System.out.println(" previous = " + prev3byte);
					// branch in CMPX state false
					if (getCompareX() == false) {
						opCode2 = memoryArray[progCounter + 1];
						int pcOffset = opCode2 - 256;
						System.out.println(" offset PC by :" + pcOffset);
						progCounter = (progCounter + pcOffset);
					} else {
						// increment PC by two
						progCounter = progCounter + 2;
					}
				}

				else {
					// increment PC by two
					progCounter = progCounter + 2;
				}
				// CCR unaffected
				// Causes a branch if Z bit is clear
			}

			else if (firstByte.equalsIgnoreCase("27")) {

				System.out.println(" BEQ, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Equal
				// CCR unaffected
				// Checks Z bit and causes a branch if Z is set
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("28")) {

				System.out.println(" BVC, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Overflow Clear
				// retrieve EA
				opCode2 = memoryArray[progCounter + 1];
				dataByte = memoryArray[opCode2];
				// check if V clear
				if (ccrFlagState.getBound_V().equalsIgnoreCase("0")) {
					progCounter = dataByte;
				}
				// CCR unaffected
				// Causes a branch if the V bit is clear
				// increment PC by two
				else
					progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("29")) {

				System.out.println(" BVS, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Overflow Set
				// retrieve EA
				opCode2 = memoryArray[progCounter + 1];
				dataByte = memoryArray[opCode2];
				// check if V clear
				if (ccrFlagState.getBound_V().equalsIgnoreCase("1")) {
					progCounter = dataByte;
				}
				// CCR unaffected
				// Causes a branch if the V bit is set
				// increment PC by two
				else
					progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("2A")) {

				System.out.println(" BPL, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Plus
				// CCR unaffected
				// Causes a branch if N bit is clear
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("2B")) {

				System.out.println(" BMI, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Minus
				// CCR unaffected
				// Causes a branch if N is set// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("2C")) {

				System.out.println(" BGE, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Greater Than or Equal to Zero
				// CCR unaffected
				// Checks N & V bits and causes a branch if both are either set
				// or clear
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("2D")) {

				System.out.println(" BLT, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Less than Zero
				// CCR unaffected
				// Causes a branch if either but not both of the N or V bits is
				// 1// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("2E")) {

				System.out.println(" BGT, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Greater
				// CCR unaffected
				// Checks N & V bits and causes a branch if both are either set
				// or clear and Z is clear
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("2F")) {

				System.out.println(" BLE, relative, 3 cycles, 2 Bytes, -----");
				// Branch on Less than or Equal to zero
				// CCR unaffected
				// Causes branch if the XOR of the N & V bits is 1 or if Z = 1
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("30")) {

				System.out.println(" LEAX, relative, 4 cycles, 2 Bytes, --a--");
				// Load Effective Address
				// H unaffected
				// N unaffected
				// Z LEAX, LEAY - set if all bits of result are clear
				// LEAS, LEAU - unaffected
				// V unaffected
				// C unaffected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("31")) {

				System.out.println(" LEAY, indexed, 4 cycles, 2 Bytes, --a--");
				// Load Effective Address
				// H unaffected
				// N unaffected
				// Z LEAX, LEAY - set if all bits of result are clear
				// LEAS, LEAU - unaffected
				// V unaffected
				// C unaffected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("32")) {

				System.out.println(" LEAS, indexed, 4 cycles, 2 Bytes, -----");
				// Load Effective Address
				// H unaffected
				// N unaffected
				// Z LEAX, LEAY - set if all bits of result are clear
				// LEAS, LEAU - unaffected
				// V unaffected
				// C unaffected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("33")) {

				System.out.println(" LEAU, indexed, 4 cycles, 2 Bytes, -----");
				// Load Effective Address
				// H unaffected
				// N unaffected
				// Z LEAX, LEAY - set if all bits of result are clear
				// LEAS, LEAU - unaffected
				// V unaffected
				// C unaffected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("34")) {

				System.out.println(" PSHS, inherent, 5 cycles, 2 Bytes, -----");
				System.out.println(" Push Registers onto Hardware Stack");
				System.out.println(" push order - CC, A, B, DP, X, Y, U, PC");
				// retrieve 2nd byte & establish which registers
				dataByte = memoryArray[progCounter + 1];
				// check for all 255 permutations
				// call method to deal with PUSH
				pushHard(dataByte);
				// CCR not affected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("35")) {

				System.out.println(" PULS, inherent, 5 cycles, 2 Bytes, ccccc");
				// Pull Registers from Hardware Stack
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("36")) {

				System.out.println(" PSHU, inherent, 5 cycles, 2 Bytes, -----");
				System.out.println(" Push Registers onto User Stack");
				System.out.println(" push order - CC, A, B, DP, X, Y, S, PC");
				// retrieve 2nd byte & establish which registers
				dataByte = memoryArray[progCounter + 1];
				// call method to deal with PUSH
				pushUser(dataByte);
				// CCR Not affected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("37")) {

				System.out.println(" PULU, inherent, 5 cycles, 2 Bytes, ccccc");
				// Pull Registers from User Stack
				// CCR may be pulled from stack, otherwise unaffected
				// increment PC by two
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("39")) {

				System.out.println(" RTS, inherent, 5 cycles, 1 Bytes, -----");
				// Return from Subroutine
				// CCR unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("3A")) {

				System.out.println(" ABX, inherent, 3 cycles, 1 Bytes, -----\n"
						+ " Add the 8 bit unsigned value in Accumulator B "
						+ "into the index register.\n Flags unaffected");
				// Add the 8 bit unsigned value in Accumulator B into the index
				int acBByte = accB.getAccB();
				int xByte = (int) xIndex.getX();
				int xNew = acBByte + xByte;
				xIndex.setX(xNew);
				// register. Flags unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("3B")) {

				System.out
						.println(" RTI, inherent, 6/15 cycles, 1 Bytes, -----");
				// Return from Interrupt
				// CCR & saved machine state recovered from hardware stack
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("3C")) {

				// Clear and wait for interrupt
				System.out
						.println(" CWAI, inherent, 21 cycles, 2 Bytes, ddddd");
				// Entire state saved
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C set
				ccrFlagState.setBound_C(set);
				// move PC on two
				progCounter = progCounter + 2;
				progCountClass.setPC(progCounter);
				emuRunning = false;
			}

			else if (firstByte.equalsIgnoreCase("3D")) {

				System.out.println(" MUL, inherent, 11 cycles, 1 Bytes, --a-a");
				// Multiply Accumulators
				// get acc A and B bytes
				int accAByte = accA.getAccA();
				int accBByte = accB.getAccB();
				result = accAByte * accBByte;
				// deal with overflow
				if (result > 255) {
					result = result - 256;
				}
				// H not affected
				// N not affected
				// Z Set if all bits of the result are clear
				// V not affected
				// C Set if ACCB bit 7 of result is set
				// store result in both accumulators
				accA.setAccA(result);
				accB.setAccB(result);
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("3E")) {

				System.out
						.println(" RESET*, inherent, * cycles, 1 Bytes, *****");
				// reset memory, registers and pc
				accA.setAccA(0);
				accB.setAccB(0);
				accD.setAccD(0);
				xIndex.setX(0);
				yIndex.setY(0);
				hardStack.setHSP(0);
				userStack.setUSP(0);
				dirPage.setDP(0);
				ccrFlagState.setBound_C(clear);
				ccrFlagState.setBound_V(clear);
				ccrFlagState.setBound_Z(clear);
				ccrFlagState.setBound_N(clear);
				ccrFlagState.setBound_I(clear);
				ccrFlagState.setBound_H(clear);
				ccrFlagState.setBound_F(clear);
				ccrFlagState.setBound_E(clear);
				progCounter = 0;
			}

			else if (firstByte.equalsIgnoreCase("3F")) {

				System.out.println(" SWI, inherent, 19 cycles, 1 Bytes, -----");
				// Software Interrupt - all registers pushed onto hardware stack
				// E set in CCR
				ccrFlagState.setBound_E(set);
				// increment PC by 1
				progCounter = progCounter + 1;
				// exit emulation
				emuRunning = false;
			}

			else if (firstByte.equalsIgnoreCase("40")) {

				System.out.println(" NEGA, inherent, 2 cycles, 1 Bytes, uaaaa");
				// Negate
				// H undefined
				// N Set if bit 7 of result is set
				// Z Set if all bits of the result are clear
				// V Set if original operand was 10000000
				// C Set if the operation did not cause a carry from bit 7 in
				// the ALU
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("43")) {

				System.out.println(" COMA, inherent, 2 cycles, 1 Bytes, -aa01");
				// Complement
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C set
				ccrFlagState.setBound_C(set);
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("44")) {

				System.out.println(" LSRA, inherent, 2 cycles, 1 Bytes, -0a-s");
				// Logical Shift Right
				// obtain operand
				dataByte = accA.getAccA();
				// convert to binary String
				firstByte = String.format("%8s",
						Integer.toBinaryString(dataByte)).replace(" ", "0");
				// create substring from first seven bits and prefix with 0
				String shiftedString = "0" + firstByte.substring(0, 7);
				// reset acc A
				accA.setBoundAccA(shiftedString);
				// C Loaded with bit 0 of the original operand
				// shift bit eight into carry bit
				ccrFlagState.setBound_C(firstByte.substring(7, 8));
				System.out.println(firstByte.substring(7, 8));
				// H not affected
				// N cleared
				ccrFlagState.getBound_N();
				// Z Set if all bits of the result are clear
				// V not affected
				// C Loaded with bit 0 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("46")) {

				System.out.println(" RORA, inherent, 2 cycles, 1 Bytes, -aa-s");
				// Rotate Right
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Not affected
				// C Loaded with bit 0 of the previous operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("47")) {

				System.out.println(" ASRA, inherent, 2 cycles, 1 Bytes, uaa-s");
				// Arithmetic shift right
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V unaffected
				// C loaded with bit 0 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("48")) {

				System.out
						.println(" LSLA/ASLA, inherent, 2 cycles, 1 Bytes, naaas\n "
								+ "Arithmetic shift left");
				// Logical Shift Left
				// Arithmetic shift left
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V loaded with result of (b7 XOR b6) of the original operand
				// C loaded with bit 7 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("49")) {

				System.out.println(" ROLA, inherent, 2 cycles, 1 Bytes, -aaas");
				// Rotate Left
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Loaded with the result of (b7 XOR b6) of the original
				// operand
				// C Loaded with bit 7 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("4A")) {

				System.out.println(" DECA, inherent, 2 cycles, 1 Bytes, -aaa-");
				// Decrement
				accByte = accA.getAccA() - 1;
				accA.setAccA(accByte);
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 10000000
				// C unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("4C")) {

				System.out.println(" INCA, inherent, 2 cycles, 1 Bytes, -aaa-");
				// Increment acc A
				accByte = accA.getAccA() + 1;
				accA.setAccA(accByte);
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 01111111
				// C unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("4D")) {

				System.out.println(" TSTA, inherent, 2 cycles, 1 Bytes, -aa0-");
				// Test
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("4F")) {

				System.out.println(" CLRA, inherent, 2 cycles, 1 Bytes, -0100");
				accA.setAccA(0);
				// sets Z flag
				ccrFlagState.setBound_Z(set);
				// clears N, V & C
				ccrFlagState.setBound_N(clear);
				ccrFlagState.setBound_V(clear);
				ccrFlagState.setBound_C(clear);
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("50")) {

				System.out.println(" NEGB, inherent, 2 cycles, 1 Bytes, uaaaa");
				// Negate
				// H undefined
				// N Set if bit 7 of result is set
				// Z Set if all bits of the result are clear
				// V Set if original operand was 10000000
				// C Set if the operation did not cause a carry from bit 7 in
				// the ALU
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("53")) {

				System.out.println(" COMB, inherent, 2 cycles, 1 Bytes, -aa01");
				// Complement
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C set
				ccrFlagState.setBound_C(set);
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("54")) {

				System.out.println(" LSRB, inherent, 2 cycles, 1 Bytes, -0a-s");
				// Logical Shift Right
				// H not affected
				// N cleared
				ccrFlagState.setBound_N(clear);
				// Z Set if all bits of the result are clear
				// V not affected
				// C Loaded with bit 0 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("56")) {

				System.out.println(" RORB, inherent, 2 cycles, 1 Bytes, -aa-s");
				// Rotate Right
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Not affected
				// C Loaded with bit 0 of the previous operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("57")) {

				System.out.println(" ASRB, inherent, 2 cycles, 1 Bytes, uaa-s");
				// Arithmetic shift right
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V unaffected
				// C loaded with bit 0 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("58")) {

				System.out
						.println(" LSLB/ASLB, inherent, 2 cycles, 1 Bytes, naaas");
				// Logical Shift Left
				// Arithmetic shift left
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V loaded with result of (b7 XOR b6) of the original operand
				// C loaded with bit 7 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("59")) {

				System.out.println(" ROLB, inherent, 2 cycles, 1 Bytes, -aaas");
				// Rotate Left
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Loaded with the result of (b7 XOR b6) of the original
				// operand
				// C Loaded with bit 7 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("5A")) {

				System.out.println(" DECB, inherent, 2 cycles, 1 Bytes, -aaa-");
				// Decrement
				accByte = accB.getAccB() - 1;
				accB.setAccB(accByte);
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 10000000
				// C unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("5C")) {

				System.out.println(" INCB, inherent, 2 cycles, 1 Bytes, -aaa-");
				// Increment acc B
				accByte = accB.getAccB() + 1;
				accB.setAccB(accByte);
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 01111111
				// C unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("5D")) {

				System.out.println(" TSTB, inherent, 2 cycles, 1 Bytes, -aa0-");
				// Test
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("5F")) {

				System.out.println(" CLRB, inherent, 2 cycles, 1 Bytes, -0100");
				accB.setAccB(0);
				// sets Z flag
				ccrFlagState.setBound_Z(set);
				// System.out.println(ccrFlagState.zBit() + " z bit");
				// clears N, V & C
				ccrFlagState.setBound_N(clear);
				ccrFlagState.setBound_V(clear);
				ccrFlagState.setBound_C(clear);
				// AACX or M is loaded with 00000000. C flag cleared for 6800
				// compatibility
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("60")) {

				System.out.println(" NEG, INDEXED, 6 cycles, 2 Bytes, uaaaa");
				// Negate
				// H undefined
				// N Set if bit 7 of result is set
				// Z Set if all bits of the result are clear
				// V Set if original operand was 10000000
				// C Set if the operation did not cause a carry from bit 7 in
				// the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("63")) {

				System.out.println(" COM, INDEXED, 6 cycles, 2 Bytes, -aa01");
				// Complement
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C set
				ccrFlagState.setBound_C(set);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("64")) {

				System.out.println(" LSR, INDEXED, 6 cycles, 2 Bytes, -0a-s");
				// Logical Shift Right
				// H not affected
				// N cleared
				ccrFlagState.setBound_N(clear);
				// Z Set if all bits of the result are clear
				// V not affected
				// C Loaded with bit 0 of the original operand
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("66")) {

				System.out.println(" ROR, INDEXED, 6 cycles, 2 Bytes, -aa-s");
				// Rotate Right
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Not affected
				// C Loaded with bit 0 of the previous operand
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("67")) {

				System.out.println(" ASR, INDEXED, 6 cycles, 2 Bytes, uaa-s");
				// Arithmetic shift right
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V unaffected
				// C loaded with bit 0 of the original operand
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("68")) {

				System.out
						.println(" LSL/ASL, INDEXED, 6 cycles, 2 Bytes, naaas");
				// Logical Shift Left
				// Arithmetic shift left
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V loaded with result of (b7 XOR b6) of the original operand
				// C loaded with bit 7 of the original operand
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("69")) {

				System.out.println(" ROL, INDEXED, 6 cycles, 2 Bytes, -aaas");
				// Rotate Left
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Loaded with the result of (b7 XOR b6) of the original
				// operand
				// C Loaded with bit 7 of the original operand
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("6A")) {

				// Decrement
				System.out.println(" DEC, INDEXED, 6 cycles, 2 Bytes, -aaa-");
				// retrieve operand
				dataByte = memoryArray[progCounter + 1];
				// determine which register
				if (dataByte == 0) {
					// PC
					progCounter = progCounter - 1;
				} else if (dataByte == 132) {
					// X index 84 (dec 132)
					dataByte2 = (int) xIndex.getX() - 1;
					xIndex.setX(dataByte2);
				} else if (dataByte == 164) {
					// Y index A4 (dec 164)
					dataByte2 = (int) yIndex.getY() - 1;
					yIndex.setY(dataByte2);
				} else if (dataByte == 196) {
					// User SP C4 (dec 196)
					dataByte2 = userStack.getUSP() - 1;
					userStack.setUSP(dataByte2);
				} else if (dataByte == 228) {
					// hwsp E4 (dec 228)
					dataByte2 = hardStack.getHSP() - 1;
					hardStack.setHSP(dataByte2);
				}
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 10000000
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("6C")) {

				System.out.println(" INC, INDEXED, 6 cycles, 2 Bytes, -aaa-");
				// Increment
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 01111111
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("6D")) {
				// TST
				// Test
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("6E")) {
				// Jump to effective address
				// retrieve operand
				dataByte = memoryArray[progCounter + 1];
				// ascertain index for EA
				if (dataByte == 0) {
					// PC (00) = PC
				} else if (dataByte == 132) {
					// X index
					progCounter = (int) xIndex.getX();
				} else if (dataByte == 164) {
					// Y index
					progCounter = (int) yIndex.getY();
				} else if (dataByte == 196) {
					// user stack
					progCounter = userStack.getUSP();
				} else if (dataByte == 228) {
					// hardware stack
					progCounter = hardStack.getHSP();
				} else {
					// CCR unaffected
					// Program control transferred to location equivalent to EA
					// increment PC by 2
					progCounter = progCounter + 2;
				}
			}

			else if (firstByte.equalsIgnoreCase("6F")) {
				// CLR
				// sets Z flag
				ccrFlagState.setBound_Z(set);
				// System.out.println(ccrFlagState.zBit() + " z bit");
				// clears N, V & C
				ccrFlagState.setBound_N(clear);
				ccrFlagState.setBound_V(clear);
				ccrFlagState.setBound_C(clear);
				// AACX or M is loaded with 00000000. C flag cleared for 6800
				// compatibility
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("70")) {

				// NEG
				// Negate
				// H undefined
				// N Set if bit 7 of result is set
				// Z Set if all bits of the result are clear
				// V Set if original operand was 10000000
				// C Set if the operation did not cause a carry from bit 7 in
				// the ALU
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("73")) {

				// Complement
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C set
				ccrFlagState.setBound_C(set);
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("74")) {

				// LSR - extended
				System.out.println(" Logical Shift Right 16 bit ");
				// obtain operand
				dataByte = memoryArray[progCounter + 1];
				dataByte2 = memoryArray[progCounter + 2];
				// convert to binary String
				firstByte = String.format("%8s",
						Integer.toBinaryString(dataByte)).replace(" ", "0");
				// create substring from first seven bits and prefix with 0
				String shiftedString = "0" + firstByte.substring(0, 7);
				System.out.println(shiftedString);
				// shift bit eight into carry bit
				ccrFlagState.setBound_C(firstByte.substring(7, 8));
				System.out.println(firstByte.substring(7, 8));

				// H not affected
				// N cleared
				ccrFlagState.getBound_N();
				// Z Set if all bits of the result are clear
				// V not affected
				// C Loaded with bit 0 of the original operand
				// move PC on two
				progCounter = progCounter + 2;
				// progCountClass.setPC(progCounter);
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("76")) {

				// ROR
				// Rotate Right
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Not affected
				// C Loaded with bit 0 of the previous operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("77")) {

				// ASR
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("78")) {

				// LSL/ASL
				// Logical Shift Left
				// Arithmetic shift left
				// H undefined
				// N set if bit 7 of the result is set
				// Z set if all bits of the result are clear
				// V loaded with result of (b7 XOR b6) of the original operand
				// C loaded with bit 7 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("79")) {

				// ROL
				// Rotate Left
				// H not affected
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Loaded with the result of (b7 XOR b6) of the original
				// operand
				// C Loaded with bit 7 of the original operand
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("7A")) {

				// Decrement
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 10000000
				// C unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("7C")) {

				// Increment
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if original operand was 01111111
				// C unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("7D")) {

				// TST
				// Test
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("7E")) {

				// Jump to effective address
				// CCR unaffected
				// Program control transferred to location equivalent to EA
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("7F")) {
				// CLR, indexed
				// sets Z flag
				ccrFlagState.setBound_Z(set);
				// System.out.println(ccrFlagState.zBit() + " z bit");
				// clears N, V & C
				ccrFlagState.setBound_N(clear);
				ccrFlagState.setBound_V(clear);
				ccrFlagState.setBound_C(clear);
				// AACX or M is loaded with 00000000. C flag cleared for 6800
				// compatibility
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("80")) {

				// SUBA - immediate
				// Subtract Memory from Register - 8 bits
				opCode2 = memoryArray[progCounter + 1];
				accByte = accA.getAccA();
				result = accByte - opCode2;
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// store result in accumulator A
				accA.setAccA(result);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("81")) {

				// CMPA
				// Compare memory from Register A - 8 bits
				accByte = accA.getAccA();
				dataByte = memoryArray[progCounter + 1];
				if (accByte == dataByte) {
					setCompareA(true);
				}
				// H undefined
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 8 bit two's complement overflow
				// C set if subtraction did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 1
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("82")) {

				// SBCA
				// Subtract with Borrow
				// H undefined
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Set if the operation causes an 8 bit 2's complement
				// overflow
				// C Set if the operation did NOT cause a carry from bit 7 in
				// the ALU
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("83")) {

				// SUBD - 3 bytes
				accByte = accA.getAccA() + accB.getAccB();
				dataByte = memoryArray[progCounter + 1];
				dataByte2 = memoryArray[progCounter + 2];
				// convert to strings and concatenate
				String dataString1 = String.format("%02x", dataByte);
				String dataString2 = String.format("%02x", dataByte2);
				long data16Bit = Integer.parseInt((dataString1 + dataString2),
						16);
				// Subtract Memory from Register - 16 bits
				result = (int) (accByte - data16Bit);
				accD.setAccD(result);
				// H undefined
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in the
				// ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("84")) {

				// ANDA
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("85")) {

				// BITA
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equals("86")) {

				System.out.println(" LDA, IMMEDIATE, 4 cycles, 2 Bytes, -aa0-");
				int accAByte = memoryArray[startAddress + 1];
				// set Accumulator A
				accA.setAccA(accAByte);
				// move PC on two
				progCounter = progCounter + 2;
				// Load Register from memory - 8 bits
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
			}

			else if (firstByte.equalsIgnoreCase("88")) {

				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("89")) {

				// ADCA - add carry flag and memory byte into A
				// retrieve memory byte
				dataByte = memoryArray[progCounter + 1];
				// add to A and C
				result = accA.getAccA()
						+ Integer.parseInt(ccrFlagState.getBound_C())
						+ dataByte;
				// set A
				accA.setAccA(result);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("8A")) {

				// ORA
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("8B")) {

				// immediate ADDA - add into accumulator A
				System.out
						.println(" ADDA, immediate, 2 cycles, 2 Bytes, aaaaa");
				// get value from accumulator a
				int accAByte = accA.getAccA();
				// add value from second byte of instruction
				int addAByte = memoryArray[progCounter + 1];
				accAByte = accAByte + addAByte;
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				if (accAByte > 255) {
					accAByte = accAByte - 256;
					System.out.println(" 8 bit overflow - V flag set");
					ccrFlagState.setBound_V(set);
				}
				// C set if carry caused from bit 7 in the ALU
				accA.setAccA(accAByte);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("8C")) {

				System.out
						.println(" CMPX, immediate, 4 cycles, 3 Bytes, -aaaa");
				// Compare memory from a Register - 16 bits
				index16bit = xIndex.getX();
				dataByte = memoryArray[progCounter + 1];
				dataByte2 = memoryArray[progCounter + 2];
				if (index16bit == (dataByte + dataByte2)) {
					setCompareX(true);
				}
				// H unaffected
				// N set if bit 15 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 16 bit two's complement overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in
				// the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("8D")) {

				System.out.println(" BSR, RELATIVE, 7 cycles, 2 Bytes, -aa0-");
				// Branch to Subroutine
				// CCR unaffected
				// Program counter is pushed onto the stack. The PC is then
				// loaded with the sum of the PC and the memory immediate offset
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("8E")) {

				System.out.println(" LDX, immediate, 3 cycles, 3 Bytes, aaaaa");
				// retrieve both bytes from memory
				int xByte = memoryArray[progCounter + 1];
				int xByte2 = memoryArray[progCounter + 2];
				// convert to string for concatenation
				// String xStringByte1 = Integer.toHexString(xByte);
				String xStringByte1 = String.format("%02x", xByte);
				String xStringByte2 = String.format("%02x", xByte2);
				String newX = xStringByte1 + xStringByte2;
				// H not affected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of loaded data are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
				// deal with 16 bit overflow
				if (xByte > 65535) {
					xByte = xByte - 65536;
					System.out.println("2's complement overflow");
				}
				// set index x to xByte
				xIndex.setX(Integer.parseInt(newX, 16));
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("90")) {

				System.out.println(" SUBA, DIRECT, 4 cycles, 2 Bytes, uaaaa");
				// SUBA
				// Subtract Memory from Register - 8 bits
				// retrieve value from memory
				opCode2 = memoryArray[progCounter + 1];
				// go to that address denoted by that value to retrieve operand
				dataByte = memoryArray[opCode2];
				// retrieve contents of acc A
				accByte = accA.getAccA();
				// subtract operand from contents of acc A
				result = accByte - dataByte;
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// store result in acc A
				accA.setAccA(result);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("91")) {

				System.out.println(" CMPA, DIRECT, 4 cycles, 2 Bytes, uaaaa");
				// Compare memory from a Register - 8 bits
				// retrieve A
				dataByte = accA.getAccA();
				// retrieve location
				opCode2 = memoryArray[progCounter + 1];
				// retrieve operand from memory location
				dataByte2 = memoryArray[opCode2];
				// compare
				if (dataByte == dataByte2) {
					setCompareA(true);
				}
				// H undefined
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 8 bit two's complement overflow
				// C set if subtraction did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("92")) {

				System.out.println(" SBCA, DIRECT, 4 cycles, 2 Bytes, uaaaa");
				// Subtract with Borrow
				// H undefined
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Set if the operation causes an 8 bit 2's complement
				// overflow
				// C Set if the operation did NOT cause a carry from bit 7 in
				// the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("93")) {

				System.out.println(" SUBD, DIRECT, 6 cycles, 2 Bytes, -aaaa");
				// Subtract Memory from Register - 16 bits
				accByte = accA.getAccA() + accB.getAccB();
				int eA = memoryArray[progCounter + 1];
				// retrieve operand from EA
				dataByte = memoryArray[eA];
				// Subtract Memory from Register - 16 bits
				result = (int) (accByte - dataByte);
				accD.setAccD(result);
				// H undefined
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("94")) {

				System.out.println(" ANDA, DIRECT, 4 cycles, 2 Bytes, -aa0-\n"
						+ "Logical AND memory into register");
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of the result are clear
				// V cleared
				// C unaffected
				// ccrFlagState.setVBit(false);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("95")) {

				System.out.println(" BITA, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Bit test
				// H unaffected
				// N set if bit 7 of result is set
				// Z Set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("96")) {

				System.out.println(" LDA, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 8 bits
				dataByte = memoryArray[progCounter + 1];
				opCode2 = memoryArray[dataByte];
				accA.setAccA(opCode2);
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("97")) {

				System.out.println(" STA, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Store Register Into Memory - 8 bits
				int accAByte = accA.getAccA();
				int addressByte = memoryArray[progCounter + 1];
				setArrayData(addressByte, accAByte);
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("98")) {

				System.out.println(" EORA, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("99")) {

				System.out.println(" ADCA, DIRECT, 4 cycles, 2 Bytes, aaaaa");
				// retrieve EA from memory byte
				int eA = memoryArray[progCounter + 1];
				// retrieve operand
				dataByte = memoryArray[eA];
				// add to A and C
				result = accA.getAccA()
						+ Integer.parseInt(ccrFlagState.getBound_C())
						+ dataByte;
				// set A
				accA.setAccA(result);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("9A")) {

				System.out.println(" ORA, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("9B")) {

				// direct ADDA
				System.out.println(" ADDA, direct, 4 cycles, 2 Bytes, aaaaa");
				// retrieve A
				accByte = accA.getAccA();
				// retrieve EA
				int eA = memoryArray[progCounter + 1];
				// retrieve operand
				dataByte = memoryArray[eA];
				// add into A
				accA.setAccA(accByte + dataByte);
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("9C")) {

				System.out.println(" CMPX, DIRECT, 6 cycles, 2 Bytes, -aaaa");
				// Compare memory from a Register - 16 bits
				// H unaffected
				// N set if bit 15 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 16 bit two's complement overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in
				// the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("9D")) {

				System.out.println(" JSR, DIRECT, 7 cycles, 2 Bytes, -----");
				// Jump to Subroutine at effective address
				// CCR unaffected
				// Program control transferred to location equivalent to EA
				// after storing return address on the hardware stack
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("9E")) {

				System.out.println("LDX, DIRECT, 5 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// retrieve EA
				int eA = memoryArray[progCounter + 1];
				// retrieve operand
				dataByte = memoryArray[eA];
				// load into X
				xIndex.setX(dataByte);
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("9F")) {

				System.out.println(" STX, DIRECT, 5 cycles, 2 Bytes, -aa0-");
				// Store X Register Into Memory - 16 bits
				long XByte = xIndex.getX();
				int addressByte = memoryArray[progCounter + 1];
				setArrayData(addressByte, (int) XByte); // TODO 16 bit value,
														// needs split
				// H unaffected
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A0")) {

				System.out.println(" SUBA, INDEXED, 4 cycles, 2 Bytes, uaaaa");
				// SUBA
				// Subtract Memory from Register - 8 bits
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A1")) {

				System.out.println(" CMPA, INDEXED, 4 cycles, 2 Bytes, uaaaa");
				System.out.println(" Compare memory from Register A - 8 bits");
				accByte = accA.getAccA();
				// retrieve operand
				dataByte = memoryArray[progCounter + 1];
				// determine which register
				if (dataByte == 0) {
					// PC
					if (accByte == progCounter)
						setCompareA(true);
				} else if (dataByte == 132) {
					// X index 84 (dec 132)
					dataByte2 = (int) xIndex.getX();
					if (accByte == dataByte2)
						setCompareA(true);
				} else if (dataByte == 164) {
					// Y index A4 (dec 164)
					dataByte2 = (int) yIndex.getY();
					if (accByte == dataByte2)
						setCompareA(true);
				} else if (dataByte == 196) {
					// User SP C4 (dec 196)
					dataByte2 = userStack.getUSP();
					if (accByte == dataByte2)
						setCompareA(true);
				} else if (dataByte == 228) {
					// hwsp E4 (dec 228)
					dataByte2 = hardStack.getHSP();
					if (accByte == dataByte2)
						setCompareA(true);
				}
				// H undefined
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 8 bit two's complement overflow
				// C set if subtraction did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A2")) {

				System.out.println("SBCA, INDEXED, 4 cycles, 2 Bytes, uaaaa");
				// Subtract with Borrow
				// H undefined
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Set if the operation causes an 8 bit 2's complement
				// overflow
				// C Set if the operation did NOT cause a carry from bit 7 in
				// the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A3")) {

				System.out.println("SUBD, INDEXED, 6 cycles, 2 Bytes, -aaaa");
				// Subtract Memory from Register - 16 bits
				// H undefined
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A4")) {

				System.out.println("ANDA, DIRECT, 4 cycles, 2 Bytes, -aa0-\n"
						+ "Logical AND memory into register");
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of the result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A5")) {

				System.out.println("BITA, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Bit test
				// H unaffected
				// N set if bit 7 of result is set
				// Z Set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A6")) {

				System.out.println(" LDA, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 8 bits
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A7")) {

				System.out.println(" STA, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Store Register A Into Memory - 8 bits
				accByte = accA.getAccA();
				// derive effective address from contents of register
				opCode2 = memoryArray[progCounter + 1];
				if (opCode2 == 0) {
					effectiveAddress = xIndex.getX();
					System.out.println(" Effective Address = contents of X: "
							+ effectiveAddress);
				}
				if (opCode2 == 32) {
					System.out.println(" EA = contents of Y");
					effectiveAddress = yIndex.getY();
				}

				if (opCode2 == 64) {
					System.out.println(" EA = contents of U");
					effectiveAddress = userStack.getUSP();
				}

				if (opCode2 == 96) {
					System.out.println(" EA = contents of S");
					effectiveAddress = hardStack.getHSP();
				}
				// store contents of A to effective address
				setArrayData((int) effectiveAddress, accByte);
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A8")) {

				System.out.println(" EORA, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("A9")) {

				System.out.println(" ADCA, INDEXED, 4 cycles, 2 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("AA")) {

				System.out.println("ORA, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("AB")) {

				// indexed ADDA
				System.out.println(" ADDA, INDEXED, 4 cycles, 2 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("AC")) {

				System.out.println(" CMPX, INDEXED, 6 cycles, 2 Bytes, -aaaa");
				// Compare memory from a Register X - 16 bits
				index16bit = xIndex.getX();
				// retrieve operand
				dataByte = memoryArray[progCounter + 1];
				// determine which register
				if (dataByte == 0) {
					// PC
					if (index16bit == progCounter)
						setCompareX(true);
				} else if (dataByte == 132) {
					// X index 84 (dec 132)
					dataByte2 = (int) xIndex.getX();
					if (index16bit == dataByte2)
						setCompareX(true);
				} else if (dataByte == 164) {
					// Y index A4 (dec 164)
					dataByte2 = (int) yIndex.getY();
					if (index16bit == dataByte2)
						setCompareX(true);
				} else if (dataByte == 196) {
					// User SP C4 (dec 196)
					dataByte2 = userStack.getUSP();
					if (index16bit == dataByte2)
						setCompareX(true);
				} else if (dataByte == 228) {
					// hwsp E4 (dec 228)
					dataByte2 = hardStack.getHSP();
					if (index16bit == dataByte2)
						setCompareX(true);
				}
				// H unaffected
				// N set if bit 15 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 16 bit two's complement overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in
				// the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("AD")) {

				System.out.println("JSR, INDEXED, 7 cycles, 2 Bytes, -----");
				// Jump to Subroutine at effective address
				// CCR unaffected
				// Program control transferred to location equivalent to EA
				// after storing return address on the hardware stack
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("AE")) {

				System.out.println(" LDX, INDEXED, 5 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("AF")) {

				System.out.println(" STX, INDEXED, 5 cycles, 2 Bytes, -aa0-");
				// Store Register X Into Memory - 16 bits
				index16bit = xIndex.getX();
				// split into single byte hex strings
				String xHexString = String.format("%016x", index16bit);
				String xByte1 = xHexString.substring(0, 8);
				String xByte2 = xHexString.substring(8, 16);
				dataByte = Integer.parseInt(xByte1);
				dataByte2 = Integer.parseInt(xByte2);
				// derive effective address from contents of register
				opCode2 = memoryArray[progCounter + 1];
				if (opCode2 == 0) {
					effectiveAddress = xIndex.getX();
					System.out.println(" EA = contents of X: "
							+ effectiveAddress);
				}
				if (opCode2 == 32) {
					System.out.println(" EA = contents of Y");
					effectiveAddress = yIndex.getY();
				}

				if (opCode2 == 64) {
					System.out.println(" EA = contents of U");
					effectiveAddress = userStack.getUSP();
				}

				if (opCode2 == 96) {
					System.out.println(" EA = contents of S");
					effectiveAddress = hardStack.getHSP();
				}
				// store first byte to effective address
				setArrayData((int) effectiveAddress, dataByte);
				// store second byte to effective address +1
				setArrayData((int) (effectiveAddress + 1), dataByte2);
				// H unaffected
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("B0")) {

				System.out.println(" SUBA, EXTENDED, 5 cycles, 3 Bytes, uaaaa");
				// SUBA
				// Subtract Memory from Register - 8 bits
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("B1")) {

				System.out.println(" CMPA, EXTENDED, 5 cycles, 3 Bytes, uaaaa");
				// Compare memory from a Register - 8 bits
				// H undefined
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 8 bit two's complement overflow
				// C set if subtraction did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("B2")) {

				System.out.println(" SBCA, EXTENDED, 5 cycles, 3 Bytes, uaaaa");
				// Subtract with Borrow
				// H undefined
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Set if the operation causes an 8 bit 2's complement
				// overflow
				// C Set if the operation did NOT cause a carry from bit 7 in
				// the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("B3")) {

				System.out.println(" SUBD, EXTENDED, 7 cycles, 3 Bytes, -aaaa");
				// Subtract Memory from Register - 16 bits
				// H undefined
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("B4")) {

				System.out.println(" ANDA, DIRECT, 4 cycles, 2 Bytes, -aa0-\n"
						+ "Logical AND memory into register");
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of the result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("B5")) {

				System.out.println(" BITA, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Bit test
				// H unaffected
				// N set if bit 7 of result is set
				// Z Set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("B6")) {

				System.out.println(" LDA, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Load Register from memory - 8 bits
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("B7")) {

				System.out.println(" STA, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Store Register Into Memory - 8 bits
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("B8")) {

				System.out.println(" EORA, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("B9")) {

				System.out.println(" ADCA, EXTENDED, 5 cycles, 3 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("BA")) {

				System.out.println(" ORA, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("BB")) {
				// extended ADDA
				System.out.println(" DAA, EXTENDED, 2 cycles, 1 Bytes, -aa0a");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 1
				progCounter = progCounter + 1;
			}

			else if (firstByte.equalsIgnoreCase("BC")) {

				System.out.println(" CMPX, EXTENDED, 7 cycles, 3 Bytes, -aaaa");
				// Compare memory from a Register - 16 bits
				// H unaffected
				// N set if bit 15 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 16 bit two's complement overflow
				// C set if operation on the MSB did NOT cause a carry from bit
				// 7 in
				// the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("BD")) {

				System.out.println(" JSR, EXTENDED, 8 cycles, 3 Bytes, -----");
				// Jump to Subroutine at effective address
				// CCR unaffected
				// Program control transferred to location equivalent to EA
				// after storing return address on the hardware stack
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("BE")) {

				System.out.println(" LDX, EXTENDED, 6 cycles, 3 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("BF")) {

				System.out.println(" STX, EXTENDED, 6 cycles, 3 Bytes, -aa0-");
				// Store Register Into Memory - 16 bits
				// H unaffected
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("C0")) {

				System.out
						.println(" SUBB, IMMEDIATE, 2 cycles, 2 Bytes, uaaaa");
				// SUBA
				// Subtract Memory from Register - 8 bits
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("C1")) {

				System.out
						.println(" CMPB, IMMEDIATE, 2 cycles, 2 Bytes, uaaaa");
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("C2")) {

				System.out
						.println(" SBCB, IMMEDIATE, 2 cycles, 2 Bytes, uaaaa");
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("C3")) {

				// immediate ADDD
				System.out
						.println(" ADDD, IMMEDIATE, 4 cycles, 3 Bytes, -aaaa\n "
								+ "Add memory into register - 16 bits");
				// get both bytes of 16 bit value
				dataByte = memoryArray[progCounter + 1];
				dataByte2 = memoryArray[progCounter + 2];
				// convert to strings and concatenate
				String dataString1 = String.format("%02x", dataByte);
				String dataString2 = String.format("%02x", dataByte2);
				long data16Bit = Integer.parseInt((dataString1 + dataString2),
						16);
				// H unaffected
				// N set if bit 15 of the result is set
				// Z set if all bits of result are clear
				// V set if 16 bit two's compliment arithmetic overflow caused
				if (data16Bit > 4294967295L) {
					data16Bit = data16Bit - 4294967296L;
					ccrFlagState.setBound_V(set);
				}
				// C set if carry caused by MSB from bit 7 in the ALU
				accD.setAccD(data16Bit);
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("C4")) {

				System.out.println(" ANDA, DIRECT, 4 cycles, 2 Bytes, -aa0-\n "
						+ "Logical AND memory into register");
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of the result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("C5")) {

				System.out
						.println(" BITB, IMMEDIATE, 2 cycles, 2 Bytes, -aa0-");
				// Bit test
				// H unaffected
				// N set if bit 7 of result is set
				// Z Set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("C6")) {

				System.out.println(" LDB, IMMEDIATE, 2 cycles, 2 Bytes, -aa0-");
				int accBByte = memoryArray[progCounter + 1];
				accB.setAccB(accBByte);
				// increment PC by two
				progCounter = progCounter + 2;
				// Load Register from memory - 8 bits
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
			}

			else if (firstByte.equalsIgnoreCase("C8")) {

				System.out
						.println(" EORB, IMMEDIATE, 2 cycles, 2 Bytes, -aa0-");
				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("C9")) {

				System.out
						.println(" ADCB, IMMEDIATE, 2 cycles, 2 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("CA")) {

				System.out.println(" ORB, IMMEDIATE, 2 cycles, 2 Bytes, -aa0-");
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("CB")) {
				// immediate ADDB
				System.out
						.println(" ADDB, IMMEDIATE, 2 cycles, 2 Bytes, aaaaa");
				int accBByte = accB.getAccB();
				// add value from second byte of instruction
				int addAByte = memoryArray[progCounter + 1];
				accBByte = accBByte + addAByte;
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				if (accBByte > 255) {
					accBByte = accBByte - 256;
					ccrFlagState.setBound_V(set);
					System.out.println("2's complement overflow");
				}
				// C set if carry caused from bit 7 in the ALU
				accB.setAccB(accBByte);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("CC")) {

				System.out.println(" LDD, IMMEDIATE, 3 cycles, 3 Bytes, -aa0-");
				// retrieve two consecutive bytes
				dataByte = memoryArray[progCounter + 1];
				dataByte2 = memoryArray[progCounter + 2];
				// convert to binary strings to concatenate
				firstByte = Integer.toBinaryString(dataByte);
				String secondByte = Integer.toBinaryString(dataByte2);
				String string16bit = firstByte + secondByte;
				System.out.println(string16bit);
				// convert to long for loading into D
				index16bit = Integer.parseInt(string16bit);
				// load into D
				accD.setAccD(index16bit);
				System.out.println(" Load Register from memory - 16 bits \n"
						+ " H unaffected \n"
						+ " N set if bit 15 of loaded data is set\n"
						+ " Z set if all bits of result are clear\n"
						+ " V cleared\n ");
				ccrFlagState.setBound_V(clear);
				// C unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("CE")) {

				System.out.println(" LDU, IMMEDIATE, 3 cycles, 3 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// retrieve both bytes from memory
				int sByte = memoryArray[progCounter + 1];
				int sByte2 = memoryArray[progCounter + 2];

				// convert to string for concatenation
				String sStringByte1 = String.format("%02x", sByte);
				String sStringByte2 = String.format("%02x", sByte2);
				String newS = sStringByte1 + sStringByte2;
				System.out.println(Integer.parseInt(newS, 16) + " = us");
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
				// set stack to sByte
				userStack.setUSP(Integer.parseInt(newS, 16));
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("DO")) {

				System.out.println(" SUBB, DIRECT, 4 cycles, 2 Bytes, uaaaa");
				// SUBB
				// Subtract Memory from Register - 8 bits
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D1")) {

				System.out.println(" CMPB, DIRECT, 4 cycles, 2 Bytes, uaaaa");
				// Compare memory from a Register - 8 bits
				// H undefined
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 8 bit two's complement overflow
				// C set if subtraction did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D2")) {

				System.out.println(" SBCB, DIRECT, 4 cycles, 2 Bytes, uaaaa");
				// Subtract with Borrow
				// H undefined
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Set if the operation causes an 8 bit 2's complement
				// overflow
				// C Set if the operation did NOT cause a carry from bit 7 in
				// the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D3")) {

				System.out
						.println(" ADDD, IMMEDIATE, 4 cycles, 3 Bytes, -aaaa\n"
								+ "Add memory into register - 16 bits");
				// adds the 16 bit memory value into the 16 bit accumulator

				// H unaffected
				// N set if bit 15 of the result is set
				// Z set if all bits of result are clear
				// V set if 16 bit two's compliment arithmetic overflow caused
				// C set if carry caused by MSB from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D4")) {

				System.out.println("ANDA, DIRECT, 4 cycles, 2 Bytes, -aa0-\n"
						+ "Logical AND memory into register");
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of the result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D5")) {

				System.out.println("BITB, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Bit test
				// H unaffected
				// N set if bit 7 of result is set
				// Z Set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D6")) {

				System.out.println("LDB, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 8 bits
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D7")) {

				System.out.println(" STB, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Store Register Into Memory - 8 bits
				int accAByte = accB.getAccB();
				int addressByte = memoryArray[progCounter + 1];
				setArrayData(addressByte, accAByte);
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D8")) {

				System.out.println("EORB, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("D9")) {

				System.out.println("ADCB, DIRECT, 4 cycles, 2 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("DA")) {

				System.out.println("ORB, DIRECT, 4 cycles, 2 Bytes, -aa0-");
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("DB")) {
				// direct ADDB
				System.out.println("ADDB, DIRECT, 4 cycles, 2 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("DC")) {

				System.out.println("LDD, DIRECT, 5 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("DD")) {

				System.out.println(" STD, DIRECT, 5 cycles, 2 Bytes, -aa0-");
				System.out.println(" Store Register Into Memory - 16 bits ");
				System.out
						.println(" Retrieve Accumulator D contents &\n place in consecutive memory locations.");
				index16bit = accD.getAccD();
				// convert to binary String
				String binString = Integer.toBinaryString((int) index16bit);
				// split into two bytes
				firstByte = binString.substring(0, 8);
				System.out.println(firstByte);
				String secondByte = binString.substring(8, 16);
				System.out.println(secondByte);
				// retrieve first memory location
				dataByte = memoryArray[progCounter + 1];
				// store consecutive bytes
				memoryArray[dataByte] = Integer.parseInt(firstByte);
				memoryArray[dataByte + 1] = Integer.parseInt(secondByte);
				System.out.println(" H unaffected \n"
						+ " N Set if bit 15 of stored data was set\n"
						+ " Z Set if all bits of stored data are clear \n "
						+ " V Cleared ");
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("DE")) {

				System.out.println(" LDU, DIRECT, 5 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// retrieve data byte from memory
				int uByte = memoryArray[progCounter + 2];
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				ccrFlagState.setBound_V(clear);
				// C unaffected
				// deal with 16 bit overflow
				if (uByte > 65535) {
					uByte = uByte - 65536;
					System.out.println("2's complement overflow");
				}
				userStack.setUSP(uByte);
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("DF")) {

				System.out.println(" STU, DIRECT, 4 cycles, 2 Bytes, uaaaa");
				// Store Register Into Memory - 16 bits
				// H unaffected
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E0")) {

				System.out.println(" SUBB, INDEXED, 4 cycles, 2 Bytes, uaaaa");
				// SUBB
				// Subtract Memory from Register - 8 bits
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E1")) {

				System.out.println(" CMPB, INDEXED, 4 cycles, 2 Bytes, uaaaa");
				// Compare memory from a Register - 8 bits
				// H undefined
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 8 bit two's complement overflow
				// C set if subtraction did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E2")) {

				System.out.println(" SBCB, INDEXED, 4 cycles, 2 Bytes, uaaaa");
				// Subtract with Borrow
				// H undefined
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Set if the operation causes an 8 bit 2's complement
				// overflow
				// C Set if the operation did NOT cause a carry from bit 7 in
				// the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E3")) {

				System.out
						.println(" ADDD, IMMEDIATE, 4 cycles, 3 Bytes, -aaaa\n"
								+ "Add memory into register - 16 bits");
				// H unaffected
				// N set if bit 15 of the result is set
				// Z set if all bits of result are clear
				// V set if 16 bit two's compliment arithmetic overflow caused
				// C set if carry caused by MSB from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E4")) {

				System.out.println(" ANDA, DIRECT, 4 cycles, 2 Bytes, -aa0-\n"
						+ "Logical AND memory into register");
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of the result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E5")) {

				System.out.println("BITB, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Bit test
				// H unaffected
				// N set if bit 7 of result is set
				// Z Set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E6")) {

				System.out.println(" LDB, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Load Register from memory - 8 bits
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E7")) {

				System.out.println(" STB, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Store Register Into Memory - 8 bits
				accByte = accB.getAccB();
				// derive effective address from contents of register
				opCode2 = memoryArray[progCounter + 1];
				if (opCode2 == 0) {
					effectiveAddress = xIndex.getX();
					System.out.println(" Effective Address = contents of X: "
							+ effectiveAddress);
				}
				if (opCode2 == 32) {
					System.out.println(" EA = contents of Y");
					effectiveAddress = yIndex.getY();
				}

				if (opCode2 == 64) {
					System.out.println(" EA = contents of U");
					effectiveAddress = userStack.getUSP();
				}

				if (opCode2 == 96) {
					System.out.println(" EA = contents of S");
					effectiveAddress = hardStack.getHSP();
				}
				// store contents of A to effective address
				setArrayData((int) effectiveAddress, accByte);
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E8")) {

				System.out.println("EORB, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("E9")) {

				System.out.println("ADCB, INDEXED, 4 cycles, 2 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("EA")) {

				System.out.println("ORB, INDEXED, 4 cycles, 2 Bytes, -aa0-");
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("EB")) {

				System.out.println(" ADDB, INDEXED, 4 cycles, 2 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("EC")) {

				System.out.println("DAA, INDEXED, 5 cycles, 2 Bytes, -aa0a");
				// Decimal Addition Adjust
				// H unaffected
				// N set if MSB of result is set
				// Z set if all bits of result are clear
				// V not defined
				// C set if the operation caused a carry from bit 7 in the ALU
				// or if the carry flag was set before the operation
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("ED")) {

				System.out.println(" STD, INDEXED, 5 cycles, 2 Bytes, -aa0a");
				System.out.println(" Store Register Into Memory - 16 bits ");
				System.out
						.println(" Retrieve Accumulator D contents &\n place in consecutive memory locations.");
				index16bit = accD.getAccD();
				// convert to binary String
				String binString = Integer.toBinaryString((int) index16bit);
				// split into two bytes
				firstByte = binString.substring(0, 8);
				System.out.println(firstByte);
				String secondByte = binString.substring(8, 16);
				System.out.println(secondByte);
				// retrieve first memory location
				dataByte = memoryArray[progCounter + 1];
				// store consecutive bytes
				memoryArray[dataByte] = Integer.parseInt(firstByte);
				memoryArray[dataByte + 1] = Integer.parseInt(secondByte);
				System.out.println(" H unaffected \n"
						+ " N Set if bit 15 of stored data was set\n"
						+ " Z Set if all bits of stored data are clear \n "
						+ " V Cleared ");
				ccrFlagState.setBound_V(clear);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("EE")) {

				System.out.println(" LDU, INDEXED, 5 cycles, 2 Bytes, -aa0a");
				// Load Register from memory - 16 bits
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("EF")) {

				System.out.println(" STU, INDEXED, 5 cycles, 2 Bytes, -aa0a");
				// Store Register Into Memory - 16 bits
				// H unaffected
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("F0")) {

				System.out.println(" SUBB, EXTENDED, 5 cycles, 3 Bytes, uaaaa");
				// SUBB
				// Subtract Memory from Register - 8 bits
				// H undefined
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of result are clear
				// V Set if the operation caused an 8 bit two's complement
				// overflow
				// C set if operation did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("F1")) {

				System.out.println(" CMPB, EXTENDED, 5 cycles, 3 Bytes, uaaaa");
				// Compare memory from a Register - 8 bits
				// H undefined
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V set if operation caused 8 bit two's complement overflow
				// C set if subtraction did NOT cause a carry from bit 7 in the
				// ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("F2")) {

				System.out.println("SBCB, EXTENDED, 5 cycles, 3 Bytes, uaaaa");
				// Subtract with Borrow
				// H undefined
				// N Set if bit 7 of result set
				// Z Set if all bits of the result are clear
				// V Set if the operation causes an 8 bit 2's complement
				// overflow
				// C Set if the operation did NOT cause a carry from bit 7 in
				// the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("F3")) {

				System.out
						.println(" ADDD, IMMEDIATE, 4 cycles, 3 Bytes, -aaaa\n"
								+ "Add memory into register - 16 bits");
				// H unaffected
				// N set if bit 15 of the result is set
				// Z set if all bits of result are clear
				// V set if 16 bit two's compliment arithmetic overflow caused
				// C set if carry caused by MSB from bit 7 in the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("F4")) {

				System.out.println("ANDA, DIRECT, 4 cycles, 2 Bytes, -aa0-\n"
						+ "Logical AND memory into register");
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of the result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("F5")) {

				System.out.println("BITB, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Bit test
				// H unaffected
				// N set if bit 7 of result is set
				// Z Set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 2
				progCounter = progCounter + 2;
			}

			else if (firstByte.equalsIgnoreCase("F6")) {

				System.out.println(" LDB, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Load Register from memory - 8 bits
				// H unaffected
				// N set if bit 7 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("F7")) {

				System.out.println(" STB, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Store Register Into Memory - 8 bits
				// H unaffected
				// N Set if bit 7 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("F8")) {

				System.out.println(" EORB, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Exclusive OR
				// H unaffected
				// N set if bit 7 of result is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("F9")) {

				System.out.println("ADCB, EXTENDED, 5 cycles, 3 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("FA")) {

				System.out.println("ORB, EXTENDED, 5 cycles, 3 Bytes, -aa0-");
				// Inclusive OR Memory into Register
				// H not affected
				// N Set if high order bits of result set
				// Z Set if all bits of the result are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Not affected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("FB")) {
				// extended ADDB
				System.out.println("ADDB, EXTENDED, 5 cycles, 3 Bytes, aaaaa");
				// H set if carry caused from bit 3 in the ALU
				// N set if bit 7 of the result is set
				// Z set if all bits of result are clear
				// V set if 8 bit two's compliment arithmetic overflow caused
				// C set if carry caused from bit 7 in the ALU
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("FC")) {

				System.out.println(" LDD, EXTENDED, 6 cycles, 3 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("FD")) {

				System.out.println("STD, EXTENDED, 6 cycles, 3 Bytes, -aa0-");
				// Store Register Into Memory - 16 bits
				// H unaffected
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("FE")) {

				System.out.println(" LDU, EXTENDED, 6 cycles, 3 Bytes, -aa0-");
				// Load Register from memory - 16 bits
				// H unaffected
				// N set if bit 15 of loaded data is set
				// Z set if all bits of result are clear
				// V cleared
				// ccrFlagState.setVBit(false);
				// C unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			else if (firstByte.equalsIgnoreCase("FF")) {

				System.out.println("STU, EXTENDED, 6 cycles, 3 Bytes, -aa0-");
				// Store Register Into Memory - 16 bits
				// H unaffected
				// N Set if bit 15 of stored data was set
				// Z Set if all bits of stored data are clear
				// V Cleared
				// ccrFlagState.setVBit(false);
				// C Unaffected
				// increment PC by 3
				progCounter = progCounter + 3;
			}

			// exit loop with error message
			else {
				System.out.println(" End of legal instructions");
				JOptionPane.showMessageDialog(
						null,
						"Error - Illegal op code encountered. \nLast op code: "
								+ String.format("%02x", opCode)
								+ ", from memory location: "
								+ String.format("%04x", progCounter));
				emuRunning = false;
			}
			// check for interrupts

			// set program counter
			progCountClass.setPC(progCounter);
		}
	}

	/**
	 * Method to handle Expanding OpCode - Page 1
	 * 
	 * @param oc2 - the second byte of the opcode
	 */
	public void p1Codes(String oc2) {

		if (oc2.equalsIgnoreCase("21")) {
			System.out.println(" LBRN, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch Never
			// CCR unaffected
			// Causes no branch
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("22")) {
			System.out.println("LBHI, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch if Higher
			// CCR unaffected
			// Causes branch if previous operation caused neither a carry nor a
			// zero result
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("23")) {
			System.out.println("LBLS, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Lower or Same
			// CCR unaffected
			// Causes a branch if the previous operation caused either a carry
			// or zero result
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("24")) {
			System.out
					.println("LBHS/LBCC, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch if Higher or same
			// CCR unaffected
			// BCC
			// Branch on Carry Clear
			// CCR unaffected
			// Checks C bit and causes a branch if C is clear
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("25")) {
			System.out
					.println("LBLO/LBCS, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Lower
			// Branch on Carry Set
			// CCR unaffected
			// Checks C bit and causes a branch if C is set
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("26")) {
			System.out.println("LBNE, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch Not Equal
			// CCR unaffected
			// Causes a branch if Z bit is clear
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("27")) {

			System.out.println("LBEQ, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Equal
			// CCR unaffected
			// Checks Z bit and causes a branch if Z is set
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("28")) {
			System.out.println("LBVC, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Overflow Clear
			// CCR unaffected
			// Causes a branch if the V bit is clear
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("29")) {
			System.out.println("LBVS, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Overflow Set
			// CCR unaffected
			// Causes a branch if the V bit is set
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("2A")) {
			System.out.println("LBPL, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Plus
			// CCR unaffected
			// Causes a branch if N bit is clear
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("2B")) {
			System.out.println("LBMI, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Minus
			// CCR unaffected
			// Causes a branch if N is set
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("2C")) {
			System.out.println(" LBGE, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Greater Than or Equal to Zero
			// CCR unaffected
			// Checks N & V bits and causes a branch if both are either set or
			// clear
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("2D")) {
			System.out.println(" LBLT, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Less than Zero
			// CCR unaffected
			// Causes a branch if either but not both of the N or V bits is 1
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("2E")) {
			System.out.println(" LBGT, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Greater
			// CCR unaffected
			// Checks N & V bits and causes a branch if both are either set
			// or clear and Z is clear
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("2F")) {
			System.out.println(" LBLE, RELATIVE, 5(6) cycles, 4 Bytes, -----");
			// Branch on Less than or Equal to zero
			// CCR unaffected
			// Causes branch if the XOR of the N & V bits is 1 or if Z = 1
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("3F")) {
			System.out.println(" SWI2, INHERENT, 20 cycles, 2 Bytes, -----");
			// Software Interrupt 2
			// CCR unaffected
			// increment PC by 1 as first byte dealt with above
			progCounter = progCounter + 1;
			// exit emulation
			emuRunning = false;
		}

		else if (oc2.equalsIgnoreCase("83")) {
			System.out.println(" CMPD, IMMEDIATE, 5 cycles, 4 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("8C")) {
			System.out.println(" CMPY, IMMEDIATE, 5 cycles, 4 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("8E")) {
			System.out.println(" LDY, IMMEDIATE, 4 cycles, 4 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// retrieve both bytes from memory
			int yByte = memoryArray[progCounter + 2];
			int yByte2 = memoryArray[progCounter + 3];
			// convert to string for concatenation
			String yStringByte1 = String.format("%02x", yByte);
			String yStringByte2 = String.format("%02x", yByte2);
			String newY = yStringByte1 + yStringByte2;
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// deal with 16 bit overflow
			if (yByte > 65535) {
				yByte = yByte - 65536;
				System.out.println(" 2's complement overflow");
			}
			// set index x to xByte
			yIndex.setY(Integer.parseInt(newY, 16));
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("93")) {
			System.out.println(" CMPD, DIRECT, 7 cycles, 3 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("9C")) {
			System.out.println(" CMPY, DIRECT, 7 cycles, 3 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("9E")) {
			System.out.println(" LDY, DIRECT, 6 cycles, 3 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("9F")) {
			System.out.println(" STY, DIRECT, 6 cycles, 3 Bytes, -aa0-");
			// // Store Register Into Memory - 16 bits
			long yByte = yIndex.getY();
			int addressByte = memoryArray[progCounter + 1];
			setArrayData(addressByte, (int) yByte);
			// H unaffected
			// N Set if bit 15 of stored data was set
			// Z Set if all bits of stored data are clear
			// V Cleared
			ccrFlagState.setBound_V(clear);
			// C Unaffected
			// increment PC by 2
			progCounter = progCounter + 2;
			// // C Unaffected
		}

		else if (oc2.equalsIgnoreCase("A3")) {
			System.out.println(" CMPD, INDEXED, 7 cycles, 3 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("AC")) {
			System.out.println(" CMPY, INDEXED, 7 cycles, 3 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("AE")) {
			System.out.println("LDY, INDEXED, 6 cycles, 3 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("AF")) {
			System.out.println(" STY, INDEXED, 6 cycles, 3 Bytes, -aa0-");
			// Store Register Into Memory - 16 bits
			// H unaffected
			// N Set if bit 15 of stored data was set
			// Z Set if all bits of stored data are clear
			// V Cleared
			ccrFlagState.setBound_V(clear);
			// C Unaffected
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("B3")) {
			System.out.println(" CMPD, EXTENDED, 8 cycles, 4 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("BC")) {
			System.out.println(" CMPY, EXTENDED, 8 cycles, 4 Bytes, -aaaa");
			// Compare memory from a Register - 16 bits
			// H unaffected
			// N set if bit 15 of result is set
			// Z set if all bits of result are clear
			// V set if operation caused 16 bit two's complement overflow
			// C set if operation on the MSB did NOT cause a carry from bit 7 in
			// the ALU
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("BE")) {
			System.out.println(" LDY, EXTENDED, 7 cycles, 4 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("BF")) {
			System.out.println(" STY, EXTENDED, 7 cycles, 4 Bytes, -aa0-");
			// Store Register Into Memory - 16 bits
			// H unaffected
			// N Set if bit 15 of stored data was set
			// Z Set if all bits of stored data are clear
			// V Cleared
			ccrFlagState.setBound_V(clear);
			// C Unaffected
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("CE")) {
			System.out.println(" LDS, IMMEDIATE, 4 cycles, 4 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// retrieve both bytes from memory
			int sByte = memoryArray[progCounter + 2];
			int sByte2 = memoryArray[progCounter + 3];

			// convert to string for concatenation
			String sStringByte1 = String.format("%02x", sByte);
			String sStringByte2 = String.format("%02x", sByte2);
			String newS = sStringByte1 + sStringByte2;
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// deal with 16 bit overflow
			int newStack = Integer.parseInt(newS, 16);
			if (newStack > 65535) {
				newStack = newStack - 65536;
				System.out.println(" 2's complement overflow");// TODO doesn't
			}
			// set stack to sByte
			hardStack.setHSP(newStack);
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("DE")) {
			System.out.println(" LDS, DIRECT, 6 cycles, 3 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// retrieve data byte from memory
			int sByte = memoryArray[progCounter + 2];
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// deal with 16 bit overflow
			if (sByte > 65535) {
				sByte = sByte - 65536;
			}
			// set stack to sByte
			hardStack.setHSP(sByte);
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("DF")) {
			System.out.println(" STS, DIRECT, 6 cycles, 3 Bytes, -aa0-");
			// Store Register Into Memory - 16 bits
			// H unaffected
			// N Set if bit 15 of stored data was set
			// Z Set if all bits of stored data are clear
			// V Cleared
			ccrFlagState.setBound_V(clear);
			// C Unaffected
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("EE")) {
			System.out.println(" LDS, INDEXED, 6 cycles, 3 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// retrieve data byte from memory
			// int sByte = myArray[progCounter + 2];
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("EF")) {
			System.out.println(" STS, INDEXED, 6 cycles, 3 Bytes, -aa0-");
			// Store Register Into Memory - 16 bits
			// H unaffected
			// N Set if bit 15 of stored data was set
			// Z Set if all bits of stored data are clear
			// V Cleared
			ccrFlagState.setBound_V(clear);
			// C Unaffected
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		else if (oc2.equalsIgnoreCase("FE")) {
			System.out.println(" LDS, EXTENDED, 7 cycles, 4 Bytes, -aa0-");
			// Load Register from memory - 16 bits
			// H unaffected
			// N set if bit 15 of loaded data is set
			// Z set if all bits of result are clear
			// V cleared
			ccrFlagState.setBound_V(clear);
			// C unaffected
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else if (oc2.equalsIgnoreCase("FF")) {
			System.out.println(" STS, EXTENDED, 7 cycles, 4 Bytes, -aa0-");
			// Store Register Into Memory - 16 bits
			// H unaffected
			// N Set if bit 15 of stored data was set
			// Z Set if all bits of stored data are clear
			// V Cleared
			ccrFlagState.setBound_V(clear);
			// C Unaffected
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		else {
			System.out.println("End of legal instructions");
			JOptionPane.showMessageDialog(
					null,
					"Error - Illegal op code encountered. \nLast op code: "
							+ String.format("%02x", opCode)
							+ ", from memory location: "
							+ String.format("%04x", progCounter));
			emuRunning = false;
		}
		progCountClass.setPC(progCounter);
	}

	/**
	 * Method to handle Expanding OpCode - Page 2
	 * 
	 * @param oc3 - the second byte of the op code
	 */
	public void p2Codes(String oc3) {

		System.out.println("");

		if (oc3.equalsIgnoreCase("3F")) {

			System.out.println(" SWI3, INHERENT, 20 cycles, 2 Bytes, -----");
			// Software Interrupt 3
			// CCR not affected
			// increment PC by 1 as first byte dealt with above
			progCounter = progCounter + 1;
			// exit emulation
			emuRunning = false;
		}

		if (oc3.equalsIgnoreCase("83")) {

			System.out.println(" CMPU, IMMEDIATE, 5 cycles, 4 Bytes, -aaaa");
			// Store Register Into Memory - 16 bits
			// H unaffected
			// N Set if bit 15 of stored data was set
			// Z Set if all bits of stored data are clear
			// V Cleared
			ccrFlagState.setBound_V(clear);
			// C Unaffected
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		if (oc3.equalsIgnoreCase("8C")) {

			System.out.println(" CMPS, IMMEDIATE, 5 cycles, 4 Bytes, -aaaa");
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		if (oc3.equalsIgnoreCase("93")) {

			System.out.println(" CMPU, DIRECT, 7 cycles, 3 Bytes, -aaaa");
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		if (oc3.equalsIgnoreCase("9C")) {

			System.out.println(" CMPS, DIRECT, 7 cycles, 3 Bytes, -aaaa");
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		if (oc3.equalsIgnoreCase("A3")) {

			System.out.println(" CMPU, INDEXED, 7 cycles, 3 Bytes, -aaaa");
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		if (oc3.equalsIgnoreCase("AC")) {

			System.out.println(" CMPS, INDEXED, 7 cycles, 3 Bytes, -aaaa");
			// increment PC by 2 as first byte dealt with above
			progCounter = progCounter + 2;
		}

		if (oc3.equalsIgnoreCase("B3")) {

			System.out.println(" CMPU, EXTENDED, 8 cycles, 4 Bytes, -aaaa");
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}

		if (oc3.equalsIgnoreCase("BC")) {

			System.out.println(" CMPS, EXTENDED, 8 cycles, 4 Bytes, -aaaa");
			// increment PC by 3 as first byte dealt with above
			progCounter = progCounter + 3;
		}
	}

	/**
	 * method to handle pushing registers onto hardware stack
	 * 
	 * @param s - int representing the 8 bit permutation of registers to be pushed
	 */
	public void pushHard(int s) {
		dataByte = s;
		// check for all 255 permutations
		if (dataByte == 1) {
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
		}

		else if (dataByte == 2) {
			// A
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 3) {
			// ccr, A
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 4) {
			// B
			dataByte = accB.getAccB();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 5) {
			// ccr, B
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accB.getAccB();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 6) {
			// a, b
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
		}

		else if (dataByte == 7) {
			// ccr,a,b (or ccr,d)
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
		}

		else if (dataByte == 8) {
			// dp
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 9) {
			// ccr, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 11) {
			// ccr, a, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 12) {
			// b,dp
			dataByte = accB.getAccB();
			hardStack.hardPush(dataByte);
			dataByte2 = dirPage.getDP();
			hardStack.hardPush(dataByte2);
		}

		else if (dataByte == 13) {
			// ccr, b, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);

		}

		else if (dataByte == 14) {
			// d,dp
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
		}

		else if (dataByte == 15) {
			// ccr, a, b, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);

		}

		else if (dataByte == 16) {
			// x
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 17) {
			// ccr, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 18) {
			// a,x
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 19) {
			// ccr, a, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 20) {
			// b,x
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 21) {
			// ccr, b, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 22) {
			// a,b,x
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 23) {
			// ccr, d, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 24) {
			// dp, x
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 25) {
			// ccr, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 26) {
			// a,dp,x
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 27) {
			// ccr, a, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 28) {
			// b,dp,x
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 29) {
			// ccr, b, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 30) {
			// d, dp, x
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 31) {
			// ccr, d, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 32) {
			// y
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 33) {
			// ccr, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 34) {
			// a,y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 35) {
			// ccr, a, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 36) {
			// b,y
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 37) {
			// ccr, b, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 38) {
			// a,b,y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 39) {
			// ccr, d, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 40) {
			// dp, y
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 41) {
			// ccr, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 42) {
			// a,dp,y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 43) {
			// ccr, a, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 44) {
			// b,dp,y
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 45) {
			// ccr, b, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 46) {
			// d, dp, y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 47) {
			// ccr, d, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 48) {
			// x,y
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 49) {
			// ccr, x, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 50) {
			// a, x, y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 51) {
			// cc,a,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 52) {
			// b, x, y
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 53) {
			// cc,b,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 54) {
			// d,x,y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 55) {
			// cc,a,b,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 56) {
			// dp,x,y
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 57) {
			// ccr, dp, x, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 58) {
			// a,dp,x,y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 59) {
			// cc,a,dp,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 60) {
			// b,dp,x,y
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 61) {
			// cc,b,dp,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 62) {
			// d, dp, x, y
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 63) {
			// ccr, d, dp, x, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 64) {
			// user stack
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 65) {
			// ccr, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 66) {
			// d, x, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 67) {
			// ccr, a, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 68) {
			// b,u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 69) {
			// ccr, b, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 70) {
			// a,b,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 71) {
			// cc,a,b,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 72) {
			// dp, u
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 73) {
			// ccr, dp, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 74) {
			// a,dp,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 75) {
			// cc,a,dp,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 76) {
			// b,dp,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 77) {
			// cc,b,dp,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 78) {
			// d, dp, u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 79) {
			// cc,a,b,dp,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 80) {
			// x,u
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 81) {
			// ccr, x, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 82) {
			// a, x, u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 83) {
			// cc, a, x, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 84) {
			// b, x, u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 85) {
			// cc, b, x, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 86) {
			// d,x,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 87) {
			// cc,a,b,x,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 88) {
			// dp,x,u
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 89) {
			// cc,dp,x,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 90) {
			// a,dp,x,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 91) {
			// cc,a,dp,x,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 92) {
			// b,dp,x,u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 93) {
			// cc,b,dp,x,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 94) {
			// a,b,dp,x,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 95) {
			// cc,a,b,dp,x,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 96) {
			// y,u
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 97) {
			// ccr, y, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 98) {
			// a, y, u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 99) {
			// cc, a, y, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 100) {
			// b, y, u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 101) {
			// cc, b, y, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 102) {
			// d, y, u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 103) {
			// cc,a,b,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 104) {
			// dp,y,u
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 105) {
			// cc,dp,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 106) {
			// a,dp,y,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 107) {
			// cc,a,dp,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 108) {
			// b,dp,y,u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 109) {
			// cc,b,dp,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 110) {
			// a,b,dp,y,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 111) {
			// cc,a,b,dp,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 112) {
			// x, y, u
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 113) {
			// cc,x,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 114) {
			// a,x,y,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 115) {
			// cc,a,x,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 116) {
			// b,x,y,u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 117) {
			// cc,b,x,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 118) {
			// d, x, y, u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 119) {
			// cc,a,b,x,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 120) {
			// dp,x,y,u
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 121) {
			// ccr, dp, x, y, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 122) {
			// a,dp,x,y,u
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 123) {
			// cc,a,dp,x,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 124) {
			// b,dp,x,y,u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 125) {
			// cc,b,dp,x,y,u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 126) {
			// d, dp, x, y, u
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 127) {
			// ccr, d, dp, x, y, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
		}

		else if (dataByte == 128) {
			// pc
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 129) {
			// ccr, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 130) {
			// a,pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 131) {
			// ccr, a, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 132) {
			// b, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 133) {
			// ccr, b, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 134) {
			// a,b,pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 135) {
			// ccr, d, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 136) {
			// dp, pc
			dataByte = dirPage.getDP();
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 137) {
			// ccr, dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 138) {
			// a,dp, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 139) {
			// cc,a,dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 140) {
			// b,dp, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 141) {
			// cc,b,dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 142) {
			// a,b,dp, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 143) {
			// cc,a,b,dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 144) {
			// x, pc
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 145) {
			// ccr, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 146) {
			// a, x, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 147) {
			// cc, a, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 148) {
			// b, x, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 149) {
			// cc, b, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 150) {
			// d, x, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 151) {
			// cc, d, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 152) {
			// dp, x, pc
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 153) {
			// cc, dp, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 154) {
			// a, dp, x, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 155) {
			// cc,a,dp,x,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 156) {
			// b, dp, x, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 157) {
			// cc, b, dp, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 158) {
			// a, b, dp, x, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 159) {
			// cc, a, b, dp, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 160) {
			// y, pc
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 161) {
			// ccr, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 162) {
			// a, y, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 163) {
			// cc, a, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 164) {
			// b, y, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 165) {
			// cc, b, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 166) {
			// d, y, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 167) {
			// cc, d, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 168) {
			// dp, y, pc
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 169) {
			// cc,dp,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 170) {
			// a,dp,y, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 171) {
			// cc,a,dp,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 172) {
			// b,dp,y,pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 173) {
			// cc,b,dp,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 174) {
			// d, dp, y, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 175) {
			// cc,d, dp,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 176) {
			// x, y, pc
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 177) {
			// cc, x, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 178) {
			// a, x, y, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 179) {
			// cc,a, x,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 180) {
			// b, x, y, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 181) {
			// b, x,y, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 182) {
			// a,b,x,y, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 183) {
			// cc,a,b,x,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 184) {
			// dp, x, y, pc
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 185) {
			// cc,dp,x,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 186) {
			// a,dp,x,y, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 187) {
			// cc,a,dp,x,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 188) {
			// b,dp,x,y, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 189) {
			// cc,b,dp,x,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 190) {
			// d, dp, x, y, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 191) {
			// ccr, d, dp, x, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 192) {
			// u, pc
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 193) {
			// ccr, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 194) {
			// a,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 195) {
			// cc,a,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 196) {
			// b,u, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 197) {
			// cc,b,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 198) {
			// d, u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 199) {
			// cc,a,b,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 200) {
			// dp, u, pc
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 201) {
			// cc,dp,u,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 202) {
			// a,dp,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 203) {
			// cc,a,dp,u,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 204) {
			// b,dp,u, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 205) {
			// cc,b,dp,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 206) {
			// a,b,dp,u,pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 207) {
			// cc,a,b,dp,u,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 208) {
			// x,u, pc
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 209) {
			// cc, x, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 210) {
			// a, x, u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 211) {
			// cc, a, x, pc, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 212) {
			// b, x, u, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 213) {
			// cc, b, x, pc, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 214) {
			// a,b,x,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 215) {
			// cc,a,b,x,u,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 216) {
			// dp, x, u, pc
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 217) {
			// cc,dp,x,u,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 218) {
			// a,dp,x,u,pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 219) {
			// cc,a,dp,x,u,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 220) {
			// b,dp,x,u,pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 221) {
			// cc,b,dp,x,u,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 222) {
			// d, dp,u x, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 223) {
			// ccr, d, dp, x, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 224) {
			// y,u, pc
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 225) {
			// cc,y,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 226) {
			// a,y,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 227) {
			// cc, a, y, pc, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 228) {
			// b,y,u, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 229) {
			// cc, b, y, pc, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 230) {
			// a,b,y,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 231) {
			// cc,d,y,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 232) {
			// dp,y,u, pc
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 233) {
			// cc,dp,y,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 234) {
			// a,dp,y,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 235) {
			// cc,a,dp,y,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 236) {
			// b,dp,y,u, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 237) {
			// cc,b,dp,y,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 238) {
			// d, dp, u, y, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 239) {
			// ccr, d, dp, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 240) {
			// x,y,u, pc
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 241) {
			// cc, x, y,u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 242) {
			// a,x,y,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 243) {
			// cc, a, x, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 244) {
			// b,x,y,u, pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 245) {
			// cc, b, x, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 246) {
			// d, x, y,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 247) {
			// ccr, d, x, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 248) {
			// dp, x, y,u, pc
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 249) {
			// ccr, dp, x, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 250) {
			// a,dp,x,y,u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 251) {
			// ccr, a, dp, x, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 252) {
			// b,dp,x,y,u,pc
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 253) {
			// ccr, b, dp, x, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 254) {
			// d, dp, x, y, u, pc
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}

		else if (dataByte == 255) {
			// ccr, d, dp, x, y, u, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			hardStack.hardPush(c);
			hardStack.hardPush(v);
			hardStack.hardPush(z);
			hardStack.hardPush(n);
			hardStack.hardPush(i);
			hardStack.hardPush(h);
			hardStack.hardPush(f);
			hardStack.hardPush(e);
			dataByte = accA.getAccA();
			hardStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			hardStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			hardStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			hardStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			hardStack.hardPush((int) index16bit);
			index16bit = userStack.getUSP();
			hardStack.hardPush((int) index16bit);
			hardStack.hardPush(progCounter);
		}
	}

	/**
	 * method to handle pushing registers onto user stack
	 * 
	 * @param s - int representing the 8 bit permutation of registers to be pushed
	 */
	public void pushUser(int s) {

		dataByte = s;
		// check for all 255 permutations
		if (dataByte == 1) {
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
		}

		else if (dataByte == 2) {
			// A
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 3) {
			// ccr, A
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 4) {
			// B
			dataByte = accB.getAccB();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 5) {
			// ccr, B
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accB.getAccB();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 6) {
			// a, b
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
		}

		else if (dataByte == 7) {
			// ccr,a,b (or ccr,d)
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
		}

		else if (dataByte == 8) {
			// dp
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 9) {
			// ccr, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 11) {
			// ccr, a, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 12) {
			// b,dp
			dataByte = accB.getAccB();
			userStack.hardPush(dataByte);
			dataByte2 = dirPage.getDP();
			userStack.hardPush(dataByte2);
		}

		else if (dataByte == 13) {
			// ccr, b, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);

		}

		else if (dataByte == 14) {
			// d,dp
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
		}

		else if (dataByte == 15) {
			// ccr, a, b, dp
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);

		}

		else if (dataByte == 16) {
			// x
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 17) {
			// ccr, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 18) {
			// a,x
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 19) {
			// ccr, a, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 20) {
			// b,x
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 21) {
			// ccr, b, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 22) {
			// a,b,x
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 23) {
			// ccr, d, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 24) {
			// dp, x
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 25) {
			// ccr, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 26) {
			// a,dp,x
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 27) {
			// ccr, a, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 28) {
			// b,dp,x
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 29) {
			// ccr, b, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 30) {
			// d, dp, x
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 31) {
			// ccr, d, dp, x
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 32) {
			// y
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 33) {
			// ccr, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 34) {
			// a,y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 35) {
			// ccr, a, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 36) {
			// b,y
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 37) {
			// ccr, b, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 38) {
			// a,b,y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 39) {
			// ccr, d, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 40) {
			// dp, y
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 41) {
			// ccr, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 42) {
			// a,dp,y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 43) {
			// ccr, a, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 44) {
			// b,dp,y
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 45) {
			// ccr, b, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 46) {
			// d, dp, y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 47) {
			// ccr, d, dp, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 48) {
			// x,y
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 49) {
			// ccr, x, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 50) {
			// a, x, y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 51) {
			// cc,a,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 52) {
			// b, x, y
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 53) {
			// cc,b,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 54) {
			// d,x,y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 55) {
			// cc,a,b,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 56) {
			// dp,x,y
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 57) {
			// ccr, dp, x, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 58) {
			// a,dp,x,y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 59) {
			// cc,a,dp,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 60) {
			// b,dp,x,y
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 61) {
			// cc,b,dp,x,y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 62) {
			// d, dp, x, y
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 63) {
			// ccr, d, dp, x, y
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 64) {
			// hw stack
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 65) {
			// ccr, u
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 66) {
			// d, x, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 67) {
			// ccr, a, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 68) {
			// b,h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 69) {
			// ccr, b, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 70) {
			// a,b,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 71) {
			// cc,a,b,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 72) {
			// dp, h
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 73) {
			// ccr, dp, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 74) {
			// a,dp,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 75) {
			// cc,a,dp,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 76) {
			// b,dp,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 77) {
			// cc,b,dp,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 78) {
			// d, dp, h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 79) {
			// cc,a,b,dp,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 80) {
			// x,h
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 81) {
			// ccr, x, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 82) {
			// a, x, h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 83) {
			// cc, a, x, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 84) {
			// b, x, h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 85) {
			// cc, b, x, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 86) {
			// d,x,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 87) {
			// cc,a,b,x,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 88) {
			// dp,x,h
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 89) {
			// cc,dp,x,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 90) {
			// a,dp,x,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 91) {
			// cc,a,dp,x,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 92) {
			// b,dp,x,h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 93) {
			// cc,b,dp,x,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 94) {
			// a,b,dp,x,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 95) {
			// cc,a,b,dp,x,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 96) {
			// y,h
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 97) {
			// ccr, y, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 98) {
			// a, y, h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 99) {
			// cc, a, y, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 100) {
			// b, y, h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 101) {
			// cc, b, y, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 102) {
			// d, y, h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 103) {
			// cc,a,b,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 104) {
			// dp,y,h
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 105) {
			// cc,dp,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 106) {
			// a,dp,y,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 107) {
			// cc,a,dp,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 108) {
			// b,dp,y,h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 109) {
			// cc,b,dp,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 110) {
			// a,b,dp,y,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 111) {
			// cc,a,b,dp,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 112) {
			// x, y, h
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 113) {
			// cc,x,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 114) {
			// a,x,y,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 115) {
			// cc,a,x,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 116) {
			// b,x,y,h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 117) {
			// cc,b,x,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 118) {
			// d, x, y, h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 119) {
			// cc,a,b,x,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 120) {
			// dp,x,y,h
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 121) {
			// ccr, dp, x, y, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 122) {
			// a,dp,x,y,h
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 123) {
			// cc,a,dp,x,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 124) {
			// b,dp,x,y,h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 125) {
			// cc,b,dp,x,y,h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 126) {
			// d, dp, x, y, h
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 127) {
			// ccr, d, dp, x, y, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
		}

		else if (dataByte == 128) {
			// pc
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 129) {
			// ccr, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 130) {
			// a,pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 131) {
			// ccr, a, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 132) {
			// b, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 133) {
			// ccr, b, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 134) {
			// a,b,pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 135) {
			// ccr, d, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 136) {
			// dp, pc
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 137) {
			// ccr, dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 138) {
			// a,dp, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 139) {
			// cc,a,dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 140) {
			// b,dp, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 141) {
			// cc,b,dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 142) {
			// a,b,dp, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 143) {
			// cc,a,b,dp, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 144) {
			// x, pc
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 145) {
			// ccr, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 146) {
			// a, x, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 147) {
			// cc, a, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 148) {
			// b, x, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 149) {
			// cc, b, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 150) {
			// d, x, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 151) {
			// cc, d, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 152) {
			// dp, x, pc
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 153) {
			// cc, dp, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 154) {
			// a, dp, x, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 155) {
			// cc,a,dp,x,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 156) {
			// b, dp, x, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 157) {
			// cc, b, dp, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 158) {
			// a, b, dp, x, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 159) {
			// cc, a, b, dp, x, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 160) {
			// y, pc
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 161) {
			// ccr, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 162) {
			// a, y, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 163) {
			// cc, a, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 164) {
			// b, y, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 165) {
			// cc, b, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 166) {
			// d, y, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 167) {
			// cc, d, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 168) {
			// dp, y, pc
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 169) {
			// cc,dp,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 170) {
			// a,dp,y, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 171) {
			// cc,a,dp,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 172) {
			// b,dp,y,pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 173) {
			// cc,b,dp,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 174) {
			// d, dp, y, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 175) {
			// cc,d, dp,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 176) {
			// x, y, pc
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 177) {
			// cc, x, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 178) {
			// a, x, y, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 179) {
			// cc,a, x,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 180) {
			// b, x, y, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 181) {
			// b, x,y, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 182) {
			// a,b,x,y, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 183) {
			// cc,a,b,x,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 184) {
			// dp, x, y, pc
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 185) {
			// cc,dp,x,y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 186) {
			// a,dp,x,y, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 187) {
			// cc,a,dp,x,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 188) {
			// b,dp,x,y, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 189) {
			// cc,b,dp,x,y,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 190) {
			// d, dp, x, y, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 191) {
			// ccr, d, dp, x, y, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 192) {
			// h, pc
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 193) {
			// ccr, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 194) {
			// a, h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 195) {
			// cc,a,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 196) {
			// b,h, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 197) {
			// cc,b,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 198) {
			// d, h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 199) {
			// cc,a,b,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 200) {
			// dp, h, pc
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 201) {
			// cc,dp,h,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 202) {
			// a,dp,h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 203) {
			// cc,a,dp,h,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 204) {
			// b,dp,h, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 205) {
			// cc,b,dp,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 206) {
			// a,b,dp,h,pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 207) {
			// cc,a,b,dp,h,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 208) {
			// x,h, pc
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 209) {
			// cc, x, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 210) {
			// a, x, h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 211) {
			// cc, a, x, pc, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 212) {
			// b, x, h, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 213) {
			// cc, b, x, pc, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 214) {
			// a,b,x,h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 215) {
			// cc,a,b,x,h,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 216) {
			// dp, x, h, pc
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 217) {
			// cc,dp,x,h,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 218) {
			// a,dp,x,h,pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 219) {
			// cc,a,dp,x,h,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 220) {
			// b,dp,x,h,pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 221) {
			// cc,b,dp,x,h,pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 222) {
			// d, dp, h, x, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 223) {
			// ccr, d, dp, x, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 224) {
			// y, h, pc
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 225) {
			// cc,y,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 226) {
			// a,y,h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 227) {
			// cc, a, y, pc, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 228) {
			// b,y,h, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 229) {
			// cc, b, y, pc, h
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 230) {
			// a,b,y,h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 231) {
			// cc,d,y,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 232) {
			// dp,y,h, pc
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 233) {
			// cc,dp,y,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 234) {
			// a,dp,y,h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 235) {
			// cc,a,dp,y,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 236) {
			// b,dp,y,h, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 237) {
			// cc,b,dp,y,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 238) {
			// d, dp, h, y, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 239) {
			// ccr, d, dp, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 240) {
			// x,y,h, pc
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 241) {
			// cc, x, y,h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 242) {
			// a,x,y,h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 243) {
			// cc, a, x, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 244) {
			// b,x,y,h, pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 245) {
			// cc, b, x, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 246) {
			// d, x, y, h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 247) {
			// ccr, d, x, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 248) {
			// dp, x, y, h, pc
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 249) {
			// ccr, dp, x, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 250) {
			// a,dp,x,y,h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 251) {
			// ccr, a, dp, x, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 252) {
			// b,dp,x,y,h,pc
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 253) {
			// ccr, b, dp, x, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 254) {
			// d, dp, x, y, h, pc
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}

		else if (dataByte == 255) {
			// ccr, d, dp, x, y, h, pc
			// CC push order - C, V, Z, N, I, H, F, E
			int c = Integer.parseInt(ccrFlagState.getBound_C());
			int v = Integer.parseInt(ccrFlagState.getBound_V());
			int z = Integer.parseInt(ccrFlagState.getBound_Z());
			int n = Integer.parseInt(ccrFlagState.getBound_N());
			int i = Integer.parseInt(ccrFlagState.getBound_I());
			int h = Integer.parseInt(ccrFlagState.getBound_H());
			int f = Integer.parseInt(ccrFlagState.getBound_F());
			int e = Integer.parseInt(ccrFlagState.getBound_E());
			userStack.hardPush(c);
			userStack.hardPush(v);
			userStack.hardPush(z);
			userStack.hardPush(n);
			userStack.hardPush(i);
			userStack.hardPush(h);
			userStack.hardPush(f);
			userStack.hardPush(e);
			dataByte = accA.getAccA();
			userStack.hardPush(dataByte);
			dataByte2 = accB.getAccB();
			userStack.hardPush(dataByte2);
			dataByte = dirPage.getDP();
			userStack.hardPush(dataByte);
			index16bit = xIndex.getX();
			userStack.hardPush((int) index16bit);
			index16bit = yIndex.getY();
			userStack.hardPush((int) index16bit);
			index16bit = hardStack.getHSP();
			userStack.hardPush((int) index16bit);
			userStack.hardPush(progCounter);
		}
	}
}
