package com.passport.photo.model;

import java.util.List;

/**
 * The complete result of a passport photo compliance analysis.
 *
 * <p>Returned as JSON by {@code POST /api/analyze}. Contains the country name,
 * the spec that was used, the result of every individual rule check, and
 * aggregate pass/fail counts.</p>
 *
 * @version 1.0
 */
public class AnalysisResult {
    /** Country display name (or {@code "Custom"} for user-entered specs). */
    private String country;
    /** The {@link CountrySpec} against which the photo was evaluated. */
    private CountrySpec spec;
    /** Ordered list of individual rule results. Always contains exactly 5 checks. */
    private List<ComplianceCheck> checks;
    /** {@code true} only when every check in {@link #checks} passed. */
    private boolean allPassed;
    /** {@code true} when the photo meets all official requirements and is passport-ready. Mirrors {@link #allPassed}. */
    private boolean certified;
    /** Number of checks that passed. */
    private int passedCount;
    /** Total number of checks evaluated (always 5). */
    private int totalCount;

    /** Required by Jackson for deserialisation. */
    public AnalysisResult() {}

    /**
     * Constructs a fully-populated analysis result.
     *
     * @param country     display name of the country or region
     * @param spec        the spec used for evaluation
     * @param checks      list of individual compliance check results
     * @param allPassed   {@code true} if every check passed
     * @param passedCount number of checks that passed
     * @param totalCount  total number of checks evaluated
     */
    public AnalysisResult(String country, CountrySpec spec, List<ComplianceCheck> checks,
                          boolean allPassed, int passedCount, int totalCount) {
        this.country = country;
        this.spec = spec;
        this.checks = checks;
        this.allPassed = allPassed;
        this.certified = allPassed;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
    }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public CountrySpec getSpec() { return spec; }
    public void setSpec(CountrySpec spec) { this.spec = spec; }

    public List<ComplianceCheck> getChecks() { return checks; }
    public void setChecks(List<ComplianceCheck> checks) { this.checks = checks; }

    public boolean isAllPassed() { return allPassed; }
    public void setAllPassed(boolean allPassed) { this.allPassed = allPassed; }

    public boolean isCertified() { return certified; }
    public void setCertified(boolean certified) { this.certified = certified; }

    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int passedCount) { this.passedCount = passedCount; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
