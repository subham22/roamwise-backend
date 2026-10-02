package com.roamwise.controller;

import com.roamwise.entity.guide.Guide;
import com.roamwise.entity.guide.Status;
import com.roamwise.repository.guide.GuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final GuideRepository guideRepository;
    private static final String BASE_URL = "https://roamwise.live";

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        xml.append("  <url><loc>").append(BASE_URL).append("/</loc></url>\n");
        xml.append("  <url><loc>").append(BASE_URL).append("/guides</loc></url>\n");

        for (Guide guide : guideRepository.findByStatus(Status.PUBLISHED)) {
            xml.append("  <url><loc>").append(BASE_URL).append("/guides/").append(guide.getSlug()).append("</loc></url>\n");
        }

        xml.append("</urlset>");

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml.toString());
    }
}