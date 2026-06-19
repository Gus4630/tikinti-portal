package az.tikinti.portal.util;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LogUtil {

    public static Map<String, Object> getParamsAsMap(String[] names, Object[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], args[i]);
        }
        return map;
    }
}
