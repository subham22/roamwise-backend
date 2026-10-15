package com.roamwise.dto;

import java.io.Serializable;
import java.util.List;

public record PlaceResult(String name, String address, Double rating, Double latitude, Double longitude, String photoReference, List<String> reviewSnippets) implements Serializable {}