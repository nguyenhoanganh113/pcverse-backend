package com.pcverse.repository;

import com.pcverse.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    // Lấy địa chỉ default lên đầu những địa chỉ còn lại sắp xếp theo thứ tự ưu tiên ngày tạo mới nhất rồi id tăng dần
    @Query("""
            SELECT address
            FROM Address address
            WHERE address.user.id = :userId
            ORDER BY
                CASE WHEN address.isDefault = true THEN 0 ELSE 1 END,
                address.createdAt DESC,
                address.id ASC
            """)
    List<Address> findAllByUserId(@Param("userId") UUID userId);

    Optional<Address> findByIdAndUser_Id(UUID addressId, UUID userId);
}
