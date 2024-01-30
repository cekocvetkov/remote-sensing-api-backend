package org.zhvtsv.service;

import java.util.Arrays;

public class SentinelRequest {
    private String dateFrom;
    private String dateTo;
    private double[] extent;
    private int cloudCoverage;

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public double[] getExtent() {
        return extent;
    }

    public void setExtent(double[] extent) {
        this.extent = extent;
    }

    public int getCloudCoverage() {
        return cloudCoverage;
    }

    public void setCloudCoverage(int cloudCoverage) {
        this.cloudCoverage = cloudCoverage;
    }

    @Override
    public String toString() {
        return "SentinelRequest{" +
                "dateFrom='" + dateFrom + '\'' +
                ", dateTo='" + dateTo + '\'' +
                ", extent=" + Arrays.toString(extent) +
                ", cloudCoverage=" + cloudCoverage +
                '}';
    }
}
