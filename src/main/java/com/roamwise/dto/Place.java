package com.roamwise.dto;

import com.roamwise.dto.direction.Location;
import com.roamwise.dto.direction.Photo;
import com.roamwise.dto.review.Review;

import java.util.List;

public record Place(DisplayName displayName, String formattedAddress, Double rating, List<String> types, Location location, List<Photo> photos, List<Review> reviews) {}