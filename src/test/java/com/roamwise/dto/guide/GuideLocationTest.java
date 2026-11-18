package com.roamwise.dto.guide;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuideLocationTest {

    private static GuideLocation loc(String slug, String dest, double lat, double lng) {
        return new GuideLocation(slug, slug, dest, "", "", lat, lng);
    }

    @Test
    void sameCityFirstThenClosest() {
        List<GuideLocation> all = List.of(
                loc("rishikesh-2", "Rishikesh, India", 30.10, 78.30),
                loc("goa-3", "Goa, India", 15.50, 73.80),
                loc("haridwar-2", "Haridwar, India", 29.95, 78.16),
                loc("rishikesh-3", "Rishikesh, India", 30.12, 78.31),
                loc("dehradun-2", "Dehradun, India", 30.32, 78.03));

        List<String> slugs = GuideLocation.nearest("rishikesh-2", all, 3).stream()
                .map(GuideSummaryResponse::slug).toList();

        assertEquals(List.of("rishikesh-3", "haridwar-2", "dehradun-2"), slugs);
    }

    @Test
    void unknownSlugGivesNothing() {
        assertEquals(List.of(), GuideLocation.nearest("nope", List.of(loc("a", "A", 0, 0)), 3));
    }
}
