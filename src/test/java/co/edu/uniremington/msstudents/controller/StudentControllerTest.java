package co.edu.uniremington.msstudents.controller;

import co.edu.uniremington.msstudents.exception.StudentNotFoundException;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

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

    // ── POST /api/students ────────────────────────────────────────────────────

    @Test
    void create_ShouldReturn200WithCreatedStudent() throws Exception {
        Student toCreate = new Student(null, "Juan", "Pérez", "juan@test.com");
        Student created  = new Student(1L,   "Juan", "Pérez", "juan@test.com");
        when(studentService.save(any(Student.class))).thenReturn(created);

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.email").value("juan@test.com"));
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
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
    void enroll_WhenStudentAndCourseExist_ShouldReturn200() throws Exception {
        Student enrolled = new Student(1L, "Juan", "Pérez", "juan@test.com");
        enrolled.setCourseId(10L);
        when(studentService.enrollInCourse(1L, 10L)).thenReturn(enrolled);

        mockMvc.perform(put("/api/students/1/enroll/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(10));
    }

    @Test
    void enroll_WhenStudentNotFound_ShouldReturn404() throws Exception {
        when(studentService.enrollInCourse(99L, 10L))
                .thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(put("/api/students/99/enroll/10"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/students/{id}/cancel-enrollment ──────────────────────────────

    @Test
    void cancelEnrollment_WhenStudentExists_ShouldReturn200() throws Exception {
        Student student = new Student(1L, "Juan", "Pérez", "juan@test.com");
        when(studentService.cancelEnrollment(1L)).thenReturn(student);

        mockMvc.perform(put("/api/students/1/cancel-enrollment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan"));
    }

    @Test
    void cancelEnrollment_WhenStudentNotFound_ShouldReturn404() throws Exception {
        when(studentService.cancelEnrollment(99L))
                .thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(put("/api/students/99/cancel-enrollment"))
                .andExpect(status().isNotFound());
    }
}
