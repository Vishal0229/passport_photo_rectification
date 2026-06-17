package com.passport.photo.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.passport.photo.service.FaceDetectionService;
import com.passport.photo.service.PhotoAnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link LoggingAspect}.
 *
 * <p>Uses a full {@code @SpringBootTest} context so that Spring AOP proxies are
 * active.  A Logback {@link ListAppender} is attached to the aspect's logger
 * before each test and removed afterwards so that captured log events do not
 * bleed between tests.</p>
 *
 * <p>{@link FaceDetectionService} is mocked to avoid the OpenCV native-library
 * initialisation cost that is not relevant to these tests.</p>
 */
@SpringBootTest
class LoggingAspectTest {

    // ─── Spring beans ─────────────────────────────────────────────────────────────

    @MockBean
    private FaceDetectionService faceDetectionService;

    @Autowired
    private PhotoAnalysisService photoAnalysisService;

    // ─── Log capture infrastructure ───────────────────────────────────────────────

    private Logger aspectLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void attachListAppender() {
        aspectLogger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
        // Ensure DEBUG events are not filtered out by the logger's own level
        aspectLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void detachListAppender() {
        aspectLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    /**
     * Returns all captured log messages as a flat list of formatted strings for
     * easy asserting with AssertJ's {@code anyMatch}.
     */
    private List<String> capturedMessages() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    // ─── Entry log tests ──────────────────────────────────────────────────────────

    /**
     * Every public service method invocation must produce a DEBUG-level entry log
     * prefixed with {@code >>} and containing the method name.
     */
    @Test
    void callingServiceMethod_producesDebugEntryLog_containingMethodName() {
        // getCountrySpecs() is a simple, side-effect-free method that always succeeds
        photoAnalysisService.getCountrySpecs();

        List<String> messages = capturedMessages();
        assertThat(messages)
                .as("Expected at least one DEBUG entry log starting with '>>'")
                .anyMatch(msg -> msg.startsWith(">>") && msg.contains("getCountrySpecs"));
    }

    /**
     * The entry log must contain ONLY argument types, not argument values.
     * {@code getCountrySpecs} takes no arguments, so the type list is empty {@code ()}.
     */
    @Test
    void entryLog_containsArgumentTypes_notValues() {
        photoAnalysisService.getCountrySpecs();

        List<String> entryLogs = listAppender.list.stream()
                .filter(e -> e.getLevel() == Level.DEBUG)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.startsWith(">>") && msg.contains("getCountrySpecs"))
                .toList();

        assertThat(entryLogs).isNotEmpty();
        // The argument section is enclosed in parentheses; for a no-arg method it must be "()"
        assertThat(entryLogs.get(0)).contains("()");
    }

    // ─── Exit log tests ───────────────────────────────────────────────────────────

    /**
     * Every successful service method invocation must produce a DEBUG-level exit log
     * prefixed with {@code <<} and containing the method name.
     */
    @Test
    void callingServiceMethod_producesDebugExitLog_containingMethodName() {
        photoAnalysisService.getCountrySpecs();

        List<String> messages = capturedMessages();
        assertThat(messages)
                .as("Expected at least one DEBUG exit log starting with '<<'")
                .anyMatch(msg -> msg.startsWith("<<") && msg.contains("getCountrySpecs"));
    }

    /**
     * The exit log must include a return-type indicator (the {@code →} arrow)
     * so that the caller can see what type was returned.
     */
    @Test
    void exitLog_containsReturnTypeArrow() {
        photoAnalysisService.getCountrySpecs();

        List<String> exitLogs = listAppender.list.stream()
                .filter(e -> e.getLevel() == Level.DEBUG)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.startsWith("<<") && msg.contains("getCountrySpecs"))
                .toList();

        assertThat(exitLogs).isNotEmpty();
        assertThat(exitLogs.get(0)).contains("→");
    }

    // ─── Exception log tests ──────────────────────────────────────────────────────

