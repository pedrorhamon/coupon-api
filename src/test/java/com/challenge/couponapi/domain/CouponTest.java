package com.challenge.couponapi.domain;

import com.challenge.couponapi.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class CouponTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(30);
    private static final BigDecimal VALID_DISCOUNT = new BigDecimal("10.0");

    // --- create() ---

    @Test
    @DisplayName("Should create coupon with valid data")
    void create_validData_returnsPopulatedCoupon() {
        Coupon coupon = Coupon.create("ABC123", "Test coupon", VALID_DISCOUNT, FUTURE_DATE, false);

        assertThat(coupon.getCode()).isEqualTo("ABC123");
        assertThat(coupon.getDescription()).isEqualTo("Test coupon");
        assertThat(coupon.getDiscountValue()).isEqualByComparingTo(VALID_DISCOUNT);
        assertThat(coupon.getExpirationDate()).isEqualTo(FUTURE_DATE);
        assertThat(coupon.isPublished()).isFalse();
        assertThat(coupon.isRedeemed()).isFalse();
        assertThat(coupon.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should create coupon as published when published=true")
    void create_publishedTrue_couponIsPublished() {
        Coupon coupon = Coupon.create("ABC123", "Test", VALID_DISCOUNT, FUTURE_DATE, true);
        assertThat(coupon.isPublished()).isTrue();
    }

    @Test
    @DisplayName("Should sanitize code removing special characters and convert to uppercase")
    void create_codeWithSpecialChars_sanitizesCode() {
        // "A!B@C#1$2%3" -> alphanumeric: "ABC123" (6 chars)
        Coupon coupon = Coupon.create("A!B@C#1$2%3", "Test", VALID_DISCOUNT, FUTURE_DATE, false);
        assertThat(coupon.getCode()).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("Should convert lowercase code to uppercase during sanitization")
    void create_lowercaseCode_convertsToUppercase() {
        Coupon coupon = Coupon.create("abc123", "Test", VALID_DISCOUNT, FUTURE_DATE, false);
        assertThat(coupon.getCode()).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("Should throw BusinessException when sanitized code has less than 6 chars")
    void create_sanitizedCodeTooShort_throwsBusinessException() {
        assertThatThrownBy(() -> Coupon.create("AB@C1", "Test", VALID_DISCOUNT, FUTURE_DATE, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("6 alphanumeric characters");
    }

    @Test
    @DisplayName("Should throw BusinessException when sanitized code has more than 6 chars")
    void create_sanitizedCodeTooLong_throwsBusinessException() {
        assertThatThrownBy(() -> Coupon.create("ABCDEFG", "Test", VALID_DISCOUNT, FUTURE_DATE, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("6 alphanumeric characters");
    }

    @Test
    @DisplayName("Should throw BusinessException when expiration date is in the past")
    void create_pastExpirationDate_throwsBusinessException() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        assertThatThrownBy(() -> Coupon.create("ABC123", "Test", VALID_DISCOUNT, pastDate, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Expiration date cannot be in the past");
    }

    @Test
    @DisplayName("Should allow coupon creation with today as expiration date")
    void create_todayAsExpirationDate_doesNotThrow() {
        assertThatCode(() -> Coupon.create("ABC123", "Test", VALID_DISCOUNT, LocalDate.now(), false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw BusinessException when discount value is below 0.5")
    void create_discountValueBelowMinimum_throwsBusinessException() {
        assertThatThrownBy(() -> Coupon.create("ABC123", "Test", new BigDecimal("0.4"), FUTURE_DATE, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Discount value must be at least 0.5");
    }

    @Test
    @DisplayName("Should allow creation with minimum discount value of 0.5")
    void create_minimumDiscountValue_doesNotThrow() {
        assertThatCode(() -> Coupon.create("ABC123", "Test", new BigDecimal("0.5"), FUTURE_DATE, false))
                .doesNotThrowAnyException();
    }

    // --- delete() ---

    @Test
    @DisplayName("Should soft delete an active coupon")
    void delete_activeCoupon_setsDeletedTrue() {
        Coupon coupon = Coupon.create("ABC123", "Test", VALID_DISCOUNT, FUTURE_DATE, false);
        coupon.delete();
        assertThat(coupon.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should throw BusinessException when deleting an already deleted coupon")
    void delete_alreadyDeletedCoupon_throwsBusinessException() {
        Coupon coupon = Coupon.create("ABC123", "Test", VALID_DISCOUNT, FUTURE_DATE, false);
        coupon.delete();

        assertThatThrownBy(coupon::delete)
                .isInstanceOf(BusinessException.class)
                .hasMessage("Coupon already deleted");
    }

    // --- getStatus() ---

    @Test
    @DisplayName("Should return ACTIVE when coupon is published and not deleted")
    void getStatus_publishedNotDeleted_returnsActive() {
        Coupon coupon = Coupon.create("ABC123", "Test", VALID_DISCOUNT, FUTURE_DATE, true);
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return INACTIVE when coupon is not published and not deleted")
    void getStatus_notPublishedNotDeleted_returnsInactive() {
        Coupon coupon = Coupon.create("ABC123", "Test", VALID_DISCOUNT, FUTURE_DATE, false);
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should return DELETED when coupon is deleted")
    void getStatus_deleted_returnsDeleted() {
        Coupon coupon = Coupon.create("ABC123", "Test", VALID_DISCOUNT, FUTURE_DATE, true);
        coupon.delete();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.DELETED);
    }
}
