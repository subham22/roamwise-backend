package com.roamwise.controller;

import com.roamwise.entity.guide.Guide;
import com.roamwise.entity.guide.Status;
import com.roamwise.repository.guide.GuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final GuideRepository guideRepository;
    private static final String BASE_URL = "https://roamwise.live";

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        List<Guide> guides = guideRepository.findByStatus(Status.PUBLISHED);

        // Home and /guides change whenever any guide changes, so use the newest guide date
        LocalDate latest = guides.stream()
                .map(this::lastModified)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, BASE_URL + "/", latest);
        appendUrl(xml, BASE_URL + "/guides", latest);

        for (Guide guide : guides) {
            appendUrl(xml, BASE_URL + "/guides/" + guide.getSlug(), lastModified(guide));
        }

        xml.append("</urlset>");

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String loc, LocalDate lastmod) {
        xml.append("  <url><loc>").append(loc).append("</loc>");
        if (lastmod != null) {
            xml.append("<lastmod>").append(lastmod).append("</lastmod>");
        }
        xml.append("</url>\n");
    }

    /** updatedAt, falling back to createdAt; null if the guide has neither. */
    private LocalDate lastModified(Guide guide) {
        LocalDateTime when = guide.getUpdatedAt() != null ? guide.getUpdatedAt() : guide.getCreatedAt();
        return when != null ? when.toLocalDate() : null;
    }
}