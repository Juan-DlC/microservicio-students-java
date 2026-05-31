package co.edu.uniremington.msstudents.service;

import co.edu.uniremington.msstudents.client.CourseClient;
import co.edu.uniremington.msstudents.exception.EnrollmentNotFoundException;
import co.edu.uniremington.msstudents.exception.StudentNotFoundException;
import co.edu.uniremington.msstudents.exception.StudentNotActiveException;
import co.edu.uniremington.msstudents.model.Enrollment;
import co.edu.uniremington.msstudents.model.EnrollmentStatus;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.repository.EnrollmentRepository;
import co.edu.uniremington.msstudents.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseClient courseClient;

    @InjectMocks
    private StudentService studentService;

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    void findAll_ShouldReturnAllStudents() {
        List<Student> students = List.of(
                new Student(1L, "Juan", "Pérez", "juan@test.com"),
                new Student(2L, "Ana",  "López", "ana@test.com")
        );
        when(studentRepository.findAll()).thenReturn(students);

        List<Student> result = studentService.findAll();

        assertEquals(2, result.size());
        verify(studentRepository, times(1)).findAll();
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_ShouldPersistAndReturnStudent() {
        Student student = new Student(null, "Juan", "Pérez", "juan@test.com");
        Student saved   = new Student(1L,   "Juan", "Pérez", "juan@test.com");
        when(studentRepository.save(any(Student.class))).thenReturn(saved);

        Student result = studentService.save(student);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_WhenStudentExists_ShouldUpdateAndReturn() {
        Student existing = new Student(1L, "Juan", "Pérez", "juan@test.com");
        Student details  = new Student(null, "Juan Carlos", "Ramírez", "jc@test.com");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenReturn(existing);

        Student result = studentService.update(1L, details);

        assertEquals("Juan Carlos", result.getFirstName());
        assertEquals("jc@test.com", result.getEmail());
        verify(studentRepository, times(1)).save(existing);
    }

    @Test
    void update_WhenStudentNotFound_ShouldThrowStudentNotFoundException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> studentService.update(99L, new Student()));
    }

    // ── updateStudentStatus ───────────────────────────────────────────────────

    @Test
    void updateStudentStatus_WhenStudentExists_ShouldSetActiveAndReturn() {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        Student result = studentService.updateStudentStatus(1L, false);

        assertFalse(result.isActive());
        verify(studentRepository, times(1)).save(student);
    }

    @Test
    void updateStudentStatus_WhenStudentNotFound_ShouldThrowStudentNotFoundException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> studentService.updateStudentStatus(99L, false));
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_WhenStudentExists_ShouldCallDeleteById() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        studentService.delete(1L);

        verify(studentRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_WhenStudentNotFound_ShouldThrowStudentNotFoundException() {
        when(studentRepository.existsById(99L)).thenReturn(false);

        assertThrows(StudentNotFoundException.class, () -> studentService.delete(99L));
        verify(studentRepository, never()).deleteById(any());
    }

    // ── enrollInCourse ────────────────────────────────────────────────────────

    @Test
    void enrollInCourse_WhenStudentActiveAndExists_ShouldReturnEnrollment() {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        Enrollment savedEnrollment = new Enrollment(1L, 1L, 10L, EnrollmentStatus.ACTIVE, LocalDateTime.now());

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        doNothing().when(courseClient).decreaseQuota(10L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnrollment);

        Enrollment result = studentService.enrollInCourse(1L, 10L);

        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
        assertEquals(10L, result.getCourseId());
        assertEquals(1L, result.getStudentId());
        verify(courseClient, times(1)).decreaseQuota(10L);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    void enrollInCourse_WhenStudentNotActive_ShouldThrowStudentNotActiveException() {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        student.setActive(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(StudentNotActiveException.class,
                () -> studentService.enrollInCourse(1L, 10L));
        verify(courseClient, never()).decreaseQuota(any());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollInCourse_WhenStudentNotFound_ShouldThrowStudentNotFoundException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> studentService.enrollInCourse(99L, 1L));
        verify(courseClient, never()).decreaseQuota(any());
    }

    // ── cancelEnrollment ──────────────────────────────────────────────────────

    @Test
    void cancelEnrollment_WhenEnrollmentExists_ShouldCallIncreaseQuotaAndCancelEnrollment() {
        Enrollment enrollment = new Enrollment(1L, 1L, 10L, EnrollmentStatus.ACTIVE, LocalDateTime.now());
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        doNothing().when(courseClient).increaseQuota(10L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);

        Enrollment result = studentService.cancelEnrollment(1L);

        assertEquals(EnrollmentStatus.CANCELLED, result.getStatus());
        verify(courseClient, times(1)).increaseQuota(10L);
        verify(enrollmentRepository, times(1)).save(enrollment);
    }

    @Test
    void cancelEnrollment_WhenEnrollmentNotFound_ShouldThrowEnrollmentNotFoundException() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EnrollmentNotFoundException.class,
                () -> studentService.cancelEnrollment(99L));
        verify(courseClient, never()).increaseQuota(any());
    }
}
