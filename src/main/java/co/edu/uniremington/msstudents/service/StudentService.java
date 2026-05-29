package co.edu.uniremington.msstudents.service;

import co.edu.uniremington.msstudents.client.CourseClient;
import co.edu.uniremington.msstudents.model.Student;
import co.edu.uniremington.msstudents.repository.StudentRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseClient courseClient;

    public StudentService(StudentRepository studentRepository, CourseClient courseClient) {
        this.studentRepository = studentRepository;
        this.courseClient = courseClient;
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Student save(Student student) {
        return studentRepository.save(student);
    }

    public Student update(Long id, Student studentDetails) {

        return studentRepository.findById(id).map(existingStudent -> {

            existingStudent.setFirstName(studentDetails.getFirstName());
            existingStudent.setLastName(studentDetails.getLastName());
            existingStudent.setEmail(studentDetails.getEmail());
            return studentRepository.save(existingStudent);
        }).orElseThrow(() -> new RuntimeException("Estudiante no encontrado con ID: " + id));
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
    }

    public Student enrollInCourse(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // ¡Llamada REST a ms-courses!
        courseClient.decreaseQuota(courseId);

        student.setCourseId(courseId);
        return studentRepository.save(student);
    }

    public Student cancelEnrollment(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getCourseId() != null) {
            // ¡Llamada REST a ms-courses!
            courseClient.increaseQuota(student.getCourseId());
            student.setCourseId(null);
            return studentRepository.save(student);
        }
        return student;
    }
}