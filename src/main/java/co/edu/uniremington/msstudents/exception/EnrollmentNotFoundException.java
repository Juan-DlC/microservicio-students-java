package co.edu.uniremington.msstudents.exception;

public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException(Long enrollmentId) {
        super("Matrícula no encontrada con ID: " + enrollmentId);
    }
}
