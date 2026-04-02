package com.challenge.couponapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCouponRequest(
        @NotBlank(message = "Code is required")
        String code,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Discount value is required")
        BigDecimal discountValue,

        @NotNull(message = "Expiration date is required")
        LocalDate expirationDate,

        Boolean published
) {}
