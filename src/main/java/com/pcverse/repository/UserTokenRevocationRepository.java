package com.pcverse.repository;

import com.pcverse.entity.UserTokenRevocation;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRevocationRepository
        extends CrudRepository<UserTokenRevocation, String> {
}
