
package com.example.javaproxy.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class WiremockAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String wiremockDir = "wiremock";
    private final Map<Integer, Object> cache = new ConcurrentHashMap<>();

    public WiremockAspect() {
        try {
            Files.createDirectories(Paths.get(wiremockDir));
            loadStubs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadStubs() throws IOException {
        File[] files = new File(wiremockDir).listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                JsonNode rootNode = objectMapper.readTree(file);
                JsonNode requestNode = rootNode.get("request");
                JsonNode responseNode = rootNode.get("response");

                Object[] args = objectMapper.treeToValue(requestNode.get("methodArguments"), Object[].class);
                int argsHashCode = Arrays.deepHashCode(args);

                // This is a simplification. For a real application, you'd need a more robust way to deserialize the return value.
                Object returnValue = objectMapper.treeToValue(responseNode.get("returnValue"), Object.class);
                cache.put(argsHashCode, returnValue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Around("@annotation(com.example.javaproxy.aspect.RecordWiremock)")
    public Object generateWiremockStub(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        int argsHashCode = Arrays.deepHashCode(args);

        if (cache.containsKey(argsHashCode)) {
            return cache.get(argsHashCode);
        }

        Object result = joinPoint.proceed();

        cache.put(argsHashCode, result);

        Map<String, Object> stub = new HashMap<>();
        stub.put("request", buildRequestStub(args));
        stub.put("response", buildResponseStub(result));

        String filename = signature.getName() + "_" + Math.abs(argsHashCode) + ".json";
        File file = new File(wiremockDir, filename);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, stub);

        return result;
    }

    private Map<String, Object> buildRequestStub(Object[] args) {
        Map<String, Object> requestStub = new HashMap<>();
        requestStub.put("methodArguments", args);
        return requestStub;
    }

    private Map<String, Object> buildResponseStub(Object result) {
        Map<String, Object> responseStub = new HashMap<>();
        responseStub.put("returnValue", result);
        return responseStub;
    }
}
