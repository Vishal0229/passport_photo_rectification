package com.passport.photo.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AnalysisResultTest {

    private CountrySpec minimalSpec() {
        CountrySpec spec = new CountrySpec();
        spec.setName("United States");
        spec.setWidthPx(600); spec.setHeightPx(600);
        spec.setDpi(300);
        return spec;
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        CountrySpec spec = minimalSpec();
        ComplianceCheck check = new ComplianceCheck("Dimensions", true, "OK", "", "");

        AnalysisResult result = new AnalysisResult("US", spec, List.of(check), true, 1, 1);

        assertThat(result.getCountry()).isEqualTo("US");
        assertThat(result.getSpec()).isSameAs(spec);
        assertThat(result.getChecks()).containsExactly(check);
        assertThat(result.isAllPassed()).isTrue();
        assertThat(result.getPassedCount()).isEqualTo(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    void defaultConstructor_leavesFieldsAtDefaults() {
        AnalysisResult result = new AnalysisResult();

        assertThat(result.getCountry()).isNull();
        assertThat(result.getSpec()).isNull();
        assertThat(result.getChecks()).isNull();
        assertThat(result.isAllPassed()).isFalse();
        assertThat(result.getPassedCount()).isZero();
        assertThat(result.getTotalCount()).isZero();
    }

    @Test
    void setters_updateAllFields() {
        AnalysisResult result = new AnalysisResult();
        CountrySpec spec = minimalSpec();

        result.setCountry("India");
        result.setSpec(spec);
        result.setChecks(List.of());
        result.setAllPassed(false);
        result.setPassedCount(3);
        result.setTotalCount(5);

        assertThat(result.getCountry()).isEqualTo("India");
        assertThat(result.getSpec()).isSameAs(spec);
        assertThat(result.getChecks()).isEmpty();
        assertThat(result.isAllPassed()).isFalse();
        assertThat(result.getPassedCount()).isEqualTo(3);
        assertThat(result.getTotalCount()).isEqualTo(5);
    }

    @Test
    void passedCount_canBeLessThanTotal() {
        AnalysisResult result = new AnalysisResult(
            "Canada", minimalSpec(), List.of(), false, 2, 5);

        assertThat(result.getPassedCount()).isLessThan(result.getTotalCount());
        assertThat(result.isAllPassed()).isFalse();
    }

    @Test
    void allPassed_requiresPassedCountEqualsTotal() {
        AnalysisResult passing = new AnalysisResult("US", minimalSpec(), List.of(), true, 5, 5);
        AnalysisResult failing = new AnalysisResult("US", minimalSpec(), List.of(), false, 4, 5);

        assertThat(passing.isAllPassed()).isTrue();
        assertThat(failing.isAllPassed()).isFalse();
    }
}
