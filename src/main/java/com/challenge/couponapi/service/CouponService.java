package com.challenge.couponapi.service;

import com.challenge.couponapi.domain.Coupon;
import com.challenge.couponapi.dto.CreateCouponRequest;
import com.challenge.couponapi.dto.CouponResponse;
import com.challenge.couponapi.exception.NotFoundException;
import com.challenge.couponapi.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        Coupon coupon = Coupon.create(
                request.code(),
                request.description(),
                request.discountValue(),
                request.expirationDate(),
                Boolean.TRUE.equals(request.published())
        );
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(UUID id) {
        Coupon coupon = couponRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new NotFoundException("Coupon not found with id: " + id));
        coupon.delete();
        couponRepository.save(coupon);
    }
}
