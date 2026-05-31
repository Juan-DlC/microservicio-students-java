package co.edu.uniremington.msstudents.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "ms-courses")
public interface CourseClient {

    @PutMapping("/api/courses/{id}/reserve-slot")
    void reserveSlot(@PathVariable("id") Long id);

    @PutMapping("/api/courses/{id}/release-slot")
    void releaseSlot(@PathVariable("id") Long id);
}
