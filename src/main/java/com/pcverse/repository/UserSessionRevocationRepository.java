package com.pcverse.repository;

import com.pcverse.entity.UserSessionRevocation;
import org.springframework.data.repository.CrudRepository;

public interface UserSessionRevocationRepository
        extends CrudRepository<UserSessionRevocation, String> {
}
