package com.example.javaproxy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JavaProxyApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance().build();

    @DynamicPropertySource
    static void overrideWebClientBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("proxy.target-url", wireMockServer::baseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
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
    void whenRequestIsProxied_thenWiremockStubIsCreated() throws Exception {
        // 1. Stub the target service response
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/test-endpoint"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Hello from Wiremock\"}")
                )
        );

        // 2. Make a request to our proxy
        mockMvc.perform(get("/test-endpoint"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"Hello from Wiremock\"}"));

        // 3. Verify the stub file was created
        String expectedFilename = "GET_" + Math.abs("/test-endpoint".hashCode()) + ".json";
        Path stubPath = Paths.get("wiremock", expectedFilename);

        assertThat(stubPath).exists();

        // 4. Verify the content of the stub file
        String stubContent = new String(Files.readAllBytes(stubPath));
        assertThat(stubContent).contains("\"method\" : \"GET\"");
        assertThat(stubContent).contains("\"url\" : \"/test-endpoint\"");
        assertThat(stubContent).contains("\"status\" : 200");
        assertThat(stubContent).contains("\"body\" : \"{\\\"message\\\":\\\"Hello from Wiremock\\\"}\"");
    }
}
