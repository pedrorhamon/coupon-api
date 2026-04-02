package com.challenge.couponapi.repository;

import com.challenge.couponapi.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    /**
     * Bypasses @SQLRestriction to find coupons regardless of deleted status.
     * Used by the delete operation to distinguish "not found" from "already deleted".
     */
    @Query(value = "SELECT * FROM coupons WHERE id = :id", nativeQuery = true)
    Optional<Coupon> findByIdIncludingDeleted(@Param("id") UUID id);
}
