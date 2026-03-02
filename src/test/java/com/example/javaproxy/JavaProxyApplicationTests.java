package com.example.javaproxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JavaProxyApplicationTests {

    @Autowired
    private TestService testService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        // Create a mock stub file
        Path wiremockDir = Paths.get("wiremock");
        Files.createDirectories(wiremockDir);
        String filename = "getGreeting_" + Math.abs(java.util.Arrays.deepHashCode(new Object[]{"CachedWorld"})) + ".json";
        Path stubPath = wiremockDir.resolve(filename);
        String content = "{\"request\":{\"methodArguments\":[\"CachedWorld\"]},\"response\":{\"returnValue\":\"Hello, CachedWorld!\"}}";
        Files.write(stubPath, content.getBytes());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up the generated stub file
        Path wiremockDir = Paths.get("wiremock");
        if (Files.exists(wiremockDir)) {
            Files.walk(wiremockDir)
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.deleteIfExists(wiremockDir);
        }
    }

    @Test
    void whenMethodIsCalled_thenStubIsCreatedAndCached() throws IOException {
        // 1. Call the method with arguments that match the cached stub
        String result1 = testService.getGreeting("CachedWorld");
        assertThat(result1).isEqualTo("Hello, CachedWorld!");

        // 2. Call the method for the first time with new arguments
        String result2 = testService.getGreeting("World");
        assertThat(result2).isEqualTo("Hello, World!");

        // 3. Verify the new stub file was created
        String expectedFilename = "getGreeting_" + Math.abs(java.util.Arrays.deepHashCode(new Object[]{"World"})) + ".json";
        Path stubPath = Paths.get("wiremock", expectedFilename);
        assertThat(stubPath).exists();

        // 4. Verify the content of the new stub file
        JsonNode stubContent = objectMapper.readTree(stubPath.toFile());
        assertThat(stubContent.get("request").get("methodArguments").get(0).asText()).isEqualTo("World");
        assertThat(stubContent.get("response").get("returnValue").asText()).isEqualTo("Hello, World!");

        // 5. Call the method again with the same arguments
        long lastModified = stubPath.toFile().lastModified();
        String result3 = testService.getGreeting("World");
        assertThat(result3).isEqualTo("Hello, World!");

        // 6. Verify that the file was not modified (i.e., it was cached)
        assertThat(stubPath.toFile().lastModified()).isEqualTo(lastModified);
    }

    @Test
    void whenReactiveMethodIsCalled_thenStubIsCreatedAndCached() {
        // 1. Call the reactive method for the first time
        StepVerifier.create(testService.getGreetingReactive("ReactiveWorld"))
                .expectNext("Hello, ReactiveWorld!")
                .verifyComplete();

        // 2. Verify the stub file was created
        String expectedFilename = "getGreetingReactive_" + Math.abs(java.util.Arrays.deepHashCode(new Object[]{"ReactiveWorld"})) + ".json";
        Path stubPath = Paths.get("wiremock", expectedFilename);
        assertThat(stubPath).exists();

        // 3. Call the method again to ensure it's served from cache
        StepVerifier.create(testService.getGreetingReactive("ReactiveWorld"))
                .expectNext("Hello, ReactiveWorld!")
                .verifyComplete();
    }

    @Test
    void whenAnotherMethodIsCalled_thenStubIsCreatedAndCached() throws IOException {
        // 1. Call the new method for the first time
        Integer result1 = testService.add(5, 10);
        assertThat(result1).isEqualTo(15);

        // 2. Verify the stub file was created
        String expectedFilename = "add_" + Math.abs(java.util.Arrays.deepHashCode(new Object[]{5, 10})) + ".json";
        Path stubPath = Paths.get("wiremock", expectedFilename);
        assertThat(stubPath).exists();

        // 3. Verify the content of the stub file
        JsonNode stubContent = objectMapper.readTree(stubPath.toFile());
        assertThat(stubContent.get("request").get("methodArguments").get(0).asInt()).isEqualTo(5);
        assertThat(stubContent.get("request").get("methodArguments").get(1).asInt()).isEqualTo(10);
        assertThat(stubContent.get("response").get("returnValue").asInt()).isEqualTo(15);

        // 4. Call the method again with the same arguments
        long lastModified = stubPath.toFile().lastModified();
        Integer result2 = testService.add(5, 10);
        assertThat(result2).isEqualTo(15);

        // 5. Verify that the file was not modified (i.e., it was cached)
        assertThat(stubPath.toFile().lastModified()).isEqualTo(lastModified);
    }
}
