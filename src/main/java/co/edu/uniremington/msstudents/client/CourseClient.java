package co.edu.uniremington.msstudents.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "ms-courses")
public interface CourseClient {

    @PutMapping("/api/courses/{id}/decrease-quota")
    void decreaseQuota(@PathVariable("id") Long id);

    @PutMapping("/api/courses/{id}/increase-quota")
    void increaseQuota(@PathVariable("id") Long id);
}