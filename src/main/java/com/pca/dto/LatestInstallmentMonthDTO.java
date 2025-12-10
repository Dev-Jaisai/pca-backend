// src/main/java/com/pca/dto/LatestInstallmentMonthDTO.java
package com.pca.dto;

public class LatestInstallmentMonthDTO {
    private int year;
    private int month;

    public LatestInstallmentMonthDTO() {}

    public LatestInstallmentMonthDTO(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
}
