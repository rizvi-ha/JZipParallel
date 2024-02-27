import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.CRC32;
import java.nio.ByteOrder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JZipParallel {   
    public static void main(String[] args) throws IOException, InterruptedException{

        //Write Header before we do anything
        System.out.write(new byte[] {
            (byte) 0x1f,
            (byte) 0x8b,
            Deflater.DEFLATED,
            0, // Flags
            0, 0, 0, 0, //File Modified Time
            0, // Extra Flags
            3, // OS Flag
        });

        
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numThreads = availableProcessors;
        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i])) {
                if (i + 1 < args.length) {
                    try {
                        numThreads = Integer.parseInt(args[i + 1]);
                        if (numThreads > 4 * availableProcessors) {
                            System.err.println("Error: Cannot use more than four times the number of available processors.");
                            System.exit(1);
                        }
                        i++; // Skip next argument since it's the value for -p
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid number of processes specified.");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Error: No value specified for -p option.");
                    System.exit(1);
                }
            } else {
                System.err.println("Error: Unrecognized option " + args[i]);
                System.exit(1);
            }
        }

        // Create array of Threads
        compThread[] compThreads = new compThread[numThreads];

        //Initialize global state variables
        int[] inputLength = {0};
        AtomicInteger currentIndex = new AtomicInteger(0);
        boolean[] compressionDone = {false};
        byte[] lastBlock = new byte[32768];
        ConcurrentHashMap<Integer, byte[]> outQ = new ConcurrentHashMap<>();
        Lock outQ_lock = new ReentrantLock();
        Lock comp_lock = new ReentrantLock();
        CRC32 c = new CRC32();

        //Make threads
        for (int i = 0; i < numThreads; i++)
        {
            compThreads[i] = new compThread(lastBlock, currentIndex, outQ, c, inputLength, outQ_lock, comp_lock);
        }
        writeThread writer = new writeThread(outQ, outQ_lock, compressionDone);

        //Start threads
        for (int i = 0; i < numThreads; i++)
        {
            compThreads[i].start();
        }
        writer.start();
        
        //Wait for threads to finish
        for (int i = 0; i < numThreads; i++)
        {
            compThreads[i].join();
        }
        compressionDone[0] = true;
        writer.join();

        int checksum = (int) c.getValue();

        byte[] crc32Bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(checksum).array();
        byte[] inputLengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(inputLength[0]).array();

        System.out.write(crc32Bytes, 0, crc32Bytes.length);
        System.out.write(inputLengthBytes, 0, inputLengthBytes.length);


    }
}



