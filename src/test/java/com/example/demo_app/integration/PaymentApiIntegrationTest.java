package com.example.demo_app.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo_app.DemoAppApplication;
import com.example.demo_app.dto.*;
import com.example.demo_app.entity.*;
import com.example.demo_app.repository.*;
import com.example.demo_app.login.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Comprehensive end-to-end integration tests for the Payment Processing REST API.
 * Tests cover complete payment workflows including merchant registration, store creation,
 * payment method management, payment creation, refunds, and error scenarios.
 */
@SpringBootTest(classes = DemoAppApplication.class)
@ActiveProfiles("test")
public class PaymentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String merchantToken;
    private String otherMerchantToken;
    private Merchant testMerchant;
    private Merchant otherMerchant;
    private Store testStore;
    private PaymentMethod testPaymentMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize MockMvc from the web application context
        if (mockMvc == null && webApplicationContext != null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }

        // Clean up repositories
        refundRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        paymentRepository.deleteAll();
        storeRepository.deleteAll();
        merchantRepository.deleteAll();

        // Create test merchants with user IDs
        testMerchant = new Merchant();
        testMerchant.setUserId(1L);
        testMerchant.setBusinessName("Test Merchant 1");
        testMerchant.setBusinessEmail("merchant1@example.com");
        testMerchant.setBusinessPhone("555-0001");
        testMerchant.setTaxId("12-3456789");
        testMerchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        testMerchant.setStripeAccountId("acct_test123");
        testMerchant = merchantRepository.save(testMerchant);

        otherMerchant = new Merchant();
        otherMerchant.setUserId(2L);
        otherMerchant.setBusinessName("Test Merchant 2");
        otherMerchant.setBusinessEmail("merchant2@example.com");
        otherMerchant.setBusinessPhone("555-0002");
        otherMerchant.setTaxId("12-3456790");
        otherMerchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        otherMerchant.setStripeAccountId("acct_test456");
        otherMerchant = merchantRepository.save(otherMerchant);

        // Generate JWT tokens for merchants
        merchantToken = jwtUtil.generateToken(testMerchant.getBusinessEmail(), null);
        otherMerchantToken = jwtUtil.generateToken(otherMerchant.getBusinessEmail(), null);

        // Create test store
        testStore = new Store();
        testStore.setMerchant(testMerchant);
        testStore.setStoreName("Test Store");
        testStore.setWebsiteUrl("https://teststore.com");
        testStore.setStatus(Store.StoreStatus.ACTIVE);
        testStore = storeRepository.save(testStore);

        // Create test payment method
        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setUserId(testMerchant.getUserId());
        testPaymentMethod.setType(PaymentMethod.PaymentMethodType.CARD);
        testPaymentMethod.setStripePaymentMethodId("pm_test123");
        testPaymentMethod.setCardLast4("4242");
        testPaymentMethod.setCardExpiryMonth(12);
        testPaymentMethod.setCardExpiryYear(2025);
        testPaymentMethod = paymentMethodRepository.save(testPaymentMethod);
    }

    // ==================== MERCHANT TESTS ====================

    @Test
    void testMerchantCreation() throws Exception {
        // Verify merchants were created
        assert merchantRepository.count() >= 2;
    }

    @Test
    void testGetMerchantDetails() throws Exception {
        // Verify merchant was created and can be retrieved
        Merchant merchant = merchantRepository.findById(testMerchant.getId()).orElse(null);
        assert merchant != null;
        assert merchant.getBusinessEmail().equals("merchant1@example.com");
    }

    // ==================== STORE MANAGEMENT TESTS ====================

    @Test
    void testCreateStore() throws Exception {
        StoreRequestDTO storeCreate = new StoreRequestDTO();
        storeCreate.setStoreName("New Store");
        storeCreate.setWebsiteUrl("https://newstore.com");
        storeCreate.setStoreEmail("store@example.com");

        MvcResult result = mockMvc.perform(post("/api/v1/store")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(storeCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        StoreResponseDTO createdStore = objectMapper.readValue(
                result.getResponse().getContentAsString(), StoreResponseDTO.class);
        assert createdStore.getId() != null;
        assert createdStore.getStoreName().equals("New Store");
    }

    @Test
    void testGetStoresForMerchant() throws Exception {
        // Create additional store
        Store store2 = new Store();
        store2.setMerchant(testMerchant);
        store2.setStoreName("Store 2");
        store2.setWebsiteUrl("https://store2.com");
        store2.setStatus(Store.StoreStatus.ACTIVE);
        storeRepository.save(store2);

        mockMvc.perform(get("/api/v1/store")
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk());
    }

    // ==================== PAYMENT METHOD MANAGEMENT TESTS ====================

    @Test
    void testAddPaymentMethod() throws Exception {
        PaymentMethodRequestDTO paymentMethodCreate = new PaymentMethodRequestDTO();
        paymentMethodCreate.setType(PaymentMethod.PaymentMethodType.CARD);
        paymentMethodCreate.setStripeToken("pm_test456");
        paymentMethodCreate.setCardLast4("4242");
        paymentMethodCreate.setCardExpiryMonth(12);
        paymentMethodCreate.setCardExpiryYear(2025);

        MvcResult result = mockMvc.perform(post("/api/v1/payment-method")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentMethodCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentMethodResponseDTO createdMethod = objectMapper.readValue(
                result.getResponse().getContentAsString(), PaymentMethodResponseDTO.class);
        assert createdMethod.getId() != null;
    }

    @Test
    void testGetPaymentMethods() throws Exception {
        mockMvc.perform(get("/api/v1/payment-methods")
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDeletePaymentMethod() throws Exception {
        mockMvc.perform(delete("/api/v1/payment-method/" + testPaymentMethod.getId())
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isNoContent());

        assert !paymentMethodRepository.existsById(testPaymentMethod.getId());
    }

    // ==================== PAYMENT CREATION & LIFECYCLE TESTS ====================

    @Test
    void testCreatePayment() throws Exception {
        PaymentRequestDTO paymentCreate = new PaymentRequestDTO();
        paymentCreate.setStoreId(testStore.getId());
        paymentCreate.setAmount(new BigDecimal("100.00"));
        paymentCreate.setCurrency("USD");
        paymentCreate.setPaymentMethodId(testPaymentMethod.getId());
        paymentCreate.setDescription("Test payment");

        MvcResult result = mockMvc.perform(post("/api/v1/payment")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponseDTO createdPayment = objectMapper.readValue(
                result.getResponse().getContentAsString(), PaymentResponseDTO.class);
        assert createdPayment.getId() != null;
        assert createdPayment.getAmount().compareTo(new BigDecimal("100.00")) == 0;
        assert createdPayment.getCurrency().equals("USD");
    }

    @Test
    void testGetPaymentDetails() throws Exception {
        Payment payment = new Payment();
        payment.setStore(testStore);
        payment.setCustomerId(123L);
        payment.setStripePaymentIntentId("pi_test_123");
        payment.setAmount(new BigDecimal("75.00"));
        payment.setCurrency("USD");
        payment.setPaymentMethodId(testPaymentMethod.getId());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payment/" + payment.getId())
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(75.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void testListPayments() throws Exception {
        Payment payment1 = new Payment();
        payment1.setStore(testStore);
        payment1.setCustomerId(123L);
        payment1.setStripePaymentIntentId("pi_test_001");
        payment1.setAmount(new BigDecimal("100.00"));
        payment1.setCurrency("USD");
        payment1.setPaymentMethodId(testPaymentMethod.getId());
        payment1.setStatus(Payment.PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment1);

        Payment payment2 = new Payment();
        payment2.setStore(testStore);
        payment2.setCustomerId(124L);
        payment2.setStripePaymentIntentId("pi_test_002");
        payment2.setAmount(new BigDecimal("50.00"));
        payment2.setCurrency("USD");
        payment2.setPaymentMethodId(testPaymentMethod.getId());
        payment2.setStatus(Payment.PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment2);

        mockMvc.perform(get("/api/v1/payments")
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk());
    }

    // ==================== REFUND & CANCELLATION TESTS ====================

    @Test
    void testRefundPayment() throws Exception {
        Payment payment = new Payment();
        payment.setStore(testStore);
        payment.setCustomerId(456L);
        payment.setStripePaymentIntentId("pi_test_refund");
        payment.setAmount(new BigDecimal("150.00"));
        payment.setCurrency("USD");
        payment.setPaymentMethodId(testPaymentMethod.getId());
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment = paymentRepository.save(payment);

        RefundRequestDTO refundCreate = new RefundRequestDTO();
        refundCreate.setAmount(new BigDecimal("150.00"));
        refundCreate.setReason("customer_request");

        MvcResult result = mockMvc.perform(post("/api/v1/payment/" + payment.getId() + "/refund")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        RefundResponseDTO refund = objectMapper.readValue(
                result.getResponse().getContentAsString(), RefundResponseDTO.class);
        assert refund.getId() != null;
        assert refund.getAmount().compareTo(new BigDecimal("150.00")) == 0;
    }

    @Test
    void testPartialRefund() throws Exception {
        Payment payment = new Payment();
        payment.setStore(testStore);
        payment.setCustomerId(457L);
        payment.setStripePaymentIntentId("pi_test_partial");
        payment.setAmount(new BigDecimal("200.00"));
        payment.setCurrency("USD");
        payment.setPaymentMethodId(testPaymentMethod.getId());
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment = paymentRepository.save(payment);

        RefundRequestDTO refundCreate = new RefundRequestDTO();
        refundCreate.setAmount(new BigDecimal("75.00"));
        refundCreate.setReason("partial_return");

        mockMvc.perform(post("/api/v1/payment/" + payment.getId() + "/refund")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundCreate)))
                .andExpect(status().isCreated());
    }

    @Test
    void testCancelPayment() throws Exception {
        Payment payment = new Payment();
        payment.setStore(testStore);
        payment.setCustomerId(458L);
        payment.setStripePaymentIntentId("pi_test_cancel");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setPaymentMethodId(testPaymentMethod.getId());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        mockMvc.perform(post("/api/v1/payment/" + payment.getId() + "/cancel")
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk());
    }

    @Test
    void testCannotRefundCancelledPayment() throws Exception {
        Payment payment = new Payment();
        payment.setStore(testStore);
        payment.setCustomerId(459L);
        payment.setStripePaymentIntentId("pi_test_00");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setPaymentMethodId(testPaymentMethod.getId());
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment = paymentRepository.save(payment);

        RefundRequestDTO refundCreate = new RefundRequestDTO();
        refundCreate.setAmount(new BigDecimal("100.00"));
        refundCreate.setReason("customer_request");

        mockMvc.perform(post("/api/v1/payment/" + payment.getId() + "/refund")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundCreate)))
                .andExpect(status().isConflict());
    }

    // ==================== AUTHORIZATION & CROSS-MERCHANT TESTS ====================

    @Test
    void testCrossMerchantPaymentAccessDenied() throws Exception {
        Payment payment = new Payment();
        payment.setStore(testStore); // Store belongs to testMerchant
        payment.setCustomerId(460L);
        payment.setStripePaymentIntentId("pi_test_cross");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setPaymentMethodId(testPaymentMethod.getId());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        // Try to access with otherMerchant's token
        mockMvc.perform(get("/api/v1/payment/" + payment.getId())
                .header("Authorization", "Bearer " + otherMerchantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCrossMerchantRefundDenied() throws Exception {
        Payment payment = new Payment();
        payment.setStore(testStore);
        payment.setCustomerId(461L);
        payment.setStripePaymentIntentId("pi_test_cross_refund");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setPaymentMethodId(testPaymentMethod.getId());
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment = paymentRepository.save(payment);

        RefundRequestDTO refundCreate = new RefundRequestDTO();
        refundCreate.setAmount(new BigDecimal("100.00"));
        refundCreate.setReason("customer_request");

        // Try to refund with otherMerchant's token
        mockMvc.perform(post("/api/v1/payment/" + payment.getId() + "/refund")
                .header("Authorization", "Bearer " + otherMerchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundCreate)))
                .andExpect(status().isForbidden());
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    void testInvalidPaymentAmount() throws Exception {
        PaymentRequestDTO paymentCreate = new PaymentRequestDTO();
        paymentCreate.setStoreId(testStore.getId());
        paymentCreate.setAmount(new BigDecimal("-100.00")); // Invalid negative amount
        paymentCreate.setCurrency("USD");
        paymentCreate.setPaymentMethodId(testPaymentMethod.getId());

        mockMvc.perform(post("/api/v1/payment")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentCreate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNonexistentPaymentReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/payment/999999")
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testMissingAuthorizationToken() throws Exception {
        mockMvc.perform(get("/api/v1/payment/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== COMPLETE WORKFLOW TEST ====================

    @Test
    void testCompletePaymentWorkflow() throws Exception {
        // Step 1: Verify merchant exists
        Merchant merchant = merchantRepository.findById(testMerchant.getId()).orElse(null);
        assert merchant != null;

        // Step 2: Verify store exists
        Store store = storeRepository.findById(testStore.getId()).orElse(null);
        assert store != null;

        // Step 3: Add payment method
        PaymentMethodRequestDTO paymentMethodCreate = new PaymentMethodRequestDTO();
        paymentMethodCreate.setType(PaymentMethod.PaymentMethodType.CARD);
        paymentMethodCreate.setStripeToken("pm_workflow123");
        paymentMethodCreate.setCardLast4("4242");
        paymentMethodCreate.setCardExpiryMonth(12);
        paymentMethodCreate.setCardExpiryYear(2025);

        mockMvc.perform(post("/api/v1/payment-method")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentMethodCreate)))
                .andExpect(status().isCreated());

        // Step 4: Create payment
        PaymentRequestDTO paymentCreate = new PaymentRequestDTO();
        paymentCreate.setStoreId(store.getId());
        paymentCreate.setAmount(new BigDecimal("299.99"));
        paymentCreate.setCurrency("USD");
        paymentCreate.setPaymentMethodId(testPaymentMethod.getId());
        paymentCreate.setDescription("Workflow test payment");

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payment")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponseDTO payment = objectMapper.readValue(
                paymentResult.getResponse().getContentAsString(), PaymentResponseDTO.class);

        // Step 5: Verify payment details
        mockMvc.perform(get("/api/v1/payment/" + payment.getId())
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(299.99))
                .andExpect(jsonPath("$.currency").value("USD"));

        // Step 6: Simulate webhook callback (payment succeeded)
        Payment paymentEntity = paymentRepository.findById(payment.getId()).orElse(null);
        assert paymentEntity != null;
        paymentEntity.setStatus(Payment.PaymentStatus.SUCCEEDED);
        paymentEntity.setSucceededAt(LocalDateTime.now());
        paymentRepository.save(paymentEntity);

        mockMvc.perform(get("/api/v1/payment/" + payment.getId())
                .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk());

        // Step 7: Perform refund
        RefundRequestDTO refundCreate = new RefundRequestDTO();
        refundCreate.setAmount(new BigDecimal("299.99"));
        refundCreate.setReason("customer_request");

        mockMvc.perform(post("/api/v1/payment/" + payment.getId() + "/refund")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundCreate)))
                .andExpect(status().isCreated());
    }
}
