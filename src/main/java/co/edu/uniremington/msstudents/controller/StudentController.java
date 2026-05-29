package co.edu.uniremington.msstudents.controller;


import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.service.StudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public Student create(@RequestBody Student student) {
        return studentService.save(student);
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @RequestBody Student student) {
        return studentService.update(id, student);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    @PutMapping("/{id}/enroll/{courseId}")
    public Student enroll(@PathVariable Long id, @PathVariable Long courseId) {
        return studentService.enrollInCourse(id, courseId);
    }

    @PutMapping("/{id}/cancel-enrollment")
    public Student cancelEnrollment(@PathVariable Long id) {
        return studentService.cancelEnrollment(id);
    }
}