package com.challenge.couponapi.controller;

import com.challenge.couponapi.domain.Coupon;
import com.challenge.couponapi.repository.CouponRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CouponControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM coupons");
    }

    // --- POST /coupon ---

    @Test
    @DisplayName("POST /coupon - should create coupon and return 201")
    void createCoupon_validRequest_returns201() throws Exception {
        Map<String, Object> body = Map.of(
                "code", "ABC123",
                "description", "Test coupon",
                "discountValue", 10.0,
                "expirationDate", LocalDate.now().plusDays(30).toString(),
                "published", false
        );

        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value("ABC123"))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.redeemed").value(false));
    }

    @Test
    @DisplayName("POST /coupon - should sanitize special chars in code and return sanitized code")
    void createCoupon_codeWithSpecialChars_sanitizesAndReturns201() throws Exception {
        Map<String, Object> body = Map.of(
                "code", "A!B@C#1$2%3",
                "description", "Test",
                "discountValue", 5.0,
                "expirationDate", LocalDate.now().plusDays(30).toString()
        );

        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ABC123"));
    }

    @Test
    @DisplayName("POST /coupon - should return ACTIVE status when published=true")
    void createCoupon_publishedTrue_returnsActiveStatus() throws Exception {
        Map<String, Object> body = Map.of(
                "code", "ABC123",
                "description", "Test",
                "discountValue", 5.0,
                "expirationDate", LocalDate.now().plusDays(30).toString(),
                "published", true
        );

        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /coupon - should return 422 when expiration date is in the past")
    void createCoupon_pastExpirationDate_returns422() throws Exception {
        Map<String, Object> body = Map.of(
                "code", "ABC123",
                "description", "Test",
                "discountValue", 10.0,
                "expirationDate", LocalDate.now().minusDays(1).toString()
        );

        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Business Rule Violation"));
    }

    @Test
    @DisplayName("POST /coupon - should return 422 when discount value is below 0.5")
    void createCoupon_discountValueBelowMinimum_returns422() throws Exception {
        Map<String, Object> body = Map.of(
                "code", "ABC123",
                "description", "Test",
                "discountValue", 0.4,
                "expirationDate", LocalDate.now().plusDays(30).toString()
        );

        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /coupon - should return 400 when required fields are missing")
    void createCoupon_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    // --- DELETE /coupon/{id} ---

    @Test
    @DisplayName("DELETE /coupon/{id} - should soft delete coupon and return 204")
    void deleteCoupon_existingCoupon_returns204() throws Exception {
        Coupon coupon = couponRepository.save(
                Coupon.create("ABC123", "Test", new BigDecimal("10.0"), LocalDate.now().plusDays(30), false)
        );

        mockMvc.perform(delete("/coupon/" + coupon.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /coupon/{id} - should return 404 when coupon does not exist")
    void deleteCoupon_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/coupon/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("DELETE /coupon/{id} - should return 422 when coupon is already deleted")
    void deleteCoupon_alreadyDeleted_returns422() throws Exception {
        Coupon coupon = couponRepository.save(
                Coupon.create("ABC123", "Test", new BigDecimal("10.0"), LocalDate.now().plusDays(30), false)
        );
        // First delete
        mockMvc.perform(delete("/coupon/" + coupon.getId()))
                .andExpect(status().isNoContent());

        // Second delete — should fail
        mockMvc.perform(delete("/coupon/" + coupon.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Coupon already deleted"));
    }
}
