# ============================================================
# Script PowerShell - microservicio-students-java
# Ejecutar desde PowerShell dentro de la carpeta del repo:
#   cd C:\Proyectos\Proyecto_Microservicios\microservicio-students-java
#   .\aplicar-students.ps1
# ============================================================

$repoPath = Get-Location
Write-Host "Aplicando cambios en: $repoPath" -ForegroundColor Cyan

Write-Host "  Creando StudentNotFoundException.java..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "$repoPath\src\main\java\co\edu\uniremington\msstudents\exception" | Out-Null
Set-Content -Path "$repoPath\src\main\java\co\edu\uniremington\msstudents\exception\StudentNotFoundException.java" -Value @'
package co.edu.uniremington.msstudents.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long id) {
        super("Estudiante no encontrado con ID: " + id);
    }
}

'@ -NoNewline

Write-Host "  Creando GlobalExceptionHandler.java..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "$repoPath\src\main\java\co\edu\uniremington\msstudents\exception" | Out-Null
Set-Content -Path "$repoPath\src\main\java\co\edu\uniremington\msstudents\exception\GlobalExceptionHandler.java" -Value @'
package co.edu.uniremington.msstudents.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleStudentNotFound(StudentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    // Cuando ms-courses responde 409 (sin cupos), Feign lanza FeignException.Conflict
    @ExceptionHandler(FeignException.Conflict.class)
    public ResponseEntity<Map<String, String>> handleFeignConflict(FeignException.Conflict ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "No hay cupos disponibles en el curso solicitado"));
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Map<String, String>> handleFeignNotFound(FeignException.NotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Curso no encontrado en ms-courses"));
    }
}

'@ -NoNewline

Write-Host "  Creando StudentService.java..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "$repoPath\src\main\java\co\edu\uniremington\msstudents\service" | Out-Null
Set-Content -Path "$repoPath\src\main\java\co\edu\uniremington\msstudents\service\StudentService.java" -Value @'
package co.edu.uniremington.msstudents.service;

import co.edu.uniremington.msstudents.client.CourseClient;
import co.edu.uniremington.msstudents.exception.StudentNotFoundException;
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
        }).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException(id);
        }
        studentRepository.deleteById(id);
    }

    public Student enrollInCourse(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        // Llamada REST a ms-courses — si falla lanza FeignException (manejada en GlobalExceptionHandler)
        courseClient.decreaseQuota(courseId);

        student.setCourseId(courseId);
        return studentRepository.save(student);
    }

    public Student cancelEnrollment(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        if (student.getCourseId() != null) {
            courseClient.increaseQuota(student.getCourseId());
            student.setCourseId(null);
            return studentRepository.save(student);
        }
        return student;
    }
}

'@ -NoNewline

Write-Host "  Creando StudentServiceTest.java..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "$repoPath\src\test\java\co\edu\uniremington\msstudents\service" | Out-Null
Set-Content -Path "$repoPath\src\test\java\co\edu\uniremington\msstudents\service\StudentServiceTest.java" -Value @'
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

'@ -NoNewline

Write-Host "  Creando StudentControllerTest.java..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "$repoPath\src\test\java\co\edu\uniremington\msstudents\controller" | Out-Null
Set-Content -Path "$repoPath\src\test\java\co\edu\uniremington\msstudents\controller\StudentControllerTest.java" -Value @'
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

'@ -NoNewline

Write-Host "  Creando application.properties..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "$repoPath\src\test\resources" | Out-Null
Set-Content -Path "$repoPath\src\test\resources\application.properties" -Value @'
spring.datasource.url=jdbc:h2:mem:teststudentsdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.h2.console.enabled=false

eureka.client.enabled=false
spring.cloud.discovery.enabled=false
spring.cloud.service-registry.auto-registration.enabled=false
spring.cloud.openfeign.lazy-attributes-resolution=true

'@ -NoNewline

Write-Host "  Creando pom.xml..." -ForegroundColor Yellow
Set-Content -Path "$repoPath\pom.xml" -Value @'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.6</version>
        <relativePath/>
    </parent>
    <groupId>co.edu.uniremington</groupId>
    <artifactId>ms-students</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>ms-students</name>
    <description>ms-students</description>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2025.1.1</spring-cloud.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-h2console</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.5.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- JACOCO: cobertura mínima 80% -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals><goal>prepare-agent</goal></goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>verify</phase>
                        <goals><goal>report</goal></goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <goals><goal>check</goal></goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.80</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                            <excludes>
                                <exclude>**/model/**</exclude>
                                <exclude>**/dto/**</exclude>
                                <exclude>**/*Application*</exclude>
                                <exclude>**/repository/**</exclude>
                                <exclude>**/client/**</exclude>
                            </excludes>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>

'@ -NoNewline

Write-Host ""
Write-Host "Haciendo commit..." -ForegroundColor Cyan
git add .
git commit -m "feat: add unit tests JUnit+Mockito, Jacoco 80%, GlobalExceptionHandler"
Write-Host ""
Write-Host "Listo! Ahora ejecuta: git push origin master" -ForegroundColor Green
Write-Host "Para verificar cobertura:  mvn clean verify" -ForegroundColor Green