package com.challenge.couponapi.domain;

import com.challenge.couponapi.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@SQLRestriction("deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean redeemed = false;

    @Column(nullable = false)
    private boolean deleted = false;

    public static Coupon create(String code, String description, BigDecimal discountValue,
                                LocalDate expirationDate, boolean published) {
        String sanitizedCode = sanitize(code);
        validateCode(sanitizedCode);
        validateExpirationDate(expirationDate);
        validateDiscountValue(discountValue);

        Coupon coupon = new Coupon();
        coupon.code = sanitizedCode;
        coupon.description = description;
        coupon.discountValue = discountValue;
        coupon.expirationDate = expirationDate;
        coupon.published = published;
        return coupon;
    }

    public void delete() {
        if (this.deleted) {
            throw new BusinessException("Coupon already deleted");
        }
        this.deleted = true;
    }

    public CouponStatus getStatus() {
        return deleted ? CouponStatus.DELETED : (published ? CouponStatus.ACTIVE : CouponStatus.INACTIVE);
//        if (deleted) return CouponStatus.DELETED;
//        if (published) return CouponStatus.ACTIVE;
//        return CouponStatus.INACTIVE;
    }

    private static String sanitize(String code) {
        return code.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    private static void validateCode(String sanitizedCode) {
        if (sanitizedCode.length() != 6) {
            throw new BusinessException(
                    "Code must have exactly 6 alphanumeric characters after sanitization, got: "
                            + sanitizedCode.length());
        }
    }

    private static void validateExpirationDate(LocalDate expirationDate) {
        if (expirationDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Expiration date cannot be in the past");
        }
    }

    private static void validateDiscountValue(BigDecimal discountValue) {
        if (discountValue.compareTo(new BigDecimal("0.5")) < 0) {
            throw new BusinessException("Discount value must be at least 0.5");
        }
    }
}
