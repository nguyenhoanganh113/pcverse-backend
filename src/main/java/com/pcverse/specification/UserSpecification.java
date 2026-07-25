package com.pcverse.specification;

import com.pcverse.dto.request.AdminUserSearchRequest;
import com.pcverse.entity.User;
import com.pcverse.enums.UserSearchMode;
import com.pcverse.enums.UserStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> filter(
            AdminUserSearchRequest request
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request == null) {
                predicates.add(criteriaBuilder.notEqual(
                        root.get("userStatus"),
                        UserStatus.DELETED
                ));
                return criteriaBuilder.and(
                        predicates.toArray(Predicate[]::new)
                );
            }

            if (request.mode() != UserSearchMode.ATTRIBUTE
                    || request.userStatus() != UserStatus.DELETED) {
                predicates.add(criteriaBuilder.notEqual(
                        root.get("userStatus"),
                        UserStatus.DELETED
                ));
            }

            boolean exact = Boolean.TRUE.equals(request.exact());

            if (request.mode() == UserSearchMode.ATTRIBUTE) {
                addTextPredicate(
                        predicates,
                        root,
                        criteriaBuilder,
                        "username",
                        request.username(),
                        exact
                );
                addTextPredicate(
                        predicates,
                        root,
                        criteriaBuilder,
                        "email",
                        request.email(),
                        exact
                );
                addTextPredicate(
                        predicates,
                        root,
                        criteriaBuilder,
                        "firstName",
                        request.firstName(),
                        exact
                );
                addTextPredicate(
                        predicates,
                        root,
                        criteriaBuilder,
                        "lastName",
                        request.lastName(),
                        exact
                );
                addTextPredicate(
                        predicates,
                        root,
                        criteriaBuilder,
                        "phoneNumber",
                        request.phoneNumber(),
                        exact
                );
                addAttributePredicates(
                        predicates,
                        root,
                        criteriaBuilder,
                        request
                );
            } else {
                addTextPredicate(
                        predicates,
                        root,
                        criteriaBuilder,
                        "username",
                        request.search(),
                        exact
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private static void addAttributePredicates(
            List<Predicate> predicates,
            Root<User> root,
            CriteriaBuilder criteriaBuilder,
            AdminUserSearchRequest request
    ) {
        if (request.gender() != null) {
            predicates.add(criteriaBuilder.equal(
                    root.get("gender"),
                    request.gender()
            ));
        }

        if (request.userStatus() != null) {
            predicates.add(criteriaBuilder.equal(
                    root.get("userStatus"),
                    request.userStatus()
            ));
        }

        if (request.dateOfBirthFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.<LocalDate>get("dateOfBirth"),
                    request.dateOfBirthFrom()
            ));
        }

        if (request.dateOfBirthTo() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.<LocalDate>get("dateOfBirth"),
                    request.dateOfBirthTo()
            ));
        }
    }

    private static void addTextPredicate(
            List<Predicate> predicates,
            Root<User> root,
            CriteriaBuilder criteriaBuilder,
            String field,
            String value,
            boolean exact
    ) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return;
        }

        Expression<String> normalizedField = criteriaBuilder.lower(
                root.get(field)
        );

        if (exact) {
            predicates.add(criteriaBuilder.equal(
                    normalizedField,
                    normalizedValue
            ));
            return;
        }

        predicates.add(criteriaBuilder.like(
                normalizedField,
                "%" + escapeLikePattern(normalizedValue) + "%",
                '\\'
        ));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private static String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
