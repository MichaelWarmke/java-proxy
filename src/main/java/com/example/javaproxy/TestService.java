package com.example.javaproxy;

import com.example.javaproxy.aspect.RecordWiremock;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class TestService {

    @RecordWiremock
    public String getGreeting(String name) {
        return "Hello, " + name + "!";
    }

    @RecordWiremock
    public Integer add(Integer a, Integer b) {
        return a + b;
    }

    @RecordWiremock
    public Mono<String> getGreetingReactive(String name) {
        return Mono.delay(Duration.ofMillis(10)).map(d -> "Hello, " + name + "!");
    }
}
