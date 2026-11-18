package com.roamwise.dto.guide;

import java.util.Comparator;
import java.util.List;

/** A published guide with the average position of its activities, used to find nearby guides. */
public record GuideLocation(String slug, String title, String destination, String metaDescription,
                            String heroImageUrl, Double latitude, Double longitude) {

    public GuideSummaryResponse toSummary() {
        return new GuideSummaryResponse(slug, title, destination, metaDescription, heroImageUrl);
    }

    /** Other durations of the same city first, then the closest guides by distance. */
    public static List<GuideSummaryResponse> nearest(String slug, List<GuideLocation> all, int limit) {
        GuideLocation self = all.stream().filter(g -> g.slug().equals(slug)).findFirst().orElse(null);
        if (self == null) {
            return List.of();
        }
        return all.stream()
                .filter(g -> !g.slug().equals(slug))
                .sorted(Comparator.comparing((GuideLocation g) -> !g.destination().equals(self.destination()))
                        .thenComparingDouble(g -> distanceKm(self, g)))
                .limit(limit)
                .map(GuideLocation::toSummary)
                .toList();
    }

    static double distanceKm(GuideLocation a, GuideLocation b) {
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLng = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(a.latitude()))
                * Math.cos(Math.toRadians(b.latitude())) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * 6371 * Math.asin(Math.sqrt(h));
    }
}
