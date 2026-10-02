package com.roamwise.controller;

import com.roamwise.dto.guide.GuideDetailResponse;
import com.roamwise.dto.guide.GuideSummaryResponse;
import com.roamwise.entity.guide.Guide;
import com.roamwise.entity.guide.GuideDay;
import com.roamwise.entity.guide.Status;
import com.roamwise.repository.guide.GuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideRepository guideRepository;

    @GetMapping
    public List<GuideSummaryResponse> list() {
        return guideRepository.findByStatus(Status.PUBLISHED).stream()
                .map(g -> new GuideSummaryResponse(g.getSlug(), g.getTitle(), g.getDestination(),
                        g.getMetaDescription(), g.getHeroImageUrl()))
                .toList();
    }

    @GetMapping("/{slug}")
    public GuideDetailResponse getBySlug(@PathVariable String slug) {
        Guide guide = guideRepository.findBySlug(slug)
                .filter(g -> g.getStatus() == Status.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guide not found"));

        List<GuideDetailResponse.GuideDayResponse> days = guide.getDays().stream()
                .sorted(Comparator.comparing(GuideDay::getDayNo))
                .map(day -> new GuideDetailResponse.GuideDayResponse(
                        day.getDayNo(), day.getDayLabel(),
                        day.getActivities().stream()
                                .sorted(Comparator.comparing(a -> a.getSequence()))
                                .map(a -> new GuideDetailResponse.GuideActivityResponse(
                                        a.getName(), a.getStartTime(), a.getEndTime(), a.getNotes(),
                                        a.getSequence(), a.getIsHiddenGem(), a.getReviewSnippet(),
                                        a.getLatitude(), a.getLongitude(), a.getPhotoReference()))
                                .toList()))
                .toList();

        return new GuideDetailResponse(guide.getSlug(), guide.getTitle(), guide.getDestination(),
                guide.getMetaDescription(), guide.getHeroImageUrl(), days);
    }
}