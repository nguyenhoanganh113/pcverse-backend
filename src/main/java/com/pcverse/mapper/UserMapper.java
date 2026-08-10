package com.pcverse.mapper;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.entity.User;
import com.pcverse.entity.UserHasRole;
import com.pcverse.enums.Gender;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "userHasRoles", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(
            target = "gender",
            source = "gender",
            qualifiedByName = "toGender"
    )
    User toUser(CreateUserRequest createUserRequest);

    CreateUserResponse toCreateUserResponse(User user);

    @Mapping(
            target = "roles",
            source = "userHasRoles",
            qualifiedByName = "toRoleNames"
    )
    UserDetailsResponse toUserDetailResponse(User user);

    @Named("toGender")
    default Gender toGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }

        return Gender.valueOf(
                gender.trim().toUpperCase(Locale.ROOT)
        );
    }

    @Named("toRoleNames")
    default List<String> toRoleNames(
            Set<UserHasRole> userHasRoles
    ) {
        if (userHasRoles == null) {
            return List.of();
        }

        return userHasRoles.stream()
                .map(userHasRole ->
                        userHasRole.getRole().getRoleName()
                )
                .sorted()
                .toList();
    }
}
