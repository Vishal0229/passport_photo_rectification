package com.passport.photo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Uses {@link MockMvcBuilders#standaloneSetup} to wire the stub controller
 * and the handler under test together without loading a full Spring context.
 * {@code standaloneSetup} registers the {@link GlobalExceptionHandler} as the
 * exception resolver, so the advice fires correctly for every test.</p>
 *
 * <p>The stub {@link ThrowingController} exposes endpoints that unconditionally
 * throw, allowing the handler's mapping logic to be exercised in isolation.</p>
 */
class GlobalExceptionHandlerTest {

    // ─── Stub controller ──────────────────────────────────────────────────────────

    /**
     * Minimal controller whose sole purpose is to throw exceptions without
     * catching them, so that {@link GlobalExceptionHandler} is invoked.
     */
    @RestController
    @RequestMapping("/test-stub")
    static class ThrowingController {

        @GetMapping("/illegal-argument")
        public String throwIllegalArgument() {
            throw new IllegalArgumentException("bad input value");
        }

        @GetMapping("/runtime-exception")
        public String throwRuntimeException() {
            throw new RuntimeException("something went wrong internally");
        }

        @GetMapping("/null-pointer")
        public String throwNullPointer() {
            throw new NullPointerException("unexpected null");
        }
    }

    // ─── Test fixture ─────────────────────────────────────────────────────────────

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standaloneSetup wires the advice directly into the MockMvc exception
        // resolver chain — @WebMvcTest + @Import does NOT do this automatically.
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─── IllegalArgumentException → 400 ──────────────────────────────────────────

    /**
     * An {@link IllegalArgumentException} thrown from a controller method must
     * produce HTTP 400 with a JSON body containing {@code "Invalid request:"}.
     */
    @Test
    void illegalArgumentException_returns400() throws Exception {
        mockMvc.perform(get("/test-stub/illegal-argument"))
                .andExpect(status().isBadRequest());
    }

    /**
     * The 400 response body must contain the {@code "error"} key with the
     * {@code "Invalid request:"} prefix.
     */
    @Test
    void illegalArgumentException_bodyContainsInvalidRequestPrefix() throws Exception {
        mockMvc.perform(get("/test-stub/illegal-argument"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(containsString("Invalid request:")));
    }

    /**
     * The 400 response body must include the original exception message so that
     * callers receive actionable feedback.
     */
    @Test
    void illegalArgumentException_bodyContainsOriginalMessage() throws Exception {
        mockMvc.perform(get("/test-stub/illegal-argument"))
                .andExpect(jsonPath("$.error").value(containsString("bad input value")));
    }

    // ─── Generic Exception → 500 ─────────────────────────────────────────────────

    /**
     * Any {@link Exception} that is NOT an {@link IllegalArgumentException} must
     * produce HTTP 500.
     */
    @Test
    void runtimeException_returns500() throws Exception {
        mockMvc.perform(get("/test-stub/runtime-exception"))
                .andExpect(status().isInternalServerError());
    }

    /**
     * The 500 response body must contain the generic safe message
     * {@code "An unexpected error occurred"} — the raw exception detail must NOT
     * be exposed to callers.
     */
    @Test
    void runtimeException_bodyContainsSafeGenericMessage() throws Exception {
        mockMvc.perform(get("/test-stub/runtime-exception"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(containsString("An unexpected error occurred")));
    }

    /**
     * {@link NullPointerException} (a subtype of {@link RuntimeException}) must
     * also produce HTTP 500, confirming the handler catches all non-IAE exceptions.
     */
    @Test
    void nullPointerException_returns500() throws Exception {
        mockMvc.perform(get("/test-stub/null-pointer"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(containsString("An unexpected error occurred")));
    }

    // ─── No stack trace in response body ─────────────────────────────────────────

    /**
     * Neither the 400 nor the 500 response body must contain raw stack-trace text.
     * Stack traces include lines like {@code "at com.foo.Bar.method(Bar.java:42)"}
     * which would leak internal class names and line numbers to callers.
     */
    @Test
    void illegalArgumentException_responseDoesNotContainStackTrace() throws Exception {
        mockMvc.perform(get("/test-stub/illegal-argument"))
                .andExpect(content().string(not(containsString("at com."))))
                .andExpect(content().string(not(containsString("\tat "))));
    }

    @Test
    void runtimeException_responseDoesNotContainStackTrace() throws Exception {
        mockMvc.perform(get("/test-stub/runtime-exception"))
                .andExpect(content().string(not(containsString("at com."))))
                .andExpect(content().string(not(containsString("\tat "))));
    }

    /**
     * The 500 response body must NOT contain the fully-qualified exception class
     * name (e.g. {@code java.lang.RuntimeException}) — exposing Java type names
     * is information leakage.
     */
    @Test
    void runtimeException_responseDoesNotContainExceptionFQN() throws Exception {
        mockMvc.perform(get("/test-stub/runtime-exception"))
                .andExpect(content().string(not(containsString("java.lang.RuntimeException"))))
                .andExpect(content().string(not(containsString("java.lang.NullPointerException"))));
    }

    @Test
    void nullPointerException_responseDoesNotContainExceptionFQN() throws Exception {
        mockMvc.perform(get("/test-stub/null-pointer"))
                .andExpect(content().string(not(containsString("java.lang.NullPointerException"))))
                .andExpect(content().string(not(containsString("at com."))));
    }

    // ─── Response content-type ────────────────────────────────────────────────────

    /**
     * Both 400 and 500 responses must use {@code application/json} so that
     * clients can reliably parse the error body.
     */
    @Test
    void illegalArgumentException_responseContentTypeIsJson() throws Exception {
        mockMvc.perform(get("/test-stub/illegal-argument"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void runtimeException_responseContentTypeIsJson() throws Exception {
        mockMvc.perform(get("/test-stub/runtime-exception"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
