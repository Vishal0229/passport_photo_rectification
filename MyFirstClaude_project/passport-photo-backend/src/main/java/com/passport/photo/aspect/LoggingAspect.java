package com.passport.photo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * AOP aspect that logs method entry, exit, and exceptions for all classes
 * in the {@code com.passport.photo} package tree.
 *
 * <p>Entry and exit are logged at DEBUG. Exceptions are logged at ERROR and
 * re-thrown unchanged. The aspect excludes itself from its own pointcut to
 * prevent infinite recursion.</p>
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Matches all methods in every sub-package of {@code com.passport.photo},
     * but excludes this aspect class itself.
     */
    @Pointcut("execution(* com.passport.photo..*(..)) && !within(com.passport.photo.aspect.LoggingAspect)")
    public void applicationMethods() {}

    @Around("applicationMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();

        // Build comma-separated list of parameter type simple names
        String paramTypes = Arrays.stream(signature.getMethod().getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));

        log.debug(">> {}.{}({})", className, methodName, paramTypes);

        try {
            Object result = joinPoint.proceed();

            // Determine return type simple name
            Class<?> returnType = signature.getMethod().getReturnType();
            String returnTypeName = returnType == void.class ? "void" : returnType.getSimpleName();

            log.debug("<< {}.{} → {}", className, methodName, returnTypeName);
            return result;

        } catch (Throwable ex) {
            String safeMsg = ex.getMessage() == null ? "" : ex.getMessage().replaceAll("[\r\n]", " ");
            log.error("!! {}.{} threw {}: {}", className, methodName,
                    ex.getClass().getSimpleName(), safeMsg);
            throw ex;
        }
    }
}
