package com.rewear.models;

/**
 * Generic id/label pair for combo boxes (category, size, condition).
 */
public class Lookup {

    private final int id;
    private final String label;

    public Lookup(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
