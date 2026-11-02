public class Semaphore {

    private int value;

    // Constructor
    public Semaphore() {
        this(0);  // this is the initial number of permits now to use resources
    }

    public Semaphore(int initialValue) {

        if (initialValue < 0) {
            throw new IllegalArgumentException(
                    "Semaphore value cannot be negative." // the permits can not be negative
            );
        }

        value = initialValue;
    }

    // Wait operation : if the resources are not found, wait
    public synchronized void P() throws InterruptedException {

        while (value <= 0) {
            wait();
        }

        value--;
    }


    // Try to acquire a permit within the specified timeout.
    // Timeout is measured in milliseconds.
    // Returns false if the timeout expires.

    public synchronized boolean P(long timeout)
            throws InterruptedException {

        if (timeout < 0) {
            throw new IllegalArgumentException(
                    "Timeout cannot be negative."
            );
        }

        long timeoutNanos = timeout * 1_000_000L;
        long startTime = System.nanoTime();

        while (value <= 0) {

            long elapsed = System.nanoTime() - startTime;
            long remaining = timeoutNanos - elapsed;

            if (remaining <= 0) {
                return false;
            }

            long millis = remaining / 1_000_000L;
            int nanos = (int) (remaining % 1_000_000L);

            wait(millis, nanos);
        }

        value--;
        return true;
    }

    // Signal operation : notify the others
    public synchronized void V() {

        value++;

        notifyAll();
    }
}