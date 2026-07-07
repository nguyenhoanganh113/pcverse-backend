package com.pcverse.service.impl;

import com.pcverse.entity.SecurityUser;
import com.pcverse.entity.User;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.DatabaseUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsServiceImpl implements DatabaseUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserServiceException(ErrorCode.USER_NOT_FOUND));

        return new SecurityUser(user);
    }
}
