package org.zhvtsv.models;

import java.util.Arrays;

public class ExtentRequest {
    private String id;
    private double [] extent;
    private String model;

    public double[] getExtent() {
        return extent;
    }

    public void setExtent(double[] extent) {
        this.extent = extent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "ExtentRequest{" +
                "id='" + id + '\'' +
                ", extent=" + Arrays.toString(extent) +
                ", model='" + model + '\'' +
                '}';
    }
}
