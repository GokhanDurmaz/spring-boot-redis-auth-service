package com.example.demo_app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, this is 'hello'.";
    }
}
