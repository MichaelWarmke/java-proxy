
package com.example.javaproxy.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class WiremockAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String wiremockDir = "wiremock";

    public WiremockAspect() {
        try {
            Files.createDirectories(Paths.get(wiremockDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Around("@annotation(com.example.javaproxy.aspect.RecordWiremock)")
    public Object generateWiremockStub(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        HttpServletRequest request = (HttpServletRequest) args[0];

        Object result = joinPoint.proceed();

        ResponseEntity<String> response = (ResponseEntity<String>) result;

        Map<String, Object> stub = new HashMap<>();
        stub.put("request", buildRequestStub(request));
        stub.put("response", buildResponseStub(response));

        String filename = request.getMethod() + "_" + Math.abs(request.getRequestURI().hashCode()) + ".json";
        File file = new File(wiremockDir, filename);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, stub);

        return result;
    }

    private Map<String, Object> buildRequestStub(HttpServletRequest request) {
        Map<String, Object> requestStub = new HashMap<>();
        requestStub.put("method", request.getMethod());
        requestStub.put("url", request.getRequestURI());
        // Add headers and body if needed
        return requestStub;
    }

    private Map<String, Object> buildResponseStub(ResponseEntity<String> response) {
        Map<String, Object> responseStub = new HashMap<>();
        responseStub.put("status", response.getStatusCode().value());
        responseStub.put("body", response.getBody());
        // Add headers if needed
        return responseStub;
    }
}
