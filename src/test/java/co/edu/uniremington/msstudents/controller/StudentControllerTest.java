package co.edu.uniremington.msstudents.controller;

import co.edu.uniremington.msstudents.dto.StudentDto;
import co.edu.uniremington.msstudents.exception.EnrollmentNotFoundException;
import co.edu.uniremington.msstudents.exception.GlobalExceptionHandler;
import co.edu.uniremington.msstudents.exception.StudentNotFoundException;
import co.edu.uniremington.msstudents.exception.StudentNotActiveException;
import co.edu.uniremington.msstudents.model.Enrollment;
import co.edu.uniremington.msstudents.model.EnrollmentStatus;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(studentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ── GET /api/students ─────────────────────────────────────────────────────

    @Test
    void findAll_ShouldReturn200WithList() throws Exception {
        when(studentService.findAll()).thenReturn(List.of(
                new Student(1L, "Juan", "Pérez", "juan@test.com"),
                new Student(2L, "Ana",  "López", "ana@test.com")
        ));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("Juan"));
    }

    // ── GET /api/students/{id} ────────────────────────────────────────────────

    @Test
    void getStudentById_WhenExists_ShouldReturn200() throws Exception {
        when(studentService.getStudentById(1L))
                .thenReturn(new Student(1L, "Juan", "Pérez", "juan@test.com"));

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Juan"));
    }

    @Test
    void getStudentById_WhenNotFound_ShouldReturn404() throws Exception {
        when(studentService.getStudentById(99L))
                .thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(get("/api/students/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/students/{id}/enrollments ────────────────────────────────────

    @Test
    void getEnrollmentsByStudentId_WhenStudentExists_ShouldReturn200WithList() throws Exception {
        List<Enrollment> enrollments = List.of(
                new Enrollment(1L, 1L, 10L, EnrollmentStatus.ACTIVE, LocalDateTime.now()),
                new Enrollment(2L, 1L, 20L, EnrollmentStatus.CANCELLED, LocalDateTime.now())
        );
        when(studentService.getEnrollmentsByStudentId(1L)).thenReturn(enrollments);

        mockMvc.perform(get("/api/students/1/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].courseId").value(10))
                .andExpect(jsonPath("$[1].status").value("CANCELLED"));
    }

    @Test
    void getEnrollmentsByStudentId_WhenStudentNotFound_ShouldReturn404() throws Exception {
        when(studentService.getEnrollmentsByStudentId(99L))
                .thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(get("/api/students/99/enrollments"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/students ────────────────────────────────────────────────────

    @Test
    void create_ShouldReturn201WithCreatedStudent() throws Exception {
        StudentDto dto   = new StudentDto("Juan", "Pérez", "juan@test.com");
        Student created  = new Student(1L, "Juan", "Pérez", "juan@test.com");
        when(studentService.createStudent(any(StudentDto.class))).thenReturn(created);

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Juan"));
    }

    // ── PUT /api/students/{id} ────────────────────────────────────────────────

    @Test
    void update_WhenStudentExists_ShouldReturn200() throws Exception {
        Student updated = new Student(1L, "Juan Carlos", "Ramírez", "jc@test.com");
        when(studentService.update(eq(1L), any(Student.class))).thenReturn(updated);

        mockMvc.perform(put("/api/students/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan Carlos"));
    }

    @Test
    void update_WhenStudentNotFound_ShouldReturn404() throws Exception {
        when(studentService.update(eq(99L), any(Student.class)))
                .thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(put("/api/students/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Student())))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/students/{id}/status ──────────────────────────────────────

    @Test
    void updateStatus_WhenStudentExists_ShouldReturn200() throws Exception {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        student.setActive(false);
        when(studentService.updateStudentStatus(1L, false)).thenReturn(student);

        mockMvc.perform(patch("/api/students/1/status")
                        .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateStatus_WhenStudentNotFound_ShouldReturn404() throws Exception {
        when(studentService.updateStudentStatus(99L, true))
                .thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(patch("/api/students/99/status")
                        .param("isActive", "true"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/students/{id} ─────────────────────────────────────────────

    @Test
    void delete_WhenStudentExists_ShouldReturn200() throws Exception {
        doNothing().when(studentService).delete(1L);

        mockMvc.perform(delete("/api/students/1"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_WhenStudentNotFound_ShouldReturn404() throws Exception {
        doThrow(new StudentNotFoundException(99L)).when(studentService).delete(99L);

        mockMvc.perform(delete("/api/students/99"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/students/{id}/enroll/{courseId} ──────────────────────────────

    @Test
    void enroll_WhenValid_ShouldReturn200WithEnrollment() throws Exception {
        Enrollment enrollment = new Enrollment(1L, 1L, 10L, EnrollmentStatus.ACTIVE, LocalDateTime.now());
        when(studentService.enrollStudent(1L, 10L)).thenReturn(enrollment);

        mockMvc.perform(put("/api/students/1/enroll/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(10))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void enroll_WhenStudentNotFound_ShouldReturn404() throws Exception {
        when(studentService.enrollStudent(99L, 10L))
                .thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(put("/api/students/99/enroll/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void enroll_WhenStudentNotActive_ShouldReturn409() throws Exception {
        when(studentService.enrollStudent(1L, 10L))
                .thenThrow(new StudentNotActiveException(1L));

        mockMvc.perform(put("/api/students/1/enroll/10"))
                .andExpect(status().isConflict());
    }

    // ── PUT /api/students/enrollments/{enrollmentId}/cancel ───────────────────

    @Test
    void cancelEnrollment_WhenValid_ShouldReturn200WithCancelledEnrollment() throws Exception {
        Enrollment enrollment = new Enrollment(1L, 1L, 10L, EnrollmentStatus.CANCELLED, LocalDateTime.now());
        when(studentService.cancelEnrollment(1L)).thenReturn(enrollment);

        mockMvc.perform(put("/api/students/enrollments/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelEnrollment_WhenEnrollmentNotFound_ShouldReturn404() throws Exception {
        when(studentService.cancelEnrollment(99L))
                .thenThrow(new EnrollmentNotFoundException(99L));

        mockMvc.perform(put("/api/students/enrollments/99/cancel"))
                .andExpect(status().isNotFound());
    }
}
