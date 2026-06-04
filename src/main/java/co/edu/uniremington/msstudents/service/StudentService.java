package co.edu.uniremington.msstudents.service;

import co.edu.uniremington.msstudents.client.CourseClient;
import co.edu.uniremington.msstudents.dto.StudentDto;
import co.edu.uniremington.msstudents.exception.EnrollmentNotFoundException;
import co.edu.uniremington.msstudents.exception.StudentNotFoundException;
import co.edu.uniremington.msstudents.exception.StudentNotActiveException;
import co.edu.uniremington.msstudents.model.Enrollment;
import co.edu.uniremington.msstudents.model.EnrollmentStatus;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.repository.EnrollmentRepository;
import co.edu.uniremington.msstudents.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseClient courseClient;

    public StudentService(StudentRepository studentRepository,
                          EnrollmentRepository enrollmentRepository,
                          CourseClient courseClient) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseClient = courseClient;
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }

    public List<Enrollment> getEnrollmentsByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new StudentNotFoundException(studentId);
        }
        return enrollmentRepository.findByStudentId(studentId);
    }

    public Student createStudent(StudentDto dto) {
        Student student = new Student(null, dto.getFirstName(), dto.getLastName(), dto.getEmail());
        return studentRepository.save(student);
    }

    public Student update(Long id, Student studentDetails) {
        return studentRepository.findById(id).map(existingStudent -> {
            existingStudent.setFirstName(studentDetails.getFirstName());
            existingStudent.setLastName(studentDetails.getLastName());
            existingStudent.setEmail(studentDetails.getEmail());
            return studentRepository.save(existingStudent);
        }).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public Student updateStudentStatus(Long id, boolean isActive) {
        return studentRepository.findById(id).map(student -> {
            student.setActive(isActive);
            return studentRepository.save(student);
        }).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException(id);
        }
        studentRepository.deleteById(id);
    }

    public Enrollment enrollStudent(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        if (!student.isActive()) {
            throw new StudentNotActiveException(studentId);
        }

        // Llamada REST a ms-courses — si falla lanza FeignException (manejada en GlobalExceptionHandler)
        courseClient.reserveSlot(courseId);

        Enrollment enrollment = new Enrollment(studentId, courseId, EnrollmentStatus.ACTIVE, LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    public Enrollment cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        courseClient.releaseSlot(enrollment.getCourseId());
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        return enrollmentRepository.save(enrollment);
    }
}
