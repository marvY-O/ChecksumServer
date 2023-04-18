package Machine;
import java.util.zip.CRC32;

public class Checksum {
	public static long computeChecksum(byte[] bytes) {
	    CRC32 crc = new CRC32();
	    crc.update(bytes);
	    return crc.getValue();
	}
	
	public static boolean verifyChecksum(byte[] bytes, long expectedChecksum) {
	    CRC32 crc = new CRC32();
	    crc.update(bytes);
	    long actualChecksum = crc.getValue();
	    return actualChecksum == expectedChecksum;
	}
	
}
