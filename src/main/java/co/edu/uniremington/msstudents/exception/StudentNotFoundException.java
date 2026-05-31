package co.edu.uniremington.msstudents.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long id) {
        super("Estudiante no encontrado con ID: " + id);
    }
}
