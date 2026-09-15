package de.muenchen.oss.pscdeakte.configuration;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AspectConfiguration {

    @Around("@annotation(de.muenchen.oss.pscdeakte.configuration.LogExecutionTime)")
    public Object logExecutionTime(final ProceedingJoinPoint joinPoint) throws Throwable {
        final long start = System.currentTimeMillis();
        final Object proceed = joinPoint.proceed();
        final long executionTime = System.currentTimeMillis() - start;
        log.debug(">> {} executed in {} seconds", joinPoint.getSignature(), executionTime / 1000.0);
        return proceed;
    }
}
