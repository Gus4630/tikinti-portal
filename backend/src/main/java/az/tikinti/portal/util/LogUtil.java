package az.tikinti.portal.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.multipart.MultipartFile;

@UtilityClass
public class LogUtil {

    private static final int MAX_STRING_LENGTH = 200;
    private static final int MAX_COLLECTION_SIZE = 10;

    public static Map<String, Object> getParamsAsMap(String[] names, Object[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], sanitize(args[i]));
        }
        return map;
    }

    private static Object sanitize(Object arg) {
        if (arg == null) return null;

        if (AopUtils.isAopProxy(arg)) {
            return arg.toString();
        }

        if (arg instanceof byte[] bytes) {
            return "<binary: " + bytes.length + " bytes>";
        }

        if (arg instanceof MultipartFile file) {
            return "<file: name=" + file.getOriginalFilename()
                    + ", size=" + file.getSize() + " bytes"
                    + ", type=" + file.getContentType() + ">";
        }

        if (arg instanceof Collection<?> col) {
            int size = col.size();
            if (size > MAX_COLLECTION_SIZE) {
                return "<collection: " + size + " items>";
            }
            return arg;
        }

        if (arg instanceof String str && str.length() > MAX_STRING_LENGTH) {
            return str.substring(0, MAX_STRING_LENGTH) + "…<truncated, total=" + str.length() + " chars>";
        }

        return arg;
    }
}