    /**
     * When a service method throws, the aspect must log an ERROR entry prefixed
     * with {@code !!} that names the exception class.
     */
    @Test
    void methodThatThrows_producesErrorLog_containingExceptionClassName() {
        // analyzePhoto with an unknown country code throws IllegalArgumentException
        assertThatThrownBy(() -> photoAnalysisService.analyzePhoto("not-an-image".getBytes(), "UNKNOWN_CODE"))
                .isInstanceOf(IllegalArgumentException.class);

        List<String> errorMessages = listAppender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(errorMessages)
                .as("Expected an ERROR log prefixed with '!!' for the thrown exception")
                .anyMatch(msg -> msg.startsWith("!!") && msg.contains("IllegalArgumentException"));
    }

    /**
     * The error log must also contain the exception message so operators have
     * enough context without needing a stack trace.
     */
    @Test
    void exceptionErrorLog_containsExceptionMessage() {
        assertThatThrownBy(() -> photoAnalysisService.analyzePhoto("not-an-image".getBytes(), "UNKNOWN_CODE"))
                .isInstanceOf(IllegalArgumentException.class);

        List<String> errorMessages = listAppender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        // The log should contain the exception message text (non-empty)
        assertThat(errorMessages)
                .as("ERROR log should include the exception message")
                .anyMatch(msg -> msg.startsWith("!!") && msg.length() > "!! ".length());
    }

    // ─── Re-throw test ────────────────────────────────────────────────────────────

    /**
     * After logging, the aspect must re-throw the original exception — it must
     * not swallow it.  The caller must still observe the exception.
     */
    @Test
    void exceptionIsRethrown_notSwallowed() {
        // If the aspect swallowed the exception this would NOT throw and the test would fail
        assertThatThrownBy(() -> photoAnalysisService.analyzePhoto("garbage".getBytes(), "UNKNOWN_CODE"))
                .as("Exception must propagate out of the aspect-proxied method")
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Re-thrown exception must be the SAME type as the original — the aspect
     * must not wrap it in a different exception type.
     */
    @Test
    void rethrownException_isSameTypeAsOriginal() {
        assertThatThrownBy(() -> photoAnalysisService.analyzePhoto("garbage".getBytes(), "UNKNOWN_CODE"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .as("Aspect must rethrow the original exception without wrapping");
    }

    // ─── Both entry and exit logs are present ─────────────────────────────────────

    /**
     * A single successful method call must produce BOTH an entry log and an exit log.
     * (Guards against accidentally logging only one side.)
     */
    @Test
    void singleMethodCall_producesBothEntryAndExitLogs() {
        photoAnalysisService.getCountrySpecs();

        List<String> messages = capturedMessages();

        boolean hasEntry = messages.stream()
                .anyMatch(msg -> msg.startsWith(">>") && msg.contains("getCountrySpecs"));
        boolean hasExit = messages.stream()
                .anyMatch(msg -> msg.startsWith("<<") && msg.contains("getCountrySpecs"));

        assertThat(hasEntry).as("Entry log (>>) missing").isTrue();
        assertThat(hasExit).as("Exit log (<<) missing").isTrue();
    }

    /**
     * When a method throws, there must be an entry log followed by an error log —
     * but NO exit log (the method never returned normally).
     */
    @Test
    void methodThatThrows_hasEntryAndErrorLog_butNoExitLog() {
        assertThatThrownBy(() -> photoAnalysisService.analyzePhoto("garbage".getBytes(), "UNKNOWN_CODE"))
                .isInstanceOf(IllegalArgumentException.class);

        List<String> messages = capturedMessages();

        boolean hasEntry = messages.stream()
                .anyMatch(msg -> msg.startsWith(">>"));
        boolean hasError = messages.stream()
                .anyMatch(msg -> msg.startsWith("!!"));
        boolean hasExit = messages.stream()
                .anyMatch(msg -> msg.startsWith("<<"));

        assertThat(hasEntry).as("Entry log (>>) must be present").isTrue();
        assertThat(hasError).as("Error log (!!) must be present").isTrue();
        assertThat(hasExit).as("Exit log (<<) must NOT be present when exception is thrown").isFalse();
    }
}
