package co.edu.uniremington.msstudents.exception;

public class StudentNotActiveException extends RuntimeException {
    public StudentNotActiveException(Long studentId) {
        super("El estudiante con ID " + studentId + " no está activo y no puede matricularse");
    }
}
