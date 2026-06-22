package az.tikinti.portal.aop;

import static net.logstash.logback.argument.StructuredArguments.v;

import az.tikinti.portal.util.LogUtil;
import az.tikinti.portal.util.WebUtil;
import java.lang.reflect.Method;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private final WebUtil webUtil;

    @Around("execution(* az.tikinti.portal.controller..*(..))")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        if (isNoLog(joinPoint)) return joinPoint.proceed();

        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        Map<String, Object> params = LogUtil.getParamsAsMap(sig.getParameterNames(), joinPoint.getArgs());
        String traceId = webUtil.getTraceId();

        log.info("[IN]  {} params={} traceId={}", method,
                v("params", params), v("traceId", traceId));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("[OUT] {} elapsed={}ms traceId={}", method,
                    System.currentTimeMillis() - start, v("traceId", traceId));
            return result;
        } catch (Throwable t) {
            log.warn("[ERR] {} elapsed={}ms error={} traceId={}", method,
                    System.currentTimeMillis() - start, t.getMessage(), v("traceId", traceId));
            throw t;
        }
    }

    @Around("execution(* az.tikinti.portal.service..*(..))")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        if (isNoLog(joinPoint)) return joinPoint.proceed();

        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        Map<String, Object> params = LogUtil.getParamsAsMap(sig.getParameterNames(), joinPoint.getArgs());
        String traceId = webUtil.getTraceId();

        log.debug("[IN]  {} params={} traceId={}", method,
                v("params", params), v("traceId", traceId));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.debug("[OUT] {} elapsed={}ms traceId={}", method,
                    System.currentTimeMillis() - start, v("traceId", traceId));
            return result;
        } catch (Throwable t) {
            log.warn("[ERR] {} elapsed={}ms error={} traceId={}", method,
                    System.currentTimeMillis() - start, t.getMessage(), v("traceId", traceId));
            throw t;
        }
    }

    private boolean isNoLog(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        return method.isAnnotationPresent(NoLog.class)
                || method.getDeclaringClass().isAnnotationPresent(NoLog.class);
    }
}
