import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class Doctor extends Thread {

    private final int doctorId;
    private final WaitingRoom waitingRoom;
    private final Semaphore treatmentRooms;
    private final CountDownLatch patientsRemaining;

    public Doctor(
            int doctorId,
            WaitingRoom waitingRoom,
            Semaphore treatmentRooms,
            CountDownLatch patientsRemaining) {

        this.doctorId = doctorId;
        this.waitingRoom = waitingRoom;
        this.treatmentRooms = treatmentRooms;
        this.patientsRemaining = patientsRemaining;
    }

    @Override
    public void run() {

        while (true) {

            Patient patient;

            // Wait until a patient becomes available.
            try {

                patient = waitingRoom.getPatient();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                break;
            }

            // null represents a shutdown signal.
            if (patient == null) {

                System.out.println(
                        "Doctor " + doctorId + " is stopping."
                );

                break;
            }

            System.out.println(
                    "Doctor " + doctorId
                            + " is assigned to "
                            + patient.getPatientName() + "."
            );

            System.out.println(
                    "Doctor " + doctorId
                            + " is trying to acquire a treatment room."
            );

            boolean acquired;

            // Try to acquire a treatment room within 3 seconds.
            try {

                acquired = treatmentRooms.P(3000);

            } catch (InterruptedException e) {

                returnPatient(patient);

                Thread.currentThread().interrupt();
                break;
            }

            // Return the patient if no treatment room is available.
            if (!acquired) {

                System.out.println(
                        "Doctor " + doctorId
                                + " could not acquire a treatment room."
                );

                System.out.println(
                        "Doctor " + doctorId
                                + " is sending "
                                + patient.getPatientName()
                                + " back to the waiting room."
                );

                returnPatient(patient);

                System.out.println(
                        "Doctor " + doctorId + " is now available."
                );

                continue;
            }

            boolean treatmentCompleted = false;

            try {

                System.out.println(
                        "Doctor " + doctorId
                                + " acquired a treatment room."
                );

                System.out.println(
                        "Doctor " + doctorId
                                + " begins treating "
                                + patient.getPatientName() + "."
                );

                // Simulate treatment for a random period (1-5 seconds).
                int treatmentTime =
                        ThreadLocalRandom.current()
                                .nextInt(1000, 5001);

                Thread.sleep(treatmentTime);

                treatmentCompleted = true;

                System.out.println(
                        "Doctor " + doctorId
                                + " finished treating "
                                + patient.getPatientName() + "."
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            } finally {

                // Always release the treatment room after acquiring it.
                treatmentRooms.V();

                System.out.println(
                        "Doctor " + doctorId
                                + " released the treatment room."
                );
            }

            // Return the patient if treatment was interrupted.
            if (!treatmentCompleted) {

                returnPatient(patient);
                break;
            }

            System.out.println(
                    patient.getPatientName() + " left."
            );

            // Count this patient only after successful treatment.
            patientsRemaining.countDown();

            System.out.println(
                    "Doctor " + doctorId + " is now available."
            );
        }
    }

    // Use a separate thread so the doctor does not wait outside the full room.
    private void returnPatient(Patient patient) {

        Thread returnThread = new Thread(() -> {

            try {

                waitingRoom.enterRoom(patient);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.out.println(
                        "Could not return "
                                + patient.getPatientName()
                                + " to the waiting room."
                );
            }

        }, "Requeue-" + patient.getPatientName());

        returnThread.start();
    }
}