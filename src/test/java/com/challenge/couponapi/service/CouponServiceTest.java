package com.challenge.couponapi.service;

import com.challenge.couponapi.domain.Coupon;
import com.challenge.couponapi.dto.CreateCouponRequest;
import com.challenge.couponapi.dto.CouponResponse;
import com.challenge.couponapi.exception.BusinessException;
import com.challenge.couponapi.exception.NotFoundException;
import com.challenge.couponapi.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(30);

    @Test
    @DisplayName("Should create coupon and return response with sanitized code")
    void create_validRequest_returnsCouponResponse() {
        CreateCouponRequest request = new CreateCouponRequest(
                "ABC123", "Test coupon", new BigDecimal("10.0"), FUTURE_DATE, false
        );

        Coupon savedCoupon = Coupon.create("ABC123", "Test coupon", new BigDecimal("10.0"), FUTURE_DATE, false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(savedCoupon);

        CouponResponse response = couponService.create(request);

        assertThat(response.code()).isEqualTo("ABC123");
        assertThat(response.description()).isEqualTo("Test coupon");
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }

    @Test
    @DisplayName("Should treat null published as false")
    void create_nullPublished_treatedAsFalse() {
        CreateCouponRequest request = new CreateCouponRequest(
                "ABC123", "Test", new BigDecimal("10.0"), FUTURE_DATE, null
        );
        Coupon savedCoupon = Coupon.create("ABC123", "Test", new BigDecimal("10.0"), FUTURE_DATE, false);
        when(couponRepository.save(any())).thenReturn(savedCoupon);

        CouponResponse response = couponService.create(request);

        assertThat(response.published()).isFalse();
    }

    @Test
    @DisplayName("Should soft delete coupon when it exists and is not deleted")
    void delete_existingActiveCoupon_deletesSuccessfully() {
        UUID id = UUID.randomUUID();
        Coupon coupon = Coupon.create("ABC123", "Test", new BigDecimal("10.0"), FUTURE_DATE, false);
        when(couponRepository.findByIdIncludingDeleted(id)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any())).thenReturn(coupon);

        assertThatCode(() -> couponService.delete(id)).doesNotThrowAnyException();

        assertThat(coupon.isDeleted()).isTrue();
        verify(couponRepository, times(1)).save(coupon);
    }

    @Test
    @DisplayName("Should throw NotFoundException when coupon does not exist")
    void delete_nonExistentCoupon_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(couponRepository.findByIdIncludingDeleted(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.delete(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("Should throw BusinessException when coupon is already deleted")
    void delete_alreadyDeletedCoupon_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        Coupon coupon = Coupon.create("ABC123", "Test", new BigDecimal("10.0"), FUTURE_DATE, false);
        coupon.delete(); // pre-delete
        when(couponRepository.findByIdIncludingDeleted(id)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Coupon already deleted");
    }
}
