package com.example.demo_app.event;

import com.example.demo_app.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentSucceededEvent extends ApplicationEvent {
    private final Payment payment;

    public PaymentSucceededEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }
}
