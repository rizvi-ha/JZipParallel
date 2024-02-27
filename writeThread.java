import java.util.concurrent.locks.Lock;
import java.util.concurrent.ConcurrentHashMap;


public class writeThread extends Thread {
    ConcurrentHashMap<Integer, byte[]> outQ;
    int next;
    Lock outQ_lock;
    boolean[] compressionDone;

    writeThread(ConcurrentHashMap<Integer, byte[]> outQ, Lock outQ_lock, boolean[] compressionDone) {
        this.outQ = outQ;
        this.next = 0;
        this.outQ_lock = outQ_lock;
        this.compressionDone = compressionDone;
    }

    public void run() {
        boolean running = true;

        while(running)
        {
            outQ_lock.lock();
            
                byte[] nextOutput = this.outQ.get(this.next);

                if (!(nextOutput == null))
                {
                    System.out.write(nextOutput,0,nextOutput.length);
                    this.outQ.remove(next);
                    next++;

                    if (System.out.checkError()) {
                        System.err.println("Error: Write error on <stdout>");
                        System.exit(1);
                    }
                           
                }
                
                if (this.outQ.isEmpty() && (this.compressionDone[0] == true))
                    running = false;

            outQ_lock.unlock();
        }
        
    }
}
