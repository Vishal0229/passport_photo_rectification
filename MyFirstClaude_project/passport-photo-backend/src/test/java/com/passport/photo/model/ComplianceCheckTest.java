package com.passport.photo.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ComplianceCheckTest {

    @Test
    void allArgsConstructor_setsAllFields() {
        ComplianceCheck check = new ComplianceCheck(
            "Image Dimensions", true, "Looks good", "≥600×600 px", "600×600 px");

        assertThat(check.getRule()).isEqualTo("Image Dimensions");
        assertThat(check.isPassed()).isTrue();
        assertThat(check.getMessage()).isEqualTo("Looks good");
        assertThat(check.getExpected()).isEqualTo("≥600×600 px");
        assertThat(check.getActual()).isEqualTo("600×600 px");
    }

    @Test
    void defaultConstructor_leavesFieldsAtDefaults() {
        ComplianceCheck check = new ComplianceCheck();

        assertThat(check.getRule()).isNull();
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).isNull();
        assertThat(check.getExpected()).isNull();
        assertThat(check.getActual()).isNull();
    }

    @Test
    void setters_updateAllFields() {
        ComplianceCheck check = new ComplianceCheck();
        check.setRule("Aspect Ratio");
        check.setPassed(false);
        check.setMessage("Ratio differs — photo will be centre-cropped to fit");
        check.setExpected("1.00 : 1  (±15%)");
        check.setActual("6.00 : 1");

        assertThat(check.getRule()).isEqualTo("Aspect Ratio");
        assertThat(check.isPassed()).isFalse();
        assertThat(check.getMessage()).contains("centre-cropped");
        assertThat(check.getExpected()).contains("±15%");
        assertThat(check.getActual()).isEqualTo("6.00 : 1");
    }

    @Test
    void passed_canBeToggledViaSetterIndependently() {
        ComplianceCheck check = new ComplianceCheck("Resolution / Quality", false, "Low res", "", "");
        assertThat(check.isPassed()).isFalse();
        check.setPassed(true);
        assertThat(check.isPassed()).isTrue();
    }
}
