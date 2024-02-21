package org.zhvtsv.models;

import java.util.Arrays;

public class ExtentRequest {
    private String id;
    private double [] extent;
    private String dataSource;

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

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String toString() {
        return "ExtentRequest{" +
                "id='" + id + '\'' +
                ", extent=" + Arrays.toString(extent) +
                ", dataSource='" + dataSource + '\'' +
                '}';
    }
}
