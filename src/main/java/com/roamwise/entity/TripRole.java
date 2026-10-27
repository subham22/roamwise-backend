package com.roamwise.entity;

/** What a person may do with a trip. Order matters: each role includes the ones before it. */
public enum TripRole {
    VIEWER, EDITOR, OWNER;

    public boolean atLeast(TripRole other) {
        return this.ordinal() >= other.ordinal();
    }
}
