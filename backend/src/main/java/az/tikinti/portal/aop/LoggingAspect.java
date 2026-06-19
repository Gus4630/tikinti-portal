package az.tikinti.portal.aop;

import static net.logstash.logback.argument.StructuredArguments.v;

import az.tikinti.portal.util.LogUtil;
import az.tikinti.portal.util.WebUtil;
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
    public Object logControllerEndpoints(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAround("Request", joinPoint);
    }

    @Around("execution(* az.tikinti.portal.service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAround("Service", joinPoint);
    }

    private Object logAround(String label, ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        Map<String, Object> params = LogUtil.getParamsAsMap(signature.getParameterNames(), joinPoint.getArgs());
        String traceId = webUtil.getTraceId();

        log.debug("[{}] | [{}.{}] | Params: {}", label, className, methodName,
                v("params", params), v("trace_id", traceId));
        final long start = System.currentTimeMillis();
        final Object result = joinPoint.proceed();
        log.debug("[{}-Done] | [{}.{}] | Elapsed: {} ms | Result: {}",
                label, className, methodName, System.currentTimeMillis() - start,
                result, v("trace_id", traceId));
        return result;
    }
}
