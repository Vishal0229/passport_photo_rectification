package com.passport.photo.model;

import java.util.List;

/**
 * Specification for a country's official passport photo requirements.
 *
 * <p>Instances are loaded at startup from {@code country-specs.json} on the
 * classpath, or constructed at runtime from a JSON string supplied by the
 * frontend when the user selects the Custom / Other option.</p>
 *
 * @version 1.0
 */
public class CountrySpec {
    /** Display name of the country or region (e.g. "United States"). */
    private String name;
    /** Required photo width in millimetres. */
    private int widthMm;
    /** Required photo height in millimetres. */
    private int heightMm;
    /** Required photo width in pixels at {@link #dpi} resolution. */
    private int widthPx;
    /** Required photo height in pixels at {@link #dpi} resolution. */
    private int heightPx;
    /** Print resolution in dots per inch (always 300 for built-in specs). */
    private int dpi;
    /** Background colour label: {@code "WHITE"} or {@code "LIGHT_GREY"}. */
    private String backgroundColor;
    /** Background colour as a 6-digit hex string, e.g. {@code "#FFFFFF"}. */
    private String backgroundColorHex;
    /** Minimum fraction (0–1) of the photo height that the face should occupy. */
    private double faceRatioMin;
    /** Maximum fraction (0–1) of the photo height that the face should occupy. */
    private double faceRatioMax;
    /** Human-readable summary of the requirements. */
    private String description;
    /** Official government link for photo requirements (optional). */
    private String officialLink;
    /** Source or authority that defined these requirements (e.g. "passport.gov"). */
    private String source;
    /** Plain-language bullet points summarising the key requirements for this country. */
    private List<String> requirementsBulletPoints;

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

    public String getOfficialLink() { return officialLink; }
    public void setOfficialLink(String officialLink) { this.officialLink = officialLink; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public List<String> getRequirementsBulletPoints() { return requirementsBulletPoints; }
    public void setRequirementsBulletPoints(List<String> requirementsBulletPoints) { this.requirementsBulletPoints = requirementsBulletPoints; }
}
