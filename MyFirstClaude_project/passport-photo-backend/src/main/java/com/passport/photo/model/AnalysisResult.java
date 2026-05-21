package com.passport.photo.model;

import java.util.List;

public class AnalysisResult {
    private String country;
    private CountrySpec spec;
    private List<ComplianceCheck> checks;
    private boolean allPassed;
    private int passedCount;
    private int totalCount;

    public AnalysisResult() {}

    public AnalysisResult(String country, CountrySpec spec, List<ComplianceCheck> checks,
                          boolean allPassed, int passedCount, int totalCount) {
        this.country = country;
        this.spec = spec;
        this.checks = checks;
        this.allPassed = allPassed;
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

    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int passedCount) { this.passedCount = passedCount; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
