package co.edu.uniremington.msstudents.controller;

import co.edu.uniremington.msstudents.dto.StudentDto;
import co.edu.uniremington.msstudents.model.Enrollment;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.service.StudentService;
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

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<Student> findAll() {
        return studentService.findAll();
    }

    @PostMapping
    public Student create(@RequestBody StudentDto dto) {
        return studentService.createStudent(dto);
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @RequestBody Student student) {
        return studentService.update(id, student);
    }

    @PatchMapping("/{id}/status")
    public Student updateStatus(@PathVariable Long id, @RequestParam boolean isActive) {
        return studentService.updateStudentStatus(id, isActive);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    @PutMapping("/{id}/enroll/{courseId}")
    public Enrollment enroll(@PathVariable Long id, @PathVariable Long courseId) {
        return studentService.enrollStudent(id, courseId);
    }

    @PutMapping("/enrollments/{enrollmentId}/cancel")
    public Enrollment cancelEnrollment(@PathVariable Long enrollmentId) {
        return studentService.cancelEnrollment(enrollmentId);
    }
}
