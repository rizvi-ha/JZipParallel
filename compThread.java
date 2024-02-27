import java.io.IOException;
import java.util.zip.Deflater;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.zip.CRC32;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class compThread extends Thread {

    //Shared state (global) variables
    ConcurrentHashMap<Integer, byte[]> outQ;
    byte[] lastBlock;
    AtomicInteger currentIndex;
    int[] inputLength;
    CRC32 c;
    Lock outQ_lock;
    Lock comp_lock;

    //Local variables
    byte[] myBlock;
    byte[] myDict;
    int index;
    boolean jobFinished;


    compThread(byte[] lastBlock, AtomicInteger currentIndex, ConcurrentHashMap<Integer, byte[]> outQ, CRC32 c, int[] inputLength, Lock outQ_lock, Lock comp_lock)
    {
        this.lastBlock = lastBlock;
        this.currentIndex = currentIndex;
        this.outQ = outQ;
        this.c = c;
        this.inputLength = inputLength;
        this.jobFinished = false;
        this.outQ_lock = outQ_lock;
        this.comp_lock = comp_lock;
    }

    //private static String bytesToHex(byte[] bytes) {
        //StringBuilder sb = new StringBuilder();
        //for (byte b : bytes) {
            //sb.append(String.format("%02X ", b));
        //}
        //return sb.toString();
    //}

    //private static synchronized void debugPrint(byte[] uncompressed, byte[] compressed)
    //{
        //System.err.println(bytesToHex(uncompressed));
        //System.err.println("00000000000000000000000000000000000000000000");
        //System.err.println(bytesToHex(compressed));
        //System.err.println("-------------------------------------------------------");
    //}

    private void grabBlock() throws IOException {

        int bsize = 131072;
        int dsize = 32768;

        this.myBlock = new byte[bsize];
        this.myDict = new byte[dsize];
        System.arraycopy(this.lastBlock, 0, this.myDict, 0, dsize);

        int amount_read = System.in.readNBytes(this.myBlock, 0, bsize);

        if (amount_read == bsize) {
            System.arraycopy(this.myBlock, bsize-dsize, this.lastBlock, 0, dsize);
        }
        else {
            this.myBlock = Arrays.copyOf(this.myBlock, amount_read);
            this.jobFinished = true;
        }
        
        if (amount_read == 0)
            return;

        this.c.update(this.myBlock);
        this.index = this.currentIndex.getAndIncrement();
        this.inputLength[0] += amount_read;

    }

    private byte[] compressBlock(byte[] block, byte[] dictionary)
    {
        int inputLength = block.length;

        Deflater d = new Deflater(6, true);

        d.setDictionary(dictionary);
        d.setInput(block);

        if (this.jobFinished)
            d.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(inputLength + 45);
        byte[] buffer = new byte[131072]; 
        while (!d.finished()) {
            int compressedLength = d.deflate(buffer, 0, buffer.length, Deflater.NO_FLUSH);
            baos.write(buffer, 0, compressedLength);
            if (d.needsInput()) {
                compressedLength = d.deflate(buffer, 0, buffer.length, Deflater.FULL_FLUSH);
                if (compressedLength > 0) {
                    baos.write(buffer, 0, compressedLength);
                }
                break;
            }
        }

        d.end();
        
        return baos.toByteArray();
    }

    public void run() {
        
        while (!jobFinished)
        {
            try {
                comp_lock.lock();
                grabBlock();
                comp_lock.unlock();
            } 
            catch (IOException e) {
                System.err.println(e.getMessage());
                System.err.println("ERROR");
                comp_lock.unlock();
                continue;
            }
    
            if (this.myBlock.length == 0)
            {
                return;
            }
                
            byte[] output = compressBlock(this.myBlock, this.myDict);
            
    
            outQ_lock.lock();
            this.outQ.put(this.index, output);
            outQ_lock.unlock();
        }
    }
}