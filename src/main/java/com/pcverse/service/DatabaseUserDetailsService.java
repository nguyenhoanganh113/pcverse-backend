package com.pcverse.service;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface DatabaseUserDetailsService extends UserDetailsService {

    UserDetails loadUserByUsername(@NonNull String email);

}
