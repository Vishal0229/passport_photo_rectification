package com.passport.photo.model;

public class CountrySpec {
    private String name;
    private int widthMm;
    private int heightMm;
    private int widthPx;
    private int heightPx;
    private int dpi;
    private String backgroundColor;
    private String backgroundColorHex;
    private double faceRatioMin;
    private double faceRatioMax;
    private String description;

    public CountrySpec() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWidthMm() { return widthMm; }
    public void setWidthMm(int widthMm) { this.widthMm = widthMm; }

    public int getHeightMm() { return heightMm; }
    public void setHeightMm(int heightMm) { this.heightMm = heightMm; }

    public int getWidthPx() { return widthPx; }
    public void setWidthPx(int widthPx) { this.widthPx = widthPx; }

    public int getHeightPx() { return heightPx; }
    public void setHeightPx(int heightPx) { this.heightPx = heightPx; }

    public int getDpi() { return dpi; }
    public void setDpi(int dpi) { this.dpi = dpi; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public String getBackgroundColorHex() { return backgroundColorHex; }
    public void setBackgroundColorHex(String backgroundColorHex) { this.backgroundColorHex = backgroundColorHex; }

    public double getFaceRatioMin() { return faceRatioMin; }
    public void setFaceRatioMin(double faceRatioMin) { this.faceRatioMin = faceRatioMin; }

    public double getFaceRatioMax() { return faceRatioMax; }
    public void setFaceRatioMax(double faceRatioMax) { this.faceRatioMax = faceRatioMax; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
