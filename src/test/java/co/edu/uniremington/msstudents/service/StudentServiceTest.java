package co.edu.uniremington.msstudents.service;

import co.edu.uniremington.msstudents.client.CourseClient;
import co.edu.uniremington.msstudents.exception.StudentNotFoundException;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void enrollInCourse_WhenStudentExists_ShouldCallDecreaseQuotaAndSave() {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        doNothing().when(courseClient).decreaseQuota(10L);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        Student result = studentService.enrollInCourse(1L, 10L);

        assertEquals(10L, result.getCourseId());
        verify(courseClient, times(1)).decreaseQuota(10L);
        verify(studentRepository, times(1)).save(student);
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
    void cancelEnrollment_WhenStudentEnrolled_ShouldCallIncreaseQuotaAndClearCourse() {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        student.setCourseId(10L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        doNothing().when(courseClient).increaseQuota(10L);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        Student result = studentService.cancelEnrollment(1L);

        assertNull(result.getCourseId());
        verify(courseClient, times(1)).increaseQuota(10L);
        verify(studentRepository, times(1)).save(student);
    }

    @Test
    void cancelEnrollment_WhenStudentNotEnrolled_ShouldReturnStudentWithoutCallingCourseClient() {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        // courseId es null: no está matriculado
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        Student result = studentService.cancelEnrollment(1L);

        assertNull(result.getCourseId());
        verify(courseClient, never()).increaseQuota(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void cancelEnrollment_WhenStudentNotFound_ShouldThrowStudentNotFoundException() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> studentService.cancelEnrollment(99L));
    }
}
