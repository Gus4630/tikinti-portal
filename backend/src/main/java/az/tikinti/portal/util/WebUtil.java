package az.tikinti.portal.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class WebUtil {

    public String getTraceId() {
        return MDC.get("traceId");
    }
}
