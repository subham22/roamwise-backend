package com.roamwise.dto;

/** Votes on one activity; mine is -1, 0 or 1 for the caller. */
public record VoteSummary(int up, int down, int mine) {}
