package com.example.javaproxy;

import com.example.javaproxy.aspect.RecordWiremock;
import org.springframework.stereotype.Service;

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
}
