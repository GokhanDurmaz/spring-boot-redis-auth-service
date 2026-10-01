package com.example.demo_app.controller;

import com.example.demo_app.event.listener.AnalyticsPaymentEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsPaymentEventListener analyticsListener;

    @GetMapping("/payments")
    public ResponseEntity<AnalyticsPaymentEventListener.AnalyticsSnapshot> getPaymentAnalytics() {
        log.debug("Fetching payment analytics");
        AnalyticsPaymentEventListener.AnalyticsSnapshot snapshot = analyticsListener.getSnapshot();
        return ResponseEntity.ok(snapshot);
    }
}
