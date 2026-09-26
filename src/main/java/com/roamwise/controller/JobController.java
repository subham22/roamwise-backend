package com.roamwise.controller;


import com.roamwise.dto.JobStatusResponse;
import com.roamwise.entity.Job;
import com.roamwise.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private  final JobRepository jobRepository;

    @GetMapping("/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable Integer jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job Doesn't exist"));
        JobStatusResponse response = new JobStatusResponse();
        response.setJobId(job.getId());
        response.setResultTripId(job.getResultTripId());
        response.setStatus(String.valueOf(job.getStatus()));
        response.setErrorMessage(job.getErrorMessage());
        return ResponseEntity.ok().body(response);

    }

}
