/**
 * This class handles Motorola S-Record files which take the following format
 * 
 * +-------------------//------------------//-----------------------+
 * | Type | Count | Address  |            Data           | Checksum |
 * +-------------------//------------------//-----------------------+
 *
 * The Type field contains two characters which describe the type of record, i.e. S0-S9
 * 
 *  The Count field contains two hexadecimal characters representing the count of
 *  remaining character pairs in the record
 *  The Address field contains 4, 6, or 8 hexadecimal characters representing the
 *  address at which the data field is to be loaded into memory 
 *  The length of the field depends on the number of bytes necessary to hold the address 
 *  A 2-byte address uses 4 characters, a 3-byte address uses 6 characters, and a 
 *  4-byte address uses 8 characters 
 *  The Data field contains between 0 and 64 hexadecimal characters representing the 
 *  memory loadable data or descriptive information 
 *  The Checksum field contains two hexadecimal characters representing the checksum, 
 *  which provides a means of detecting data which has been corrupted during transmission
 *  Each S-record is terminated with a line feed and there are nine possible types, S0-S9
 *  
 * @author Robert Wilson
 *
 */
public class S_Record {

}
