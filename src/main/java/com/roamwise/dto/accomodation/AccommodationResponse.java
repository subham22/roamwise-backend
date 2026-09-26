package com.roamwise.dto.accomodation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AccommodationResponse {
    private Integer id;
    private String hotelName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer pricePerNight;
    private String notes;
}