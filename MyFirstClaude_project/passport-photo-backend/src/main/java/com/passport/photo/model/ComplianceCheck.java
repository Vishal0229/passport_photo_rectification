package com.passport.photo.model;

public class ComplianceCheck {
    private String rule;
    private boolean passed;
    private String message;
    private String expected;
    private String actual;

    public ComplianceCheck() {}

    public ComplianceCheck(String rule, boolean passed, String message, String expected, String actual) {
        this.rule = rule;
        this.passed = passed;
        this.message = message;
        this.expected = expected;
        this.actual = actual;
    }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }
}
