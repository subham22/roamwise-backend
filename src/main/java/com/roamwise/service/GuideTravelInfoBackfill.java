package com.roamwise.service;

import com.roamwise.repository.guide.GuideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/** After startup, fills travel info for published guides that don't have it yet (one AI call per guide). */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuideTravelInfoBackfill {

    private final GuideRepository guideRepository;
    private final GuideGenerationService guideGenerationService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.guides.backfill-travel-info:true}")
    private boolean enabled;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        if (!enabled) {
            return;
        }
        List<Integer> ids = guideRepository.findPublishedIdsWithoutTravelInfo();
        if (ids.isEmpty()) {
            return;
        }
        log.info("Backfilling travel info for {} guides", ids.size());
        int done = 0;
        for (Integer id : ids) {
            try {
                transactionTemplate.executeWithoutResult(status -> guideRepository.findById(id).ifPresent(guide -> {
                    guide.setTravelInfo(guideGenerationService.generateTravelInfo(guide));
                    guide.setUpdatedAt(LocalDateTime.now()); // new content: sitemap lastmod moves
                }));
                done++;
            } catch (RuntimeException e) {
                log.warn("Travel info backfill failed for guide {}: {}", id, e.getMessage());
            }
        }
        log.info("Travel info backfill finished: {}/{} guides", done, ids.size());
    }
}
