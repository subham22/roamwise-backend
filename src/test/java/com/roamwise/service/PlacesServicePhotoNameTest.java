package com.roamwise.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlacesServicePhotoNameTest {

    @Test
    void acceptsGooglePhotoNamesAndExtractsPlaceId() {
        var m = PlacesService.PHOTO_NAME.matcher("places/ChIJ4_pD8GDRCDkR9eaagdIsq5Q/photos/Aa-ngMZ2End_oQCh7");
        assertEquals(true, m.matches());
        assertEquals("ChIJ4_pD8GDRCDkR9eaagdIsq5Q", m.group(1));
    }

    @Test
    void rejectsAnythingElse() {
        assertFalse(PlacesService.PHOTO_NAME.matcher("places/abc:searchText").matches());
        assertFalse(PlacesService.PHOTO_NAME.matcher("places/abc/photos/x?fields=*").matches());
        assertFalse(PlacesService.PHOTO_NAME.matcher("../places/abc/photos/x").matches());
    }
}
