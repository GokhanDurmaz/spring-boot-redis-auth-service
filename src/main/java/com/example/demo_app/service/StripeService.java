package com.example.demo_app.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class StripeService {

    public Account createConnectedAccount(String businessName, String businessEmail) throws StripeException {
        log.info("Creating Stripe connected account for: {}", businessName);
        
        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry("US")
                .setEmail(businessEmail)
                .putMetadata("business_name", businessName)
                .build();

        Account account = Account.create(params);
        log.info("Created Stripe account: {}", account.getId());
        return account;
    }

    public Account retrieveAccount(String stripeAccountId) throws StripeException {
        log.info("Retrieving Stripe account: {}", stripeAccountId);
        return Account.retrieve(stripeAccountId);
    }

    public Customer createOrRetrieveCustomer(String customerId, String email, String name) throws StripeException {
        log.info("Creating/retrieving Stripe customer: {}", customerId);
        
        // Try to retrieve existing customer
        try {
            return Customer.retrieve(customerId);
        } catch (StripeException e) {
            log.debug("Customer not found, creating new one");
        }

        // Create new customer
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .putMetadata("customer_id", customerId)
                .build();

        Customer customer = Customer.create(params);
        log.info("Created Stripe customer: {}", customer.getId());
        return customer;
    }

    public PaymentMethod createPaymentMethod(String token, String type) throws StripeException {
        log.info("Creating Stripe payment method with token: {}", token);
        
        PaymentMethodCreateParams.Builder paramsBuilder = PaymentMethodCreateParams.builder();
        
        if ("card".equalsIgnoreCase(type)) {
            paramsBuilder.setType(PaymentMethodCreateParams.Type.CARD);
        } else if ("bank_account".equalsIgnoreCase(type)) {
            paramsBuilder.setType(PaymentMethodCreateParams.Type.US_BANK_ACCOUNT);
        }

        PaymentMethod paymentMethod = PaymentMethod.create(paramsBuilder.build());
        log.info("Created Stripe payment method: {}", paymentMethod.getId());
        return paymentMethod;
    }

    public PaymentMethod retrievePaymentMethod(String stripePaymentMethodId) throws StripeException {
        log.debug("Retrieving Stripe payment method: {}", stripePaymentMethodId);
        return PaymentMethod.retrieve(stripePaymentMethodId);
    }

    public void detachPaymentMethod(String stripePaymentMethodId) throws StripeException {
        log.info("Detaching Stripe payment method: {}", stripePaymentMethodId);
        PaymentMethod paymentMethod = PaymentMethod.retrieve(stripePaymentMethodId);
        paymentMethod.detach();
    }

    public PaymentIntent createPaymentIntent(String customerId, String paymentMethodId, 
                                            BigDecimal amount, String currency, 
                                            String description, String idempotencyKey) throws StripeException {
        log.info("Creating Stripe payment intent for amount: {} {}", amount, currency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCustomer(customerId)
                .setPaymentMethod(paymentMethodId)
                .setAmount(amount.multiply(new BigDecimal("100")).longValue()) // Convert to cents
                .setCurrency(currency)
                .setDescription(description)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.AUTOMATIC)
                .setConfirm(true)
                .putMetadata("idempotency_key", idempotencyKey)
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        log.info("Created Stripe payment intent: {}", intent.getId());
        return intent;
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        log.debug("Retrieving Stripe payment intent: {}", paymentIntentId);
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public com.stripe.model.Refund createRefund(String paymentIntentId, Long amountInCents, String reason) throws StripeException {
        log.info("Creating Stripe refund for amount: {} cents", amountInCents);
        
        com.stripe.param.RefundCreateParams params = com.stripe.param.RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(amountInCents)
                .setReason(com.stripe.param.RefundCreateParams.Reason.valueOf(reason.toUpperCase()))
                .build();

        com.stripe.model.Refund refund = com.stripe.model.Refund.create(params);
        log.info("Created Stripe refund: {}", refund.getId());
        return refund;
    }

    public com.stripe.model.Refund retrieveRefund(String refundId) throws StripeException {
        log.debug("Retrieving Stripe refund: {}", refundId);
        return com.stripe.model.Refund.retrieve(refundId);
    }
}