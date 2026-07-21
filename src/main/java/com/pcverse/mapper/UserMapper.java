package com.pcverse.mapper;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.entity.User;
import com.pcverse.enums.Gender;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Locale;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    User toUser(CreateUserRequest createUserRequest);

    CreateUserResponse toCreateUserResponse(User user);

    @Mapping(target = "roles", expression = "java(toRoleNames(user))")
    UserDetailsResponse toUserDetailResponse(User user);

    default List<String> toRoleNames(User user) {
        return user.getUserHasRoles().stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .sorted()
                .toList();
    }

}
