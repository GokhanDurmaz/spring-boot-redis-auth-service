package com.example.demo_app.event;

import com.example.demo_app.entity.Payment;
import com.example.demo_app.entity.Refund;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentRefundedEvent extends ApplicationEvent {
    private final Payment payment;
    private final Refund refund;

    public PaymentRefundedEvent(Object source, Payment payment, Refund refund) {
        super(source);
        this.payment = payment;
        this.refund = refund;
    }
}
