package com.pcverse.mapper;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.entity.User;
import com.pcverse.enums.Gender;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Locale;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "gender", expression = "java(toGender(createUserRequest.gender()))")
    User toUser(CreateUserRequest createUserRequest);

    CreateUserResponse toCreateUserResponse(User user);

    default Gender toGender(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

}
