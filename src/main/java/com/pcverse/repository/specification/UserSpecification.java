package com.pcverse.repository.specification;

import com.pcverse.entity.User;
import com.pcverse.enums.Gender;
import com.pcverse.enums.UserSearchField;
import com.pcverse.enums.UserStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Locale;

import static com.pcverse.repository.specification.SpecificationUtils.escapeLikePattern;

@UtilityClass
public class UserSpecification {

    /**
     * Default Search:
     * Một keyword tìm đồng thời trên nhiều field bằng OR.
     */
    public Specification<User> hasKeyword(String keyword) {
        return (root, query, cb) -> {

            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }

            String normalizedKeyword = keyword
                    .trim()
                    .toLowerCase(Locale.ROOT);

            String pattern = "%"
                    + escapeLikePattern(normalizedKeyword)
                    + "%";

            return cb.or(
                    cb.like(
                            cb.lower(root.get("username")),
                            pattern,
                            '\\'
                    ),
                    cb.like(
                            cb.lower(root.get("email")),
                            pattern,
                            '\\'
                    ),
                    cb.like(
                            cb.lower(root.get("firstName")),
                            pattern,
                            '\\'
                    ),
                    cb.like(
                            cb.lower(root.get("lastName")),
                            pattern,
                            '\\'
                    ),
                    cb.like(
                            cb.lower(root.get("phoneNumber")),
                            pattern,
                            '\\'
                    )
            );
        };
    }

    /**
     * Attribute Search:
     * Chỉ tìm kiếm trên đúng một field mà user đã chọn.
     */
    public Specification<User> hasAttribute(
            UserSearchField field,
            String value,
            boolean exact
    ) {
        return (root, query, cb) -> {

            if(!StringUtils.hasText(value) || field == null) {
                return cb.conjunction();
            }

            return switch (field) {
                case USERNAME,
                     EMAIL,
                     FIRST_NAME,
                     LAST_NAME,
                     PHONE_NUMBER -> {
                    Expression<String> attributeExpression =
                            cb.lower(
                                    root.get(field.path())
                            );
                    yield createTextPredicate(
                            cb,
                            attributeExpression,
                            value,
                            exact
                    );
                }
                case GENDER -> cb.equal(
                        root.get(field.path()),
                        parseEnum(Gender.class, value)
                );
                case USER_STATUS -> cb.equal(
                        root.get(field.path()),
                        parseEnum(UserStatus.class, value)
                );
                case DATE_OF_BIRTH -> cb.equal(
                        root.<LocalDate>get(field.path()),
                        LocalDate.parse(value.trim())
                );
            };
        };
    }

    /**
     * Loại user đã soft delete khỏi kết quả.
     */
    public Specification<User> isNotDeleted() {
        return (root, query, cb) ->
                cb.notEqual(
                        root.get("userStatus"),
                        UserStatus.DELETED
                );
    }

    private Predicate createTextPredicate(
            CriteriaBuilder cb,
            Expression<String> attributeExpression,
            String value,
            boolean exact
    ) {
        String normalizedValue = normalize(value);

        if (exact) {
            return cb.equal(
                    attributeExpression,
                    normalizedValue
            );
        }

        String pattern = "%"
                + escapeLikePattern(normalizedValue)
                + "%";

        return cb.like(attributeExpression, pattern, '\\');
    }

    private <E extends Enum<E>> E parseEnum(
            Class<E> enumClass,
            String value
    ) {
        return Enum.valueOf(
                enumClass,
                value.trim().toUpperCase(Locale.ROOT)
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
