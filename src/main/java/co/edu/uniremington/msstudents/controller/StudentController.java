package co.edu.uniremington.msstudents.controller;

import co.edu.uniremington.msstudents.dto.StudentDto;
import co.edu.uniremington.msstudents.model.Enrollment;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Students", description = "Student management and enrollment operations")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(summary = "List all students", description = "Returns a list of all registered students")
    @GetMapping
    public List<Student> findAll() {
        return studentService.findAll();
    }

    @Operation(summary = "Get student by ID", description = "Returns a single student by their ID")
    @GetMapping("/{id}")
    public Student getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }

    @Operation(summary = "Get enrollments by student", description = "Returns all enrollments for a given student ID")
    @GetMapping("/{id}/enrollments")
    public List<Enrollment> getEnrollmentsByStudentId(@PathVariable Long id) {
        return studentService.getEnrollmentsByStudentId(id);
    }

    @Operation(summary = "Create a student", description = "Registers a new student with first name, last name and email")
    @PostMapping
    public Student create(@RequestBody StudentDto dto) {
        return studentService.createStudent(dto);
    }

    @Operation(summary = "Update a student", description = "Updates the personal data of an existing student")
    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @RequestBody Student student) {
        return studentService.update(id, student);
    }

    @Operation(summary = "Activate or deactivate a student", description = "Changes the active status of a student. Inactive students cannot enroll in courses")
    @PatchMapping("/{id}/status")
    public Student updateStatus(@PathVariable Long id, @RequestParam boolean isActive) {
        return studentService.updateStudentStatus(id, isActive);
    }

    @Operation(summary = "Delete a student", description = "Removes a student from the system by their ID")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    @Operation(summary = "Enroll student in a course", description = "Enrolls a student in a course. The student must be active and the course must have available quotas. Communicates with ms-courses via Feign to reserve a slot")
    @PutMapping("/{id}/enroll/{courseId}")
    public Enrollment enroll(@PathVariable Long id, @PathVariable Long courseId) {
        return studentService.enrollStudent(id, courseId);
    }

    @Operation(summary = "Cancel an enrollment", description = "Cancels an active enrollment and releases the reserved slot back to the course via Feign")
    @PutMapping("/enrollments/{enrollmentId}/cancel")
    public Enrollment cancelEnrollment(@PathVariable Long enrollmentId) {
        return studentService.cancelEnrollment(enrollmentId);
    }
}
