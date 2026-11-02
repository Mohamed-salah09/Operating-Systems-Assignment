public class WaitingRoom {

    private final int capacity;
    private final Patient[] store;

    private int inptr = 0;
    private int outptr = 0;
    private int count = 0;

    // Semaphores for Bounded Buffer synchronization
    private final Semaphore spaces;
    private final Semaphore elements;
    private final Semaphore mutex;

    public WaitingRoom(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Waiting room capacity must be positive."
            );
        }

        this.capacity = capacity;
        store = new Patient[capacity];

        spaces = new Semaphore(capacity);
        elements = new Semaphore(0);
        mutex = new Semaphore(1);
    }

    public void enterRoom(Patient patient)
            throws InterruptedException {

        if (patient == null) {
            throw new IllegalArgumentException(
                    "Patient cannot be null."
            );
        }

        // Try to reserve a seat immediately without waiting.
        if (!spaces.P(0)) {

            System.out.println("The waiting room is full.");

            System.out.println(
                    patient.getPatientName()
                            + " is waiting outside for an available seat."
            );

            // Wait until a doctor releases an empty seat.
            spaces.P();
        }

        boolean inserted = false;

        try {

            // Protect the shared buffer from concurrent modifications.
            mutex.P();

            try {

                store[inptr] = patient;

                // Circular movement: return to index 0 after the last position.
                inptr = (inptr + 1) % capacity;

                count++;
                inserted = true;

                System.out.println(
                        patient.getPatientName()
                                + " entered the waiting room."
                );

            } finally {

                mutex.V();
            }

        } finally {

            // Return the reserved seat if insertion fails.
            if (!inserted) {
                spaces.V();
            }
        }

        // Notify doctors that a new patient is available.
        elements.V();
    }

    public Patient getPatient() throws InterruptedException {

        // Wait until a patient or a shutdown signal becomes available.
        elements.P();

        Patient patient = null;
        boolean removed = false;

        try {

            mutex.P();

            try {

                patient = store[outptr];
                store[outptr] = null;

                outptr = (outptr + 1) % capacity;

                count--;
                removed = true;

            } finally {

                mutex.V();
            }

        } finally {

            // Restore the element permit if removal fails.
            if (!removed) {
                elements.V();
            }
        }

        // Release the seat so another patient can enter.
        spaces.V();

        return patient;
    }

    public void stopDoctors(int numberOfDoctors)
            throws InterruptedException {

        if (numberOfDoctors < 0) {
            throw new IllegalArgumentException(
                    "Number of doctors cannot be negative."
            );
        }

        for (int i = 0; i < numberOfDoctors; i++) {

            spaces.P();

            boolean inserted = false;

            try {

                mutex.P();

                try {

                    // null represents a special shutdown signal (Poison Pill).
                    store[inptr] = null;

                    inptr = (inptr + 1) % capacity;

                    count++;
                    inserted = true;

                } finally {

                    mutex.V();
                }

            } finally {

                if (!inserted) {
                    spaces.V();
                }
            }

            // Wake up a waiting doctor to receive the shutdown signal.
            elements.V();
        }
    }
}