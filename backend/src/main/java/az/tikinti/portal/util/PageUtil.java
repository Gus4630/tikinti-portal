package az.tikinti.portal.util;

import az.tikinti.portal.model.dto.request.PageableRequest;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UtilityClass
public class PageUtil {

    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public static Pageable createPageable(PageableRequest request) {
        if (request == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD));
        }
        return PageRequest.of(
                Optional.ofNullable(request.getPage()).orElse(0),
                Optional.ofNullable(request.getPerPage()).orElse(20),
                Sort.by(Optional.ofNullable(request.getSortOrder()).orElse(Sort.Direction.DESC),
                        DEFAULT_SORT_FIELD));
    }
}
