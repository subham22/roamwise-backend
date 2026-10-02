package com.roamwise.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.roamwise.entity.guide.Guide;
import com.roamwise.entity.guide.Status;
import com.roamwise.repository.guide.GuideRepository;
import com.roamwise.service.GuideGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/guides")
@RequiredArgsConstructor
public class GuideAdminController {

    private final GuideGenerationService guideGenerationService;
    private final GuideRepository guideRepository;

    public record GenerateGuideRequest(String destination, Integer numberOfDays) {}

    @PostMapping("/generate")
    public void generate(@RequestBody GenerateGuideRequest request) throws JsonProcessingException {
        guideGenerationService.generateGuide(request.destination(), request.numberOfDays());
    }

    @PostMapping("/{id}/publish")
    public void publish(@PathVariable Integer id) {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guide not found"));
        guide.setStatus(Status.PUBLISHED);
        guideRepository.save(guide);
    }
}