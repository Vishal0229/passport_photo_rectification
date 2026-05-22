package com.passport.photo.model;

/**
 * The result of a single passport photo compliance rule evaluation.
 *
 * <p>Each {@code ComplianceCheck} captures whether a specific rule passed,
 * a human-readable explanation, and the expected vs. actual values so the
 * frontend can display a detailed diff to the user.</p>
 *
 * @version 1.0
 */
public class ComplianceCheck {
    /** Name of the rule, e.g. {@code "Image Dimensions"}. */
    private String rule;
    /** {@code true} if the photo satisfies this rule. */
    private boolean passed;
    /** Human-readable explanation of the result. */
    private String message;
    /** What the country spec requires, formatted for display. */
    private String expected;
    /** What was measured in the uploaded photo, formatted for display. */
    private String actual;

    /** Required by Jackson for deserialisation. */
    public ComplianceCheck() {}

    /**
     * Constructs a fully-populated compliance check result.
     *
     * @param rule     name of the rule being checked
     * @param passed   {@code true} if the photo satisfies the rule
     * @param message  human-readable explanation
     * @param expected what the spec requires (display string)
     * @param actual   what was found in the photo (display string)
     */
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
