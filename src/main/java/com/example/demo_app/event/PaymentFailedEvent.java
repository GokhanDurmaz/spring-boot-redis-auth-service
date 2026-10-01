package com.example.demo_app.event;

import com.example.demo_app.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentFailedEvent extends ApplicationEvent {
    private final Payment payment;
    private final String failureReason;

    public PaymentFailedEvent(Object source, Payment payment, String failureReason) {
        super(source);
        this.payment = payment;
        this.failureReason = failureReason;
    }
}
